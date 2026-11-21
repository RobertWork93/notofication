package com.notificationapp.notification.service;

import com.notificationapp.notification.configuration.NotificationProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Polls the delivery jobs and sends pending ones, up to the batch size per run.
 * Several instances can run at once; the processor's claim query keeps them apart.
 */
@Component
@RequiredArgsConstructor
public class DeliveryWorker {

    private final DeliveryProcessor deliveryProcessor;
    private final NotificationProperties properties;

    @Scheduled(fixedDelayString = "${app.notifications.delivery-poll-interval-ms:2000}")
    public void poll() {
        int processed = 0;
        while (processed < properties.batchSize() && deliveryProcessor.processNext()) {
            processed++;
        }
    }

}
