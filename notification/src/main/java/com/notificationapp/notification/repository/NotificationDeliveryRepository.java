package com.notificationapp.notification.repository;

import com.notificationapp.notification.model.NotificationDelivery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface NotificationDeliveryRepository extends JpaRepository<NotificationDelivery, UUID> {

    /**
     * Creates a pending delivery job, unless one already exists for this event and alert.
     *
     * @return 1 when a job was created, 0 when it already existed
     */
    @Modifying
    @Query(value = """
            INSERT INTO notification_deliveries
                (id, event_id, alert_id, user_id, channel, recipient, status, created_at)
            VALUES (:id, :eventId, :alertId, :userId, :channel, :recipient, 'PENDING', :createdAt)
            ON CONFLICT (event_id, alert_id) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(@Param("id") UUID id, @Param("eventId") UUID eventId, @Param("alertId") UUID alertId,
                       @Param("userId") UUID userId, @Param("channel") String channel,
                       @Param("recipient") String recipient, @Param("createdAt") Instant createdAt);

    /**
     * Claims the oldest pending job by locking its row. Rows locked by other workers are skipped.
     */
    @Query(value = """
            SELECT * FROM notification_deliveries
            WHERE status = 'PENDING'
            ORDER BY created_at
            LIMIT 1
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    Optional<NotificationDelivery> claimNext();

    /**
     * Marks a job FAILED, unless another worker has already sent it.
     */
    @Modifying
    @Query(value = """
            UPDATE notification_deliveries SET status = 'FAILED', last_error = :error
            WHERE id = :id AND status = 'PENDING'
            """, nativeQuery = true)
    int markFailed(@Param("id") UUID id, @Param("error") String error);

}
