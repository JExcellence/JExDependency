package com.raindropcentral.rdq.database.converter;

import com.raindropcentral.rdq.database.json.requirement.RequirementParser;
import com.raindropcentral.rdq.requirement.AbstractRequirement;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mockStatic;

class RequirementConverterTest {

    private RequirementConverter converter;

    @BeforeEach
    void setUp() {
        this.converter = new RequirementConverter();
    }

    @Test
    void convertToDatabaseColumnReturnsNullWhenRequirementNull() {
        assertNull(this.converter.convertToDatabaseColumn(null));
    }

    @Test
    void convertToEntityAttributeReturnsNullWhenJsonNull() {
        assertNull(this.converter.convertToEntityAttribute(null));
    }

    @Test
    void convertToDatabaseColumnUsesParserSerialization() {
        final AbstractRequirement requirement = new StubRequirement();
        final String expectedJson = "{\"type\":\"CUSTOM\"}";

        try (MockedStatic<RequirementParser> parserMock = mockStatic(RequirementParser.class)) {
            parserMock.when(() -> RequirementParser.serialize(requirement)).thenReturn(expectedJson);

            final String json = this.converter.convertToDatabaseColumn(requirement);
            assertEquals(expectedJson, json);
        }
    }

    @Test
    void convertToEntityAttributeUsesParserDeserialization() {
        final String json = "{\"type\":\"CUSTOM\"}";
        final AbstractRequirement requirement = new StubRequirement();

        try (MockedStatic<RequirementParser> parserMock = mockStatic(RequirementParser.class)) {
            parserMock.when(() -> RequirementParser.parse(json)).thenReturn(requirement);

            final AbstractRequirement result = this.converter.convertToEntityAttribute(json);
            assertSame(requirement, result);
        }
    }

    @Test
    void convertToDatabaseColumnWrapsIOExceptionFromParser() {
        final AbstractRequirement requirement = new StubRequirement();
        final IOException failure = new IOException("serialize failure");

        try (MockedStatic<RequirementParser> parserMock = mockStatic(RequirementParser.class)) {
            parserMock.when(() -> RequirementParser.serialize(requirement)).thenThrow(failure);

            final RuntimeException exception = assertThrows(
                    RuntimeException.class,
                    () -> this.converter.convertToDatabaseColumn(requirement)
            );

            assertEquals("Failed to serialize requirement", exception.getMessage());
            assertSame(failure, exception.getCause());
        }
    }

    @Test
    void convertToEntityAttributeWrapsIOExceptionFromParser() {
        final String json = "{\"type\":\"CUSTOM\"}";
        final IOException failure = new IOException("deserialize failure");

        try (MockedStatic<RequirementParser> parserMock = mockStatic(RequirementParser.class)) {
            parserMock.when(() -> RequirementParser.parse(json)).thenThrow(failure);

            final RuntimeException exception = assertThrows(
                    RuntimeException.class,
                    () -> this.converter.convertToEntityAttribute(json)
            );

            assertEquals("Failed to deserialize requirement", exception.getMessage());
            assertSame(failure, exception.getCause());
        }
    }

    private static final class StubRequirement extends AbstractRequirement {

        private StubRequirement() {
            super(Type.CUSTOM);
        }

        @Override
        public boolean isMet(@NotNull final Player player) {
            return false;
        }

        @Override
        public double calculateProgress(@NotNull final Player player) {
            return 0.0D;
        }

        @Override
        public void consume(@NotNull final Player player) {
        }

        @Override
        public @NotNull String getDescriptionKey() {
            return "stub.requirement";
        }
    }
}
