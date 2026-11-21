package com.notificationapp.unit.service;

import com.notificationapp.app.dto.CreateAlertRequest;
import com.notificationapp.app.dto.UpdateAlertRequest;
import com.notificationapp.app.model.Alert;
import com.notificationapp.notification.api.Channel;
import com.notificationapp.event.api.EventType;
import com.notificationapp.app.model.User;
import com.notificationapp.app.repository.AlertRepository;
import com.notificationapp.app.repository.UserRepository;
import com.notificationapp.app.service.AlertService;
import org.springframework.web.server.ResponseStatusException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static com.notificationapp.support.TestDataFactory.ALERT_ID;
import static com.notificationapp.support.TestDataFactory.USER_ID;
import static com.notificationapp.support.TestDataFactory.earthquakeAlert;
import static com.notificationapp.support.TestDataFactory.jane;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlertServiceTest {

    private static final Instant CREATED = Instant.parse("2026-11-21T10:00:00Z");
    private static final Instant NOW = Instant.parse("2026-11-21T15:00:00Z");

    @Mock
    private AlertRepository alertRepository;

    @Mock
    private UserRepository userRepository;

    // Built by hand instead of @InjectMocks, because the service also needs a fixed Clock.
    private AlertService alertService;

    @BeforeEach
    void setUp() {
        alertService = new AlertService(alertRepository, userRepository, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void shouldEnableAlertByDefaultAndSetTimestampsWhenCreating() {
        User user = jane(CREATED);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(alertRepository.save(any(Alert.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Alert alert = alertService.create(
                new CreateAlertRequest(USER_ID, "Earthquakes", EventType.NATURAL_DISASTER, Channel.EMAIL, null));

        assertThat(alert.getUser()).isSameAs(user);
        assertThat(alert.getName()).isEqualTo("Earthquakes");
        assertThat(alert.getEventType()).isEqualTo(EventType.NATURAL_DISASTER);
        assertThat(alert.getChannel()).isEqualTo(Channel.EMAIL);
        assertThat(alert.isEnabled()).isTrue();
        assertThat(alert.getCreatedAt()).isEqualTo(NOW);
        assertThat(alert.getUpdatedAt()).isEqualTo(NOW);
    }

    @Test
    void shouldKeepAlertDisabledWhenCreatedDisabled() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(jane(CREATED)));
        when(alertRepository.save(any(Alert.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Alert alert = alertService.create(
                new CreateAlertRequest(USER_ID, "News", EventType.BREAKING_NEWS, Channel.SLACK, false));

        assertThat(alert.isEnabled()).isFalse();
    }

    @Test
    void shouldThrowNotFoundWhenCreatingAlertForMissingUser() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> alertService.create(
                new CreateAlertRequest(USER_ID, "News", EventType.BREAKING_NEWS, Channel.EMAIL, true)))
                .isInstanceOf(ResponseStatusException.class);
        verify(alertRepository, never()).save(any());
    }

    @Test
    void shouldReturnUsersAlertsOldestFirst() {
        PageRequest expectedPage = PageRequest.of(0, 20, Sort.by("createdAt", "id").ascending());
        when(userRepository.existsById(USER_ID)).thenReturn(true);
        when(alertRepository.findByUserId(USER_ID, expectedPage))
                .thenReturn(new PageImpl<>(List.of(earthquakeAlert(CREATED)), expectedPage, 1));

        Page<Alert> page = alertService.listForUser(USER_ID, 0, 20);

        assertThat(page.getContent()).extracting(Alert::getId).containsExactly(ALERT_ID);
    }

    @Test
    void shouldThrowNotFoundWhenListingAlertsOfMissingUser() {
        when(userRepository.existsById(USER_ID)).thenReturn(false);

        assertThatThrownBy(() -> alertService.listForUser(USER_ID, 0, 20)).isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void shouldReturnAlertWhenOwnedByUser() {
        Alert alert = earthquakeAlert(CREATED);
        when(alertRepository.findByIdAndUserId(ALERT_ID, USER_ID)).thenReturn(Optional.of(alert));

        assertThat(alertService.get(ALERT_ID, USER_ID)).isSameAs(alert);
    }

    @Test
    void shouldThrowNotFoundWhenAlertIsMissingOrOwnedByAnotherUser() {
        when(alertRepository.findByIdAndUserId(ALERT_ID, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> alertService.get(ALERT_ID, USER_ID)).isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void shouldReplaceFieldsAndKeepCreationTimeWhenUpdatingAlert() {
        when(alertRepository.findByIdAndUserId(ALERT_ID, USER_ID)).thenReturn(Optional.of(earthquakeAlert(CREATED)));

        Alert alert = alertService.update(ALERT_ID, USER_ID,
                new UpdateAlertRequest("Breaking news", EventType.BREAKING_NEWS, Channel.SLACK, false));

        assertThat(alert.getName()).isEqualTo("Breaking news");
        assertThat(alert.getEventType()).isEqualTo(EventType.BREAKING_NEWS);
        assertThat(alert.getChannel()).isEqualTo(Channel.SLACK);
        assertThat(alert.isEnabled()).isFalse();
        assertThat(alert.getCreatedAt()).isEqualTo(CREATED);
        assertThat(alert.getUpdatedAt()).isEqualTo(NOW);
    }

    @Test
    void shouldThrowNotFoundWhenUpdatingAlertNotOwnedByUser() {
        when(alertRepository.findByIdAndUserId(ALERT_ID, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> alertService.update(ALERT_ID, USER_ID,
                new UpdateAlertRequest("News", EventType.BREAKING_NEWS, Channel.EMAIL, true)))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void shouldDeleteAlertWhenOwnedByUser() {
        Alert alert = earthquakeAlert(CREATED);
        when(alertRepository.findByIdAndUserId(ALERT_ID, USER_ID)).thenReturn(Optional.of(alert));

        alertService.delete(ALERT_ID, USER_ID);

        verify(alertRepository).delete(alert);
    }

    @Test
    void shouldThrowNotFoundWhenDeletingAlertNotOwnedByUser() {
        when(alertRepository.findByIdAndUserId(ALERT_ID, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> alertService.delete(ALERT_ID, USER_ID)).isInstanceOf(ResponseStatusException.class);
        verify(alertRepository, never()).delete(any());
    }

}
