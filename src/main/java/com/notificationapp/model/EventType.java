package com.notificationapp.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * The two hardcoded event types. The lowercase value is used in JSON and in the database.
 */
public enum EventType {

    BREAKING_NEWS("breaking_news"),
    NATURAL_DISASTER("natural_disaster");

    private final String value;

    EventType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
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
