<?php
/**
 * admin/requests/controller.php
 *
 * Handles the approve/reject buttons on requests/index.php.
 *
 * Approve: copies the request into tblpeople (using the GRAVENO the
 * admin typed in) and marks the request approved.
 * Reject: just marks the request rejected, with an optional note.
 *
 * Same note as index.php: replace the placeholder session check with
 * this project's real admin auth guard before deploying.
 */
require_once(__DIR__ . "/../../include/initialize.php");

session_start();
if (!isset($_SESSION['ADMINID']) && !isset($_SESSION['U_USERNAME'])) {
    header("Location: ../login.php");
    exit;
}

$action = isset($_POST['action']) ? $_POST['action'] : '';
$requestId = isset($_POST['requestid']) ? (int)$_POST['requestid'] : 0;

if ($requestId <= 0 || !in_array($action, array('approve', 'reject'))) {
    header("Location: index.php?status=pending&error=bad_request");
    exit;
}

// Load the pending request first - never trust requestid alone without
// checking it's actually still pending, to avoid double-processing.
$mydb->setQuery(
    "SELECT * FROM tblgraverequests WHERE REQUESTID = {$requestId} AND STATUS = 'pending'"
);
$cur = $mydb->executeQuery();
if ($mydb->num_rows($cur) === 0) {
    header("Location: index.php?status=pending&error=not_found_or_already_reviewed");
    exit;
}
$request = $mydb->loadSingleResult();

if ($action === 'reject') {
    $adminNote = isset($_POST['admin_note']) ? trim($_POST['admin_note']) : '';
    $safeNote = $mydb->escape($adminNote);
    $mydb->setQuery(
        "UPDATE tblgraverequests
         SET STATUS = 'rejected', ADMIN_NOTE = '{$safeNote}', REVIEWED_AT = NOW()
         WHERE REQUESTID = {$requestId}"
    );
    $mydb->executeQuery();
    header("Location: index.php?status=pending&rejected=1");
    exit;
}

// action === 'approve'
$graveNo = isset($_POST['graveno']) ? trim($_POST['graveno']) : '';
if ($graveNo === '') {
    header("Location: index.php?status=pending&error=graveno_required");
    exit;
}

$safeGraveNo = $mydb->escape($graveNo);

// Refuse to overwrite an existing grave number silently.
$mydb->setQuery("SELECT GRAVENO FROM tblpeople WHERE GRAVENO = '{$safeGraveNo}'");
$dupeCheck = $mydb->executeQuery();
if ($mydb->num_rows($dupeCheck) > 0) {
    header("Location: index.php?status=pending&error=graveno_taken");
    exit;
}

$safeName = $mydb->escape($request->NAME);
$safeSex = $request->SEX !== null ? "'" . $mydb->escape($request->SEX) . "'" : "NULL";
$safeBorn = $request->BORNDATE !== null ? "'" . $mydb->escape($request->BORNDATE) . "'" : "NULL";
$safeDied = $request->DIEDDATE !== null ? "'" . $mydb->escape($request->DIEDDATE) . "'" : "NULL";
$safeLocation = $request->LOCATION !== null ? "'" . $mydb->escape($request->LOCATION) . "'" : "NULL";
$lat = (float)$request->LAT;
$lng = (float)$request->LNG;

$mydb->setQuery(
    "INSERT INTO tblpeople (GRAVENO, NAME, SEX, BORNDATE, DIEDDATE, LOCATION, LAT, LNG)
     VALUES ('{$safeGraveNo}', '{$safeName}', {$safeSex}, {$safeBorn}, {$safeDied}, {$safeLocation}, {$lat}, {$lng})"
);
$mydb->executeQuery();

$mydb->setQuery(
    "UPDATE tblgraverequests
     SET STATUS = 'approved', RESULTING_GRAVENO = '{$safeGraveNo}', REVIEWED_AT = NOW()
     WHERE REQUESTID = {$requestId}"
);
$mydb->executeQuery();

header("Location: index.php?status=pending&approved=1");
exit;
?>
