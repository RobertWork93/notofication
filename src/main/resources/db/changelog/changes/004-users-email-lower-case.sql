--liquibase formatted sql

--changeset notification-app:004-users-email-lower-case
-- Emails are stored in lower case, so the unique constraint is also unique ignoring case.
ALTER TABLE users ADD CONSTRAINT ck_users_email_lower_case CHECK (email = lower(email));
--rollback ALTER TABLE users DROP CONSTRAINT ck_users_email_lower_case;
