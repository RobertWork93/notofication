package com.notificationapp.event.service;

import com.notificationapp.event.api.EventCreated;
import com.notificationapp.event.dto.CreateEventRequest;
import com.notificationapp.event.model.Event;
import com.notificationapp.event.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/**
 * Stores incoming events and publishes {@link EventCreated} in the same transaction.
 * The notification module listens to it and writes its outbox record, without this module knowing about it.
 */
@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    @Transactional
    public Event create(CreateEventRequest request) {
        Instant now = Instant.now(clock);
        Event event = eventRepository.save(new Event(request.type(), request.title(), request.source(),
                request.occurredAt(), request.payload(), now));
        eventPublisher.publishEvent(new EventCreated(event.getId(), now));
        return event;
    }

    @Transactional(readOnly = true)
    public Event get(UUID eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event " + eventId + " not found"));
    }

}
