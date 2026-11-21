package com.notificationapp.unit.service;

import com.notificationapp.event.api.EventCreated;
import com.notificationapp.event.api.EventType;
import com.notificationapp.event.dto.CreateEventRequest;
import com.notificationapp.event.model.Event;
import com.notificationapp.event.repository.EventRepository;
import com.notificationapp.event.service.EventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    private static final Instant OCCURRED = Instant.parse("2026-11-21T14:52:00Z");
    private static final Instant NOW = Instant.parse("2026-11-21T14:53:10Z");
    private static final UUID EVENT_ID = UUID.fromString("3f6c1a52-9a1e-4c0e-9a7e-2a1b5f0c9d11");

    @Mock
    private EventRepository eventRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    // Built by hand instead of @InjectMocks, because the service also needs a fixed Clock.
    private EventService eventService;

    @BeforeEach
    void setUp() {
        eventService = new EventService(eventRepository, eventPublisher, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void shouldStoreEventAndSetReceivedTime() {
        Map<String, Object> payload = Map.of("magnitude", 6.8, "region", "Pacific");
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Event event = eventService.create(new CreateEventRequest(
                EventType.NATURAL_DISASTER, "M6.8 earthquake", "usgs", OCCURRED, payload));

        assertThat(event.getType()).isEqualTo(EventType.NATURAL_DISASTER);
        assertThat(event.getTitle()).isEqualTo("M6.8 earthquake");
        assertThat(event.getSource()).isEqualTo("usgs");
        assertThat(event.getOccurredAt()).isEqualTo(OCCURRED);
        assertThat(event.getPayload()).isEqualTo(payload);
        assertThat(event.getReceivedAt()).isEqualTo(NOW);
    }

    @Test
    void shouldPublishEventCreatedForNewEvent() {
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> {
            Event saved = invocation.getArgument(0);
            saved.setId(EVENT_ID);
            return saved;
        });

        eventService.create(new CreateEventRequest(EventType.BREAKING_NEWS, "Election called", "reuters", OCCURRED, null));

        verify(eventPublisher).publishEvent(new EventCreated(EVENT_ID, NOW));
    }

    @Test
    void shouldStoreEventWithoutPayload() {
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Event event = eventService.create(new CreateEventRequest(
                EventType.BREAKING_NEWS, "Election called", "reuters", OCCURRED, null));

        assertThat(event.getPayload()).isNull();
        assertThat(event.getReceivedAt()).isEqualTo(NOW);
    }

    @Test
    void shouldReturnEventWhenEventExists() {
        Event existing = new Event(EventType.BREAKING_NEWS, "Election called", "reuters", OCCURRED, null, NOW);
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(existing));

        assertThat(eventService.get(EVENT_ID)).isSameAs(existing);
    }

    @Test
    void shouldThrowNotFoundWhenEventDoesNotExist() {
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.get(EVENT_ID))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

}
