<?php
/**
 * admin/requests/index.php
 *
 * Lists public grave-location submissions awaiting review, with a small
 * form per row to approve or reject.
 *
 * IMPORTANT: this needs to sit behind whatever session/auth check the
 * rest of the admin panel already uses. I don't have the exact session
 * variable name this project uses (it wasn't in the files GitHub exposed
 * to me), so replace the placeholder check below with the real one -
 * e.g. copy the guard clause from the top of an existing file like
 * admin/person/index.php.
 */
require_once(__DIR__ . "/../../include/initialize.php");

// --- REPLACE THIS with the project's actual admin auth check ---
session_start();
if (!isset($_SESSION['ADMINID']) && !isset($_SESSION['U_USERNAME'])) {
    header("Location: ../login.php");
    exit;
}
// -----------------------------------------------------------------

$statusFilter = isset($_GET['status']) ? $_GET['status'] : 'pending';
$allowedStatuses = array('pending', 'approved', 'rejected', 'all');
if (!in_array($statusFilter, $allowedStatuses)) {
    $statusFilter = 'pending';
}

$sql = "SELECT r.REQUESTID, r.NAME, r.SEX, r.BORNDATE, r.DIEDDATE, r.LOCATION,
               r.LAT, r.LNG, r.NOTES, r.STATUS, r.ADMIN_NOTE, r.RESULTING_GRAVENO,
               r.SUBMITTED_AT, u.EMAIL, u.FULL_NAME AS SUBMITTER_NAME
        FROM tblgraverequests r
        JOIN tblpublicusers u ON u.USERID = r.USERID";
if ($statusFilter !== 'all') {
    $safeStatus = $mydb->escape($statusFilter);
    $sql .= " WHERE r.STATUS = '{$safeStatus}'";
}
$sql .= " ORDER BY r.SUBMITTED_AT DESC";

$mydb->setQuery($sql);
$requests = $mydb->loadResultList();
?>
<!DOCTYPE html>
<html>
<head>
  <meta charset="utf-8">
  <title>Grave Location Requests</title>
  <!-- Swap this for whatever CSS/layout the rest of the admin panel
       already includes (header/footer partials, existing stylesheet). -->
  <style>
    body { font-family: sans-serif; margin: 20px; }
    table { border-collapse: collapse; width: 100%; }
    th, td { border: 1px solid #ddd; padding: 8px; text-align: left; vertical-align: top; }
    th { background: #f5f5f5; }
    .status-pending { color: #b8860b; font-weight: bold; }
    .status-approved { color: #2e7d32; font-weight: bold; }
    .status-rejected { color: #c62828; font-weight: bold; }
    .filters a { margin-right: 12px; }
    .filters a.active { font-weight: bold; text-decoration: underline; }
    .actions form { display: inline-block; margin-right: 6px; }
    .actions input[type="text"] { width: 100px; }
    small.coords { color: #666; }
  </style>
</head>
<body>
  <h1>Grave Location Requests</h1>

  <div class="filters">
    <a href="?status=pending" class="<?php echo $statusFilter === 'pending' ? 'active' : ''; ?>">Pending</a>
    <a href="?status=approved" class="<?php echo $statusFilter === 'approved' ? 'active' : ''; ?>">Approved</a>
    <a href="?status=rejected" class="<?php echo $statusFilter === 'rejected' ? 'active' : ''; ?>">Rejected</a>
    <a href="?status=all" class="<?php echo $statusFilter === 'all' ? 'active' : ''; ?>">All</a>
  </div>

  <table>
    <tr>
      <th>Submitted</th>
      <th>Name</th>
      <th>Details</th>
      <th>Location</th>
      <th>Submitted by</th>
      <th>Status</th>
      <th>Actions</th>
    </tr>
    <?php if (empty($requests)): ?>
      <tr><td colspan="7">No requests in this view.</td></tr>
    <?php endif; ?>
    <?php foreach ($requests as $r): ?>
      <tr>
        <td><?php echo htmlspecialchars($r->SUBMITTED_AT); ?></td>
        <td><?php echo htmlspecialchars($r->NAME); ?></td>
        <td>
          <?php echo htmlspecialchars($r->SEX ?: '—'); ?><br>
          Born: <?php echo htmlspecialchars($r->BORNDATE ?: '—'); ?><br>
          Died: <?php echo htmlspecialchars($r->DIEDDATE ?: '—'); ?>
          <?php if (!empty($r->NOTES)): ?>
            <br><em><?php echo nl2br(htmlspecialchars($r->NOTES)); ?></em>
          <?php endif; ?>
        </td>
        <td>
          <?php echo htmlspecialchars($r->LOCATION ?: '—'); ?><br>
          <small class="coords"><?php echo htmlspecialchars($r->LAT . ', ' . $r->LNG); ?></small><br>
          <a href="https://www.openstreetmap.org/?mlat=<?php echo urlencode($r->LAT); ?>&mlon=<?php echo urlencode($r->LNG); ?>&zoom=19"
             target="_blank" rel="noopener">View on map</a>
        </td>
        <td>
          <?php echo htmlspecialchars($r->SUBMITTER_NAME); ?><br>
          <small><?php echo htmlspecialchars($r->EMAIL); ?></small>
        </td>
        <td>
          <span class="status-<?php echo htmlspecialchars($r->STATUS); ?>">
            <?php echo htmlspecialchars(ucfirst($r->STATUS)); ?>
          </span>
          <?php if (!empty($r->RESULTING_GRAVENO)): ?>
            <br><small>Grave #<?php echo htmlspecialchars($r->RESULTING_GRAVENO); ?></small>
          <?php endif; ?>
          <?php if (!empty($r->ADMIN_NOTE)): ?>
            <br><small><?php echo htmlspecialchars($r->ADMIN_NOTE); ?></small>
          <?php endif; ?>
        </td>
        <td class="actions">
          <?php if ($r->STATUS === 'pending'): ?>
            <form method="post" action="controller.php">
              <input type="hidden" name="action" value="approve">
              <input type="hidden" name="requestid" value="<?php echo (int)$r->REQUESTID; ?>">
              Grave #: <input type="text" name="graveno" placeholder="e.g. C-104" required>
              <button type="submit">Approve</button>
            </form>
            <form method="post" action="controller.php"
                  onsubmit="return confirm('Reject this submission?');">
              <input type="hidden" name="action" value="reject">
              <input type="hidden" name="requestid" value="<?php echo (int)$r->REQUESTID; ?>">
              <input type="text" name="admin_note" placeholder="Reason (optional)">
              <button type="submit">Reject</button>
            </form>
          <?php else: ?>
            &mdash;
          <?php endif; ?>
        </td>
      </tr>
    <?php endforeach; ?>
  </table>
</body>
</html>
