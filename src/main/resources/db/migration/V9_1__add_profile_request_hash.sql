-- Fingerprint the last accepted Agent profile PATCH so an identical retry is
-- idempotent while a different payload using the same revision is rejected.
-- Older rows cannot be reconstructed reliably; their hash remains NULL and
-- require a newer revision for the next accepted update.
ALTER TABLE memories
    ADD COLUMN profile_request_hash CHAR(64) NULL;
