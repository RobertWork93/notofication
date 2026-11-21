package com.notificationapp.integration.repository;

import com.notificationapp.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Checks the Liquibase schema directly with SQL, so database rules hold even for writes that bypass the API.
 */
class SchemaConstraintsIT extends AbstractIntegrationTest {

    @Test
    void shouldApplyAllMigrations() {
        Integer applied = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM databasechangelog", Integer.class);

        assertThat(applied).isEqualTo(8);
    }

    @Test
    void shouldRejectUnknownOutboxStatus() {
        UUID eventId = UUID.fromString(createEvent("breaking_news", "Election called"));

        assertThatThrownBy(() -> jdbcTemplate.update("""
                INSERT INTO event_outbox (id, event_id, status, created_at) VALUES (?, ?, 'DONE', now())
                """, UUID.randomUUID(), eventId))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldRejectOutboxRecordForMissingEvent() {
        assertThatThrownBy(() -> jdbcTemplate.update("""
                INSERT INTO event_outbox (id, event_id, status, created_at) VALUES (?, ?, 'PENDING', now())
                """, UUID.randomUUID(), UUID.randomUUID()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldAllowOnlyOneDeliveryPerEventAndAlert() {
        UUID userId = UUID.fromString(createUser("jane.doe@example.com", "Jane Doe"));
        UUID alertId = UUID.fromString(createAlert(userId.toString(), "Quakes", "natural_disaster"));
        UUID eventId = UUID.fromString(createEvent("natural_disaster", "Earthquake"));
        insertDelivery(eventId, alertId, userId, "email", "PENDING");

        assertThatThrownBy(() -> insertDelivery(eventId, alertId, userId, "email", "PENDING"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldRejectUnknownDeliveryStatusAndChannel() {
        UUID userId = UUID.fromString(createUser("jane.doe@example.com", "Jane Doe"));
        UUID alertId = UUID.fromString(createAlert(userId.toString(), "Quakes", "natural_disaster"));
        UUID eventId = UUID.fromString(createEvent("natural_disaster", "Earthquake"));

        assertThatThrownBy(() -> insertDelivery(eventId, alertId, userId, "email", "DELIVERED"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> insertDelivery(eventId, alertId, userId, "sms", "PENDING"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldDeleteDeliveriesWhenTheirUserIsDeleted() {
        UUID userId = UUID.fromString(createUser("jane.doe@example.com", "Jane Doe"));
        UUID alertId = UUID.fromString(createAlert(userId.toString(), "Quakes", "natural_disaster"));
        UUID eventId = UUID.fromString(createEvent("natural_disaster", "Earthquake"));
        insertDelivery(eventId, alertId, userId, "email", "PENDING");

        jdbcTemplate.update("DELETE FROM users WHERE id = ?", userId);

        Integer deliveries = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM notification_deliveries WHERE alert_id = ?", Integer.class, alertId);
        assertThat(deliveries).isZero();
    }

    private void insertDelivery(UUID eventId, UUID alertId, UUID userId, String channel, String status) {
        jdbcTemplate.update("""
                INSERT INTO notification_deliveries
                    (id, event_id, alert_id, user_id, channel, recipient, status, created_at)
                VALUES (?, ?, ?, ?, ?, 'jane.doe@example.com', ?, now())
                """, UUID.randomUUID(), eventId, alertId, userId, channel, status);
    }

    @Test
    void shouldAcceptValidUserRow() {
        jdbcTemplate.update(INSERT_USER, UUID.randomUUID(), "jane.doe@example.com", "{bcrypt}x", "USER");

        Integer users = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE email = 'jane.doe@example.com'", Integer.class);
        assertThat(users).isEqualTo(1);
    }

    @Test
    void shouldRejectEmailThatIsNotLowerCase() {
        assertThatThrownBy(() -> jdbcTemplate.update(
                INSERT_USER, UUID.randomUUID(), "Jane.Doe@example.com", "{bcrypt}x", "USER"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldRejectUnknownRole() {
        assertThatThrownBy(() -> jdbcTemplate.update(
                INSERT_USER, UUID.randomUUID(), "jane.doe@example.com", "{bcrypt}x", "SUPERUSER"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldRejectUserWithoutPasswordHash() {
        assertThatThrownBy(() -> jdbcTemplate.update(
                INSERT_USER, UUID.randomUUID(), "jane.doe@example.com", null, "USER"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private static final String INSERT_USER = """
            INSERT INTO users (id, email, name, password_hash, role, created_at, updated_at)
            VALUES (?, ?, 'Jane Doe', ?, ?, now(), now())
            """;

    @Test
    void shouldRejectUnknownAlertEventTypeAndChannel() {
        UUID userId = UUID.fromString(createUser("jane.doe@example.com", "Jane Doe"));
        String insert = """
                INSERT INTO alerts (id, user_id, name, event_type, channel, enabled, created_at, updated_at)
                VALUES (?, ?, 'x', ?, ?, TRUE, now(), now())
                """;

        assertThatThrownBy(() -> jdbcTemplate.update(insert, UUID.randomUUID(), userId, "market_movement", "email"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbcTemplate.update(insert, UUID.randomUUID(), userId, "breaking_news", "sms"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldRejectUnknownEventType() {
        assertThatThrownBy(() -> jdbcTemplate.update("""
                INSERT INTO events (id, type, title, source, occurred_at, received_at)
                VALUES (?, 'market_movement', 'x', 'y', now(), now())
                """, UUID.randomUUID()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

}
