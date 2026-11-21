package com.notificationapp.unit.service;

import com.notificationapp.event.api.EventInfo;
import com.notificationapp.event.api.EventLookup;
import com.notificationapp.event.api.EventType;
import com.notificationapp.notification.api.AlertLookup;
import com.notificationapp.notification.api.Channel;
import com.notificationapp.notification.api.MatchedAlert;
import com.notificationapp.notification.model.OutboxRecord;
import com.notificationapp.notification.model.OutboxStatus;
import com.notificationapp.notification.repository.NotificationDeliveryRepository;
import com.notificationapp.notification.repository.OutboxRepository;
import com.notificationapp.notification.service.OutboxProcessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxProcessorTest {

    private static final Instant CREATED = Instant.parse("2026-11-21T14:53:00Z");
    private static final Instant NOW = Instant.parse("2026-11-21T14:54:00Z");
    private static final UUID OUTBOX_ID = UUID.fromString("c0000000-0000-4000-8000-000000000001");
    private static final UUID EVENT_ID = UUID.fromString("3f6c1a52-9a1e-4c0e-9a7e-2a1b5f0c9d11");
    private static final UUID ALERT_ID = UUID.fromString("8d2e4b7a-1c3f-4e5a-9b6d-0f1e2d3c4b5a");
    private static final UUID USER_ID = UUID.fromString("0b9f1d2e-3c4a-4b5c-8d6e-7f8091a2b3c4");

    @Mock
    private OutboxRepository outboxRepository;

    @Mock
    private NotificationDeliveryRepository deliveryRepository;

    @Mock
    private EventLookup eventLookup;

    @Mock
    private AlertLookup alertLookup;

    @Mock
    private PlatformTransactionManager transactionManager;

    // Built by hand instead of @InjectMocks, because the processor also needs a fixed Clock.
    private OutboxProcessor outboxProcessor;

    @BeforeEach
    void setUp() {
        outboxProcessor = new OutboxProcessor(outboxRepository, deliveryRepository, eventLookup, alertLookup,
                transactionManager, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void shouldReturnFalseWhenNoRecordIsPending() {
        when(outboxRepository.claimNext()).thenReturn(Optional.empty());

        assertThat(outboxProcessor.processNext()).isFalse();

        verifyNoInteractions(eventLookup, alertLookup, deliveryRepository);
    }

    @Test
    void shouldCreateOneDeliveryPerMatchingAlertAndMarkRecordProcessed() {
        OutboxRecord record = pendingRecord();
        when(outboxRepository.claimNext()).thenReturn(Optional.of(record));
        when(eventLookup.findById(EVENT_ID)).thenReturn(Optional.of(
                new EventInfo(EVENT_ID, EventType.NATURAL_DISASTER, "Earthquake", "usgs")));
        when(alertLookup.findEnabledAlerts(EventType.NATURAL_DISASTER)).thenReturn(List.of(
                new MatchedAlert(ALERT_ID, "Quakes", USER_ID, "jane.doe@example.com", Channel.EMAIL)));

        assertThat(outboxProcessor.processNext()).isTrue();

        verify(deliveryRepository).insertIfAbsent(any(UUID.class), eq(EVENT_ID), eq(ALERT_ID), eq(USER_ID),
                eq("email"), eq("jane.doe@example.com"), eq(NOW));
        assertThat(record.getStatus()).isEqualTo(OutboxStatus.PROCESSED);
        assertThat(record.getProcessedAt()).isEqualTo(NOW);
    }

    @Test
    void shouldMarkRecordProcessedWhenNoAlertMatches() {
        OutboxRecord record = pendingRecord();
        when(outboxRepository.claimNext()).thenReturn(Optional.of(record));
        when(eventLookup.findById(EVENT_ID)).thenReturn(Optional.of(
                new EventInfo(EVENT_ID, EventType.BREAKING_NEWS, "Election called", "reuters")));
        when(alertLookup.findEnabledAlerts(EventType.BREAKING_NEWS)).thenReturn(List.of());

        assertThat(outboxProcessor.processNext()).isTrue();

        verify(deliveryRepository, never()).insertIfAbsent(any(), any(), any(), any(), any(), any(), any());
        assertThat(record.getStatus()).isEqualTo(OutboxStatus.PROCESSED);
    }

    @Test
    void shouldMarkRecordFailedWhenProcessingFails() {
        when(outboxRepository.claimNext()).thenReturn(Optional.of(pendingRecord()));
        when(eventLookup.findById(EVENT_ID)).thenReturn(Optional.empty());

        assertThat(outboxProcessor.processNext()).isTrue();

        verify(outboxRepository).markFailed(OUTBOX_ID, "Event " + EVENT_ID + " not found");
        verify(deliveryRepository, never()).insertIfAbsent(any(), any(), any(), any(), any(), any(), any());
    }

    private static OutboxRecord pendingRecord() {
        OutboxRecord record = new OutboxRecord(EVENT_ID, CREATED);
        record.setId(OUTBOX_ID);
        return record;
    }

}
