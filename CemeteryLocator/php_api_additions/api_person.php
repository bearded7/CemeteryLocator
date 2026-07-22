<?php
/**
 * api/person.php
 *
 * Returns a single grave record as JSON, for the Android app's detail screen.
 *
 * GET params: graveno (required), location (optional, narrows the match
 * the same way the existing person.php does when both are present)
 */
require_once(__DIR__ . "/../include/initialize.php");

header("Content-Type: application/json");

$graveno = isset($_GET['graveno']) ? $mydb->escape($_GET['graveno']) : '';
$location = isset($_GET['location']) ? $mydb->escape($_GET['location']) : '';

if ($graveno === '') {
    http_response_code(400);
    echo json_encode(array("error" => "graveno is required"));
    exit;
}

$sql = "SELECT GRAVENO, NAME, SEX, BORNDATE, DIEDDATE, LOCATION, LAT, LNG, CATEGORIES, PHOTO, GRAVEPIC, MEMORABILIA
        FROM tblpeople WHERE GRAVENO = '{$graveno}'";
if ($location !== '') {
    $sql .= " AND LOCATION = '{$location}'";
}
$sql .= " LIMIT 1";

$mydb->setQuery($sql);
$cur = $mydb->executeQuery();

if ($mydb->num_rows($cur) === 0) {
    http_response_code(404);
    echo json_encode(array("error" => "not found"));
    exit;
}

$res = $mydb->loadSingleResult();

$borndate = ($res->BORNDATE != '0000-00-00') ? $res->BORNDATE : null;
$dieddate = ($res->DIEDDATE != '0000-00-00') ? $res->DIEDDATE : null;
$age = null;
if ($borndate && $dieddate) {
    $age = date_diff(date_create($borndate), date_create($dieddate))->y;
}

echo json_encode(array(
    "graveno"      => $res->GRAVENO,
    "name"         => $res->NAME,
    "sex"          => $res->SEX,
    "borndate"     => $borndate,
    "dieddate"     => $dieddate,
    "age"          => $age,
    "location"     => $res->LOCATION,
    "lat"          => ($res->LAT !== null) ? (float)$res->LAT : null,
    "lng"          => ($res->LNG !== null) ? (float)$res->LNG : null,
    "categories"   => $res->CATEGORIES,
    "photo_url"    => !empty($res->PHOTO) ? web_root . 'admin/person/' . $res->PHOTO : null,
    "gravepic_url" => !empty($res->GRAVEPIC) ? web_root . 'admin/person/' . $res->GRAVEPIC : null,
    "memorabilia"  => $res->MEMORABILIA,
));
?>
