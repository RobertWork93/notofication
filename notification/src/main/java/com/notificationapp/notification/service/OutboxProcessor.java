package com.notificationapp.notification.service;

import com.notificationapp.event.api.EventInfo;
import com.notificationapp.event.api.EventLookup;
import com.notificationapp.notification.api.AlertLookup;
import com.notificationapp.notification.api.MatchedAlert;
import com.notificationapp.notification.model.OutboxRecord;
import com.notificationapp.notification.repository.NotificationDeliveryRepository;
import com.notificationapp.notification.repository.OutboxRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Turns one pending outbox record into notification delivery jobs:
 * claim the record, load its event, find the matching alerts, create one job per alert, mark it processed.
 * All of this runs in one transaction. If it fails, the record is marked FAILED in a second transaction.
 * Between the rollback and that second transaction the record is briefly PENDING and unlocked again,
 * so another worker may try it once more before it is marked FAILED.
 * Events and alerts are read through the event module's {@link EventLookup} and the {@link AlertLookup} port.
 */
@Slf4j
@Service
public class OutboxProcessor {

    private final OutboxRepository outboxRepository;
    private final NotificationDeliveryRepository deliveryRepository;
    private final EventLookup eventLookup;
    private final AlertLookup alertLookup;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;

    public OutboxProcessor(OutboxRepository outboxRepository, NotificationDeliveryRepository deliveryRepository,
                           EventLookup eventLookup, AlertLookup alertLookup,
                           PlatformTransactionManager transactionManager, Clock clock) {
        this.outboxRepository = outboxRepository;
        this.deliveryRepository = deliveryRepository;
        this.eventLookup = eventLookup;
        this.alertLookup = alertLookup;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.clock = clock;
    }

    /**
     * Processes the oldest pending record that no other worker has claimed.
     *
     * @return false when no record was waiting, true when one was processed or marked FAILED
     */
    public boolean processNext() {
        AtomicReference<UUID> claimedId = new AtomicReference<>();
        try {
            Boolean processed = transactionTemplate.execute(status -> {
                Optional<OutboxRecord> claimed = outboxRepository.claimNext();
                if (claimed.isEmpty()) {
                    return false;
                }
                claimedId.set(claimed.get().getId());
                process(claimed.get());
                return true;
            });
            return Boolean.TRUE.equals(processed);
        } catch (RuntimeException failure) {
            UUID recordId = claimedId.get();
            if (recordId == null) {
                throw failure;
            }
            log.warn("Outbox record {} failed and is marked FAILED", recordId, failure);
            transactionTemplate.executeWithoutResult(
                    status -> outboxRepository.markFailed(recordId, FailureMessages.of(failure)));
            return true;
        }
    }

    private void process(OutboxRecord record) {
        EventInfo event = eventLookup.findById(record.getEventId())
                .orElseThrow(() -> new IllegalStateException("Event " + record.getEventId() + " not found"));
        Instant now = Instant.now(clock);
        for (MatchedAlert alert : alertLookup.findEnabledAlerts(event.type())) {
            deliveryRepository.insertIfAbsent(UUID.randomUUID(), event.id(), alert.alertId(), alert.userId(),
                    alert.channel().getValue(), alert.recipientEmail(), now);
        }
        record.markProcessed(now);
    }

}
