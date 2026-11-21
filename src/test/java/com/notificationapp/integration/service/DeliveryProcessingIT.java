package com.notificationapp.integration.service;

import com.notificationapp.model.Channel;
import com.notificationapp.service.DeliveryProcessor;
import com.notificationapp.service.OutboxProcessor;
import com.notificationapp.service.SlackNotificationSender;
import com.notificationapp.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

/**
 * The real email sender (log only) and a Slack sender that always fails, against a real database.
 */
class DeliveryProcessingIT extends AbstractIntegrationTest {

    @Autowired
    private OutboxProcessor outboxProcessor;

    @Autowired
    private DeliveryProcessor deliveryProcessor;

    @MockitoBean
    private SlackNotificationSender slackSender;

    @BeforeEach
    void makeSlackFail() {
        when(slackSender.channel()).thenReturn(Channel.SLACK);
        doThrow(new IllegalStateException("Slack is down")).when(slackSender).send(any());
    }

    @Test
    void shouldMarkSentDeliveriesSentAndFailedDeliveriesFailedWithoutStoppingOthers() {
        String jane = createUser("jane.doe@example.com", "Jane Doe");
        String ann = createUser("ann.lee@example.com", "Ann Lee");
        String janesAlert = createAlert(jane, "Quakes by email", "natural_disaster", "email", true);
        String annsAlert = createAlert(ann, "Quakes on Slack", "natural_disaster", "slack", true);
        createEvent("natural_disaster", "Earthquake");
        outboxProcessor.processNext();

        int processed = 0;
        while (deliveryProcessor.processNext()) {
            processed++;
        }

        assertThat(processed).isEqualTo(2);
        Map<String, Object> emailDelivery = delivery(janesAlert);
        assertThat(emailDelivery).containsEntry("status", "SENT").containsEntry("last_error", null);
        assertThat(emailDelivery.get("sent_at")).isNotNull();
        Map<String, Object> slackDelivery = delivery(annsAlert);
        assertThat(slackDelivery).containsEntry("status", "FAILED").containsEntry("last_error", "Slack is down");
        assertThat(slackDelivery.get("sent_at")).isNull();
    }

    private Map<String, Object> delivery(String alertId) {
        return jdbcTemplate.queryForMap(
                "SELECT status, last_error, sent_at FROM notification_deliveries WHERE alert_id = ?",
                UUID.fromString(alertId));
    }

}
