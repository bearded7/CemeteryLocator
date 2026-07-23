-- Adds public user accounts (separate from the existing admin
-- tbluseraccount table) plus a moderation queue for grave submissions.
-- Run after migration_add_gps_columns.sql.

CREATE TABLE IF NOT EXISTS tblpublicusers (
  USERID INT AUTO_INCREMENT PRIMARY KEY,
  EMAIL VARCHAR(255) NOT NULL UNIQUE,
  PASSWORD_HASH VARCHAR(255) NOT NULL,
  FULL_NAME VARCHAR(255) NOT NULL,
  CREATED_AT DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- Simple bearer-token auth for the Android/API client. A token is issued
-- on login/register and sent as `Authorization: Bearer <token>` on
-- subsequent requests. Tokens expire (see EXPIRES_AT) rather than living
-- forever.
CREATE TABLE IF NOT EXISTS tblapitokens (
  TOKEN CHAR(64) PRIMARY KEY,
  USERID INT NOT NULL,
  CREATED_AT DATETIME DEFAULT CURRENT_TIMESTAMP,
  EXPIRES_AT DATETIME NOT NULL,
  FOREIGN KEY (USERID) REFERENCES tblpublicusers(USERID) ON DELETE CASCADE
) ENGINE=InnoDB;

-- Public submissions of a grave location, awaiting admin approval before
-- becoming a real tblpeople record.
CREATE TABLE IF NOT EXISTS tblgraverequests (
  REQUESTID INT AUTO_INCREMENT PRIMARY KEY,
  USERID INT NOT NULL,
  NAME VARCHAR(255) NOT NULL,
  SEX VARCHAR(10) NULL,
  BORNDATE DATE NULL,
  DIEDDATE DATE NULL,
  LOCATION VARCHAR(255) NULL,
  LAT DECIMAL(10,7) NOT NULL,
  LNG DECIMAL(10,7) NOT NULL,
  NOTES TEXT NULL,
  STATUS ENUM('pending', 'approved', 'rejected') NOT NULL DEFAULT 'pending',
  ADMIN_NOTE VARCHAR(500) NULL,
  RESULTING_GRAVENO VARCHAR(50) NULL, -- set when approved and copied into tblpeople
  SUBMITTED_AT DATETIME DEFAULT CURRENT_TIMESTAMP,
  REVIEWED_AT DATETIME NULL,
  FOREIGN KEY (USERID) REFERENCES tblpublicusers(USERID) ON DELETE CASCADE
) ENGINE=InnoDB;
