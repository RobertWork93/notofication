package com.notificationapp.integration.service;

import com.notificationapp.notification.service.OutboxProcessor;
import com.notificationapp.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OutboxProcessingIT extends AbstractIntegrationTest {

    @Autowired
    private OutboxProcessor outboxProcessor;

    @Test
    void shouldWritePendingOutboxRecordWhenEventIsCreated() {
        String eventId = createEvent("natural_disaster", "Earthquake");

        String status = jdbcTemplate.queryForObject(
                "SELECT status FROM event_outbox WHERE event_id = ?", String.class, UUID.fromString(eventId));
        assertThat(status).isEqualTo("PENDING");
    }

    @Test
    void shouldCreateDeliveriesOnlyForEnabledAlertsOfTheEventType() {
        String jane = createUser("jane.doe@example.com", "Jane Doe");
        String john = createUser("john.doe@example.com", "John Doe");
        String ann = createUser("ann.lee@example.com", "Ann Lee");
        String janesAlert = createAlert(jane, "Quakes by email", "natural_disaster", "email", true);
        String annsAlert = createAlert(ann, "Quakes on Slack", "natural_disaster", "slack", true);
        createAlert(john, "Disabled quakes", "natural_disaster", "email", false);
        createAlert(john, "News", "breaking_news", "email", true);
        String eventId = createEvent("natural_disaster", "Earthquake");

        boolean processed = outboxProcessor.processNext();

        assertThat(processed).isTrue();
        List<Map<String, Object>> deliveries = jdbcTemplate.queryForList("""
                SELECT alert_id, channel, recipient, status FROM notification_deliveries
                WHERE event_id = ? ORDER BY recipient
                """, UUID.fromString(eventId));
        assertThat(deliveries).hasSize(2);
        assertThat(deliveries.get(0)).containsEntry("alert_id", UUID.fromString(annsAlert))
                .containsEntry("channel", "slack")
                .containsEntry("recipient", "ann.lee@example.com")
                .containsEntry("status", "PENDING");
        assertThat(deliveries.get(1)).containsEntry("alert_id", UUID.fromString(janesAlert))
                .containsEntry("channel", "email")
                .containsEntry("recipient", "jane.doe@example.com");
        assertThat(outboxStatus(eventId)).isEqualTo("PROCESSED");
        assertThat(outboxProcessor.processNext()).isFalse();
    }

    @Test
    void shouldNotCreateDuplicateDeliveriesWhenRecordIsProcessedTwice() {
        String jane = createUser("jane.doe@example.com", "Jane Doe");
        createAlert(jane, "Quakes", "natural_disaster");
        String eventId = createEvent("natural_disaster", "Earthquake");
        outboxProcessor.processNext();

        jdbcTemplate.update("UPDATE event_outbox SET status = 'PENDING', processed_at = NULL WHERE event_id = ?",
                UUID.fromString(eventId));
        outboxProcessor.processNext();

        Integer deliveries = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM notification_deliveries WHERE event_id = ?", Integer.class,
                UUID.fromString(eventId));
        assertThat(deliveries).isEqualTo(1);
        assertThat(outboxStatus(eventId)).isEqualTo("PROCESSED");
    }

    @Test
    void shouldProcessEventWithoutMatchingAlerts() {
        String eventId = createEvent("breaking_news", "Election called");

        assertThat(outboxProcessor.processNext()).isTrue();

        assertThat(outboxStatus(eventId)).isEqualTo("PROCESSED");
    }

    private String outboxStatus(String eventId) {
        return jdbcTemplate.queryForObject(
                "SELECT status FROM event_outbox WHERE event_id = ?", String.class, UUID.fromString(eventId));
    }

}
