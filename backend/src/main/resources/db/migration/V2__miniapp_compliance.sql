ALTER TABLE miniapp_users
    ADD COLUMN privacy_version VARCHAR(32) NULL AFTER status,
    ADD COLUMN terms_version VARCHAR(32) NULL AFTER privacy_version,
    ADD COLUMN consented_at TIMESTAMP(3) NULL AFTER terms_version,
    ADD COLUMN cancelled_at TIMESTAMP(3) NULL AFTER consented_at;

