package com.notificationapp.integration.api;

import com.notificationapp.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class UserApiIT extends AbstractIntegrationTest {

    @Test
    void shouldCreateReadUpdateListAndDeleteUser() {
        String userId = createUser("  Jane.Doe@Example.com ", "Jane Doe");

        client.get().uri("/api/v1/users/{userId}", userId)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.email").isEqualTo("jane.doe@example.com")
                .jsonPath("$.role").isEqualTo("USER")
                .jsonPath("$.passwordHash").doesNotExist();

        client.put().uri("/api/v1/users/{userId}", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"email": "jane.smith@example.com", "name": "Jane Smith", "password": "new-password"}
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.name").isEqualTo("Jane Smith");

        // The startup admin is the oldest user, so the updated user comes second.
        client.get().uri("/api/v1/users?size=10")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.content[0].email").isEqualTo(ADMIN_EMAIL)
                .jsonPath("$.content[1].email").isEqualTo("jane.smith@example.com")
                .jsonPath("$.page.totalElements").isEqualTo(2);

        client.delete().uri("/api/v1/users/{userId}", userId)
                .exchange()
                .expectStatus().isNoContent();

        client.get().uri("/api/v1/users/{userId}", userId)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void shouldReturnSecondPageOfUsers() {
        createUser("first@example.com", "First");
        createUser("second@example.com", "Second");
        createUser("third@example.com", "Third");

        // Four users in total, counting the startup admin.
        client.get().uri("/api/v1/users?page=1&size=2")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.content.length()").isEqualTo(2)
                .jsonPath("$.content[0].email").isEqualTo("second@example.com")
                .jsonPath("$.content[1].email").isEqualTo("third@example.com")
                .jsonPath("$.page.number").isEqualTo(1)
                .jsonPath("$.page.totalPages").isEqualTo(2);
    }

    @Test
    void shouldReturnBadRequestProblemForInvalidUser() {
        anonymousClient().post().uri("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"email": "not-an-email", "name": "", "password": "short"}
                        """)
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON);
    }

}
