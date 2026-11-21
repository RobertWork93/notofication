package com.notificationapp.app.dto;

import com.notificationapp.notification.api.Channel;
import com.notificationapp.event.api.EventType;

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
