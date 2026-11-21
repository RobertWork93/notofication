package com.notificationapp.app.service;

import com.notificationapp.app.dto.UserRequest;
import com.notificationapp.app.model.Role;
import com.notificationapp.app.model.User;
import com.notificationapp.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final String RESOURCE = "User";
    private static final String ADMIN_NAME = "Administrator";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    /**
     * Signs up a new user. Sign-up always creates the {@code USER} role.
     */
    @Transactional
    public User create(UserRequest request) {
        User user = new User(normalizeEmail(request.email()), request.name(),
                passwordEncoder.encode(request.password()), Role.USER, Instant.now(clock));
        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public Page<User> list(int page, int size) {
        return userRepository.findAll(PageRequest.of(page, size, Sort.by("createdAt", "id").ascending()));
    }

    @Transactional(readOnly = true)
    public User get(UUID userId) {
        return userRepository.findById(userId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, RESOURCE + " " + userId + " not found"));
    }

    /**
     * Replaces the user's email, name and password. The role is never changed here.
     */
    @Transactional
    public User update(UUID userId, UserRequest request) {
        User user = get(userId);
        user.setEmail(normalizeEmail(request.email()));
        user.setName(request.name());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setUpdatedAt(Instant.now(clock));
        return user;
    }

    @Transactional
    public void delete(UUID userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, RESOURCE + " " + userId + " not found");
        }
        userRepository.deleteById(userId);
    }

    /**
     * Creates the startup admin, unless the email or password is empty or the email is already taken.
     *
     * @return the new admin, or empty if nothing was created
     */
    @Transactional
    public Optional<User> createAdminIfMissing(String email, String password) {
        if (!StringUtils.hasText(email) || !StringUtils.hasText(password)) {
            return Optional.empty();
        }
        String normalizedEmail = normalizeEmail(email.trim());
        if (userRepository.findByEmail(normalizedEmail).isPresent()) {
            return Optional.empty();
        }
        User admin = new User(normalizedEmail, ADMIN_NAME, passwordEncoder.encode(password), Role.ADMIN,
                Instant.now(clock));
        return Optional.of(userRepository.save(admin));
    }

    private static String normalizeEmail(String email) {
        return email.toLowerCase(Locale.ROOT);
    }

}
