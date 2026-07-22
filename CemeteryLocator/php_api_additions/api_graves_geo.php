<?php
/**
 * api/graves_geo.php
 *
 * Returns every grave record that has GPS coordinates set — used to
 * populate map markers. Deliberately lightweight (no photos/memorabilia)
 * since this can return the whole cemetery in one call.
 *
 * Optional GET param: location  (narrows to one section/row, same value
 * used elsewhere in the site as tblpeople.LOCATION)
 */
require_once(__DIR__ . "/../include/initialize.php");

header("Content-Type: application/json");

$location = isset($_GET['location']) ? $mydb->escape($_GET['location']) : '';

$sql = "SELECT GRAVENO, NAME, LOCATION, LAT, LNG
        FROM tblpeople
        WHERE LAT IS NOT NULL AND LNG IS NOT NULL";
if ($location !== '') {
    $sql .= " AND LOCATION = '{$location}'";
}

$mydb->setQuery($sql);
$cur = $mydb->loadResultList();

$graves = array();
foreach ($cur as $res) {
    $graves[] = array(
        "graveno"  => $res->GRAVENO,
        "name"     => $res->NAME,
        "location" => $res->LOCATION,
        "lat"      => (float)$res->LAT,
        "lng"      => (float)$res->LNG,
    );
}

echo json_encode(array("graves" => $graves));
?>
