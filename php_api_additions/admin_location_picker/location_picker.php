<?php
/**
 * admin/includes/location_picker.php
 *
 * Drop-in "pick on map" widget for the admin add/edit grave forms.
 * Include this file wherever the form currently has its LOCATION field,
 * inside the existing <form> tag. It renders:
 *   - a Leaflet map (OpenStreetMap tiles, no API key)
 *   - an address search box (OSM Nominatim geocoding)
 *   - a "Use my current location" button (handy when standing at the
 *     actual grave with a phone or laptop)
 *   - two hidden inputs, name="LAT" and name="LNG", that get submitted
 *     along with the rest of the form
 *
 * USAGE
 * -----
 * In admin/person/add.php and admin/person/edit.php, inside the <form>,
 * near the existing LOCATION field:
 *
 *   <?php
 *   // When editing, pass the record's existing coordinates (if any) so
 *   // the marker starts in the right place instead of a default center.
 *   $picker_lat = isset($res->LAT) ? $res->LAT : null;
 *   $picker_lng = isset($res->LNG) ? $res->LNG : null;
 *   include __DIR__ . '/../includes/location_picker.php';
 *   ?>
 *
 * On the server side (admin/person/controller.php), wherever GRAVENO,
 * NAME, etc. are read from $_POST and inserted/updated, add:
 *
 *   $lat = isset($_POST['LAT']) && $_POST['LAT'] !== '' ? (float)$_POST['LAT'] : null;
 *   $lng = isset($_POST['LNG']) && $_POST['LNG'] !== '' ? (float)$_POST['LNG'] : null;
 *
 * and include LAT/LNG in the INSERT/UPDATE column list, binding $lat/$lng
 * as parameters (or NULL literals when unset) rather than string-concatenating
 * them into the SQL - same caution applies here as to every other field in
 * that form, given the project's existing string-concatenated queries are
 * a known SQL-injection / XSS risk.
 *
 * DEFAULT MAP CENTER
 * -------------------
 * Change $default_center_lat / $default_center_lng below to your cemetery's
 * actual location, so the map opens centered on the right place instead of
 * (0, 0) when adding a brand-new record with no coordinates yet.
 */

$default_center_lat = isset($default_center_lat) ? $default_center_lat : 0.0;
$default_center_lng = isset($default_center_lng) ? $default_center_lng : 0.0;
$picker_lat = isset($picker_lat) ? $picker_lat : null;
$picker_lng = isset($picker_lng) ? $picker_lng : null;

