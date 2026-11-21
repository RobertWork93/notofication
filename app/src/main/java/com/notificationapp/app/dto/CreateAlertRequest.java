package com.notificationapp.app.dto;

import com.notificationapp.notification.api.Channel;
import com.notificationapp.event.api.EventType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Body of {@code POST /api/v1/alerts}. {@code enabled} is optional and defaults to {@code true}.
 */
public record CreateAlertRequest(
        @NotNull UUID userId,
        @NotBlank @Size(max = 100) String name,
        @NotNull EventType eventType,
        @NotNull Channel channel,
        Boolean enabled
) {
}
