package com.notificationapp.unit.service;

import com.notificationapp.app.repository.AlertRepository;
import com.notificationapp.app.service.AlertLookupAdapter;
import com.notificationapp.event.api.EventType;
import com.notificationapp.notification.api.Channel;
import com.notificationapp.notification.api.MatchedAlert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static com.notificationapp.support.TestDataFactory.ALERT_ID;
import static com.notificationapp.support.TestDataFactory.USER_ID;
import static com.notificationapp.support.TestDataFactory.earthquakeAlert;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlertLookupAdapterTest {

    private static final Instant CREATED = Instant.parse("2026-11-21T10:00:00Z");
    private static final MatchedAlert EARTHQUAKES =
            new MatchedAlert(ALERT_ID, "Earthquakes", USER_ID, "jane.doe@example.com", Channel.EMAIL);

    @Mock
    private AlertRepository alertRepository;

    @InjectMocks
    private AlertLookupAdapter alertLookupAdapter;

    @Test
    void shouldMapEnabledAlertsToMatchedAlertsWithOwnerEmail() {
        when(alertRepository.findByEventTypeAndEnabledTrue(EventType.NATURAL_DISASTER))
                .thenReturn(List.of(earthquakeAlert(CREATED)));

        assertThat(alertLookupAdapter.findEnabledAlerts(EventType.NATURAL_DISASTER)).containsExactly(EARTHQUAKES);
    }

    @Test
    void shouldFindSingleAlertById() {
        when(alertRepository.findWithUserById(ALERT_ID)).thenReturn(Optional.of(earthquakeAlert(CREATED)));

        assertThat(alertLookupAdapter.findAlert(ALERT_ID)).contains(EARTHQUAKES);
    }

    @Test
    void shouldReturnEmptyForMissingAlert() {
        when(alertRepository.findWithUserById(ALERT_ID)).thenReturn(Optional.empty());

        assertThat(alertLookupAdapter.findAlert(ALERT_ID)).isEmpty();
    }

}
