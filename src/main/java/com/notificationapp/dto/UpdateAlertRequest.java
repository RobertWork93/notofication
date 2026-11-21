package com.notificationapp.dto;

import com.notificationapp.model.Channel;
import com.notificationapp.model.EventType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Body of {@code PUT /api/v1/alerts/{alertId}/users/{userId}}. Replaces the whole alert, so every field is required.
 */
public record UpdateAlertRequest(
        @NotBlank @Size(max = 100) String name,
        @NotNull EventType eventType,
        @NotNull Channel channel,
        @NotNull Boolean enabled
) {
}