$has_existing_point = ($picker_lat !== null && $picker_lng !== null);
$start_lat = $has_existing_point ? $picker_lat : $default_center_lat;
$start_lng = $has_existing_point ? $picker_lng : $default_center_lng;
$start_zoom = $has_existing_point ? 19 : 17;
?>
<link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/leaflet.min.css" />
<style>
  .location-picker-wrap { margin: 12px 0 20px; max-width: 640px; }
  .location-picker-wrap label { font-weight: bold; display: block; margin-bottom: 4px; }
  #location-picker-map { width: 100%; height: 320px; border: 1px solid #ccc; border-radius: 4px; }
  .location-picker-controls { display: flex; gap: 8px; margin-bottom: 8px; flex-wrap: wrap; }
  .location-picker-controls input[type="text"] { flex: 1; min-width: 180px; padding: 6px 8px; }
  .location-picker-controls button { padding: 6px 12px; cursor: pointer; }
  .location-picker-coords { margin-top: 6px; font-size: 13px; color: #444; }
  .location-picker-hint { font-size: 12px; color: #777; margin-top: 4px; }
</style>

<div class="location-picker-wrap">
  <label>Grave GPS Location</label>
  <div class="location-picker-controls">
    <input type="text" id="location-picker-search" placeholder="Search an address to jump the map there…" />
    <button type="button" id="location-picker-search-btn">Search</button>
    <button type="button" id="location-picker-mylocation-btn">Use My Current Location</button>
  </div>
  <div id="location-picker-map"></div>
  <div class="location-picker-coords" id="location-picker-coords">
    <?php echo $has_existing_point
        ? "Selected: {$start_lat}, {$start_lng}"
        : "No location picked yet — click the map to place a pin."; ?>
  </div>
  <div class="location-picker-hint">Click anywhere on the map to place or move the pin for this grave.</div>

  <input type="hidden" name="LAT" id="location-picker-lat" value="<?php echo $has_existing_point ? htmlspecialchars($start_lat) : ''; ?>" />
  <input type="hidden" name="LNG" id="location-picker-lng" value="<?php echo $has_existing_point ? htmlspecialchars($start_lng) : ''; ?>" />
</div>

<script src="https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/leaflet.min.js"></script>
<script>
(function () {
  var startLat = <?php echo json_encode((float)$start_lat); ?>;
  var startLng = <?php echo json_encode((float)$start_lng); ?>;
  var startZoom = <?php echo json_encode((int)$start_zoom); ?>;
  var hasExisting = <?php echo $has_existing_point ? 'true' : 'false'; ?>;

  var map = L.map('location-picker-map').setView([startLat, startLng], startZoom);

  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    maxZoom: 20,
    attribution: '&copy; OpenStreetMap contributors'
  }).addTo(map);

  var marker = null;
  var latInput = document.getElementById('location-picker-lat');
  var lngInput = document.getElementById('location-picker-lng');
  var coordsLabel = document.getElementById('location-picker-coords');

  function setMarker(lat, lng) {
    if (marker) {
      marker.setLatLng([lat, lng]);
    } else {
      marker = L.marker([lat, lng], { draggable: true }).addTo(map);
      marker.on('dragend', function () {
        var pos = marker.getLatLng();
        updateCoords(pos.lat, pos.lng);
      });
    }
    updateCoords(lat, lng);
  }

  function updateCoords(lat, lng) {
    var latFixed = lat.toFixed(7);
    var lngFixed = lng.toFixed(7);
    latInput.value = latFixed;
    lngInput.value = lngFixed;
    coordsLabel.textContent = 'Selected: ' + latFixed + ', ' + lngFixed;
  }

  if (hasExisting) {
    setMarker(startLat, startLng);
  }

  map.on('click', function (e) {
    setMarker(e.latlng.lat, e.latlng.lng);
  });

  // "Use My Current Location" - handy when the admin is physically
  // standing at the grave with a phone or laptop.
  document.getElementById('location-picker-mylocation-btn').addEventListener('click', function () {
    if (!navigator.geolocation) {
      alert('Geolocation is not supported by this browser.');
      return;
    }
    navigator.geolocation.getCurrentPosition(
      function (pos) {
        var lat = pos.coords.latitude;
        var lng = pos.coords.longitude;
        map.setView([lat, lng], 20);
        setMarker(lat, lng);
      },
      function (err) {
        alert('Could not get your location: ' + err.message);
      },
      { enableHighAccuracy: true, timeout: 10000 }
    );
  });

  // Address search via OSM Nominatim (free, no key - low-volume admin
  // tool use is within their usage policy; don't wire this up to
  // high-frequency/bulk lookups).
  function runSearch() {
    var query = document.getElementById('location-picker-search').value.trim();
    if (!query) return;
    fetch('https://nominatim.openstreetmap.org/search?format=json&limit=1&q=' + encodeURIComponent(query))
      .then(function (r) { return r.json(); })
      .then(function (results) {
        if (!results || results.length === 0) {
          alert('No results found for that address.');
          return;
        }
        var lat = parseFloat(results[0].lat);
        var lng = parseFloat(results[0].lon);
        map.setView([lat, lng], 18);
        // Note: this only jumps the map view - it does NOT place the pin,
        // since a street address isn't the same as the exact grave spot.
        // Click the map to actually set the pin once you've navigated there.
      })
      .catch(function () {
        alert('Address search failed - try again or just click the map directly.');
      });
  }

  document.getElementById('location-picker-search-btn').addEventListener('click', runSearch);
  document.getElementById('location-picker-search').addEventListener('keydown', function (e) {
    if (e.key === 'Enter') {
      e.preventDefault();
      runSearch();
    }
  });
})();
</script>
