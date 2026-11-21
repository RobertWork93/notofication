package com.notificationapp.integration.api;

import com.notificationapp.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AlertApiIT extends AbstractIntegrationTest {

    @Test
    void shouldCreateUpdateReadAndDeleteAlert() {
        String userId = createUser("jane.doe@example.com", "Jane Doe");
        String alertId = createAlert(userId, "Earthquakes", "natural_disaster");

        client.put().uri("/api/v1/alerts/{alertId}/users/{userId}", alertId, userId)
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"name": "Big earthquakes", "eventType": "natural_disaster",
                         "channel": "slack", "enabled": false}
                        """)
                .exchange()
                .expectStatus().isOk();

        client.get().uri("/api/v1/alerts/{alertId}/users/{userId}", alertId, userId)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.name").isEqualTo("Big earthquakes")
                .jsonPath("$.channel").isEqualTo("slack")
                .jsonPath("$.enabled").isEqualTo(false);

        client.delete().uri("/api/v1/alerts/{alertId}/users/{userId}", alertId, userId)
                .exchange()
                .expectStatus().isNoContent();

        client.get().uri("/api/v1/alerts/{alertId}/users/{userId}", alertId, userId)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void shouldListOnlyTheUsersAlertsOldestFirst() {
        String janeId = createUser("jane.doe@example.com", "Jane Doe");
        String johnId = createUser("john.doe@example.com", "John Doe");
        createAlert(janeId, "First", "breaking_news");
        createAlert(janeId, "Second", "natural_disaster");
        createAlert(johnId, "John's alert", "breaking_news");

        client.get().uri("/api/v1/alerts?userId={userId}", janeId)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.content.length()").isEqualTo(2)
                .jsonPath("$.content[0].name").isEqualTo("First")
                .jsonPath("$.content[1].name").isEqualTo("Second")
                .jsonPath("$.page.totalElements").isEqualTo(2);
    }

    @Test
    void shouldNotReadUpdateOrDeleteAlertOfAnotherUser() {
        String janeId = createUser("jane.doe@example.com", "Jane Doe");
        String johnId = createUser("john.doe@example.com", "John Doe");
        String janesAlert = createAlert(janeId, "Earthquakes", "natural_disaster");

        client.get().uri("/api/v1/alerts/{alertId}/users/{userId}", janesAlert, johnId)
                .exchange()
                .expectStatus().isNotFound();
        client.put().uri("/api/v1/alerts/{alertId}/users/{userId}", janesAlert, johnId)
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"name": "Hijacked", "eventType": "breaking_news", "channel": "email", "enabled": true}
                        """)
                .exchange()
                .expectStatus().isNotFound();
        client.delete().uri("/api/v1/alerts/{alertId}/users/{userId}", janesAlert, johnId)
                .exchange()
                .expectStatus().isNotFound();

        client.get().uri("/api/v1/alerts/{alertId}/users/{userId}", janesAlert, janeId)
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.name").isEqualTo("Earthquakes");
    }

    @Test
    void shouldReturnNotFoundWhenCreatingAlertForMissingUser() {
        client.post().uri("/api/v1/alerts")
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"userId": "%s", "name": "News", "eventType": "breaking_news", "channel": "email"}
                        """.formatted(UUID.randomUUID()))
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void shouldDeleteUsersAlertsWhenUserIsDeleted() {
        String userId = createUser("jane.doe@example.com", "Jane Doe");
        createAlert(userId, "Earthquakes", "natural_disaster");

        client.delete().uri("/api/v1/users/{userId}", userId)
                .exchange()
                .expectStatus().isNoContent();

        Integer remaining = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM alerts WHERE user_id = ?", Integer.class, UUID.fromString(userId));
        assertThat(remaining).isZero();
    }

}
