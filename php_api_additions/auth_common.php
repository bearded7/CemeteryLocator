<?php
/**
 * api/auth_common.php
 *
 * Shared helper for endpoints that require a logged-in public user.
 * Include this, then call require_auth($mydb) - it returns the USERID
 * on success, or sends a 401 JSON response and exits on failure.
 *
 * Client sends: Authorization: Bearer <token>
 */

function get_bearer_token() {
    $headers = null;
    if (function_exists('getallheaders')) {
        $headers = getallheaders();
    }
    $authHeader = null;
    if ($headers) {
        foreach ($headers as $key => $value) {
            if (strtolower($key) === 'authorization') {
                $authHeader = $value;
                break;
            }
        }
    }
    if (!$authHeader && isset($_SERVER['HTTP_AUTHORIZATION'])) {
        $authHeader = $_SERVER['HTTP_AUTHORIZATION'];
    }
    if (!$authHeader) {
        return null;
    }
    if (preg_match('/Bearer\s+(\S+)/i', $authHeader, $matches)) {
        return $matches[1];
    }
    return null;
}

/**
 * Validates the bearer token against tblapitokens.
 * Returns the USERID (int) on success. On failure, emits a 401 JSON
 * error and calls exit - callers can assume this function never
 * "returns null" to code that keeps executing.
 */
function require_auth($mydb) {
    $token = get_bearer_token();

    if (!$token || !preg_match('/^[a-f0-9]{64}$/', $token)) {
        http_response_code(401);
        header("Content-Type: application/json");
        echo json_encode(array("error" => "Missing or malformed auth token"));
        exit;
    }

    $safeToken = $mydb->escape($token);
    $mydb->setQuery(
        "SELECT USERID FROM tblapitokens WHERE TOKEN = '{$safeToken}' AND EXPIRES_AT > NOW()"
    );
    $cur = $mydb->executeQuery();

    if ($mydb->num_rows($cur) === 0) {
        http_response_code(401);
        header("Content-Type: application/json");
        echo json_encode(array("error" => "Invalid or expired token"));
        exit;
    }

    $row = $mydb->loadSingleResult();
    return (int)$row->USERID;
}

/** Issues a new token for a user, valid for 30 days. Returns the token string. */
function issue_token($mydb, $userId) {
    $token = bin2hex(random_bytes(32)); // 64 hex chars
    $userIdInt = (int)$userId;
    $mydb->setQuery(
        "INSERT INTO tblapitokens (TOKEN, USERID, EXPIRES_AT)
         VALUES ('{$token}', {$userIdInt}, DATE_ADD(NOW(), INTERVAL 30 DAY))"
    );
    $mydb->executeQuery();
    return $token;
}
?>
