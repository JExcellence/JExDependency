package com.raindropcentral.rdq.database.converter;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConverterToolTest {

    private final ConverterTool tool   = new ConverterTool();
    private final Logger        logger = Logger.getLogger(ConverterTool.class.getName());

    private TestLogHandler handler;

    @BeforeEach
    void setUp() {
        this.handler = new TestLogHandler();
        this.logger.addHandler(this.handler);
    }

    @AfterEach
    void tearDown() {
        this.logger.removeHandler(this.handler);
    }

    @Test
    void setPrivateFieldUpdatesValue() {
        final Fixture fixture = new Fixture();

        this.tool.setPrivateField(fixture, "value", "updated", this.logger);

        assertEquals("updated", fixture.getValue());
    }

    @Test
    void setPrivateFieldLogsAndThrowsWhenFieldMissing() {
        final Fixture fixture = new Fixture();

        final RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> this.tool.setPrivateField(fixture, "missing", "data", this.logger)
        );

        assertEquals("Failed to set field: missing", exception.getMessage());

        final LogRecord record = this.handler.getLastRecord();
        assertEquals(Level.SEVERE, record.getLevel());
        assertEquals("Failed to set field: missing", record.getMessage());
    }

    private static final class Fixture {

        private String value = "original";

        String getValue() {
            return this.value;
        }
    }

    private static final class TestLogHandler extends Handler {

        private LogRecord lastRecord;

        @Override
        public void publish(final LogRecord record) {
            this.lastRecord = record;
        }

        @Override
        public void flush() {
            // no-op
        }

        @Override
        public void close() {
            // no-op
        }

        LogRecord getLastRecord() {
            return this.lastRecord;
        }
    }
}
