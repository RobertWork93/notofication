package com.notificationapp.dto;

import com.notificationapp.model.Channel;
import com.notificationapp.model.EventType;

import java.time.Instant;
import java.util.UUID;

public record AlertResponse(
        UUID id,
        UUID userId,
        String name,
        EventType eventType,
        Channel channel,
        boolean enabled,
        Instant createdAt,
        Instant updatedAt
) {
}
