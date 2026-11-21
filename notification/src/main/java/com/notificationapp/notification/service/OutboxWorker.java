package com.notificationapp.notification.service;

import com.notificationapp.notification.configuration.NotificationProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Polls the outbox and processes pending records, up to the batch size per run.
 * Several instances can run at once; the processor's claim query keeps them apart.
 */
@Component
@RequiredArgsConstructor
public class OutboxWorker {

    private final OutboxProcessor outboxProcessor;
    private final NotificationProperties properties;

    @Scheduled(fixedDelayString = "${app.notifications.outbox-poll-interval-ms:2000}")
    public void poll() {
        int processed = 0;
        while (processed < properties.batchSize() && outboxProcessor.processNext()) {
            processed++;
        }
    }

}
