package com.notificationapp.integration.service;

import com.notificationapp.service.OutboxProcessor;
import com.notificationapp.support.AbstractIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Makes outbox processing fail halfway with a real database error, and checks that the work rolls back.
 * A test-only trigger rejects deliveries to boom@example.com, so the first delivery is inserted
 * and the second one fails in the same transaction.
 */
class OutboxFailureIT extends AbstractIntegrationTest {

    @Autowired
    private OutboxProcessor outboxProcessor;

    @BeforeEach
    void rejectDeliveriesToBoom() {
        jdbcTemplate.execute("""
                CREATE OR REPLACE FUNCTION reject_boom_delivery() RETURNS trigger AS $$
                BEGIN
                    IF NEW.recipient = 'boom@example.com' THEN
                        RAISE EXCEPTION 'boom recipient rejected';
                    END IF;
                    RETURN NEW;
                END
                $$ LANGUAGE plpgsql
                """);
        jdbcTemplate.execute("""
                CREATE TRIGGER reject_boom_delivery BEFORE INSERT ON notification_deliveries
                FOR EACH ROW EXECUTE FUNCTION reject_boom_delivery()
                """);
    }

    @AfterEach
    void removeTrigger() {
        jdbcTemplate.execute("DROP TRIGGER IF EXISTS reject_boom_delivery ON notification_deliveries");
        jdbcTemplate.execute("DROP FUNCTION IF EXISTS reject_boom_delivery()");
    }

    @Test
    void shouldRollBackPartialDeliveriesAndMarkRecordFailedWithTheError() {
        String jane = createUser("jane.doe@example.com", "Jane Doe");
        String boom = createUser("boom@example.com", "Boom");
        createAlert(jane, "Quakes", "natural_disaster");
        createAlert(boom, "Quakes", "natural_disaster");
        String eventId = createEvent("natural_disaster", "Earthquake");

        assertThat(outboxProcessor.processNext()).isTrue();

        Integer deliveries = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM notification_deliveries WHERE event_id = ?", Integer.class,
                UUID.fromString(eventId));
        assertThat(deliveries).isZero();
        Map<String, Object> record = jdbcTemplate.queryForMap(
                "SELECT status, last_error, processed_at FROM event_outbox WHERE event_id = ?",
                UUID.fromString(eventId));
        assertThat(record).containsEntry("status", "FAILED").containsEntry("processed_at", null);
        assertThat((String) record.get("last_error")).contains("boom recipient rejected");
        assertThat(outboxProcessor.processNext()).isFalse();
    }

}
