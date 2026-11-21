package com.notificationapp.unit.mapper;

import com.notificationapp.event.api.EventType;
import com.notificationapp.event.model.EventTypeConverter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EventTypeTest {

    private final JsonMapper jsonMapper = JsonMapper.builder().build();
    private final EventTypeConverter converter = new EventTypeConverter();

    @ParameterizedTest
    @CsvSource({"breaking_news,BREAKING_NEWS", "natural_disaster,NATURAL_DISASTER"})
    void shouldMapLowercaseValueToConstant(String value, EventType expected) {
        assertThat(EventType.fromValue(value)).isEqualTo(expected);
        assertThat(expected.getValue()).isEqualTo(value);
    }

    @ParameterizedTest
    @ValueSource(strings = {"BREAKING_NEWS", "breaking_new", "market_movement", ""})
    void shouldRejectUnknownValues(String value) {
        assertThatThrownBy(() -> EventType.fromValue(value))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(value);
    }

    @Test
    void shouldUseLowercaseValueInJson() {
        assertThat(jsonMapper.writeValueAsString(EventType.NATURAL_DISASTER)).isEqualTo("\"natural_disaster\"");
        assertThat(jsonMapper.readValue("\"breaking_news\"", EventType.class)).isEqualTo(EventType.BREAKING_NEWS);
    }

    @Test
    void shouldRejectUnknownValueInJson() {
        assertThatThrownBy(() -> jsonMapper.readValue("\"earthquake\"", EventType.class))
                .isInstanceOf(JacksonException.class);
    }

    @Test
    void shouldStoreLowercaseValueInDatabase() {
        assertThat(converter.convertToDatabaseColumn(EventType.BREAKING_NEWS)).isEqualTo("breaking_news");
        assertThat(converter.convertToEntityAttribute("natural_disaster")).isEqualTo(EventType.NATURAL_DISASTER);
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
        assertThat(converter.convertToEntityAttribute(null)).isNull();
    }

}
