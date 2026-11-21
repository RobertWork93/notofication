package com.notificationapp.repository;

import com.notificationapp.model.Alert;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AlertRepository extends JpaRepository<Alert, UUID> {

    Page<Alert> findByUserId(UUID userId, Pageable pageable);

    /**
     * Finds an alert only if it belongs to the given user, so ownership is checked in the query.
     */
    Optional<Alert> findByIdAndUserId(UUID id, UUID userId);

}
