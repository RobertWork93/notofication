--liquibase formatted sql

--changeset notification-app:001-create-users
CREATE TABLE users (
    id         UUID         PRIMARY KEY,
    email      VARCHAR(320) NOT NULL,
    name       VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL,
    updated_at TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uk_users_email UNIQUE (email)
);
--rollback DROP TABLE users;
