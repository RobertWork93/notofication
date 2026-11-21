package com.notificationapp.dto;

import com.notificationapp.model.EventType;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record EventResponse(
        UUID id,
        EventType type,
        String title,
        String source,
        Instant occurredAt,
        Map<String, Object> payload,
        Instant receivedAt
) {
}
