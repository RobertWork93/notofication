package com.notificationapp.app.service;

import com.notificationapp.app.model.Alert;
import com.notificationapp.app.repository.AlertRepository;
import com.notificationapp.event.api.EventType;
import com.notificationapp.notification.api.AlertLookup;
import com.notificationapp.notification.api.MatchedAlert;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Gives the notification module the alerts it needs, through its {@link AlertLookup} port.
 * Alerts and users stay in this module; the notification module only sees {@link MatchedAlert}s.
 */
@Service
@RequiredArgsConstructor
public class AlertLookupAdapter implements AlertLookup {

    private final AlertRepository alertRepository;

    @Override
    @Transactional(readOnly = true)
    public List<MatchedAlert> findEnabledAlerts(EventType eventType) {
        return alertRepository.findByEventTypeAndEnabledTrue(eventType).stream()
                .map(AlertLookupAdapter::toMatchedAlert)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MatchedAlert> findAlert(UUID alertId) {
        return alertRepository.findWithUserById(alertId).map(AlertLookupAdapter::toMatchedAlert);
    }

    private static MatchedAlert toMatchedAlert(Alert alert) {
        return new MatchedAlert(alert.getId(), alert.getName(), alert.getUser().getId(), alert.getUser().getEmail(),
                alert.getChannel());
    }

}
