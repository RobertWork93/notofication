package com.notificationapp.notification.channel;

import com.notificationapp.notification.api.Channel;
import com.notificationapp.event.api.EventType;

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
