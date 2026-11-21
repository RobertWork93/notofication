package com.notificationapp.app.repository;

import com.notificationapp.app.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Finds a user by email. Emails are stored in lower case, so pass a lower-cased email.
     */
    Optional<User> findByEmail(String email);

}
