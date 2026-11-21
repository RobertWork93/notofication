package com.notificationapp.controller;

import com.notificationapp.dto.CreateEventRequest;
import com.notificationapp.dto.EventResponse;
import com.notificationapp.model.Event;
import com.notificationapp.service.EventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    @PostMapping
    public ResponseEntity<EventResponse> create(@Valid @RequestBody CreateEventRequest request) {
        EventResponse event = toResponse(eventService.create(request));
        return ResponseEntity.created(URI.create("/api/v1/events/" + event.id())).body(event);
    }

    @GetMapping("/{eventId}")
    public EventResponse get(@PathVariable UUID eventId) {
        return toResponse(eventService.get(eventId));
    }

    private static EventResponse toResponse(Event event) {
        return new EventResponse(event.getId(), event.getType(), event.getTitle(), event.getSource(),
                event.getOccurredAt(), event.getPayload(), event.getReceivedAt());
    }

}
