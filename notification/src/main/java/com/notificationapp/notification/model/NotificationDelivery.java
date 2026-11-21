package com.notificationapp.notification.model;

import com.notificationapp.notification.api.Channel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * One notification to send: an event, an alert that matched it, and where to send it.
 * Rows are created with an idempotent native insert, so this entity is only read and updated.
 */
@Entity
@Table(name = "notification_deliveries")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationDelivery {

    @Id
    private UUID id;

    @Column(name = "event_id", nullable = false, updatable = false)
    private UUID eventId;

    @Column(name = "alert_id", nullable = false, updatable = false)
    private UUID alertId;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(nullable = false, length = 16, updatable = false)
    private Channel channel;

    @Column(nullable = false, length = 320, updatable = false)
    private String recipient;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private DeliveryStatus status;

    @Column(name = "last_error", length = 1000)
    private String lastError;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    public NotificationDelivery(UUID id, UUID eventId, UUID alertId, UUID userId, Channel channel, String recipient,
                                Instant now) {
        this.id = id;
        this.eventId = eventId;
        this.alertId = alertId;
        this.userId = userId;
        this.channel = channel;
        this.recipient = recipient;
        this.status = DeliveryStatus.PENDING;
        this.createdAt = now;
    }

    public void markSent(Instant now) {
        this.status = DeliveryStatus.SENT;
        this.sentAt = now;
    }

}
