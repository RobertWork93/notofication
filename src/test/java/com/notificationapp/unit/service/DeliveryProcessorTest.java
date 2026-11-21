package com.notificationapp.unit.service;

import com.notificationapp.model.Channel;
import com.notificationapp.model.DeliveryStatus;
import com.notificationapp.model.EventType;
import com.notificationapp.model.NotificationDelivery;
import com.notificationapp.repository.AlertRepository;
import com.notificationapp.repository.EventRepository;
import com.notificationapp.repository.NotificationDeliveryRepository;
import com.notificationapp.service.DeliveryProcessor;
import com.notificationapp.service.NotificationMessage;
import com.notificationapp.service.NotificationSender;
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
import static com.notificationapp.support.TestDataFactory.earthquakeAlert;
import static com.notificationapp.support.TestDataFactory.event;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeliveryProcessorTest {

    private static final Instant CREATED = Instant.parse("2026-11-21T14:53:00Z");
    private static final Instant NOW = Instant.parse("2026-11-21T14:55:00Z");
    private static final UUID DELIVERY_ID = UUID.fromString("d0000000-0000-4000-8000-000000000001");

    @Mock
    private NotificationDeliveryRepository deliveryRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private AlertRepository alertRepository;

    @Mock
    private NotificationSender emailSender;

    @Mock
    private NotificationSender slackSender;

    @Mock
    private PlatformTransactionManager transactionManager;

    @Test
    void shouldReturnFalseWhenNoDeliveryIsPending() {
        when(deliveryRepository.claimNext()).thenReturn(Optional.empty());

        assertThat(processor(List.of()).processNext()).isFalse();
    }

    @Test
    void shouldSendThroughTheSenderOfTheChannelAndMarkDeliverySent() {
        NotificationDelivery delivery = pendingDelivery(Channel.EMAIL);
        givenSenders();
        givenDeliveryWithEventAndAlert(delivery);

        assertThat(processor(List.of(emailSender, slackSender)).processNext()).isTrue();

        verify(emailSender).send(new NotificationMessage(Channel.EMAIL, "jane.doe@example.com", "Earthquakes",
                EventType.NATURAL_DISASTER, "M6.8 earthquake", "usgs"));
        verify(slackSender, never()).send(any());
        assertThat(delivery.getStatus()).isEqualTo(DeliveryStatus.SENT);
        assertThat(delivery.getSentAt()).isEqualTo(NOW);
    }

    @Test
    void shouldMarkDeliveryFailedWhenSendingFails() {
        NotificationDelivery delivery = pendingDelivery(Channel.SLACK);
        givenSenders();
        givenDeliveryWithEventAndAlert(delivery);
        doThrow(new IllegalStateException("Slack is down")).when(slackSender).send(any());

        assertThat(processor(List.of(emailSender, slackSender)).processNext()).isTrue();

        verify(deliveryRepository).markFailed(DELIVERY_ID, "Slack is down");
    }

    @Test
    void shouldMarkDeliveryFailedWhenNoSenderHandlesTheChannel() {
        NotificationDelivery delivery = pendingDelivery(Channel.SLACK);
        lenient().when(emailSender.channel()).thenReturn(Channel.EMAIL);
        givenDeliveryWithEventAndAlert(delivery);

        assertThat(processor(List.of(emailSender)).processNext()).isTrue();

        verify(deliveryRepository).markFailed(DELIVERY_ID, "No sender for channel slack");
    }

    private DeliveryProcessor processor(List<NotificationSender> senders) {
        return new DeliveryProcessor(deliveryRepository, eventRepository, alertRepository, senders, transactionManager,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private void givenSenders() {
        lenient().when(emailSender.channel()).thenReturn(Channel.EMAIL);
        lenient().when(slackSender.channel()).thenReturn(Channel.SLACK);
    }

    private void givenDeliveryWithEventAndAlert(NotificationDelivery delivery) {
        when(deliveryRepository.claimNext()).thenReturn(Optional.of(delivery));
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(
                event(EventType.NATURAL_DISASTER, "M6.8 earthquake", "usgs", CREATED, null, CREATED)));
        when(alertRepository.findById(ALERT_ID)).thenReturn(Optional.of(earthquakeAlert(CREATED)));
    }

    private static NotificationDelivery pendingDelivery(Channel channel) {
        return new NotificationDelivery(DELIVERY_ID, EVENT_ID, ALERT_ID, USER_ID, channel, "jane.doe@example.com",
                CREATED);
    }

}
