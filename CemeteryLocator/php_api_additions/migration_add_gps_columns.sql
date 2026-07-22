-- Run this against the existing database to add GPS coordinates per grave.
-- Nullable, since older records won't have coordinates until someone
-- walks the cemetery with a phone and records them (or the admin panel
-- is extended to let a location be picked on a map when adding a record).

ALTER TABLE tblpeople
  ADD COLUMN LAT DECIMAL(10,7) NULL AFTER LOCATION,
  ADD COLUMN LNG DECIMAL(10,7) NULL AFTER LAT;

-- Optional: quick sanity check after populating some rows
-- SELECT GRAVENO, NAME, LAT, LNG FROM tblpeople WHERE LAT IS NOT NULL;
