(function () {
  var state = {
    bookings: [],
    connectorsById: {},
    editingId: null
  };

  function byId(id) {
    return document.getElementById(id);
  }

  function toDate(dt) {
    return new Date(dt);
  }

  function formatDate(dt) {
    return new Intl.DateTimeFormat("en-US", {
      month: "short",
      day: "2-digit",
      year: "numeric"
    }).format(toDate(dt));
  }

  function formatTime(dt) {
    return new Intl.DateTimeFormat("en-US", {
      hour: "2-digit",
      minute: "2-digit",
      hour12: false
    }).format(toDate(dt));
  }

  function statusClass(status) {
    var s = (status || "").toLowerCase();
    if (s === "active" || s === "confirmed") return "success";
    if (s === "cancelled") return "danger";
    return "neutral";
  }

  function canEdit(booking) {
    return new Date(booking.startTime).getTime() > Date.now();
  }

  function combineToIso(date, time) {
    return date + "T" + time + ":00Z";
  }

  function setPageMessage(text, isError) {
    var el = byId("bookings-message");
    if (!el) return;
    el.textContent = text || "";
    el.className = "bookings-message" + (text ? (isError ? " error" : " success") : "");
  }

  function renderBookings() {
    var list = byId("bookings-list");
    if (!list) return;

    if (!state.bookings.length) {
      list.innerHTML = "<article class=\"booking-card\"><p>No bookings yet.</p></article>";
      return;
    }

    list.innerHTML = state.bookings.map(function (b) {
      var editable = canEdit(b);
      var start = toDate(b.startTime);
      var end = toDate(b.endTime);
      var date = formatDate(b.startTime);
      var timeRange = formatTime(b.startTime) + " - " + formatTime(b.endTime);
      var connectorInfo = state.connectorsById[b.connectorId];
      var connector = connectorInfo
        ? (connectorInfo.stationName + " - " + connectorInfo.connectorType)
        : ("Connector #" + b.connectorId);
      var status = (b.status || "UNKNOWN").toUpperCase();

      return ""
        + "<article class=\"booking-card\" data-id=\"" + b.id + "\">"
        + "  <div class=\"booking-top\">"
        + "    <h3>Booking #" + b.id + "</h3>"
        + "    <span class=\"badge " + statusClass(b.status) + "\">" + status + "</span>"
        + "  </div>"
        + "  <div class=\"booking-meta-grid\">"
        + "    <div><strong>Date</strong><span>" + date + "</span></div>"
        + "    <div><strong>Time</strong><span>" + timeRange + "</span></div>"
        + "    <div><strong>Connector</strong><span>" + connector + "</span></div>"
        + "    <div><strong>Created</strong><span>" + formatDate(b.createdAt || b.startTime) + "</span></div>"
        + "  </div>"
        + (editable
          ? "<div class=\"booking-actions\">"
          + "  <button class=\"modify-btn\" data-action=\"modify\" data-id=\"" + b.id + "\">Modify</button>"
          + "  <button class=\"cancel-btn\" data-action=\"cancel\" data-id=\"" + b.id + "\">Cancel</button>"
          + "</div>"
          : "")
        + "</article>";
    }).join("");
  }

  async function fetchBookings() {
    setPageMessage("", false);
    try {
      var response = await window.Auth.authFetch("/bookings/my");
      if (!response.ok) {
        throw new Error("Could not load bookings (" + response.status + ")");
      }
      state.bookings = await response.json();
      renderBookings();
    } catch (err) {
      state.bookings = [];
      renderBookings();
      setPageMessage(err.message || "Could not load bookings.", true);
    }
  }

  async function fetchConnectorMap() {
    try {
      var response = await window.Auth.authFetch("/api/connectors");
      if (!response.ok) return;
      var connectors = await response.json();
      state.connectorsById = {};
      connectors.forEach(function (c) {
        state.connectorsById[c.id] = c;
      });
    } catch (err) {
      state.connectorsById = {};
    }
  }

  function openModifyDialog(bookingId) {
    var booking = state.bookings.find(function (b) { return b.id === bookingId; });
    if (!booking) return;

    state.editingId = bookingId;
    byId("modify-booking-id").textContent = "Booking #" + bookingId;

    var start = toDate(booking.startTime);
    var end = toDate(booking.endTime);

    byId("modify-date").value = start.toISOString().slice(0, 10);
    byId("modify-start").value = start.toISOString().slice(11, 16);
    byId("modify-end").value = end.toISOString().slice(11, 16);
    byId("modify-message").textContent = "";
    byId("modify-message").className = "booking-message";

    byId("modify-dialog").showModal();
  }

  async function submitModify(evt) {
    evt.preventDefault();
    if (!state.editingId) return;

    var date = byId("modify-date").value;
    var start = byId("modify-start").value;
    var end = byId("modify-end").value;
    var message = byId("modify-message");

    if (!date || !start || !end) {
      message.textContent = "Please fill date and times.";
      message.className = "booking-message error";
      return;
    }

    try {
      var response = await window.Auth.authFetch("/bookings/" + state.editingId, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          startTime: combineToIso(date, start),
          endTime: combineToIso(date, end)
        })
      });

      var raw = await response.text();
      var data = null;
      try { data = raw ? JSON.parse(raw) : null; } catch (e) { data = null; }

      if (!response.ok) {
        var errMsg = (data && (data.message || data.error)) || ("Modify failed (" + response.status + ")");
        throw new Error(errMsg);
      }

      message.textContent = "Booking updated.";
      message.className = "booking-message success";
      await fetchBookings();
      setTimeout(function () {
        byId("modify-dialog").close();
      }, 300);
    } catch (err) {
      message.textContent = err.message || "Modify failed.";
      message.className = "booking-message error";
    }
  }

  async function cancelBooking(bookingId) {
    if (!window.confirm("Cancel this booking?")) return;

    try {
      var response = await window.Auth.authFetch("/bookings/" + bookingId, {
        method: "DELETE"
      });

      var raw = await response.text();
      var data = null;
      try { data = raw ? JSON.parse(raw) : null; } catch (e) { data = null; }

      if (!response.ok) {
        var errMsg = (data && (data.message || data.error)) || ("Cancel failed (" + response.status + ")");
        throw new Error(errMsg);
      }

      setPageMessage("Booking cancelled.", false);
      await fetchBookings();
    } catch (err) {
      setPageMessage(err.message || "Cancel failed.", true);
    }
  }

  function bindListActions() {
    var list = byId("bookings-list");
    if (!list) return;

    list.addEventListener("click", function (evt) {
      var target = evt.target;
      var action = target.getAttribute("data-action");
      var idRaw = target.getAttribute("data-id");
      if (!action || !idRaw) return;
      var id = Number(idRaw);

      if (action === "modify") {
        openModifyDialog(id);
      } else if (action === "cancel") {
        cancelBooking(id);
      }
    });
  }

  function initDialog() {
    var dlg = byId("modify-dialog");
    var closeBtn = byId("modify-cancel");
    var form = byId("modify-form");

    if (!dlg || !closeBtn || !form) return;

    closeBtn.addEventListener("click", function () {
      dlg.close();
    });

    form.addEventListener("submit", submitModify);
  }

  async function init() {
    if (!window.Auth || !window.Auth.requireAuth()) {
      return;
    }
    bindListActions();
    initDialog();
    await fetchConnectorMap();
    await fetchBookings();
  }

  document.addEventListener("DOMContentLoaded", init);
})();
