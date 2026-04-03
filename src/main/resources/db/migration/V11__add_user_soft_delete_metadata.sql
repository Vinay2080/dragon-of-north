ALTER TABLE users
    ADD COLUMN deleted_at TIMESTAMP,
    ADD COLUMN deletion_reason VARCHAR(255);
