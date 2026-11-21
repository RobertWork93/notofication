--liquibase formatted sql

--changeset notification-app:002-create-alerts
CREATE TABLE alerts (
    id         UUID         PRIMARY KEY,
    user_id    UUID         NOT NULL,
    name       VARCHAR(100) NOT NULL,
    event_type VARCHAR(32)  NOT NULL,
    channel    VARCHAR(16)  NOT NULL,
    enabled    BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ  NOT NULL,
    updated_at TIMESTAMPTZ  NOT NULL,
    CONSTRAINT fk_alerts_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT ck_alerts_event_type CHECK (event_type IN ('breaking_news', 'natural_disaster')),
    CONSTRAINT ck_alerts_channel CHECK (channel IN ('email', 'slack'))
);

CREATE INDEX idx_alerts_user_id ON alerts (user_id);
--rollback DROP TABLE alerts;
