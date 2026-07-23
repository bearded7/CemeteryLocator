<?php
/**
 * api/search.php
 *
 * Companion JSON endpoint for the Android app. The existing search.php
 * only returns a bare list of names (for the web UI's autocomplete), so
 * this returns full record objects instead.
 *
 * Drop this file into an /api folder at the repo root, alongside a copy
 * of (or symlink to) include/initialize.php.
 *
 * GET/POST param: q  (search text - matches NAME or GRAVENO)
 */
require_once(__DIR__ . "/../include/initialize.php");

header("Content-Type: application/json");

$q = isset($_REQUEST['q']) ? $_REQUEST['q'] : '';
$q = $mydb->escape($q); // assumes the project's db wrapper exposes an escape() method;
                        // if not, replace with mysqli_real_escape_string($mydb->conn, $q)

$sql = "SELECT GRAVENO, NAME, SEX, BORNDATE, DIEDDATE, LOCATION, LAT, LNG, CATEGORIES, PHOTO, GRAVEPIC, MEMORABILIA
        FROM tblpeople
        WHERE NAME LIKE '%{$q}%' OR GRAVENO LIKE '%{$q}%'
        ORDER BY NAME ASC
        LIMIT 50";

$mydb->setQuery($sql);
$cur = $mydb->loadResultList();

$results = array();
foreach ($cur as $res) {
    $results[] = array(
        "graveno"     => $res->GRAVENO,
        "name"        => $res->NAME,
        "sex"         => $res->SEX,
        "borndate"    => $res->BORNDATE,
        "dieddate"    => $res->DIEDDATE,
        "location"    => $res->LOCATION,
        "lat"         => ($res->LAT !== null) ? (float)$res->LAT : null,
        "lng"         => ($res->LNG !== null) ? (float)$res->LNG : null,
        "categories"  => $res->CATEGORIES,
        "photo_url"   => !empty($res->PHOTO) ? web_root . 'admin/person/' . $res->PHOTO : null,
        "gravepic_url"=> !empty($res->GRAVEPIC) ? web_root . 'admin/person/' . $res->GRAVEPIC : null,
        "memorabilia" => $res->MEMORABILIA,
    );
}

echo json_encode(array("results" => $results));
?>
