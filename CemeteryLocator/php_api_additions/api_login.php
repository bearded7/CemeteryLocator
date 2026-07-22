<?php
/**
 * api/login.php
 *
 * POST: email, password
 * Verifies credentials and returns a fresh auth token.
 */
require_once(__DIR__ . "/../include/initialize.php");
require_once(__DIR__ . "/auth_common.php");

header("Content-Type: application/json");

$email = isset($_POST['email']) ? trim($_POST['email']) : '';
$password = isset($_POST['password']) ? $_POST['password'] : '';

if ($email === '' || $password === '') {
    http_response_code(400);
    echo json_encode(array("error" => "Email and password are required"));
    exit;
}

$safeEmail = $mydb->escape($email);
$mydb->setQuery(
    "SELECT USERID, PASSWORD_HASH, FULL_NAME FROM tblpublicusers WHERE EMAIL = '{$safeEmail}'"
);
$cur = $mydb->executeQuery();

if ($mydb->num_rows($cur) === 0) {
    // Same error for "no such user" and "wrong password" - don't leak
    // which one it was.
    http_response_code(401);
    echo json_encode(array("error" => "Invalid email or password"));
    exit;
}

$row = $mydb->loadSingleResult();

if (!password_verify($password, $row->PASSWORD_HASH)) {
    http_response_code(401);
    echo json_encode(array("error" => "Invalid email or password"));
    exit;
}

$token = issue_token($mydb, $row->USERID);

echo json_encode(array(
    "token" => $token,
    "user" => array(
        "userid" => (int)$row->USERID,
        "email" => $email,
        "full_name" => $row->FULL_NAME,
    ),
));
?>
