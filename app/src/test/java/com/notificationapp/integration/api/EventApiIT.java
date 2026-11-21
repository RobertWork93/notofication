package com.notificationapp.integration.api;

import com.jayway.jsonpath.JsonPath;
import com.notificationapp.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class EventApiIT extends AbstractIntegrationTest {

    @Test
    void shouldStoreEventWithNestedPayloadAndReturnItUnchanged() {
        String body = client.post().uri("/api/v1/events")
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"type": "natural_disaster", "title": "M6.8 earthquake", "source": "usgs",
                         "occurredAt": "2026-11-21T14:52:00Z",
                         "payload": {"magnitude": 6.8, "region": "Pacific", "tags": ["tsunami"],
                                     "location": {"lat": -10.5, "lon": 160.2}}}
                        """)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();
        String eventId = JsonPath.read(body, "$.id");

        client.get().uri("/api/v1/events/{eventId}", eventId)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.type").isEqualTo("natural_disaster")
                .jsonPath("$.occurredAt").isEqualTo("2026-11-21T14:52:00Z")
                .jsonPath("$.payload.magnitude").isEqualTo(6.8)
                .jsonPath("$.payload.tags[0]").isEqualTo("tsunami")
                .jsonPath("$.payload.location.lon").isEqualTo(160.2)
                .jsonPath("$.receivedAt").exists();

        String storedType = jdbcTemplate.queryForObject(
                "SELECT type FROM events WHERE id = ?", String.class, UUID.fromString(eventId));
        assertThat(storedType).isEqualTo("natural_disaster");
    }

    @Test
    void shouldStoreEventWithoutPayload() {
        client.post().uri("/api/v1/events")
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"type": "breaking_news", "title": "Election called", "source": "reuters",
                         "occurredAt": "2026-11-21T14:52:00Z"}
                        """)
                .exchange()
                .expectStatus().isCreated()
                .expectBody().jsonPath("$.payload").doesNotExist();
    }

    @Test
    void shouldReturnNotFoundForMissingEvent() {
        client.get().uri("/api/v1/events/{eventId}", UUID.randomUUID())
                .exchange()
                .expectStatus().isNotFound()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON);
    }

}
