package com.notificationapp.notification.api;

import com.notificationapp.event.api.EventType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Port the notification module needs to find alerts. Alerts and users live in the app module,
 * which implements this interface, so the notification module never depends on the app module.
 */
public interface AlertLookup {

    /**
     * The enabled alerts for an event type, oldest first.
     */
    List<MatchedAlert> findEnabledAlerts(EventType eventType);

    Optional<MatchedAlert> findAlert(UUID alertId);

}
