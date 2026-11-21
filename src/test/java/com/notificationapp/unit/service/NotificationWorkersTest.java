package com.notificationapp.unit.service;

import com.notificationapp.configuration.NotificationProperties;
import com.notificationapp.service.DeliveryProcessor;
import com.notificationapp.service.DeliveryWorker;
import com.notificationapp.service.OutboxProcessor;
import com.notificationapp.service.OutboxWorker;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationWorkersTest {

    private static final NotificationProperties PROPERTIES = new NotificationProperties(3);

    @Mock
    private OutboxProcessor outboxProcessor;

    @Mock
    private DeliveryProcessor deliveryProcessor;

    @Test
    void shouldStopOutboxPollWhenNoRecordIsLeft() {
        when(outboxProcessor.processNext()).thenReturn(true, false);

        new OutboxWorker(outboxProcessor, PROPERTIES).poll();

        verify(outboxProcessor, times(2)).processNext();
    }

    @Test
    void shouldStopOutboxPollAtBatchSize() {
        when(outboxProcessor.processNext()).thenReturn(true);

        new OutboxWorker(outboxProcessor, PROPERTIES).poll();

        verify(outboxProcessor, times(3)).processNext();
    }

    @Test
    void shouldStopDeliveryPollAtBatchSizeOrWhenNoJobIsLeft() {
        when(deliveryProcessor.processNext()).thenReturn(true, true, false);

        new DeliveryWorker(deliveryProcessor, PROPERTIES).poll();

        verify(deliveryProcessor, times(3)).processNext();
    }

}
