package com.notificationapp.event.service;

import com.notificationapp.event.api.EventInfo;
import com.notificationapp.event.api.EventLookup;
import com.notificationapp.event.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Implements the event module's public read API.
 */
@Service
@RequiredArgsConstructor
public class EventLookupService implements EventLookup {

    private final EventRepository eventRepository;

    @Override
    @Transactional(readOnly = true)
    public Optional<EventInfo> findById(UUID eventId) {
        return eventRepository.findById(eventId)
                .map(event -> new EventInfo(event.getId(), event.getType(), event.getTitle(), event.getSource()));
    }

}
