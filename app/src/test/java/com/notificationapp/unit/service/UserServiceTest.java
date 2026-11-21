package com.notificationapp.unit.service;

import com.notificationapp.app.dto.UserRequest;
import com.notificationapp.app.model.Role;
import com.notificationapp.app.model.User;
import com.notificationapp.app.repository.UserRepository;
import org.springframework.web.server.ResponseStatusException;
import com.notificationapp.app.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static com.notificationapp.support.TestDataFactory.USER_ID;
import static com.notificationapp.support.TestDataFactory.jane;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    private static final Instant CREATED = Instant.parse("2026-11-21T10:00:00Z");
    private static final Instant NOW = Instant.parse("2026-11-21T15:00:00Z");

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    // Built by hand instead of @InjectMocks, because the service also needs a fixed Clock.
    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, passwordEncoder, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void shouldNormalizeEmailHashPasswordAndAssignUserRoleWhenCreatingUser() {
        when(passwordEncoder.encode("secret123")).thenReturn("{bcrypt}hashed");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User user = userService.create(new UserRequest("  Jane.Doe@Example.COM ", "Jane Doe", "secret123"));

        assertThat(user.getEmail()).isEqualTo("jane.doe@example.com");
        assertThat(user.getName()).isEqualTo("Jane Doe");
        assertThat(user.getPasswordHash()).isEqualTo("{bcrypt}hashed");
        assertThat(user.getRole()).isEqualTo(Role.USER);
        assertThat(user.getCreatedAt()).isEqualTo(NOW);
        assertThat(user.getUpdatedAt()).isEqualTo(NOW);
    }

    @Test
    void shouldReturnUserWhenUserExists() {
        User existing = jane(CREATED);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(existing));

        assertThat(userService.get(USER_ID)).isSameAs(existing);
    }

    @Test
    void shouldThrowNotFoundWhenUserDoesNotExist() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.get(USER_ID)).isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void shouldReturnRequestedPageSortedByCreationTime() {
        when(userRepository.findAll(any(Pageable.class)))
                .thenAnswer(invocation -> new PageImpl<>(List.of(jane(CREATED)), invocation.getArgument(0), 21));

        Page<User> page = userService.list(1, 20);

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(userRepository).findAll(pageable.capture());
        assertThat(pageable.getValue()).isEqualTo(PageRequest.of(1, 20, Sort.by("createdAt", "id").ascending()));
        assertThat(page.getContent()).extracting(User::getEmail).containsExactly("jane.doe@example.com");
        assertThat(page.getTotalElements()).isEqualTo(21);
    }

    @Test
    void shouldReplaceFieldsHashNewPasswordAndKeepRoleWhenUpdatingUser() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(jane(CREATED)));
        when(passwordEncoder.encode("newSecret1")).thenReturn("{bcrypt}new");

        User user = userService.update(USER_ID, new UserRequest("John.Doe@example.com", "John Doe", "newSecret1"));

        assertThat(user.getEmail()).isEqualTo("john.doe@example.com");
        assertThat(user.getName()).isEqualTo("John Doe");
        assertThat(user.getPasswordHash()).isEqualTo("{bcrypt}new");
        assertThat(user.getRole()).isEqualTo(Role.USER);
        assertThat(user.getCreatedAt()).isEqualTo(CREATED);
        assertThat(user.getUpdatedAt()).isEqualTo(NOW);
    }

    @Test
    void shouldThrowNotFoundWhenUpdatingMissingUser() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.update(USER_ID,
                new UserRequest("jane.doe@example.com", "Jane Doe", "secret123")))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void shouldDeleteUserWhenUserExists() {
        when(userRepository.existsById(USER_ID)).thenReturn(true);

        userService.delete(USER_ID);

        verify(userRepository).deleteById(USER_ID);
    }

    @Test
    void shouldThrowNotFoundWhenDeletingMissingUser() {
        when(userRepository.existsById(USER_ID)).thenReturn(false);

        assertThatThrownBy(() -> userService.delete(USER_ID)).isInstanceOf(ResponseStatusException.class);
        verify(userRepository, never()).deleteById(any());
    }

    @Test
    void shouldCreateAdminWhenEmailIsNotTaken() {
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("adminSecret1")).thenReturn("{bcrypt}admin");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Optional<User> admin = userService.createAdminIfMissing("Admin@Example.com", "adminSecret1");

        assertThat(admin).hasValueSatisfying(created -> {
            assertThat(created.getEmail()).isEqualTo("admin@example.com");
            assertThat(created.getRole()).isEqualTo(Role.ADMIN);
            assertThat(created.getPasswordHash()).isEqualTo("{bcrypt}admin");
        });
    }

    @Test
    void shouldNotCreateAdminWhenEmailAlreadyExists() {
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(jane(CREATED)));

        assertThat(userService.createAdminIfMissing("admin@example.com", "adminSecret1")).isEmpty();
        verify(userRepository, never()).save(any());
    }

    @ParameterizedTest
    @CsvSource(value = {"'',adminSecret1", "admin@example.com,''", "NULL,NULL"}, nullValues = "NULL")
    void shouldNotCreateAdminWhenEmailOrPasswordIsMissing(String email, String password) {
        assertThat(userService.createAdminIfMissing(email, password)).isEmpty();
        verifyNoInteractions(userRepository, passwordEncoder);
    }

}
