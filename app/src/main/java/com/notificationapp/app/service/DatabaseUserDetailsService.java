package com.notificationapp.app.service;

import com.notificationapp.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

/**
 * Loads users for HTTP Basic login. The username is the email, matched ignoring case.
 */
@Service
@RequiredArgsConstructor
public class DatabaseUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) {
        return userRepository.findByEmail(username.trim().toLowerCase(Locale.ROOT))
                .map(AuthenticatedUser::from)
                .orElseThrow(() -> new UsernameNotFoundException("Unknown user"));
    }

}
