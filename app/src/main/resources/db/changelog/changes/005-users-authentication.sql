--liquibase formatted sql

--changeset notification-app:005-users-authentication
-- HTTP Basic credentials and the user's role. Needs an empty users table, because both columns are NOT NULL.
ALTER TABLE users ADD COLUMN password_hash VARCHAR(100) NOT NULL;
ALTER TABLE users ADD COLUMN role VARCHAR(16) NOT NULL;
ALTER TABLE users ADD CONSTRAINT ck_users_role CHECK (role IN ('ADMIN', 'USER'));
--rollback ALTER TABLE users DROP CONSTRAINT ck_users_role;
--rollback ALTER TABLE users DROP COLUMN role;
--rollback ALTER TABLE users DROP COLUMN password_hash;
