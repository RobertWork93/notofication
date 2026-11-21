package com.notificationapp.app.dto;

import com.notificationapp.app.model.Role;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(UUID id,
                           String email,
                           String name,
                           Role role,
                           Instant createdAt,
                           Instant updatedAt) {
}
