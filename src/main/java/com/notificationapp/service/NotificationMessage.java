package com.notificationapp.service;

import com.notificationapp.model.Channel;
import com.notificationapp.model.EventType;

/**
 * Everything a sender needs to notify one user about one event.
 */
public record NotificationMessage(
        Channel channel,
        String recipient,
        String alertName,
        EventType eventType,
        String eventTitle,
        String eventSource
) {

    /**
     * One-line description, for example {@code alert "Earthquakes": Natural disaster: M6.8 earthquake (source: usgs)}.
     */
    public String summary() {
        return "alert \"%s\": %s: %s (source: %s)".formatted(alertName, eventType.getLabel(), eventTitle, eventSource);
    }

}
