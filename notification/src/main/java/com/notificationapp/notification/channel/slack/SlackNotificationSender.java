package com.notificationapp.notification.channel.slack;

import com.notificationapp.notification.api.Channel;
import com.notificationapp.notification.channel.NotificationMessage;
import com.notificationapp.notification.channel.NotificationSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Slack channel. Stage 2 only logs the notification; no Slack message is sent.
 */
@Slf4j
@Component
public class SlackNotificationSender implements NotificationSender {

    @Override
    public Channel channel() {
        return Channel.SLACK;
    }

    @Override
    public void send(NotificationMessage message) {
        log.info("Notification [slack] to {} — {}", message.recipient(), message.summary());
    }

}
