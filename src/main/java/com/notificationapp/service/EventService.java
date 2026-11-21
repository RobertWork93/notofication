package com.notificationapp.service;

import com.notificationapp.dto.CreateEventRequest;
import com.notificationapp.model.Event;
import com.notificationapp.model.OutboxRecord;
import com.notificationapp.repository.EventRepository;
import com.notificationapp.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/**
 * Stores incoming events. Each new event also gets an outbox record in the same transaction,
 * which the OutboxWorker later turns into notification deliveries.
 */
@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;
    private final OutboxRepository outboxRepository;
    private final Clock clock;

    @Transactional
    public Event create(CreateEventRequest request) {
        Instant now = Instant.now(clock);
        Event event = eventRepository.save(new Event(request.type(), request.title(), request.source(),
                request.occurredAt(), request.payload(), now));
        outboxRepository.save(new OutboxRecord(event.getId(), now));
        return event;
    }

    @Transactional(readOnly = true)
    public Event get(UUID eventId) {
        return eventRepository.findById(eventId).orElseThrow(() -> new NotFoundException("Event", eventId));
    }

}
