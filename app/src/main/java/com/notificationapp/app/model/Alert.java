package com.notificationapp.app.model;

import com.notificationapp.event.api.EventType;
import com.notificationapp.notification.api.Channel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "alerts")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "event_type", nullable = false, length = 32)
    private EventType eventType;

    @Column(nullable = false, length = 16)
    private Channel channel;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Alert(User user, String name, EventType eventType, Channel channel, boolean enabled, Instant now) {
        this.user = user;
        this.name = name;
        this.eventType = eventType;
        this.channel = channel;
        this.enabled = enabled;
        this.createdAt = now;
        this.updatedAt = now;
    }

}
