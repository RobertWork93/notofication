package com.notificationapp.notification.service;

import com.notificationapp.event.api.EventCreated;
import com.notificationapp.notification.model.OutboxRecord;
import com.notificationapp.notification.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Writes the outbox record for each new event (transactional outbox).
 * A plain {@code @EventListener} runs synchronously inside the transaction that saved the event,
 * so the event and its outbox record commit or roll back together.
 */
@Component
@RequiredArgsConstructor
public class OutboxWriter {

    private final OutboxRepository outboxRepository;

    @EventListener
    public void onEventCreated(EventCreated eventCreated) {
        outboxRepository.save(new OutboxRecord(eventCreated.eventId(), eventCreated.createdAt()));
    }

}
