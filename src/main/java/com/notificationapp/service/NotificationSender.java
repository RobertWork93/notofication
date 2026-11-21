package com.notificationapp.service;

import com.notificationapp.model.Channel;

/**
 * Sends notifications through one channel. Adding a channel means adding one implementation (NFR-02).
 * A sender signals failure by throwing; the delivery is then marked FAILED.
 */
public interface NotificationSender {

    Channel channel();

    void send(NotificationMessage message);

}
