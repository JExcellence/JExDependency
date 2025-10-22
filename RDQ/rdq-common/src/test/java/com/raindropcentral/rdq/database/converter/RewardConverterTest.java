package com.raindropcentral.rdq.database.converter;

import com.raindropcentral.rdq.database.json.reward.RewardParser;
import com.raindropcentral.rdq.reward.AbstractReward;
import com.raindropcentral.rdq.reward.CommandReward;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.slf4j.event.Level;
import org.slf4j.impl.StaticLoggerBinder;
import org.slf4j.impl.TestLogger;
import org.slf4j.impl.TestLoggerFactory;
import org.slf4j.impl.TestLoggingEvent;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mockStatic;

class RewardConverterTest {

    private RewardConverter converter;
    private TestLoggerFactory loggerFactory;

    @BeforeEach
    void setUp() {
        this.converter = new RewardConverter();
        this.loggerFactory = StaticLoggerBinder.getSingleton().getTestLoggerFactory();
        this.loggerFactory.clear();
    }

    @Test
    void convertToDatabaseColumnReturnsNullWhenRewardNull() {
        assertNull(this.converter.convertToDatabaseColumn(null));
    }

    @Test
    void convertToEntityAttributeReturnsNullWhenJsonNull() {
        assertNull(this.converter.convertToEntityAttribute(null));
    }

    @Test
    void convertToDatabaseColumnAndEntityAttributeRoundTrip() {
        final CommandReward reward = new CommandReward("say hello", true, 40L);

        final String json = this.converter.convertToDatabaseColumn(reward);
        assertNotNull(json);

        final AbstractReward restored = this.converter.convertToEntityAttribute(json);
        final CommandReward restoredReward = assertInstanceOf(CommandReward.class, restored);
        assertEquals(reward.getCommand(), restoredReward.getCommand());
        assertEquals(reward.isExecuteAsPlayer(), restoredReward.isExecuteAsPlayer());
        assertEquals(reward.getDelayTicks(), restoredReward.getDelayTicks());
    }

    @Test
    void convertToDatabaseColumnLogsAndWrapsOnSerializationError() {
        final CommandReward reward = new CommandReward("say boom", false, 0L);
        final IOException failure = new IOException("serialize failure");

        try (MockedStatic<RewardParser> parserMock = mockStatic(RewardParser.class)) {
            parserMock.when(() -> RewardParser.serialize(reward)).thenThrow(failure);

            final RuntimeException exception = assertThrows(
                    RuntimeException.class,
                    () -> this.converter.convertToDatabaseColumn(reward)
            );

            assertEquals("Failed to serialize requirement", exception.getMessage());
            assertSame(failure, exception.getCause());
        }

        final TestLogger logger = this.loggerFactory.getLoggerFor(RewardConverter.class.getName());
        final TestLoggingEvent event = logger.getLastEvent();
        assertNotNull(event);
        assertEquals(Level.ERROR, event.level());
        assertEquals("Failed to serialize requirement: {}", event.message());
        assertArrayEquals(new Object[]{reward, failure}, event.arguments());
        assertNull(event.throwable());
    }

    @Test
    void convertToEntityAttributeLogsAndWrapsOnDeserializationError() {
        final String json = "{\"type\":\"COMMAND\"}";
        final IOException failure = new IOException("parse failure");

        try (MockedStatic<RewardParser> parserMock = mockStatic(RewardParser.class)) {
            parserMock.when(() -> RewardParser.parse(json)).thenThrow(failure);

            final RuntimeException exception = assertThrows(
                    RuntimeException.class,
                    () -> this.converter.convertToEntityAttribute(json)
            );

            assertEquals("Failed to deserialize requirement", exception.getMessage());
            assertSame(failure, exception.getCause());
        }

        final TestLogger logger = this.loggerFactory.getLoggerFor(RewardConverter.class.getName());
        final TestLoggingEvent event = logger.getLastEvent();
        assertNotNull(event);
        assertEquals(Level.ERROR, event.level());
        assertEquals("Failed to deserialize requirement from database string: {}", event.message());
        assertArrayEquals(new Object[]{json, failure}, event.arguments());
        assertNull(event.throwable());
    }
}
