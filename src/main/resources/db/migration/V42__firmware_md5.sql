-- Add MD5 checksum column for OTA firmware downloads (ESP32 validation).
-- Nullable so existing rows are not affected; populated on next upload.
ALTER TABLE firmwares ADD COLUMN IF NOT EXISTS checksum_md5 VARCHAR(32);
