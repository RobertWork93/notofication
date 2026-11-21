package com.notificationapp.app.repository;

import com.notificationapp.app.model.Alert;
import com.notificationapp.event.api.EventType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AlertRepository extends JpaRepository<Alert, UUID> {

    Page<Alert> findByUserId(UUID userId, Pageable pageable);

    /**
     * Finds an alert only if it belongs to the given user, so ownership is checked in the query.
     */
    Optional<Alert> findByIdAndUserId(UUID id, UUID userId);

    /**
     * The enabled alerts for an event type, oldest first, with their owners loaded in the same query.
     */
    @Query("""
            SELECT a FROM Alert a JOIN FETCH a.user
            WHERE a.eventType = :eventType AND a.enabled = true
            ORDER BY a.createdAt, a.id
            """)
    List<Alert> findByEventTypeAndEnabledTrue(@Param("eventType") EventType eventType);

    /**
     * One alert with its owner loaded in the same query.
     */
    @Query("SELECT a FROM Alert a JOIN FETCH a.user WHERE a.id = :alertId")
    Optional<Alert> findWithUserById(@Param("alertId") UUID alertId);

}
