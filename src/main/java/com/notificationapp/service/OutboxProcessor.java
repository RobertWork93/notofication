package com.notificationapp.service;

import com.notificationapp.model.Alert;
import com.notificationapp.model.Event;
import com.notificationapp.model.OutboxRecord;
import com.notificationapp.model.User;
import com.notificationapp.repository.AlertRepository;
import com.notificationapp.repository.EventRepository;
import com.notificationapp.repository.NotificationDeliveryRepository;
import com.notificationapp.repository.OutboxRepository;
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
 */
@Slf4j
@Service
public class OutboxProcessor {

    private final OutboxRepository outboxRepository;
    private final EventRepository eventRepository;
    private final AlertRepository alertRepository;
    private final NotificationDeliveryRepository deliveryRepository;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;

    public OutboxProcessor(OutboxRepository outboxRepository, EventRepository eventRepository,
                           AlertRepository alertRepository, NotificationDeliveryRepository deliveryRepository,
                           PlatformTransactionManager transactionManager, Clock clock) {
        this.outboxRepository = outboxRepository;
        this.eventRepository = eventRepository;
        this.alertRepository = alertRepository;
        this.deliveryRepository = deliveryRepository;
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
        Event event = eventRepository.findById(record.getEventId())
                .orElseThrow(() -> new IllegalStateException("Event " + record.getEventId() + " not found"));
        Instant now = Instant.now(clock);
        for (Alert alert : alertRepository.findByEventTypeAndEnabledTrue(event.getType())) {
            User owner = alert.getUser();
            deliveryRepository.insertIfAbsent(UUID.randomUUID(), event.getId(), alert.getId(), owner.getId(),
                    alert.getChannel().getValue(), owner.getEmail(), now);
        }
        record.markProcessed(now);
    }

}
