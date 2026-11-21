package com.notificationapp.event.api;

import java.util.UUID;

/**
 * The parts of an event that other modules need.
 */
public record EventInfo(UUID id, EventType type, String title, String source) {
}
