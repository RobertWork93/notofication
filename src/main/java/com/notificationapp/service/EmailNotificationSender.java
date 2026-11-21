package com.notificationapp.service;

import com.notificationapp.model.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Email channel. Stage 2 only logs the notification; no email is sent.
 */
@Slf4j
@Component
public class EmailNotificationSender implements NotificationSender {

    @Override
    public Channel channel() {
        return Channel.EMAIL;
    }

    @Override
    public void send(NotificationMessage message) {
        log.info("Notification [email] to {} — {}", message.recipient(), message.summary());
    }

}
