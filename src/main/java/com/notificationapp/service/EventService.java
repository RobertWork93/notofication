package com.notificationapp.service;

import com.notificationapp.dto.CreateEventRequest;
import com.notificationapp.model.Event;
import com.notificationapp.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/**
 * Stores incoming events. Matching events to alerts and sending notifications are not part of this stage.
 */
@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;
    private final Clock clock;

    @Transactional
    public Event create(CreateEventRequest request) {
        Event event = new Event(request.type(), request.title(), request.source(), request.occurredAt(),
                request.payload(), Instant.now(clock));
        return eventRepository.save(event);
    }

    @Transactional(readOnly = true)
    public Event get(UUID eventId) {
        return eventRepository.findById(eventId).orElseThrow(() -> new NotFoundException("Event", eventId));
    }

}
