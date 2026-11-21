package com.notificationapp.integration.api;

import com.notificationapp.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

import static org.assertj.core.api.Assertions.assertThat;

class AuthenticationApiIT extends AbstractIntegrationTest {

    private static final String EVENT_JSON = """
            {"type": "breaking_news", "title": "Election called", "source": "reuters",
             "occurredAt": "2026-11-21T14:52:00Z"}
            """;

    @Test
    void shouldLetNewUserSignUpAndLogInWithHttpBasic() {
        String userId = createUser("Jane.Doe@example.com", "Jane Doe");

        clientAs("jane.doe@example.com", USER_PASSWORD).get().uri("/api/v1/users/{userId}", userId)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.email").isEqualTo("jane.doe@example.com")
                .jsonPath("$.role").isEqualTo("USER");
    }

    @Test
    void shouldReturnLoggedInUserFromMe() {
        String userId = createUser("jane.doe@example.com", "Jane Doe");

        clientAs("Jane.Doe@example.com", USER_PASSWORD).get().uri("/api/v1/users/me")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(userId)
                .jsonPath("$.role").isEqualTo("USER");
    }

    @Test
    void shouldStoreOnlyAPasswordHash() {
        String userId = createUser("jane.doe@example.com", "Jane Doe");

        String storedHash = jdbcTemplate.queryForObject(
                "SELECT password_hash FROM users WHERE id = ?::uuid", String.class, userId);
        assertThat(storedHash).startsWith("{bcrypt}").doesNotContain(USER_PASSWORD);
    }

    @Test
    void shouldRejectMissingOrWrongCredentials() {
        String userId = createUser("jane.doe@example.com", "Jane Doe");

        anonymousClient().get().uri("/api/v1/users/{userId}", userId)
                .exchange()
                .expectStatus().isUnauthorized()
                .expectHeader().value(HttpHeaders.WWW_AUTHENTICATE, value -> assertThat(value).startsWith("Basic"));
        clientAs("jane.doe@example.com", "wrong-password").get().uri("/api/v1/users/{userId}", userId)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void shouldKeepUsersAwayFromOtherUsersData() {
        String janeId = createUser("jane.doe@example.com", "Jane Doe");
        String johnId = createUser("john.doe@example.com", "John Doe");
        String johnsAlert = createAlert(johnId, "Earthquakes", "natural_disaster");
        RestTestClient jane = clientAs("jane.doe@example.com", USER_PASSWORD);

        jane.get().uri("/api/v1/users/{userId}", johnId).exchange().expectStatus().isForbidden();
        jane.get().uri("/api/v1/users").exchange().expectStatus().isForbidden();
        jane.get().uri("/api/v1/alerts?userId={userId}", johnId).exchange().expectStatus().isForbidden();
        jane.get().uri("/api/v1/alerts/{alertId}/users/{userId}", johnsAlert, johnId)
                .exchange().expectStatus().isForbidden();
        jane.post().uri("/api/v1/alerts")
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"userId": "%s", "name": "Sneaky", "eventType": "breaking_news", "channel": "email"}
                        """.formatted(johnId))
                .exchange()
                .expectStatus().isForbidden();

        jane.get().uri("/api/v1/alerts?userId={userId}", janeId).exchange().expectStatus().isOk();
    }

    @Test
    void shouldReturnNotFoundWhenUserUsesOwnIdWithAnotherUsersAlert() {
        String janeId = createUser("jane.doe@example.com", "Jane Doe");
        String johnId = createUser("john.doe@example.com", "John Doe");
        String johnsAlert = createAlert(johnId, "Earthquakes", "natural_disaster");

        clientAs("jane.doe@example.com", USER_PASSWORD)
                .get().uri("/api/v1/alerts/{alertId}/users/{userId}", johnsAlert, janeId)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void shouldLetOnlyAdminCreateEvents() {
        createUser("jane.doe@example.com", "Jane Doe");

        clientAs("jane.doe@example.com", USER_PASSWORD).post().uri("/api/v1/events")
                .contentType(MediaType.APPLICATION_JSON)
                .body(EVENT_JSON)
                .exchange()
                .expectStatus().isForbidden();
        client.post().uri("/api/v1/events")
                .contentType(MediaType.APPLICATION_JSON)
                .body(EVENT_JSON)
                .exchange()
                .expectStatus().isCreated();
    }

    @Test
    void shouldCreateStartupAdminFromConfiguration() {
        client.get().uri("/api/v1/users")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.content[0].email").isEqualTo(ADMIN_EMAIL)
                .jsonPath("$.content[0].role").isEqualTo("ADMIN");
    }

}
