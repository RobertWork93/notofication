package com.notificationapp.repository;

import com.notificationapp.model.OutboxRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface OutboxRepository extends JpaRepository<OutboxRecord, UUID> {

    /**
     * Claims the oldest pending record by locking its row. Rows locked by other workers are skipped,
     * so two workers never get the same record. The lock lasts until the surrounding transaction ends.
     */
    @Query(value = """
            SELECT * FROM event_outbox
            WHERE status = 'PENDING'
            ORDER BY created_at
            LIMIT 1
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    Optional<OutboxRecord> claimNext();

    /**
     * Marks a record FAILED, unless another worker has already processed it.
     */
    @Modifying
    @Query(value = """
            UPDATE event_outbox SET status = 'FAILED', last_error = :error
            WHERE id = :id AND status = 'PENDING'
            """, nativeQuery = true)
    int markFailed(@Param("id") UUID id, @Param("error") String error);

}
