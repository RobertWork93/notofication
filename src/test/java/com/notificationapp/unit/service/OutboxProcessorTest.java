package com.notificationapp.unit.service;

import com.notificationapp.model.Alert;
import com.notificationapp.model.Channel;
import com.notificationapp.model.EventType;
import com.notificationapp.model.OutboxRecord;
import com.notificationapp.model.OutboxStatus;
import com.notificationapp.repository.AlertRepository;
import com.notificationapp.repository.EventRepository;
import com.notificationapp.repository.NotificationDeliveryRepository;
import com.notificationapp.repository.OutboxRepository;
import com.notificationapp.service.OutboxProcessor;
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

import static com.notificationapp.support.TestDataFactory.ALERT_ID;
import static com.notificationapp.support.TestDataFactory.EVENT_ID;
import static com.notificationapp.support.TestDataFactory.USER_ID;
import static com.notificationapp.support.TestDataFactory.alert;
import static com.notificationapp.support.TestDataFactory.event;
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

    @Mock
    private OutboxRepository outboxRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private AlertRepository alertRepository;

    @Mock
    private NotificationDeliveryRepository deliveryRepository;

    @Mock
    private PlatformTransactionManager transactionManager;

    // Built by hand instead of @InjectMocks, because the processor also needs a fixed Clock.
    private OutboxProcessor outboxProcessor;

    @BeforeEach
    void setUp() {
        outboxProcessor = new OutboxProcessor(outboxRepository, eventRepository, alertRepository, deliveryRepository,
                transactionManager, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void shouldReturnFalseWhenNoRecordIsPending() {
        when(outboxRepository.claimNext()).thenReturn(Optional.empty());

        assertThat(outboxProcessor.processNext()).isFalse();

        verifyNoInteractions(eventRepository, alertRepository, deliveryRepository);
    }

    @Test
    void shouldCreateOneDeliveryPerMatchingAlertAndMarkRecordProcessed() {
        OutboxRecord record = pendingRecord();
        Alert emailAlert = alert("Quakes", EventType.NATURAL_DISASTER, Channel.EMAIL, true, CREATED);
        when(outboxRepository.claimNext()).thenReturn(Optional.of(record));
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(
                event(EventType.NATURAL_DISASTER, "Earthquake", "usgs", CREATED, null, CREATED)));
        when(alertRepository.findByEventTypeAndEnabledTrue(EventType.NATURAL_DISASTER)).thenReturn(List.of(emailAlert));

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
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(
                event(EventType.BREAKING_NEWS, "Election called", "reuters", CREATED, null, CREATED)));
        when(alertRepository.findByEventTypeAndEnabledTrue(EventType.BREAKING_NEWS)).thenReturn(List.of());

        assertThat(outboxProcessor.processNext()).isTrue();

        verify(deliveryRepository, never()).insertIfAbsent(any(), any(), any(), any(), any(), any(), any());
        assertThat(record.getStatus()).isEqualTo(OutboxStatus.PROCESSED);
    }

    @Test
    void shouldMarkRecordFailedWhenProcessingFails() {
        when(outboxRepository.claimNext()).thenReturn(Optional.of(pendingRecord()));
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.empty());

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
