package com.notificationapp.event.api;

import java.util.Optional;
import java.util.UUID;

/**
 * Read access to events for other modules.
 */
public interface EventLookup {

    Optional<EventInfo> findById(UUID eventId);

}
