package com.notificationapp.event.api;

import java.time.Instant;
import java.util.UUID;

/**
 * Published when a new event has been saved. Listeners run synchronously in the same transaction,
 * so whatever they write commits or rolls back together with the event.
 *
 * @param eventId   the saved event
 * @param createdAt when the event was received
 */
public record EventCreated(UUID eventId, Instant createdAt) {
}
