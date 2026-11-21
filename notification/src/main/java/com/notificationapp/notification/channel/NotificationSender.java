package com.notificationapp.notification.channel;

import com.notificationapp.notification.api.Channel;

/**
 * Sends notifications through one channel. Adding a channel means adding one implementation (NFR-02).
 * A sender signals failure by throwing; the delivery is then marked FAILED.
 */
public interface NotificationSender {

    Channel channel();

    void send(NotificationMessage message);

}
