package com.notificationapp.controller;

import com.notificationapp.dto.AlertResponse;
import com.notificationapp.dto.CreateAlertRequest;
import com.notificationapp.dto.UpdateAlertRequest;
import com.notificationapp.model.Alert;
import com.notificationapp.service.AlertService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

/**
 * Alerts are a top-level resource. A single alert is addressed together with its owner.
 * A USER may only reach their own alerts. An ADMIN may reach every user's alerts.
 */
@RestController
@RequestMapping("/api/v1/alerts")
@RequiredArgsConstructor
public class AlertController {

    private static final String ADMIN_OR_OWNER = "hasRole('ADMIN') or #userId == authentication.principal.id";

    private final AlertService alertService;

    @PreAuthorize("hasRole('ADMIN') or #request.userId() == authentication.principal.id")
    @PostMapping
    public ResponseEntity<AlertResponse> create(@Valid @RequestBody CreateAlertRequest request) {
        AlertResponse alert = toResponse(alertService.create(request));
        URI location = URI.create("/api/v1/alerts/" + alert.id() + "/users/" + alert.userId());
        return ResponseEntity.created(location).body(alert);
    }

    @PreAuthorize(ADMIN_OR_OWNER)
    @GetMapping
    public PagedModel<AlertResponse> listForUser(
            @RequestParam UUID userId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return new PagedModel<>(alertService.listForUser(userId, page, size).map(AlertController::toResponse));
    }

    @PreAuthorize(ADMIN_OR_OWNER)
    @GetMapping("/{alertId}/users/{userId}")
    public AlertResponse get(@PathVariable UUID alertId, @PathVariable UUID userId) {
        return toResponse(alertService.get(alertId, userId));
    }

    @PreAuthorize(ADMIN_OR_OWNER)
    @PutMapping("/{alertId}/users/{userId}")
    public AlertResponse update(@PathVariable UUID alertId, @PathVariable UUID userId,
                                @Valid @RequestBody UpdateAlertRequest request) {
        return toResponse(alertService.update(alertId, userId, request));
    }

    @PreAuthorize(ADMIN_OR_OWNER)
    @DeleteMapping("/{alertId}/users/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID alertId, @PathVariable UUID userId) {
        alertService.delete(alertId, userId);
    }

    private static AlertResponse toResponse(Alert alert) {
        return new AlertResponse(alert.getId(), alert.getUser().getId(), alert.getName(), alert.getEventType(),
                alert.getChannel(), alert.isEnabled(), alert.getCreatedAt(), alert.getUpdatedAt());
    }

}
