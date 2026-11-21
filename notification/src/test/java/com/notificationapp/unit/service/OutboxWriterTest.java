package com.notificationapp.unit.service;

import com.notificationapp.event.api.EventCreated;
import com.notificationapp.notification.model.OutboxRecord;
import com.notificationapp.notification.model.OutboxStatus;
import com.notificationapp.notification.repository.OutboxRepository;
import com.notificationapp.notification.service.OutboxWriter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OutboxWriterTest {

    private static final UUID EVENT_ID = UUID.fromString("3f6c1a52-9a1e-4c0e-9a7e-2a1b5f0c9d11");
    private static final Instant NOW = Instant.parse("2026-11-21T14:53:10Z");

    @Mock
    private OutboxRepository outboxRepository;

    @InjectMocks
    private OutboxWriter outboxWriter;

    @Test
    void shouldSavePendingOutboxRecordForCreatedEvent() {
        outboxWriter.onEventCreated(new EventCreated(EVENT_ID, NOW));

        ArgumentCaptor<OutboxRecord> record = ArgumentCaptor.forClass(OutboxRecord.class);
        verify(outboxRepository).save(record.capture());
        assertThat(record.getValue().getEventId()).isEqualTo(EVENT_ID);
        assertThat(record.getValue().getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(record.getValue().getCreatedAt()).isEqualTo(NOW);
    }

}
