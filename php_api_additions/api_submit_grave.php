<?php
/**
 * api/submit_grave.php
 *
 * POST (requires Authorization: Bearer <token>):
 *   name, sex (optional), borndate (optional, YYYY-MM-DD),
 *   dieddate (optional, YYYY-MM-DD), location (optional),
 *   lat, lng (required), notes (optional)
 *
 * Creates a pending row in tblgraverequests. It does NOT touch
 * tblpeople - an admin has to approve it first (see admin/requests/).
 */
require_once(__DIR__ . "/../include/initialize.php");
require_once(__DIR__ . "/auth_common.php");

header("Content-Type: application/json");

$userId = require_auth($mydb); // exits with 401 on failure

$name = isset($_POST['name']) ? trim($_POST['name']) : '';
$sex = isset($_POST['sex']) ? trim($_POST['sex']) : null;
$bornDate = isset($_POST['borndate']) && $_POST['borndate'] !== '' ? $_POST['borndate'] : null;
$diedDate = isset($_POST['dieddate']) && $_POST['dieddate'] !== '' ? $_POST['dieddate'] : null;
$location = isset($_POST['location']) ? trim($_POST['location']) : null;
$notes = isset($_POST['notes']) ? trim($_POST['notes']) : null;
$lat = isset($_POST['lat']) ? $_POST['lat'] : null;
$lng = isset($_POST['lng']) ? $_POST['lng'] : null;

if ($name === '') {
    http_response_code(400);
    echo json_encode(array("error" => "name is required"));
    exit;
}
if ($lat === null || $lng === null || !is_numeric($lat) || !is_numeric($lng)) {
    http_response_code(400);
    echo json_encode(array("error" => "lat and lng are required and must be numeric"));
    exit;
}
foreach (array('borndate' => $bornDate, 'dieddate' => $diedDate) as $label => $val) {
    if ($val !== null && !preg_match('/^\d{4}-\d{2}-\d{2}$/', $val)) {
        http_response_code(400);
        echo json_encode(array("error" => "$label must be in YYYY-MM-DD format"));
        exit;
    }
}

$latF = (float)$lat;
$lngF = (float)$lng;
if ($latF < -90 || $latF > 90 || $lngF < -180 || $lngF > 180) {
    http_response_code(400);
    echo json_encode(array("error" => "lat/lng out of range"));
    exit;
}

$safeName = $mydb->escape($name);
$safeSex = $sex !== null ? "'" . $mydb->escape($sex) . "'" : "NULL";
$safeBorn = $bornDate !== null ? "'" . $mydb->escape($bornDate) . "'" : "NULL";
$safeDied = $diedDate !== null ? "'" . $mydb->escape($diedDate) . "'" : "NULL";
$safeLocation = $location !== null ? "'" . $mydb->escape($location) . "'" : "NULL";
$safeNotes = $notes !== null ? "'" . $mydb->escape($notes) . "'" : "NULL";

$mydb->setQuery(
    "INSERT INTO tblgraverequests
        (USERID, NAME, SEX, BORNDATE, DIEDDATE, LOCATION, LAT, LNG, NOTES, STATUS)
     VALUES
        ({$userId}, '{$safeName}', {$safeSex}, {$safeBorn}, {$safeDied}, {$safeLocation}, {$latF}, {$lngF}, {$safeNotes}, 'pending')"
);
$mydb->executeQuery();
$requestId = $mydb->insertId();

echo json_encode(array(
    "requestid" => $requestId,
    "status" => "pending",
    "message" => "Submitted - an admin will review this before it appears publicly.",
));
?>
