package com.notificationapp.unit.service;

import com.notificationapp.repository.UserRepository;
import com.notificationapp.service.AuthenticatedUser;
import com.notificationapp.service.DatabaseUserDetailsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.time.Instant;
import java.util.Optional;

import static com.notificationapp.support.TestDataFactory.ADMIN_ID;
import static com.notificationapp.support.TestDataFactory.PASSWORD_HASH;
import static com.notificationapp.support.TestDataFactory.USER_ID;
import static com.notificationapp.support.TestDataFactory.admin;
import static com.notificationapp.support.TestDataFactory.jane;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DatabaseUserDetailsServiceTest {

    private static final Instant CREATED = Instant.parse("2026-11-21T10:00:00Z");

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private DatabaseUserDetailsService userDetailsService;

    @Test
    void shouldLoadUserByEmailIgnoringCaseWithUserRole() {
        when(userRepository.findByEmail("jane.doe@example.com")).thenReturn(Optional.of(jane(CREATED)));

        UserDetails details = userDetailsService.loadUserByUsername("Jane.Doe@Example.com");

        assertThat(details).isInstanceOf(AuthenticatedUser.class);
        assertThat(((AuthenticatedUser) details).getId()).isEqualTo(USER_ID);
        assertThat(details.getUsername()).isEqualTo("jane.doe@example.com");
        assertThat(details.getPassword()).isEqualTo(PASSWORD_HASH);
        assertThat(details.getAuthorities()).extracting(GrantedAuthority::getAuthority).containsExactly("ROLE_USER");
    }

    @Test
    void shouldGiveAdminTheAdminRole() {
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(admin(CREATED)));

        AuthenticatedUser details = (AuthenticatedUser) userDetailsService.loadUserByUsername("admin@example.com");

        assertThat(details.getId()).isEqualTo(ADMIN_ID);
        assertThat(details.getAuthorities()).extracting(GrantedAuthority::getAuthority).containsExactly("ROLE_ADMIN");
    }

    @Test
    void shouldFailForUnknownEmail() {
        when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("nobody@example.com"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

}
