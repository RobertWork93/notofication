package com.notificationapp.web.controller;

import com.notificationapp.configuration.SecurityConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import com.notificationapp.controller.EventController;
import com.notificationapp.dto.CreateEventRequest;
import com.notificationapp.model.EventType;
import com.notificationapp.service.EventService;
import com.notificationapp.service.NotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Map;

import static com.notificationapp.support.TestDataFactory.EVENT_ID;
import static com.notificationapp.support.TestDataFactory.event;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EventController.class)
@Import(SecurityConfiguration.class)
@WithMockUser(roles = "ADMIN")
class EventControllerTest {

    private static final Instant OCCURRED = Instant.parse("2026-11-21T14:52:00Z");
    private static final Instant RECEIVED = Instant.parse("2026-11-21T14:53:10Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EventService eventService;

    @Test
    void shouldReturnCreatedEventWithLocation() throws Exception {
        Map<String, Object> payload = Map.of("region", "Pacific");
        when(eventService.create(new CreateEventRequest(
                EventType.NATURAL_DISASTER, "M6.8 earthquake", "usgs", OCCURRED, payload)))
                .thenReturn(event(EventType.NATURAL_DISASTER, "M6.8 earthquake", "usgs", OCCURRED, payload, RECEIVED));

        mockMvc.perform(post("/api/v1/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type": "natural_disaster", "title": "M6.8 earthquake", "source": "usgs",
                                 "occurredAt": "2026-11-21T14:52:00Z", "payload": {"region": "Pacific"}}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/events/" + EVENT_ID))
                .andExpect(jsonPath("$.id").value(EVENT_ID.toString()))
                .andExpect(jsonPath("$.type").value("natural_disaster"))
                .andExpect(jsonPath("$.occurredAt").value("2026-11-21T14:52:00Z"))
                .andExpect(jsonPath("$.payload.region").value("Pacific"))
                .andExpect(jsonPath("$.receivedAt").value("2026-11-21T14:53:10Z"));
    }

    @Test
    void shouldRejectEventWithMissingFields() throws Exception {
        mockMvc.perform(post("/api/v1/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        verify(eventService, never()).create(any());
    }

    @Test
    void shouldRejectUnknownEventType() throws Exception {
        mockMvc.perform(post("/api/v1/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type": "market_movement", "title": "Stocks fall", "source": "nyse",
                                 "occurredAt": "2026-11-21T14:52:00Z"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        verify(eventService, never()).create(any());
    }

    @Test
    void shouldRejectPayloadThatIsNotAnObject() throws Exception {
        mockMvc.perform(post("/api/v1/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type": "breaking_news", "title": "Election called", "source": "reuters",
                                 "occurredAt": "2026-11-21T14:52:00Z", "payload": [1, 2]}
                                """))
                .andExpect(status().isBadRequest());
        verify(eventService, never()).create(any());
    }

    @Test
    void shouldRejectTitleLongerThan200Characters() throws Exception {
        mockMvc.perform(post("/api/v1/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type": "breaking_news", "title": "%s", "source": "reuters",
                                 "occurredAt": "2026-11-21T14:52:00Z"}
                                """.formatted("x".repeat(201))))
                .andExpect(status().isBadRequest());
        verify(eventService, never()).create(any());
    }

    @Test
    void shouldReturnEventWhenEventExists() throws Exception {
        when(eventService.get(EVENT_ID))
                .thenReturn(event(EventType.BREAKING_NEWS, "Election called", "reuters", OCCURRED, null, RECEIVED));

        mockMvc.perform(get("/api/v1/events/{eventId}", EVENT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("breaking_news"));
    }

    @Test
    void shouldReturnNotFoundWhenEventDoesNotExist() throws Exception {
        when(eventService.get(EVENT_ID)).thenThrow(new NotFoundException("Event", EVENT_ID));

        mockMvc.perform(get("/api/v1/events/{eventId}", EVENT_ID))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

}
