package com.notificationapp.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Where an alert's notifications are delivered. The lowercase value is used in JSON and in the database.
 */
public enum Channel {

    EMAIL("email"),
    SLACK("slack");

    private final String value;

    Channel(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static Channel fromValue(String value) {
        for (Channel channel : values()) {
            if (channel.value.equals(value)) {
                return channel;
            }
        }
        throw new IllegalArgumentException("Unknown channel: '" + value + "'");
    }

}
