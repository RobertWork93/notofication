package com.notificationapp.support;

import com.notificationapp.model.Alert;
import com.notificationapp.model.Channel;
import com.notificationapp.model.Event;
import com.notificationapp.model.EventType;
import com.notificationapp.model.Role;
import com.notificationapp.model.User;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Builds domain objects with fixed ids for unit and Web MVC tests.
 */
public final class TestDataFactory {

    public static final UUID USER_ID = UUID.fromString("0b9f1d2e-3c4a-4b5c-8d6e-7f8091a2b3c4");
    public static final UUID OTHER_USER_ID = UUID.fromString("5a6b7c8d-9e0f-4a1b-8c2d-3e4f5a6b7c8d");
    public static final UUID ADMIN_ID = UUID.fromString("a0000000-0000-4000-8000-000000000001");
    public static final UUID ALERT_ID = UUID.fromString("8d2e4b7a-1c3f-4e5a-9b6d-0f1e2d3c4b5a");
    public static final UUID EVENT_ID = UUID.fromString("3f6c1a52-9a1e-4c0e-9a7e-2a1b5f0c9d11");

    /**
     * Stored hash for test users. The {@code {noop}} prefix means the password is "password123" in plain text.
     */
    public static final String PASSWORD_HASH = "{noop}password123";

    private TestDataFactory() {
    }

    public static User user(String email, String name, Instant createdAt) {
        User user = new User(email, name, PASSWORD_HASH, Role.USER, createdAt);
        user.setId(USER_ID);
        return user;
    }

    public static User jane(Instant createdAt) {
        return user("jane.doe@example.com", "Jane Doe", createdAt);
    }

    public static User admin(Instant createdAt) {
        User admin = new User("admin@example.com", "Administrator", PASSWORD_HASH, Role.ADMIN, createdAt);
        admin.setId(ADMIN_ID);
        return admin;
    }

    public static Alert alert(String name, EventType eventType, Channel channel, boolean enabled, Instant createdAt) {
        Alert alert = new Alert(jane(createdAt), name, eventType, channel, enabled, createdAt);
        alert.setId(ALERT_ID);
        return alert;
    }

    public static Alert earthquakeAlert(Instant createdAt) {
        return alert("Earthquakes", EventType.NATURAL_DISASTER, Channel.EMAIL, true, createdAt);
    }

    public static Event event(EventType type, String title, String source, Instant occurredAt,
                              Map<String, Object> payload, Instant receivedAt) {
        Event event = new Event(type, title, source, occurredAt, payload, receivedAt);
        event.setId(EVENT_ID);
        return event;
    }

}
