package com.notificationapp.integration.service;

import com.notificationapp.support.AbstractIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The event module saves the event and the notification module writes the outbox record from an
 * {@code EventCreated} listener. Both must commit together: if the outbox write fails, the event must not be stored.
 * A test-only trigger makes every outbox insert fail.
 */
class EventOutboxAtomicityIT extends AbstractIntegrationTest {

    @BeforeEach
    void rejectOutboxInserts() {
        jdbcTemplate.execute("""
                CREATE OR REPLACE FUNCTION reject_outbox_insert() RETURNS trigger AS $$
                BEGIN
                    RAISE EXCEPTION 'outbox insert rejected';
                END
                $$ LANGUAGE plpgsql
                """);
        jdbcTemplate.execute("""
                CREATE TRIGGER reject_outbox_insert BEFORE INSERT ON event_outbox
                FOR EACH ROW EXECUTE FUNCTION reject_outbox_insert()
                """);
    }

    @AfterEach
    void removeTrigger() {
        jdbcTemplate.execute("DROP TRIGGER IF EXISTS reject_outbox_insert ON event_outbox");
        jdbcTemplate.execute("DROP FUNCTION IF EXISTS reject_outbox_insert()");
    }

    @Test
    void shouldNotStoreEventWhenOutboxWriteFails() {
        client.post().uri("/api/v1/events")
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"type": "natural_disaster", "title": "Earthquake", "source": "usgs",
                         "occurredAt": "2026-11-21T14:52:00Z"}
                        """)
                .exchange()
                .expectStatus().is5xxServerError();

        Integer events = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM events", Integer.class);
        Integer outbox = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM event_outbox", Integer.class);
        assertThat(events).isZero();
        assertThat(outbox).isZero();
    }

}
