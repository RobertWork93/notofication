--liquibase formatted sql

--changeset notification-app:003-create-events
CREATE TABLE events (
    id          UUID         PRIMARY KEY,
    type        VARCHAR(32)  NOT NULL,
    title       VARCHAR(200) NOT NULL,
    source      VARCHAR(100) NOT NULL,
    occurred_at TIMESTAMPTZ  NOT NULL,
    payload     JSONB,
    received_at TIMESTAMPTZ  NOT NULL,
    CONSTRAINT ck_events_type CHECK (type IN ('breaking_news', 'natural_disaster'))
);
--rollback DROP TABLE events;
