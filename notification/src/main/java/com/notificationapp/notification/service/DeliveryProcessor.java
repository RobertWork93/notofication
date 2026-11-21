package com.notificationapp.notification.service;

import com.notificationapp.event.api.EventInfo;
import com.notificationapp.event.api.EventLookup;
import com.notificationapp.notification.api.AlertLookup;
import com.notificationapp.notification.api.MatchedAlert;
import com.notificationapp.notification.channel.NotificationMessage;
import com.notificationapp.notification.channel.NotificationSender;
import com.notificationapp.notification.model.NotificationDelivery;
import com.notificationapp.notification.repository.NotificationDeliveryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Sends one pending delivery job through the sender of its channel and marks it SENT,
 * all in one transaction. If sending fails, the job is marked FAILED in a second transaction.
 * Delivery is at least once: a crash after sending but before the commit sends the job again.
 * Between a failure's rollback and the FAILED update the job is briefly PENDING and unlocked again,
 * so another worker may attempt it once more. The sender is called while the row is locked; real senders
 * must use short timeouts, or move the call outside the lock.
 */
@Slf4j
@Service
public class DeliveryProcessor {

    private final NotificationDeliveryRepository deliveryRepository;
    private final EventLookup eventLookup;
    private final AlertLookup alertLookup;
    private final List<NotificationSender> senders;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;

    public DeliveryProcessor(NotificationDeliveryRepository deliveryRepository, EventLookup eventLookup,
                             AlertLookup alertLookup, List<NotificationSender> senders,
                             PlatformTransactionManager transactionManager, Clock clock) {
        this.deliveryRepository = deliveryRepository;
        this.eventLookup = eventLookup;
        this.alertLookup = alertLookup;
        this.senders = senders;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.clock = clock;
    }

    /**
     * Sends the oldest pending job that no other worker has claimed.
     *
     * @return false when no job was waiting, true when one was sent or marked FAILED
     */
    public boolean processNext() {
        AtomicReference<UUID> claimedId = new AtomicReference<>();
        try {
            Boolean processed = transactionTemplate.execute(status -> {
                Optional<NotificationDelivery> claimed = deliveryRepository.claimNext();
                if (claimed.isEmpty()) {
                    return false;
                }
                claimedId.set(claimed.get().getId());
                deliver(claimed.get());
                return true;
            });
            return Boolean.TRUE.equals(processed);
        } catch (RuntimeException failure) {
            UUID deliveryId = claimedId.get();
            if (deliveryId == null) {
                throw failure;
            }
            log.warn("Notification delivery {} failed and is marked FAILED", deliveryId, failure);
            transactionTemplate.executeWithoutResult(
                    status -> deliveryRepository.markFailed(deliveryId, FailureMessages.of(failure)));
            return true;
        }
    }

    private void deliver(NotificationDelivery delivery) {
        EventInfo event = eventLookup.findById(delivery.getEventId())
                .orElseThrow(() -> new IllegalStateException("Event " + delivery.getEventId() + " not found"));
        MatchedAlert alert = alertLookup.findAlert(delivery.getAlertId())
                .orElseThrow(() -> new IllegalStateException("Alert " + delivery.getAlertId() + " not found"));
        senderFor(delivery).send(new NotificationMessage(delivery.getChannel(), delivery.getRecipient(),
                alert.name(), event.type(), event.title(), event.source()));
        delivery.markSent(Instant.now(clock));
    }

    private NotificationSender senderFor(NotificationDelivery delivery) {
        return senders.stream()
                .filter(sender -> sender.channel() == delivery.getChannel())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No sender for channel " + delivery.getChannel().getValue()));
    }

}
