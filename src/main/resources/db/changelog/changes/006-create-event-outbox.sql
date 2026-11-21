--liquibase formatted sql

--changeset notification-app:006-create-event-outbox
-- One record per new event, written in the same transaction as the event (transactional outbox).
CREATE TABLE event_outbox (
    id           UUID          PRIMARY KEY,
    event_id     UUID          NOT NULL,
    status       VARCHAR(16)   NOT NULL,
    last_error   VARCHAR(1000),
    created_at   TIMESTAMPTZ   NOT NULL,
    processed_at TIMESTAMPTZ,
    CONSTRAINT fk_event_outbox_event FOREIGN KEY (event_id) REFERENCES events (id),
    CONSTRAINT ck_event_outbox_status CHECK (status IN ('PENDING', 'PROCESSED', 'FAILED'))
);

-- The OutboxWorker only looks at pending records, oldest first.
CREATE INDEX idx_event_outbox_pending ON event_outbox (created_at) WHERE status = 'PENDING';
--rollback DROP TABLE event_outbox;
