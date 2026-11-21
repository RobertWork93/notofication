package com.notificationapp.event.dto;

import com.notificationapp.event.api.EventType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.Map;

/**
 * Body of {@code POST /api/v1/events}. {@code payload} is optional and must be a JSON object.
 */
public record CreateEventRequest(
        @NotNull EventType type,
        @NotBlank @Size(max = 200) String title,
        @NotBlank @Size(max = 100) String source,
        @NotNull Instant occurredAt,
        Map<String, Object> payload
) {
}
