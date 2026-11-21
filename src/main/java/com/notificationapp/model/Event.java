package com.notificationapp.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "events")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 32, updatable = false)
    private EventType type;

    @Column(nullable = false, length = 200, updatable = false)
    private String title;

    @Column(nullable = false, length = 100, updatable = false)
    private String source;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    /**
     * Type-specific details, stored as sent in a {@code JSONB} column.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(updatable = false)
    private Map<String, Object> payload;

    @Column(name = "received_at", nullable = false, updatable = false)
    private Instant receivedAt;

    public Event(EventType type, String title, String source, Instant occurredAt,
                 Map<String, Object> payload, Instant receivedAt) {
        this.type = type;
        this.title = title;
        this.source = source;
        this.occurredAt = occurredAt;
        this.payload = payload;
        this.receivedAt = receivedAt;
    }

}
