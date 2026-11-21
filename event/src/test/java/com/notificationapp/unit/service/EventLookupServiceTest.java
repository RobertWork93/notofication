package com.notificationapp.unit.service;

import com.notificationapp.event.api.EventInfo;
import com.notificationapp.event.api.EventType;
import com.notificationapp.event.model.Event;
import com.notificationapp.event.repository.EventRepository;
import com.notificationapp.event.service.EventLookupService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventLookupServiceTest {

    private static final UUID EVENT_ID = UUID.fromString("3f6c1a52-9a1e-4c0e-9a7e-2a1b5f0c9d11");
    private static final Instant NOW = Instant.parse("2026-11-21T14:53:10Z");

    @Mock
    private EventRepository eventRepository;

    @InjectMocks
    private EventLookupService eventLookupService;

    @Test
    void shouldExposeTheEventFieldsOtherModulesNeed() {
        Event event = new Event(EventType.NATURAL_DISASTER, "M6.8 earthquake", "usgs", NOW, null, NOW);
        event.setId(EVENT_ID);
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event));

        assertThat(eventLookupService.findById(EVENT_ID))
                .contains(new EventInfo(EVENT_ID, EventType.NATURAL_DISASTER, "M6.8 earthquake", "usgs"));
    }

    @Test
    void shouldReturnEmptyWhenEventDoesNotExist() {
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.empty());

        assertThat(eventLookupService.findById(EVENT_ID)).isEmpty();
    }

}
