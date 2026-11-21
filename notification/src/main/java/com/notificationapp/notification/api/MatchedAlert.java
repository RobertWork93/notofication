package com.notificationapp.notification.api;

import java.util.UUID;

/**
 * An alert as the notification module sees it: who to notify, and how.
 *
 * @param alertId        the alert
 * @param name           the alert's name, shown in the notification
 * @param userId         the alert's owner
 * @param recipientEmail the owner's email address
 * @param channel        where to send the notification
 */
public record MatchedAlert(UUID alertId, String name, UUID userId, String recipientEmail, Channel channel) {
}
