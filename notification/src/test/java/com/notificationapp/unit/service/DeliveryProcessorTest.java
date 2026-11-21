package com.notificationapp.unit.service;

import com.notificationapp.event.api.EventInfo;
import com.notificationapp.event.api.EventLookup;
import com.notificationapp.event.api.EventType;
import com.notificationapp.notification.api.AlertLookup;
import com.notificationapp.notification.api.Channel;
import com.notificationapp.notification.api.MatchedAlert;
import com.notificationapp.notification.model.DeliveryStatus;
import com.notificationapp.notification.model.NotificationDelivery;
import com.notificationapp.notification.repository.NotificationDeliveryRepository;
import com.notificationapp.notification.service.DeliveryProcessor;
import com.notificationapp.notification.channel.NotificationMessage;
import com.notificationapp.notification.channel.NotificationSender;
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
    private static final UUID EVENT_ID = UUID.fromString("3f6c1a52-9a1e-4c0e-9a7e-2a1b5f0c9d11");
    private static final UUID ALERT_ID = UUID.fromString("8d2e4b7a-1c3f-4e5a-9b6d-0f1e2d3c4b5a");
    private static final UUID USER_ID = UUID.fromString("0b9f1d2e-3c4a-4b5c-8d6e-7f8091a2b3c4");

    @Mock
    private NotificationDeliveryRepository deliveryRepository;

    @Mock
    private EventLookup eventLookup;

    @Mock
    private AlertLookup alertLookup;

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
        return new DeliveryProcessor(deliveryRepository, eventLookup, alertLookup, senders, transactionManager,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private void givenSenders() {
        lenient().when(emailSender.channel()).thenReturn(Channel.EMAIL);
        lenient().when(slackSender.channel()).thenReturn(Channel.SLACK);
    }

    private void givenDeliveryWithEventAndAlert(NotificationDelivery delivery) {
        when(deliveryRepository.claimNext()).thenReturn(Optional.of(delivery));
        when(eventLookup.findById(EVENT_ID)).thenReturn(Optional.of(
                new EventInfo(EVENT_ID, EventType.NATURAL_DISASTER, "M6.8 earthquake", "usgs")));
        when(alertLookup.findAlert(ALERT_ID)).thenReturn(Optional.of(
                new MatchedAlert(ALERT_ID, "Earthquakes", USER_ID, "jane.doe@example.com", delivery.getChannel())));
    }

    private static NotificationDelivery pendingDelivery(Channel channel) {
        return new NotificationDelivery(DELIVERY_ID, EVENT_ID, ALERT_ID, USER_ID, channel, "jane.doe@example.com",
                CREATED);
    }

}
