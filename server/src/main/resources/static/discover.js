(function () {
  var center = [37.7749, -122.4194];

  var map = L.map("discover-map", {
    zoomControl: false,
    preferCanvas: true
  }).setView(center, 13);

  L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
    maxZoom: 19,
    attribution: "&copy; OpenStreetMap contributors"
  }).addTo(map);

  var markersLayer = L.layerGroup().addTo(map);
  var stationListEl = document.querySelector(".station-list");

  var connectorFilter = "all";
  var allStations = [];
  var visibleStations = [];

  var detailsModal = document.getElementById("station-details-modal");
  var detailsNameEl = document.getElementById("details-station-name");
  var detailsSubtitleEl = document.getElementById("details-station-subtitle");
  var detailsMetaEl = document.getElementById("details-station-meta");
  var detailsCloseBtn = document.getElementById("details-close");

  var connectorSelect = document.getElementById("connector-select");
  var connectorList = document.getElementById("connector-list");
  var slotGrid = document.getElementById("slot-grid");
  var selectedSlotLabel = document.getElementById("selected-slot-label");
  var reserveDate = document.getElementById("reserve-date");
  var bookingForm = document.getElementById("booking-form");
  var startTimeInput = document.getElementById("start-time");
  var endTimeInput = document.getElementById("end-time");
  var bookingMessage = document.getElementById("booking-message");

  var selectedSlot = null;
  var availableSlots = [];

  function escapeHtml(value) {
    if (value == null) return "";
    return String(value)
      .replaceAll("&", "&amp;")
      .replaceAll("<", "&lt;")
      .replaceAll(">", "&gt;")
      .replaceAll('"', "&quot;")
      .replaceAll("'", "&#39;");
  }

  function formatLocation(station) {
    var parts = [station.city, station.region, station.country].filter(Boolean);
    return parts.length > 0 ? parts.join(", ") : "Unknown location";
  }

  function stationCardHtml(station, index) {
    var name = escapeHtml(station.name || "Charging Station");
    var address = escapeHtml(station.address || "Address not available");
    var location = escapeHtml(formatLocation(station));
    var connector = escapeHtml(station.connector || "Unknown");
    var level = station.level ? "L" + station.level : "-";

    var availability = "Unknown";
    var badgeClass = "neutral";
    if (Number.isInteger(station.available) && Number.isInteger(station.total) && station.total > 0) {
      availability = station.available + "/" + station.total + " Available";
      badgeClass = station.available > 0 ? "success" : "danger";
    } else if (Number.isInteger(station.total) && station.total > 0) {
      availability = station.total + " Ports";
    }

    if (station.active === false) {
      availability = "Inactive";
      badgeClass = "danger";
    }

    return [
      '<article class="station-card">',
      '  <div class="station-head"><h3>' + name + '</h3><span>' + level + '</span></div>',
      '  <p>' + location + '</p>',
      '  <p>' + address + '</p>',
      '  <div class="station-meta"><span>&#128267; ' + connector + '</span></div>',
      '  <div class="station-actions"><span class="badge ' + badgeClass + '">' + availability + '</span><button class="details-btn" data-index="' + index + '" type="button">Details</button></div>',
      '</article>'
    ].join("");
  }

  function matchesConnector(station) {
    if (connectorFilter === "all") return true;

    var connector = (station.connector || "").toLowerCase();
    if (connectorFilter === "ccs") return connector.includes("ccs") || connector.includes("combo");
    if (connectorFilter === "type2") return connector.includes("type 2") || connector.includes("j1772");
    if (connectorFilter === "chademo") return connector.includes("chademo");

    return true;
  }

  function applyFilters(stations) {
    return stations.filter(matchesConnector);
  }

  function renderStations(stations) {
    markersLayer.clearLayers();
    visibleStations = stations;

    if (!Array.isArray(stations) || stations.length === 0) {
      stationListEl.innerHTML = '<article class="station-card"><p>No stations match your connector filter in this area.</p></article>';
      return;
    }

    stationListEl.innerHTML = stations.map(function (station, index) {
      return stationCardHtml(station, index);
    }).join("");

    stations.forEach(function (station) {
      if (typeof station.latitude !== "number" || typeof station.longitude !== "number") return;

      var popupName = escapeHtml(station.name || "Charging Station");
      var popupAddress = escapeHtml(station.address || "Address not available");
      var popupLocation = escapeHtml(formatLocation(station));
      var popupConnector = escapeHtml(station.connector || "Unknown");

      L.marker([station.latitude, station.longitude])
        .bindPopup("<strong>" + popupName + "</strong><br>" + popupLocation + "<br>" + popupAddress + "<br>" + popupConnector)
        .addTo(markersLayer);
    });
  }

  function renderFilteredStations() {
    renderStations(applyFilters(allStations));
  }

  function renderError(message) {
    markersLayer.clearLayers();
    stationListEl.innerHTML = '<article class="station-card"><p>' + escapeHtml(message) + '</p></article>';
  }

  async function loadStations() {
    try {
      var bounds = map.getBounds();
      var lat = map.getCenter().lat;
      var lng = map.getCenter().lng;
      var latSpan = Math.abs(bounds.getNorth() - bounds.getSouth());
      var radiusKm = Math.max(20, Math.min(100, Math.round(latSpan * 111 / 2)));

      var params = new URLSearchParams({
        latitude: String(lat),
        longitude: String(lng),
        radius: String(radiusKm)
      });

      var apiUrl = new URL("api/stations", window.location.href);
      apiUrl.search = params.toString();
      var response = await fetch(apiUrl.toString());
      if (!response.ok) throw new Error("Could not load stations (" + response.status + ")");

      allStations = await response.json();
      renderFilteredStations();
    } catch (error) {
      renderError(error.message || "Could not load charging stations right now.");
    }
  }

  async function searchPlace(query) {
    var trimmed = query.trim();
    if (!trimmed) return;

    try {
      var geocodeUrl = "https://nominatim.openstreetmap.org/search?format=json&limit=1&q=" + encodeURIComponent(trimmed);
      var response = await fetch(geocodeUrl, { headers: { "Accept": "application/json" } });
      if (!response.ok) throw new Error("Search failed");

      var results = await response.json();
      if (!Array.isArray(results) || results.length === 0) {
        renderError("No places found for '" + trimmed + "'.");
        return;
      }

      var lat = Number(results[0].lat);
      var lon = Number(results[0].lon);
      if (!Number.isFinite(lat) || !Number.isFinite(lon)) {
        renderError("Could not read search coordinates.");
        return;
      }

      map.setView([lat, lon], 13);
    } catch (error) {
      renderError("Could not search this place right now.");
    }
  }

  function wireFilterGroup(groupSelector, onSelect) {
    var chips = Array.from(document.querySelectorAll(groupSelector + " .chip"));
    chips.forEach(function (chip) {
      chip.addEventListener("click", function () {
        chips.forEach(function (c) { c.classList.remove("active"); });
        chip.classList.add("active");
        onSelect(chip.dataset.value || "");
        renderFilteredStations();
      });
    });
  }

  function formatSlotRange(start, end) {
    return start + " - " + end;
  }

  async function loadConnectors() {
    var response = await fetch("/api/connectors");
    if (!response.ok) throw new Error("Could not load connectors");
    return response.json();
  }

  async function loadSlots(connectorId, date) {
    var url = new URL("/api/connectors/" + connectorId + "/slots", window.location.origin);
    url.searchParams.set("date", date);
    var response = await fetch(url.toString());
    if (!response.ok) throw new Error("Could not load slots");
    return response.json();
  }

  function connectorsForStation(connectors, station) {
    if (!Array.isArray(connectors)) return [];
    var desired = Number.isInteger(station && station.total) && station.total > 0 ? station.total : null;
    if (!desired) return connectors;
    return connectors.slice(0, desired);
  }

  function renderConnectorList(connectors, station) {
    var stationConnectors = connectorsForStation(connectors, station);

    connectorList.innerHTML = stationConnectors.map(function (c, index) {
      var statusClass = index % 3 === 1 ? "busy" : "available";
      var statusText = statusClass === "busy" ? "IN USE" : "AVAILABLE";
      var maxKw = c.maxKw != null ? c.maxKw : "N/A";
      return [
        '<div class="connector-item">',
        '  <div class="connector-icon">&#128267;</div>',
        '  <div class="connector-main">',
        '    <span>ID: CONN-' + c.id + '</span>',
        '    <b>' + escapeHtml(c.connectorType) + ' (' + maxKw + 'kW)</b>',
        '  </div>',
        '  <span class="connector-status ' + statusClass + '">' + statusText + '</span>',
        '</div>'
      ].join("");
    }).join("");

    connectorSelect.innerHTML = stationConnectors.map(function (c) {
      return '<option value="' + c.id + '">#' + c.id + ' - ' + escapeHtml(c.connectorType) + ' (' + c.maxKw + 'kW)</option>';
    }).join("");
  }

  function renderSlots(slots) {
    availableSlots = slots;
    slotGrid.innerHTML = slots.map(function (slot) {
      var cls = slot.available ? "slot-btn" : "slot-btn disabled";
      var disabled = slot.available ? "" : " disabled";
      return '<button class="' + cls + '" data-start="' + slot.startTime + '" data-end="' + slot.endTime + '" type="button"' + disabled + '>' + slot.startTime + '</button>';
    }).join("");

    Array.from(slotGrid.querySelectorAll(".slot-btn")).forEach(function (btn) {
      btn.addEventListener("click", function () {
        if (btn.classList.contains("disabled")) return;
        Array.from(slotGrid.querySelectorAll(".slot-btn")).forEach(function (b) { b.classList.remove("active"); });
        btn.classList.add("active");
        startTimeInput.value = btn.dataset.start;
        endTimeInput.value = btn.dataset.end;
        selectedSlot = formatSlotRange(btn.dataset.start, btn.dataset.end);
        selectedSlotLabel.textContent = selectedSlot;
      });
    });
  }

  function setBookingMessage(text, type) {
    bookingMessage.textContent = text || "";
    bookingMessage.className = "booking-message" + (type ? " " + type : "");
  }

  async function refreshSlotsForSelection() {
    var connectorId = connectorSelect.value;
    var date = reserveDate.value;
    if (!connectorId || !date) return;

    try {
      var slots = await loadSlots(connectorId, date);
      renderSlots(slots);
      selectedSlot = null;
      selectedSlotLabel.textContent = "None";
    } catch (e) {
      setBookingMessage("Could not load slots.", "error");
    }
  }

  async function openDetails(station) {
    if (!detailsModal || !station) return;

    detailsNameEl.textContent = station.name || "Charging Station";
    detailsSubtitleEl.textContent = station.address || "";
    detailsMetaEl.textContent = formatLocation(station);
    setBookingMessage("", "");
    selectedSlot = null;
    selectedSlotLabel.textContent = "None";

    try {
      var connectors = await loadConnectors();
      renderConnectorList(connectors, station);
      await refreshSlotsForSelection();
    } catch (e) {
      setBookingMessage("Could not load connectors.", "error");
    }

    detailsModal.classList.add("open");
    detailsModal.setAttribute("aria-hidden", "false");
  }

  function closeDetails() {
    if (!detailsModal) return;
    detailsModal.classList.remove("open");
    detailsModal.setAttribute("aria-hidden", "true");
  }

  async function submitBooking(event) {
    event.preventDefault();
    setBookingMessage("", "");

    var connectorId = Number(connectorSelect.value);
    var date = reserveDate.value;
    var start = startTimeInput.value;
    var end = endTimeInput.value;

    if (!connectorId || !date || !start || !end) {
      setBookingMessage("Please select connector, date, start and end time.", "error");
      return;
    }

    var payload = {
      connectorId: connectorId,
      startTime: date + "T" + start + ":00Z",
      endTime: date + "T" + end + ":00Z"
    };

    try {
      var response = await fetch("/bookings", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload)
      });

      var raw = await response.text();
      var parsed = null;
      try { parsed = raw ? JSON.parse(raw) : null; } catch (e) { parsed = null; }

      if (!response.ok) {
        var message = (parsed && (parsed.message || parsed.error)) || (raw || "Booking failed.");
        throw new Error(message);
      }

      setBookingMessage("Booking created successfully.", "success");
      await refreshSlotsForSelection();
    } catch (error) {
      setBookingMessage(error.message || "Booking failed.", "error");
    }
  }

  wireFilterGroup('[data-filter-group="connector"]', function (value) {
    connectorFilter = value || "all";
  });

  map.on("moveend", loadStations);
  loadStations();

  var zoomInButton = document.getElementById("map-zoom-in");
  if (zoomInButton) zoomInButton.addEventListener("click", function () { map.zoomIn(); });

  var recenterButton = document.getElementById("map-recenter");
  if (recenterButton) recenterButton.addEventListener("click", function () { map.setView(center, 13); });

  var searchForm = document.getElementById("discover-search-form");
  var searchInput = document.getElementById("discover-search-input");
  if (searchForm && searchInput) {
    searchForm.addEventListener("submit", function (event) {
      event.preventDefault();
      searchPlace(searchInput.value);
    });
  }

  stationListEl.addEventListener("click", function (event) {
    var button = event.target.closest(".details-btn");
    if (!button) return;
    var index = Number(button.dataset.index);
    if (!Number.isFinite(index)) return;
    openDetails(visibleStations[index]);
  });

  if (detailsModal) {
    detailsModal.addEventListener("click", function (event) {
      var closeTrigger = event.target.closest('[data-close-modal="true"]');
      if (closeTrigger) closeDetails();
    });
  }

  if (detailsCloseBtn) detailsCloseBtn.addEventListener("click", closeDetails);

  if (connectorSelect) connectorSelect.addEventListener("change", refreshSlotsForSelection);
  if (reserveDate) reserveDate.addEventListener("change", refreshSlotsForSelection);
  if (bookingForm) bookingForm.addEventListener("submit", submitBooking);

  document.addEventListener("keydown", function (event) {
    if (event.key === "Escape") closeDetails();
  });

  if (reserveDate) {
    var today = new Date();
    reserveDate.value = today.toISOString().slice(0, 10);
  }
})();
