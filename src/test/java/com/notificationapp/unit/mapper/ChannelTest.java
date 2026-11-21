package com.notificationapp.unit.mapper;

import com.notificationapp.model.Channel;
import com.notificationapp.model.ChannelConverter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChannelTest {

    private final JsonMapper jsonMapper = JsonMapper.builder().build();
    private final ChannelConverter converter = new ChannelConverter();

    @ParameterizedTest
    @CsvSource({"email,EMAIL", "slack,SLACK"})
    void shouldMapLowercaseValueToConstant(String value, Channel expected) {
        assertThat(Channel.fromValue(value)).isEqualTo(expected);
        assertThat(expected.getValue()).isEqualTo(value);
    }

    @ParameterizedTest
    @ValueSource(strings = {"EMAIL", "sms", ""})
    void shouldRejectUnknownValues(String value) {
        assertThatThrownBy(() -> Channel.fromValue(value))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(value);
    }

    @Test
    void shouldUseLowercaseValueInJson() {
        assertThat(jsonMapper.writeValueAsString(Channel.SLACK)).isEqualTo("\"slack\"");
        assertThat(jsonMapper.readValue("\"email\"", Channel.class)).isEqualTo(Channel.EMAIL);
    }

    @Test
    void shouldRejectUnknownValueInJson() {
        assertThatThrownBy(() -> jsonMapper.readValue("\"sms\"", Channel.class))
                .isInstanceOf(JacksonException.class);
    }

    @Test
    void shouldStoreLowercaseValueInDatabase() {
        assertThat(converter.convertToDatabaseColumn(Channel.EMAIL)).isEqualTo("email");
        assertThat(converter.convertToEntityAttribute("slack")).isEqualTo(Channel.SLACK);
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
        assertThat(converter.convertToEntityAttribute(null)).isNull();
    }

}
