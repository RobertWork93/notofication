package com.notificationapp.web.controller;

import org.springframework.http.HttpStatus;
import com.notificationapp.app.model.Alert;
import com.notificationapp.app.model.User;
import com.notificationapp.app.configuration.SecurityConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import com.notificationapp.app.controller.AlertController;
import com.notificationapp.app.dto.CreateAlertRequest;
import com.notificationapp.app.dto.UpdateAlertRequest;
import com.notificationapp.notification.api.Channel;
import com.notificationapp.event.api.EventType;
import com.notificationapp.app.service.AlertService;
import org.springframework.web.server.ResponseStatusException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static com.notificationapp.support.TestDataFactory.ALERT_ID;
import static com.notificationapp.support.TestDataFactory.USER_ID;
import static com.notificationapp.support.TestDataFactory.alert;
import static com.notificationapp.support.TestDataFactory.earthquakeAlert;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AlertController.class)
@Import(SecurityConfiguration.class)
@WithMockUser(roles = "ADMIN")
class AlertControllerTest {

    private static final Instant NOW = Instant.parse("2026-11-21T15:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AlertService alertService;

    @Test
    void shouldReturnCreatedAlertWithLocation() throws Exception {
        when(alertService.create(new CreateAlertRequest(
                USER_ID, "Earthquakes", EventType.NATURAL_DISASTER, Channel.EMAIL, null)))
                .thenReturn(earthquakeAlert(NOW));

        mockMvc.perform(post("/api/v1/alerts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId": "%s", "name": "Earthquakes",
                                 "eventType": "natural_disaster", "channel": "email"}
                                """.formatted(USER_ID)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/alerts/" + ALERT_ID + "/users/" + USER_ID))
                .andExpect(jsonPath("$.id").value(ALERT_ID.toString()))
                .andExpect(jsonPath("$.userId").value(USER_ID.toString()))
                .andExpect(jsonPath("$.eventType").value("natural_disaster"))
                .andExpect(jsonPath("$.channel").value("email"))
                .andExpect(jsonPath("$.enabled").value(true));
    }

    @Test
    void shouldRejectAlertWithMissingFields() throws Exception {
        mockMvc.perform(post("/api/v1/alerts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        verify(alertService, never()).create(any());
    }

    @Test
    void shouldRejectUnknownEventType() throws Exception {
        mockMvc.perform(post("/api/v1/alerts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId": "%s", "name": "Markets",
                                 "eventType": "market_movement", "channel": "email"}
                                """.formatted(USER_ID)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        verify(alertService, never()).create(any());
    }

    @Test
    void shouldRejectAlertNameLongerThan100Characters() throws Exception {
        mockMvc.perform(post("/api/v1/alerts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId": "%s", "name": "%s",
                                 "eventType": "breaking_news", "channel": "email"}
                                """.formatted(USER_ID, "x".repeat(101))))
                .andExpect(status().isBadRequest());
        verify(alertService, never()).create(any());
    }

    @Test
    void shouldReturnNotFoundWhenCreatingAlertForMissingUser() throws Exception {
        when(alertService.create(any())).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "User " + USER_ID + " not found"));

        mockMvc.perform(post("/api/v1/alerts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId": "%s", "name": "News",
                                 "eventType": "breaking_news", "channel": "slack"}
                                """.formatted(USER_ID)))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldListUsersAlertsWithDefaultPaging() throws Exception {
        when(alertService.listForUser(USER_ID, 0, 20))
                .thenReturn(new PageImpl<>(List.of(earthquakeAlert(NOW)), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/v1/alerts").param("userId", USER_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(ALERT_ID.toString()))
                .andExpect(jsonPath("$.page.size").value(20))
                .andExpect(jsonPath("$.page.totalElements").value(1));
    }

    @Test
    void shouldRequireUserIdWhenListingAlerts() throws Exception {
        mockMvc.perform(get("/api/v1/alerts"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void shouldRejectMalformedUserIdWhenListingAlerts() throws Exception {
        mockMvc.perform(get("/api/v1/alerts").param("userId", "not-a-uuid"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectInvalidPagingWhenListingAlerts() throws Exception {
        mockMvc.perform(get("/api/v1/alerts").param("userId", USER_ID.toString()).param("page", "-1"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/alerts").param("userId", USER_ID.toString()).param("size", "0"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/alerts").param("userId", USER_ID.toString()).param("size", "101"))
                .andExpect(status().isBadRequest());
        verify(alertService, never()).listForUser(any(), anyInt(), anyInt());
    }

    @Test
    void shouldReturnNotFoundWhenListingAlertsOfMissingUser() throws Exception {
        when(alertService.listForUser(USER_ID, 0, 20)).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "User " + USER_ID + " not found"));

        mockMvc.perform(get("/api/v1/alerts").param("userId", USER_ID.toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnAlertWhenOwnedByUser() throws Exception {
        when(alertService.get(ALERT_ID, USER_ID)).thenReturn(earthquakeAlert(NOW));

        mockMvc.perform(get("/api/v1/alerts/{alertId}/users/{userId}", ALERT_ID, USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Earthquakes"));
    }

    @Test
    void shouldReturnNotFoundWhenAlertIsNotOwnedByUser() throws Exception {
        when(alertService.get(ALERT_ID, USER_ID)).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Alert " + ALERT_ID + " not found"));

        mockMvc.perform(get("/api/v1/alerts/{alertId}/users/{userId}", ALERT_ID, USER_ID))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void shouldReturnUpdatedAlert() throws Exception {
        when(alertService.update(ALERT_ID, USER_ID,
                new UpdateAlertRequest("News", EventType.BREAKING_NEWS, Channel.SLACK, false)))
                .thenReturn(alert("News", EventType.BREAKING_NEWS, Channel.SLACK, false, NOW));

        mockMvc.perform(put("/api/v1/alerts/{alertId}/users/{userId}", ALERT_ID, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "News", "eventType": "breaking_news",
                                 "channel": "slack", "enabled": false}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false))
                .andExpect(jsonPath("$.channel").value("slack"));
    }

    @Test
    void shouldRequireEnabledWhenUpdatingAlert() throws Exception {
        mockMvc.perform(put("/api/v1/alerts/{alertId}/users/{userId}", ALERT_ID, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "News", "eventType": "breaking_news", "channel": "slack"}
                                """))
                .andExpect(status().isBadRequest());
        verify(alertService, never()).update(any(), any(), any());
    }

    @Test
    void shouldReturnNoContentWhenDeletingAlert() throws Exception {
        mockMvc.perform(delete("/api/v1/alerts/{alertId}/users/{userId}", ALERT_ID, USER_ID))
                .andExpect(status().isNoContent());

        verify(alertService).delete(ALERT_ID, USER_ID);
    }

    @Test
    void shouldReturnNotFoundWhenDeletingAlertNotOwnedByUser() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Alert " + ALERT_ID + " not found")).when(alertService).delete(ALERT_ID, USER_ID);

        mockMvc.perform(delete("/api/v1/alerts/{alertId}/users/{userId}", ALERT_ID, USER_ID))
                .andExpect(status().isNotFound());
    }

}
