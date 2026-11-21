package com.notificationapp.notification.model;

import com.notificationapp.notification.api.Channel;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ChannelConverter implements AttributeConverter<Channel, String> {

    @Override
    public String convertToDatabaseColumn(Channel channel) {
        return channel == null ? null : channel.getValue();
    }

    @Override
    public Channel convertToEntityAttribute(String value) {
        return value == null ? null : Channel.fromValue(value);
    }

}
