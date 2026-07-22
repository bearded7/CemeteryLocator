# Admin "Pick on Map" — Integration Guide

This adds a GPS location picker (OpenStreetMap/Leaflet, no API key) to the
admin panel's add/edit grave forms, so LAT/LNG can be set by clicking a map
instead of typing coordinates by hand.

I wasn't able to pull the exact current contents of `admin/person/add.php`,
`edit.php`, and `controller.php` from the repo (GitHub only exposed the
top-level file tree to me, not nested admin subfolders), so rather than
guess and hand you edits that might silently conflict with what's actually
there, this ships as a **drop-in include** plus exact wiring steps for you
to apply against your real files.

## 1. Run the schema migration (if you haven't already)

See `migration_add_gps_columns.sql` from the previous round — adds nullable
`LAT`/`LNG` columns to `tblpeople`. Skip if already applied.

## 2. Install the picker file

Copy `location_picker.php` into `admin/includes/location_picker.php`
(create the `includes/` folder if it doesn't exist).

## 3. Wire it into `admin/person/add.php`

Find where the form renders the `LOCATION` field (the text input for the
section/row name). Right after it, inside the same `<form>`, add:

```php
<?php
$default_center_lat = 14.5995;   // <-- set to your cemetery's actual latitude
$default_center_lng = 120.9842;  // <-- set to your cemetery's actual longitude
$picker_lat = null; // new record - no coordinates yet
$picker_lng = null;
include __DIR__ . '/../includes/location_picker.php';
?>
```

Replace the example `$default_center_lat`/`$default_center_lng` with real
coordinates for your cemetery, so the map opens centered on the right spot
instead of the middle of the ocean (0, 0).

## 4. Wire it into `admin/person/edit.php`

Same include, but pass the record's existing coordinates so the pin starts
in the right place:

```php
<?php
$default_center_lat = 14.5995;
$default_center_lng = 120.9842;
// $res here is whatever variable already holds the loaded record in this
// file (e.g. from $mydb->loadSingleResult()) - match its actual name.
$picker_lat = isset($res->LAT) && $res->LAT !== null ? (float)$res->LAT : null;
$picker_lng = isset($res->LNG) && $res->LNG !== null ? (float)$res->LNG : null;
include __DIR__ . '/../includes/location_picker.php';
?>
```

## 5. Handle LAT/LNG in `admin/person/controller.php`

Wherever the existing `add`/`edit` actions read `$_POST['GRAVENO']`,
`$_POST['NAME']` (or `FNAME` — the exploit-db writeup for this project
shows the edit form using `FNAME`, so check which your copy actually uses),
etc., add:

```php
$lat = (isset($_POST['LAT']) && $_POST['LAT'] !== '') ? (float)$_POST['LAT'] : null;
$lng = (isset($_POST['LNG']) && $_POST['LNG'] !== '') ? (float)$_POST['LNG'] : null;
```

Then include `LAT` and `LNG` in whatever INSERT/UPDATE statement writes the
record. **Use prepared statements / bound parameters for these (and ideally
for the rest of the form's fields too)** — don't string-concatenate them
into the SQL. For example, with mysqli prepared statements:

```php
$stmt = $conn->prepare(
    "UPDATE tblpeople SET GRAVENO=?, NAME=?, LAT=?, LNG=? WHERE PEOPLEID=?"
);
$stmt->bind_param("ssddi", $graveno, $name, $lat, $lng, $peopleId);
$stmt->execute();
```

(Adjust field list/types to match whatever columns the real UPDATE already
sets — this is just showing the LAT/LNG piece.)

A worthwhile side note: this project has a couple of publicly documented
vulnerabilities in exactly this area — a SQL-injection auth bypass on
`admin/login.php` and stored XSS in `admin/person/controller.php`'s edit
action, both from string-concatenated/unescaped input. Since you're already
touching `controller.php` to add LAT/LNG, it's a reasonable time to switch
the rest of that file's queries to prepared statements too.

## 6. Test

1. Open the add-grave form → the map should load centered on your default
   coordinates → click anywhere → a pin drops, and the coordinates show
   underneath.
2. Save the record → confirm `LAT`/`LNG` land in the database row.
3. Open that record in edit mode → the map should open with the pin already
   placed where you left it.
4. Confirm the Android app's Map screen (`api/graves_geo.php`) now returns
   that grave and shows its pin.

## Notes

- **"Use My Current Location"** uses the browser's geolocation API — handy
  if whoever's entering records is standing at the actual grave with a
  phone or laptop while filling out the form.
- **Address search** only pans the map to a typed address (via OSM
  Nominatim) — it deliberately does *not* auto-place the pin, since a
  street address isn't the same as the exact grave spot. Search to get
  close, then click precisely.
- No API key, billing account, or external service signup needed — same
  as the Android app's map, this all runs on free OpenStreetMap
  infrastructure.
