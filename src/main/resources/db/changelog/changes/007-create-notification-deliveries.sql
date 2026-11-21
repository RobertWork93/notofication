--liquibase formatted sql

--changeset notification-app:007-create-notification-deliveries
-- One delivery job per event and matching alert, sent by the DeliveryWorker.
CREATE TABLE notification_deliveries (
    id         UUID          PRIMARY KEY,
    event_id   UUID          NOT NULL,
    alert_id   UUID          NOT NULL,
    user_id    UUID          NOT NULL,
    channel    VARCHAR(16)   NOT NULL,
    recipient  VARCHAR(320)  NOT NULL,
    status     VARCHAR(16)   NOT NULL,
    last_error VARCHAR(1000),
    created_at TIMESTAMPTZ   NOT NULL,
    sent_at    TIMESTAMPTZ,
    CONSTRAINT fk_notification_deliveries_event FOREIGN KEY (event_id) REFERENCES events (id),
    CONSTRAINT fk_notification_deliveries_alert FOREIGN KEY (alert_id) REFERENCES alerts (id) ON DELETE CASCADE,
    CONSTRAINT fk_notification_deliveries_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uk_notification_deliveries_event_alert UNIQUE (event_id, alert_id),
    CONSTRAINT ck_notification_deliveries_channel CHECK (channel IN ('email', 'slack')),
    CONSTRAINT ck_notification_deliveries_status CHECK (status IN ('PENDING', 'SENT', 'FAILED'))
);

-- The DeliveryWorker only looks at pending jobs, oldest first.
CREATE INDEX idx_notification_deliveries_pending ON notification_deliveries (created_at) WHERE status = 'PENDING';
--rollback DROP TABLE notification_deliveries;
