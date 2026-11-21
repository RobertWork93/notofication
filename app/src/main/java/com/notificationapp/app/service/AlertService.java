package com.notificationapp.app.service;

import com.notificationapp.app.dto.CreateAlertRequest;
import com.notificationapp.app.dto.UpdateAlertRequest;
import com.notificationapp.app.model.Alert;
import com.notificationapp.app.model.User;
import com.notificationapp.app.repository.AlertRepository;
import com.notificationapp.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/**
 * Manages a user's alerts. The user id is always passed in explicitly, so an authenticated user
 * can be supplied here later without changing these rules.
 */
@Service
@RequiredArgsConstructor
public class AlertService {

    private final AlertRepository alertRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    @Transactional
    public Alert create(CreateAlertRequest request) {
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User " + request.userId() + " not found"));
        boolean enabled = request.enabled() == null || request.enabled();
        Alert alert = new Alert(user, request.name(), request.eventType(), request.channel(), enabled,
                Instant.now(clock));
        return alertRepository.save(alert);
    }

    @Transactional(readOnly = true)
    public Page<Alert> listForUser(UUID userId, int page, int size) {
        if (!userRepository.existsById(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User " + userId + " not found");
        }
        return alertRepository.findByUserId(userId, PageRequest.of(page, size, Sort.by("createdAt", "id").ascending()));
    }

    /**
     * A missing alert and an alert owned by another user both give the same not-found error,
     * so the API does not reveal other users' alerts.
     */
    @Transactional(readOnly = true)
    public Alert get(UUID alertId, UUID userId) {
        return alertRepository.findByIdAndUserId(alertId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Alert " + alertId + " not found"));
    }

    @Transactional
    public Alert update(UUID alertId, UUID userId, UpdateAlertRequest request) {
        Alert alert = get(alertId, userId);
        alert.setName(request.name());
        alert.setEventType(request.eventType());
        alert.setChannel(request.channel());
        alert.setEnabled(request.enabled());
        alert.setUpdatedAt(Instant.now(clock));
        return alert;
    }

    @Transactional
    public void delete(UUID alertId, UUID userId) {
        alertRepository.delete(get(alertId, userId));
    }

}
