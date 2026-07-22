# Cemetery Locator (Android)

A native Android app for the [Cemetry-Mapping-Information-System](https://github.com/bearded7/Cemetry-Mapping-Information-System)
PHP/MySQL backend — search burial records by name or grave number, and view
a person's details (photo, dates, grave location, grave photo) on mobile.

## Why a companion API is included

The original site's PHP files (`search.php`, `person.php`, `map.php`) mostly
render server-side HTML (tables, hover popovers) for the web UI — `search.php`
is the one exception, but it only returns a bare array of names. There was no
clean JSON endpoint an app could consume directly, so `php_api_additions/`
adds two small, focused ones that reuse the existing project's DB connection
and reflect the same `tblpeople` schema:

- `api/search.php` — `GET ?q=<text>` → JSON array of matching records
  (matches `NAME` or `GRAVENO`, same as the site's own search)
- `api/person.php` — `GET ?graveno=<no>&location=<optional>` → JSON for one record

### Installing the API additions

1. Copy `php_api_additions/api_search.php` and `php_api_additions/api_person.php`
   into a new `api/` folder at the root of the existing PHP repo, renaming
   them to `search.php` and `person.php` respectively (i.e.
   `api/search.php`, `api/person.php`).
2. They `require_once` `../include/initialize.php`, matching how the
   existing `search.php`/`person.php` already connect to the DB — no schema
   or config changes needed.
3. One thing to check: these use `$mydb->escape(...)` for basic SQL
   escaping. If the project's DB wrapper class doesn't expose an `escape()`
   method, swap that call for whatever escaping method it does provide (or
   `mysqli_real_escape_string()` against its underlying connection) — the
   existing site code doesn't escape input at all, which is worth fixing
   here rather than carrying forward.

## The Android app

Kotlin + Gradle, MVVM-lite structure:

```
app/src/main/java/com/example/cemeterylocator/
  api/
    CemeteryApi.kt      - Retrofit interface for the two endpoints above
    ApiClient.kt         - builds Retrofit against a user-configurable base URL
  model/
    GraveRecord.kt        - matches the JSON shape from api/search.php & api/person.php
  ui/
    MainActivity.kt       - debounced search box + results list
    PersonDetailActivity.kt - single record: photos, dates, location
    SettingsActivity.kt   - set the backend's base URL
    GraveRecordAdapter.kt - RecyclerView adapter for the list
```

### Configuring the server address

The app ships pointed at `http://10.0.2.2/` (localhost as seen from the
Android emulator). Open the app → menu (⋮) → **Settings** to point it at a
real deployment, e.g. `https://yourcemetery.example.com/`.

### Building

```
git clone <this project>
cd CemeteryLocator
./gradlew assembleDebug
```

or open in Android Studio and hit Run.

## GPS map

The web app's grid-based grave map (`map.php` / `map_function.php`) has been
replaced on Android with a real, geographic map using **osmdroid**
(OpenStreetMap tiles — no API key or billing account needed):

- Every grave with GPS coordinates shows as a pin; tap a pin for its name,
  grave number, and section.
- A floating button centers on the device's live GPS position ("you are
  here"), using osmdroid's own location provider — no Google Play Services
  dependency.
- From a person's detail screen, **View on Map** jumps straight to that
  grave's pin.

### What you need to do to make this work

Grave coordinates don't exist in the current schema, so:

1. Run `php_api_additions/migration_add_gps_columns.sql` against the
   database — adds nullable `LAT`/`LNG` columns to `tblpeople`.
2. Populate them. `php_api_additions/admin_location_picker/` adds a
   "pick on map" widget to the admin add/edit forms — click a map to set
   a grave's coordinates instead of typing them by hand. See
   `admin_location_picker/INTEGRATION.md` for exact wiring steps.
3. Install `php_api_additions/api_graves_geo.php` alongside the other two
   API files — it returns every grave that has coordinates, for the map to
   plot. `api_search.php` and `api_person.php` were also updated to include
   `lat`/`lng` in their existing JSON output.

Until coordinates are populated, the map screen will load but show no pins
(with a toast saying so) — everything else in the app works unaffected.

## Public sign-up, grave submission, and admin approval

Users can now create an account, submit a grave location (tap-to-place pin
+ basic details), and see the status of their submissions — nothing goes
live until an admin approves it.

### Backend setup

1. Run `migration_public_users_and_requests.sql` — adds `tblpublicusers`
   (accounts), `tblapitokens` (auth), and `tblgraverequests` (the
   moderation queue). Run after `migration_add_gps_columns.sql`.
2. Install into `api/`: `auth_common.php` (shared token-validation helper —
   `require_once`'d by the others, not a public endpoint itself),
   `api_register.php` → `api/register.php`, `api_login.php` →
   `api/login.php`, `api_submit_grave.php` → `api/submit_grave.php`,
   `api_my_submissions.php` → `api/my_submissions.php`.
3. Install the admin approval queue: copy
   `admin_location_picker/requests_index.php` and
   `admin_location_picker/requests_controller.php` into a new
   `admin/requests/` folder, as `index.php` and `controller.php`. **Before
   deploying**, replace the placeholder session check at the top of each
   (`$_SESSION['ADMINID']` / `$_SESSION['U_USERNAME']`) with whatever this
   project's real admin login actually sets — I couldn't see
   `admin/login.php`'s contents to match it exactly.
4. Add a link to the new page from the existing admin nav/sidebar
   (wherever "Deceased Persons" etc. already link from).

Auth is simple bearer tokens (`Authorization: Bearer <token>`), issued on
register/login and stored client-side — no OAuth or session cookies
needed for the API since the Android app is a pure REST client.

### How it flows

1. User signs up / logs in from the Android app (toolbar menu).
2. **Submit a Grave Location** — tap the map to drop a pin (drag to
   fine-tune), fill in name/section/notes, submit. This goes into
   `tblgraverequests` as `pending` — it does **not** touch `tblpeople` or
   show up in search/map yet.
3. Admin visits `admin/requests/`, reviews pending submissions (with a
   one-click link to view the exact coordinates on OpenStreetMap),
   assigns a grave number, and approves or rejects.
4. On approval, the record is copied into `tblpeople` with that grave
   number — now it appears in search, the map, everywhere.
5. **My Submissions** in the app shows the user their own submissions and
   current status (pending / approved with the assigned grave # / rejected
   with the admin's note, if given).

## Directions

Two complementary ways to get to a grave, both wired into search results
and the person detail screen:

- **Get Directions** — hands off to whatever maps app is already
  installed (Google Maps, etc.) via a standard `geo:` intent for full
  turn-by-turn from wherever the user currently is. This is deliberately
  *not* reimplemented in-app — external maps apps are already good at
  roads/turn-by-turn, and this needs zero maintenance.
- **View on Map** → opens the in-app OSM map centered on that grave, with
  a live green line from the user's GPS position to the pin, plus a
  distance + compass-direction readout ("140 m • head NE") that updates
  every 2 seconds. This covers the part external maps apps can't: walking
  around *inside* the cemetery grounds to the exact plot, where there's
  usually no road/path data for a router to follow anyway.

Both buttons only appear when a record actually has coordinates.

## Notes / where to go from here

- **Admin/CRUD**: the site's `admin/` folder (add/edit records, upload
  photos) isn't ported — this app is read-only/lookup-focused, matching the
  README's stated purpose ("locate graves of loved ones easily").
- **Offline caching**: right now every search hits the network. A Room
  cache of the last search's results would let the list stay usable
  offline/on poor connections. osmdroid does cache map tiles it has already
  downloaded, so the map itself partially works offline once visited.
- **HTTPS**: `usesCleartextTraffic` is enabled in the manifest since small
  self-hosted PHP sites are often still on plain HTTP. Turn it off once the
  backend has TLS.
- **Turn-by-turn directions to a grave**: right now the map just shows
  "you are here" + pins. A "walk to this grave" line/bearing indicator
  would be a reasonable next step using the same `MyLocationNewOverlay`
  data already being read.
