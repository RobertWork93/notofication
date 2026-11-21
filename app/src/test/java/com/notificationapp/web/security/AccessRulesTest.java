package com.notificationapp.web.security;

import com.notificationapp.app.configuration.SecurityConfiguration;
import com.notificationapp.app.controller.AlertController;
import com.notificationapp.event.controller.EventController;
import com.notificationapp.app.controller.HelloController;
import com.notificationapp.app.controller.UserController;
import com.notificationapp.app.model.Alert;
import com.notificationapp.notification.api.Channel;
import com.notificationapp.event.api.EventType;
import com.notificationapp.app.model.User;
import com.notificationapp.app.service.AlertService;
import com.notificationapp.event.service.EventService;
import com.notificationapp.app.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static com.notificationapp.support.SecurityTestSupport.as;
import static com.notificationapp.support.TestDataFactory.ALERT_ID;
import static com.notificationapp.support.TestDataFactory.OTHER_USER_ID;
import static com.notificationapp.support.TestDataFactory.USER_ID;
import static com.notificationapp.support.TestDataFactory.admin;
import static com.notificationapp.support.TestDataFactory.earthquakeAlert;
import static com.notificationapp.support.TestDataFactory.event;
import static com.notificationapp.support.TestDataFactory.jane;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The access rules from {@code docs/stage-1/auth-implementation-plan.md}, section 3.
 */
@WebMvcTest({UserController.class, AlertController.class, EventController.class, HelloController.class})
@Import(SecurityConfiguration.class)
class AccessRulesTest {

    private static final Instant NOW = Instant.parse("2026-11-21T15:00:00Z");
    private static final User JANE = jane(NOW);
    private static final User ADMIN = admin(NOW);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private AlertService alertService;

    @MockitoBean
    private EventService eventService;

    @Test
    void shouldAllowAnonymousHello() throws Exception {
        mockMvc.perform(get("/hello"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldAllowAnonymousSignUp() throws Exception {
        when(userService.create(any())).thenReturn(JANE);

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "jane.doe@example.com", "name": "Jane Doe", "password": "secret123"}
                                """))
                .andExpect(status().isCreated());
    }

    @Test
    void shouldRequireCredentialsForEveryOtherEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/users/{userId}", USER_ID))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, org.hamcrest.Matchers.startsWith("Basic")));
        mockMvc.perform(get("/api/v1/alerts").param("userId", USER_ID.toString()))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/events/{eventId}", ALERT_ID))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldForbidUserFromListingUsers() throws Exception {
        mockMvc.perform(get("/api/v1/users").with(as(JANE)))
                .andExpect(status().isForbidden());
        verify(userService, never()).list(anyInt(), anyInt());
    }

    @Test
    void shouldAllowAdminToListUsers() throws Exception {
        when(userService.list(0, 20)).thenReturn(Page.empty());

        mockMvc.perform(get("/api/v1/users").with(as(ADMIN)))
                .andExpect(status().isOk());
    }

    @Test
    void shouldReturnLoggedInUserFromMe() throws Exception {
        when(userService.get(USER_ID)).thenReturn(JANE);

        mockMvc.perform(get("/api/v1/users/me").with(as(JANE)))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.id")
                        .value(USER_ID.toString()))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.role")
                        .value("USER"));
    }

    @Test
    void shouldRequireCredentialsForMe() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldNotSendBasicChallengeToBrowserScripts() throws Exception {
        mockMvc.perform(get("/api/v1/users/me").header("X-Requested-With", "XMLHttpRequest"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist(HttpHeaders.WWW_AUTHENTICATE));
    }

    @Test
    void shouldAllowUserToReadOwnUser() throws Exception {
        when(userService.get(USER_ID)).thenReturn(JANE);

        mockMvc.perform(get("/api/v1/users/{userId}", USER_ID).with(as(JANE)))
                .andExpect(status().isOk());
    }

    @Test
    void shouldForbidUserFromReadingOrDeletingAnotherUser() throws Exception {
        mockMvc.perform(get("/api/v1/users/{userId}", OTHER_USER_ID).with(as(JANE)))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/v1/users/{userId}", OTHER_USER_ID).with(as(JANE)))
                .andExpect(status().isForbidden());
        verify(userService, never()).get(any());
        verify(userService, never()).delete(any());
    }

    @Test
    void shouldAllowAdminToReadAnyUser() throws Exception {
        when(userService.get(USER_ID)).thenReturn(JANE);

        mockMvc.perform(get("/api/v1/users/{userId}", USER_ID).with(as(ADMIN)))
                .andExpect(status().isOk());
    }

    @Test
    void shouldAllowUserToCreateOwnAlert() throws Exception {
        when(alertService.create(any())).thenReturn(earthquakeAlert(NOW));

        mockMvc.perform(post("/api/v1/alerts").with(as(JANE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(alertBody(USER_ID.toString())))
                .andExpect(status().isCreated());
    }

    @Test
    void shouldForbidUserFromCreatingAlertForAnotherUser() throws Exception {
        mockMvc.perform(post("/api/v1/alerts").with(as(JANE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(alertBody(OTHER_USER_ID.toString())))
                .andExpect(status().isForbidden());
        verify(alertService, never()).create(any());
    }

    @Test
    void shouldAllowAdminToCreateAlertForAnyUser() throws Exception {
        when(alertService.create(any())).thenReturn(earthquakeAlert(NOW));

        mockMvc.perform(post("/api/v1/alerts").with(as(ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(alertBody(USER_ID.toString())))
                .andExpect(status().isCreated());
    }

    @Test
    void shouldAllowUserToListOwnAlertsButNotAnotherUsers() throws Exception {
        when(alertService.listForUser(USER_ID, 0, 20)).thenReturn(Page.empty());

        mockMvc.perform(get("/api/v1/alerts").param("userId", USER_ID.toString()).with(as(JANE)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/alerts").param("userId", OTHER_USER_ID.toString()).with(as(JANE)))
                .andExpect(status().isForbidden());
        verify(alertService, never()).listForUser(eq(OTHER_USER_ID), anyInt(), anyInt());
    }

    @Test
    void shouldForbidUserFromReadingUpdatingOrDeletingAnotherUsersAlert() throws Exception {
        mockMvc.perform(get("/api/v1/alerts/{alertId}/users/{userId}", ALERT_ID, OTHER_USER_ID).with(as(JANE)))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/v1/alerts/{alertId}/users/{userId}", ALERT_ID, OTHER_USER_ID).with(as(JANE)))
                .andExpect(status().isForbidden());
        verify(alertService, never()).get(any(), any());
        verify(alertService, never()).delete(any(), any());
    }

    @Test
    void shouldAllowUserToReadOwnAlert() throws Exception {
        Alert alert = earthquakeAlert(NOW);
        when(alertService.get(ALERT_ID, USER_ID)).thenReturn(alert);

        mockMvc.perform(get("/api/v1/alerts/{alertId}/users/{userId}", ALERT_ID, USER_ID).with(as(JANE)))
                .andExpect(status().isOk());
    }

    @Test
    void shouldRequireCredentialsForWritesAndUserListing() throws Exception {
        mockMvc.perform(post("/api/v1/alerts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(alertBody(USER_ID.toString())))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventBody()))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldForbidUserFromUpdatingAnotherUserOrTheirAlert() throws Exception {
        mockMvc.perform(put("/api/v1/users/{userId}", OTHER_USER_ID).with(as(JANE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "john.doe@example.com", "name": "John Doe", "password": "secret123"}
                                """))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/v1/alerts/{alertId}/users/{userId}", ALERT_ID, OTHER_USER_ID).with(as(JANE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateAlertBody()))
                .andExpect(status().isForbidden());
        verify(userService, never()).update(any(), any());
        verify(alertService, never()).update(any(), any(), any());
    }

