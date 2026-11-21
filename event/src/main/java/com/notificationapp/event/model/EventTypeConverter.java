package com.notificationapp.event.model;

import com.notificationapp.event.api.EventType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class EventTypeConverter implements AttributeConverter<EventType, String> {

    @Override
    public String convertToDatabaseColumn(EventType type) {
        return type == null ? null : type.getValue();
    }

    @Override
    public EventType convertToEntityAttribute(String value) {
        return value == null ? null : EventType.fromValue(value);
    }

}
