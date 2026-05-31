(function () {
  if (!window.Auth || !window.Auth.requireAuth()) return;

  // Decode JWT and redirect non-admins before rendering anything
  (function checkAdminRole() {
    var token = window.Auth.getToken();
    if (!token) { window.location.href = 'discover.html'; return; }
    try {
      var payload = JSON.parse(atob(token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')));
      if (payload.role !== 'ADMIN') window.location.href = 'discover.html';
    } catch (e) {
      window.location.href = 'discover.html';
    }
  })();

  var state = {
    stations: [],
    connectors: [],
    bookings: [],
    editingBookingId: null
  };

  function el(id) { return document.getElementById(id); }

  function setMessage(text, isError) {
    var msg = el("admin-message");
    if (!msg) return;
    msg.textContent = text || "";
    msg.className = "bookings-message" + (text ? (isError ? " error" : " success") : "");
  }

  async function parseResponse(response, fallback) {
    var raw = await response.text();
    var data = null;
    try { data = raw ? JSON.parse(raw) : null; } catch (e) { data = null; }
    if (!response.ok) {
      var msg = (data && (data.message || data.error)) || fallback + " (" + response.status + ")";
      throw new Error(msg);
    }
    return data;
  }

  function fmt(dt) {
    if (!dt) return "-";
    return new Intl.DateTimeFormat("en-US", {
      month: "short",
      day: "2-digit",
      year: "numeric",
      hour: "2-digit",
      minute: "2-digit",
      hour12: false,
      timeZone: "UTC"
    }).format(new Date(dt));
  }

  function canModify(booking) {
    return booking.status === 'ACTIVE' && new Date(booking.startTime) > Date.now();
  }

  async function loadBookings() {
    var res = await window.Auth.authFetch("/api/admin/bookings");
    var bookings = await parseResponse(res, "Could not load bookings");
    state.bookings = Array.isArray(bookings) ? bookings : [];
    var box = el("admin-bookings");
    if (!state.bookings.length) {
      box.innerHTML = "<article class=\"booking-card\"><p>No bookings found.</p></article>";
      return;
    }
    box.innerHTML = state.bookings.map(function (b) {
      var actions = canModify(b)
        ? "<div class=\"booking-actions\">"
          + "<button class=\"modify-btn\" data-action=\"modify\" data-id=\"" + b.id + "\">Modify</button>"
          + "<button class=\"cancel-btn\" data-action=\"cancel\" data-id=\"" + b.id + "\">Cancel</button>"
          + "</div>"
        : "";
      return "<article class=\"booking-card\" data-id=\"" + b.id + "\">"
        + "<div class=\"booking-top\"><h3>Booking #" + b.id + "</h3><span class=\"badge neutral\">" + (b.status || "UNKNOWN") + "</span></div>"
        + "<div class=\"booking-meta-grid\">"
        + "<div><strong>User</strong><span>#" + b.userId + "</span></div>"
        + "<div><strong>Connector</strong><span>#" + b.connectorId + "</span></div>"
        + "<div><strong>Start</strong><span>" + fmt(b.startTime) + "</span></div>"
        + "<div><strong>End</strong><span>" + fmt(b.endTime) + "</span></div>"
        + "</div>"
        + actions
        + "</article>";
    }).join("");
  }

  async function cancelBookingAdmin(id) {
    if (!confirm("Cancel booking #" + id + "? This cannot be undone.")) return;
    try {
      var res = await window.Auth.authFetch("/bookings/" + id, { method: "DELETE" });
      await parseResponse(res, "Could not cancel booking");
      setMessage("Booking #" + id + " cancelled.", false);
      await loadBookings();
    } catch (err) {
      setMessage(err.message || "Cancel failed.", true);
    }
  }

  function toLocalDateInput(date) {
    var y = date.getUTCFullYear();
    var m = String(date.getUTCMonth() + 1).padStart(2, "0");
    var d = String(date.getUTCDate()).padStart(2, "0");
    return y + "-" + m + "-" + d;
  }

  function toLocalTimeInput(date) {
    return String(date.getUTCHours()).padStart(2, "0") + ":" + String(date.getUTCMinutes()).padStart(2, "0");
  }

  function combineToIso(date, time) {
    return date + "T" + time + ":00Z";
  }

  function openAdminModifyDialog(id) {
    var booking = state.bookings.find(function (b) { return b.id === id; });
    if (!booking) return;
    state.editingBookingId = id;

    el("admin-modify-booking-id").textContent = "Booking #" + id + " (User #" + booking.userId + ")";
    var start = new Date(booking.startTime);
    var end = new Date(booking.endTime);
    el("admin-modify-date").value = toLocalDateInput(start);
    el("admin-modify-start").value = toLocalTimeInput(start);
    el("admin-modify-end").value = toLocalTimeInput(end);
    el("admin-modify-message").textContent = "";
    el("admin-modify-message").className = "booking-message";

    el("admin-modify-dialog").showModal();
  }

  async function submitAdminModify(evt) {
    evt.preventDefault();
    if (!state.editingBookingId) return;

    var date = el("admin-modify-date").value;
    var start = el("admin-modify-start").value;
    var end = el("admin-modify-end").value;
    var msgEl = el("admin-modify-message");

    if (!date || !start || !end) {
      msgEl.textContent = "Please fill in date and times.";
      msgEl.className = "booking-message error";
      return;
    }

    try {
      var res = await window.Auth.authFetch("/bookings/" + state.editingBookingId, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          startTime: combineToIso(date, start),
          endTime: combineToIso(date, end)
        })
      });
      var raw = await res.text();
      var data = null;
      try { data = raw ? JSON.parse(raw) : null; } catch (e) { data = null; }
      if (!res.ok) {
        throw new Error((data && (data.message || data.error)) || "Modify failed (" + res.status + ")");
      }
      msgEl.textContent = "Booking updated.";
      msgEl.className = "booking-message success";
      await loadBookings();
      setTimeout(function () { el("admin-modify-dialog").close(); }, 400);
    } catch (err) {
      msgEl.textContent = err.message || "Modify failed.";
      msgEl.className = "booking-message error";
    }
  }

  function bindBookingActions() {
    el("admin-bookings").addEventListener("click", function (e) {
      var btn = e.target.closest("button[data-action]");
      if (!btn) return;
      var id = Number(btn.getAttribute("data-id"));
      var action = btn.getAttribute("data-action");
      if (action === "cancel") cancelBookingAdmin(id).catch(function (err) { setMessage(err.message, true); });
      if (action === "modify") openAdminModifyDialog(id);
    });
  }

  function initModifyDialog() {
    var dlg = el("admin-modify-dialog");
    var form = el("admin-modify-form");
    var closeBtn = el("admin-modify-cancel");
    if (!dlg || !form || !closeBtn) return;
    closeBtn.addEventListener("click", function () { dlg.close(); });
    form.addEventListener("submit", submitAdminModify);
  }

  function renderStationSelect() {
    var select = el("connector-station");
    select.innerHTML = state.stations.map(function (s) {
      return "<option value=\"" + s.id + "\">#" + s.id + " - " + s.name + "</option>";
    }).join("");
  }

  function renderStations() {
    var box = el("admin-stations");
    if (!state.stations.length) {
      box.innerHTML = "<p>No stations.</p>";
      return;
    }
    box.innerHTML = "<table><thead><tr><th>ID</th><th>Name</th><th>Address</th><th>Lat</th><th>Lng</th><th>Actions</th></tr></thead><tbody>"
      + state.stations.map(function (s) {
        return "<tr>"
          + "<td>" + s.id + "</td>"
          + "<td>" + s.name + "</td>"
          + "<td>" + (s.address || "") + "</td>"
          + "<td>" + s.latitude + "</td>"
          + "<td>" + s.longitude + "</td>"
          + "<td><button data-act=\"edit-station\" data-id=\"" + s.id + "\">Edit</button> <button data-act=\"del-station\" data-id=\"" + s.id + "\">Delete</button></td>"
          + "</tr>";
      }).join("")
      + "</tbody></table>";
    renderStationSelect();
  }

  function renderConnectors() {
    var box = el("admin-connectors");
    if (!state.connectors.length) {
      box.innerHTML = "<p>No connectors.</p>";
      return;
    }
    box.innerHTML = "<table><thead><tr><th>ID</th><th>Station</th><th>Type</th><th>kW</th><th>Actions</th></tr></thead><tbody>"
      + state.connectors.map(function (c) {
        return "<tr>"
          + "<td>" + c.id + "</td>"
          + "<td>#" + c.stationId + " " + (c.stationName || "") + "</td>"
          + "<td>" + c.connectorType + "</td>"
          + "<td>" + c.maxKw + "</td>"
          + "<td><button data-act=\"edit-connector\" data-id=\"" + c.id + "\">Edit</button> <button data-act=\"del-connector\" data-id=\"" + c.id + "\">Delete</button></td>"
          + "</tr>";
      }).join("")
      + "</tbody></table>";
  }

  async function loadStations() {
    var res = await window.Auth.authFetch("/api/admin/stations");
    state.stations = await parseResponse(res, "Could not load stations") || [];
    renderStations();
  }

  async function loadConnectors() {
    var res = await window.Auth.authFetch("/api/admin/connectors");
    state.connectors = await parseResponse(res, "Could not load connectors") || [];
    renderConnectors();
  }

  function clearStationForm() {
    el("station-id").value = "";
    el("station-name").value = "";
    el("station-address").value = "";
    el("station-lat").value = "";
    el("station-lng").value = "";
  }

  function clearConnectorForm() {
    el("connector-id").value = "";
    el("connector-kw").value = "";
  }

  async function saveStation(evt) {
    evt.preventDefault();
    var id = el("station-id").value;
    var payload = {
      name: el("station-name").value.trim(),
      address: el("station-address").value.trim(),
      latitude: Number(el("station-lat").value),
      longitude: Number(el("station-lng").value)
    };
    var url = id ? "/api/admin/stations/" + id : "/api/admin/stations";
    var method = id ? "PUT" : "POST";
    var res = await window.Auth.authFetch(url, {
      method: method,
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload)
    });
    await parseResponse(res, "Could not save station");
    clearStationForm();
    await loadStations();
    setMessage("Station saved.", false);
  }

  async function saveConnector(evt) {
    evt.preventDefault();
    var id = el("connector-id").value;
    var payload = {
      stationId: Number(el("connector-station").value),
      connectorType: el("connector-type").value,
      maxKw: Number(el("connector-kw").value)
    };
    var url = id ? "/api/admin/connectors/" + id : "/api/admin/connectors";
    var method = id ? "PUT" : "POST";
    var res = await window.Auth.authFetch(url, {
      method: method,
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload)
    });
    await parseResponse(res, "Could not save connector");
    clearConnectorForm();
    await loadConnectors();
    setMessage("Connector saved.", false);
  }

  async function deleteStation(id) {
    if (!confirm("Delete station #" + id + "?")) return;
    var res = await window.Auth.authFetch("/api/admin/stations/" + id, { method: "DELETE" });
    if (!res.ok) throw new Error("Could not delete station");
    await loadStations();
    await loadConnectors();
    setMessage("Station deleted.", false);
  }

  async function deleteConnector(id) {
    if (!confirm("Delete connector #" + id + "?")) return;
    var res = await window.Auth.authFetch("/api/admin/connectors/" + id, { method: "DELETE" });
    if (!res.ok) throw new Error("Could not delete connector");
    await loadConnectors();
    setMessage("Connector deleted.", false);
  }

  function bindTableActions() {
    el("admin-stations").addEventListener("click", function (e) {
      var btn = e.target.closest("button[data-act]");
      if (!btn) return;
      var id = Number(btn.getAttribute("data-id"));
      var act = btn.getAttribute("data-act");
      var station = state.stations.find(function (s) { return s.id === id; });
      if (act === "edit-station" && station) {
        el("station-id").value = station.id;
        el("station-name").value = station.name || "";
        el("station-address").value = station.address || "";
        el("station-lat").value = station.latitude;
        el("station-lng").value = station.longitude;
      }
      if (act === "del-station") deleteStation(id).catch(function (err) { setMessage(err.message, true); });
    });

    el("admin-connectors").addEventListener("click", function (e) {
      var btn = e.target.closest("button[data-act]");
      if (!btn) return;
      var id = Number(btn.getAttribute("data-id"));
      var act = btn.getAttribute("data-act");
      var connector = state.connectors.find(function (c) { return c.id === id; });
      if (act === "edit-connector" && connector) {
        el("connector-id").value = connector.id;
        el("connector-station").value = connector.stationId;
        el("connector-type").value = connector.connectorType;
        el("connector-kw").value = connector.maxKw;
      }
      if (act === "del-connector") deleteConnector(id).catch(function (err) { setMessage(err.message, true); });
    });
  }

  async function init() {
    try {
      bindBookingActions();
      initModifyDialog();
      bindTableActions();
      el("station-form").addEventListener("submit", function (e) { saveStation(e).catch(function (err) { setMessage(err.message, true); }); });
      el("connector-form").addEventListener("submit", function (e) { saveConnector(e).catch(function (err) { setMessage(err.message, true); }); });
      el("station-clear").addEventListener("click", clearStationForm);
      el("connector-clear").addEventListener("click", clearConnectorForm);
      el("refresh-bookings").addEventListener("click", function () { loadBookings().catch(function (err) { setMessage(err.message, true); }); });

      await loadBookings();
      await loadStations();
      await loadConnectors();
    } catch (err) {
      setMessage(err.message || "Admin page failed to load.", true);
    }
  }

  document.addEventListener("DOMContentLoaded", init);
})();