    @Test
    void shouldAllowAdminToReadUpdateAndDeleteAnotherUsersAlert() throws Exception {
        when(alertService.get(ALERT_ID, USER_ID)).thenReturn(earthquakeAlert(NOW));
        when(alertService.update(eq(ALERT_ID), eq(USER_ID), any())).thenReturn(earthquakeAlert(NOW));

        mockMvc.perform(get("/api/v1/alerts/{alertId}/users/{userId}", ALERT_ID, USER_ID).with(as(ADMIN)))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/v1/alerts/{alertId}/users/{userId}", ALERT_ID, USER_ID).with(as(ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateAlertBody()))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/api/v1/alerts/{alertId}/users/{userId}", ALERT_ID, USER_ID).with(as(ADMIN)))
                .andExpect(status().isNoContent());
        verify(alertService).delete(ALERT_ID, USER_ID);
    }

    @Test
    void shouldAllowAdminToUpdateAndDeleteAnotherUser() throws Exception {
        when(userService.update(eq(USER_ID), any())).thenReturn(JANE);

        mockMvc.perform(put("/api/v1/users/{userId}", USER_ID).with(as(ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "jane.doe@example.com", "name": "Jane Doe", "password": "secret123"}
                                """))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/api/v1/users/{userId}", USER_ID).with(as(ADMIN)))
                .andExpect(status().isNoContent());
        verify(userService).delete(USER_ID);
    }

    @Test
    void shouldForbidUserFromCreatingEvents() throws Exception {
        mockMvc.perform(post("/api/v1/events").with(as(JANE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventBody()))
                .andExpect(status().isForbidden());
        verify(eventService, never()).create(any());
    }

    @Test
    void shouldAllowAdminToCreateEvents() throws Exception {
        when(eventService.create(any())).thenReturn(event(EventType.BREAKING_NEWS, "Election called", "reuters",
                NOW, null, NOW));

        mockMvc.perform(post("/api/v1/events").with(as(ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventBody()))
                .andExpect(status().isCreated());
    }

    @Test
    void shouldAllowUserToReadEvents() throws Exception {
        when(eventService.get(any())).thenReturn(event(EventType.BREAKING_NEWS, "Election called", "reuters",
                NOW, null, NOW));

        mockMvc.perform(get("/api/v1/events/{eventId}", ALERT_ID).with(as(JANE)))
                .andExpect(status().isOk());
    }

    private static String alertBody(String userId) {
        return """
                {"userId": "%s", "name": "Earthquakes", "eventType": "%s", "channel": "%s"}
                """.formatted(userId, EventType.NATURAL_DISASTER.getValue(), Channel.EMAIL.getValue());
    }

    private static String updateAlertBody() {
        return """
                {"name": "News", "eventType": "breaking_news", "channel": "email", "enabled": true}
                """;
    }

    private static String eventBody() {
        return """
                {"type": "breaking_news", "title": "Election called", "source": "reuters",
                 "occurredAt": "2026-11-21T14:52:00Z"}
                """;
    }

}
