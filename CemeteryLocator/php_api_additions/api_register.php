<?php
/**
 * api/register.php
 *
 * POST: email, password, full_name
 * Creates a public user account and returns an auth token, so the
 * Android app can go straight from sign-up into being logged in.
 */
require_once(__DIR__ . "/../include/initialize.php");
require_once(__DIR__ . "/auth_common.php");

header("Content-Type: application/json");

$email = isset($_POST['email']) ? trim($_POST['email']) : '';
$password = isset($_POST['password']) ? $_POST['password'] : '';
$fullName = isset($_POST['full_name']) ? trim($_POST['full_name']) : '';

if ($email === '' || !filter_var($email, FILTER_VALIDATE_EMAIL)) {
    http_response_code(400);
    echo json_encode(array("error" => "A valid email is required"));
    exit;
}
if (strlen($password) < 8) {
    http_response_code(400);
    echo json_encode(array("error" => "Password must be at least 8 characters"));
    exit;
}
if ($fullName === '') {
    http_response_code(400);
    echo json_encode(array("error" => "Full name is required"));
    exit;
}

$safeEmail = $mydb->escape($email);
$mydb->setQuery("SELECT USERID FROM tblpublicusers WHERE EMAIL = '{$safeEmail}'");
$existing = $mydb->executeQuery();
if ($mydb->num_rows($existing) > 0) {
    http_response_code(409);
    echo json_encode(array("error" => "An account with that email already exists"));
    exit;
}

$hash = password_hash($password, PASSWORD_BCRYPT);
$safeHash = $mydb->escape($hash);
$safeFullName = $mydb->escape($fullName);

$mydb->setQuery(
    "INSERT INTO tblpublicusers (EMAIL, PASSWORD_HASH, FULL_NAME)
     VALUES ('{$safeEmail}', '{$safeHash}', '{$safeFullName}')"
);
$mydb->executeQuery();
$userId = $mydb->insertId(); // adjust to whatever this project's DB wrapper calls its last-insert-id method

$token = issue_token($mydb, $userId);

echo json_encode(array(
    "token" => $token,
    "user" => array(
        "userid" => $userId,
        "email" => $email,
        "full_name" => $fullName,
    ),
));
?>
