--liquibase formatted sql

--changeset notification-app:008-alerts-matching-index
-- Finding the enabled alerts for an event type when an event is processed.
CREATE INDEX idx_alerts_enabled_event_type ON alerts (event_type) WHERE enabled;
--rollback DROP INDEX idx_alerts_enabled_event_type;
