package com.notificationapp.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * The two hardcoded event types. The lowercase value is used in JSON and in the database;
 * the label is used in notification messages.
 */
public enum EventType {

    BREAKING_NEWS("breaking_news", "Breaking news"),
    NATURAL_DISASTER("natural_disaster", "Natural disaster");

    private final String value;
    private final String label;

    EventType(String value, String label) {
        this.value = value;
        this.label = label;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    public String getLabel() {
        return label;
    }

    @JsonCreator
    public static EventType fromValue(String value) {
        for (EventType type : values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown event type: '" + value + "'");
    }

}
