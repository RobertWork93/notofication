package com.notificationapp.support;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.client.RestTestClient;

/**
 * Base class for integration tests: the full application on a random port, backed by
 * Testcontainers PostgreSQL. The startup admin is created from the {@code app.admin.*} properties.
 * Every test starts with only that admin in the database.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "app.admin.email=" + AbstractIntegrationTest.ADMIN_EMAIL,
                "app.admin.password=" + AbstractIntegrationTest.ADMIN_PASSWORD
        })
@Import(TestcontainersConfiguration.class)
public abstract class AbstractIntegrationTest {

    protected static final String ADMIN_EMAIL = "admin@example.com";
    protected static final String ADMIN_PASSWORD = "admin-password";
    protected static final String USER_PASSWORD = "user-password";

    @LocalServerPort
    private int port;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    /**
     * Sends requests as the startup admin.
     */
    protected RestTestClient client;

    @BeforeEach
    void setUpClientAndKeepOnlyAdmin() {
        client = clientAs(ADMIN_EMAIL, ADMIN_PASSWORD);
        jdbcTemplate.execute("DELETE FROM events");
        jdbcTemplate.execute("DELETE FROM alerts");
        jdbcTemplate.execute("DELETE FROM users WHERE role <> 'ADMIN'");
    }

    protected RestTestClient anonymousClient() {
        return RestTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
    }

    protected RestTestClient clientAs(String email, String password) {
        return RestTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .defaultHeaders(headers -> headers.setBasicAuth(email, password))
                .build();
    }

    /**
     * Signs up a user with {@link #USER_PASSWORD} and returns its id.
     */
    protected String createUser(String email, String name) {
        return createAndReturnId(anonymousClient(), "/api/v1/users", """
                {"email": "%s", "name": "%s", "password": "%s"}
                """.formatted(email, name, USER_PASSWORD));
    }

    /**
     * Creates an email alert as the admin and returns its id.
     */
    protected String createAlert(String userId, String name, String eventType) {
        return createAndReturnId(client, "/api/v1/alerts", """
                {"userId": "%s", "name": "%s", "eventType": "%s", "channel": "email"}
                """.formatted(userId, name, eventType));
    }

    private static String createAndReturnId(RestTestClient restClient, String path, String json) {
        String body = restClient.post().uri(path)
                .contentType(MediaType.APPLICATION_JSON)
                .body(json)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();
        return JsonPath.read(body, "$.id");
    }

}
