package com.notificationapp.unit.service;

import com.notificationapp.model.Channel;
import com.notificationapp.model.EventType;
import com.notificationapp.service.NotificationMessage;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationMessageTest {

    @Test
    void shouldDescribeAlertAndEventInOneLine() {
        NotificationMessage message = new NotificationMessage(Channel.EMAIL, "jane.doe@example.com", "Earthquakes",
                EventType.NATURAL_DISASTER, "M6.8 earthquake", "usgs");

        assertThat(message.summary())
                .isEqualTo("alert \"Earthquakes\": Natural disaster: M6.8 earthquake (source: usgs)");
    }

    @Test
    void shouldUseReadableEventTypeLabel() {
        NotificationMessage message = new NotificationMessage(Channel.SLACK, "jane.doe@example.com", "News",
                EventType.BREAKING_NEWS, "Election called", "reuters");

        assertThat(message.summary()).isEqualTo("alert \"News\": Breaking news: Election called (source: reuters)");
    }

}
