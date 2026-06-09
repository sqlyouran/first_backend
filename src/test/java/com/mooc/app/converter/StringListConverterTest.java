package com.mooc.app.converter;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StringListConverterTest {

    private final StringListConverter converter = new StringListConverter();

    @Test
    void serializeListToJsonString() {
        List<String> input = List.of("travel", "food");
        String result = converter.convertToDatabaseColumn(input);
        assertEquals("[\"travel\",\"food\"]", result);
    }

    @Test
    void deserializeJsonStringToList() {
        String input = "[\"travel\",\"food\"]";
        List<String> result = converter.convertToEntityAttribute(input);
        assertEquals(List.of("travel", "food"), result);
    }

    @Test
    void serializeNullReturnsNull() {
        assertNull(converter.convertToDatabaseColumn(null));
    }

    @Test
    void deserializeNullReturnsEmptyList() {
        List<String> result = converter.convertToEntityAttribute(null);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void serializeEmptyListReturnsEmptyJsonArray() {
        String result = converter.convertToDatabaseColumn(List.of());
        assertEquals("[]", result);
    }
}
