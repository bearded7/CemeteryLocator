<?php
/**
 * api/my_submissions.php
 *
 * GET (requires Authorization: Bearer <token>)
 * Returns the calling user's own grave submissions, newest first,
 * so the app can show "pending / approved / rejected" status.
 */
require_once(__DIR__ . "/../include/initialize.php");
require_once(__DIR__ . "/auth_common.php");

header("Content-Type: application/json");

$userId = require_auth($mydb);

$mydb->setQuery(
    "SELECT REQUESTID, NAME, SEX, BORNDATE, DIEDDATE, LOCATION, LAT, LNG, NOTES,
            STATUS, ADMIN_NOTE, RESULTING_GRAVENO, SUBMITTED_AT, REVIEWED_AT
     FROM tblgraverequests
     WHERE USERID = {$userId}
     ORDER BY SUBMITTED_AT DESC"
);
$cur = $mydb->loadResultList();

$results = array();
foreach ($cur as $r) {
    $results[] = array(
        "requestid" => (int)$r->REQUESTID,
        "name" => $r->NAME,
        "sex" => $r->SEX,
        "borndate" => $r->BORNDATE,
        "dieddate" => $r->DIEDDATE,
        "location" => $r->LOCATION,
        "lat" => (float)$r->LAT,
        "lng" => (float)$r->LNG,
        "notes" => $r->NOTES,
        "status" => $r->STATUS,
        "admin_note" => $r->ADMIN_NOTE,
        "resulting_graveno" => $r->RESULTING_GRAVENO,
        "submitted_at" => $r->SUBMITTED_AT,
        "reviewed_at" => $r->REVIEWED_AT,
    );
}

echo json_encode(array("submissions" => $results));
?>
