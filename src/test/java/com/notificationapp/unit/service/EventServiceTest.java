package com.notificationapp.unit.service;

import com.notificationapp.dto.CreateEventRequest;
import com.notificationapp.model.Event;
import com.notificationapp.model.EventType;
import com.notificationapp.repository.EventRepository;
import com.notificationapp.service.EventService;
import com.notificationapp.service.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;

import static com.notificationapp.support.TestDataFactory.EVENT_ID;
import static com.notificationapp.support.TestDataFactory.event;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    private static final Instant OCCURRED = Instant.parse("2026-11-21T14:52:00Z");
    private static final Instant NOW = Instant.parse("2026-11-21T14:53:10Z");

    @Mock
    private EventRepository eventRepository;

    // Built by hand instead of @InjectMocks, because the service also needs a fixed Clock.
    private EventService eventService;

    @BeforeEach
    void setUp() {
        eventService = new EventService(eventRepository, Clock.fixed(NOW, ZoneOffset.UTC));
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
    void shouldStoreEventWithoutPayload() {
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Event event = eventService.create(new CreateEventRequest(
                EventType.BREAKING_NEWS, "Election called", "reuters", OCCURRED, null));

        assertThat(event.getPayload()).isNull();
        assertThat(event.getReceivedAt()).isEqualTo(NOW);
    }

    @Test
    void shouldReturnEventWhenEventExists() {
        Event existing = event(EventType.BREAKING_NEWS, "Election called", "reuters", OCCURRED, null, NOW);
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(existing));

        assertThat(eventService.get(EVENT_ID)).isSameAs(existing);
    }

    @Test
    void shouldThrowNotFoundWhenEventDoesNotExist() {
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.get(EVENT_ID)).isInstanceOf(NotFoundException.class);
    }

}
