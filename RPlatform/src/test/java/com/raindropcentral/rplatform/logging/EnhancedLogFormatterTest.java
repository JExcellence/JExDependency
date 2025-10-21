package com.raindropcentral.rplatform.logging;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import static org.junit.jupiter.api.Assertions.*;

class EnhancedLogFormatterTest {

    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss.SSS")
            .withZone(ZoneId.systemDefault());

    @Test
    void formatIncludesTimestampLevelAndMessage() {
        final EnhancedLogFormatter formatter = new EnhancedLogFormatter(false);
        final LogRecord record = new LogRecord(Level.INFO, "hello world");
        record.setLoggerName("com.raindropcentral.sample.Service");
        record.setMillis(Instant.parse("2024-05-14T10:15:30.456Z").toEpochMilli());

        final String formatted = formatter.format(record);

        final String expectedTimestamp = TIMESTAMP_FORMATTER.format(Instant.ofEpochMilli(record.getMillis()));
        final String expected = "[" + expectedTimestamp + "] [INFO] com.raindropcentral.sample.Service: hello world\n";

        assertEquals(expected, formatted);
    }

    @Test
    void formatStripsControlCharactersFromMultiLineMessages() {
        final EnhancedLogFormatter formatter = new EnhancedLogFormatter(false);
        final LogRecord record = new LogRecord(Level.WARNING, "Line one\nLine two\tTabbed");
        record.setLoggerName("com.raindropcentral.sample.MultiLine");
        record.setMillis(Instant.parse("2024-05-14T11:30:00.000Z").toEpochMilli());

        final String formatted = formatter.format(record);
        final String expectedTimestamp = TIMESTAMP_FORMATTER.format(Instant.ofEpochMilli(record.getMillis()));
        final String expectedMessage = "Line oneLine twoTabbed";
        final String expected = "[" + expectedTimestamp + "] [WARNING] com.raindropcentral.sample.MultiLine: "
                + expectedMessage + "\n";

        assertEquals(expected, formatted);
        assertFalse(formatted.contains("Line two\n"));
    }

    @Test
    void formatAppendsThrowableStackTrace() {
        final EnhancedLogFormatter formatter = new EnhancedLogFormatter(false);
        final LogRecord record = new LogRecord(Level.SEVERE, "critical failure");
        record.setLoggerName("com.raindropcentral.sample.Exceptional");
        record.setMillis(Instant.parse("2024-05-14T12:45:00.789Z").toEpochMilli());

        final IllegalStateException throwable = new IllegalStateException("boom");
        throwable.setStackTrace(new StackTraceElement[]{
                new StackTraceElement("example.Service", "handle", "Service.java", 27),
                new StackTraceElement("example.Service", "invoke", "Service.java", 31)
        });
        record.setThrown(throwable);

        final String formatted = formatter.format(record);
        final String expectedTimestamp = TIMESTAMP_FORMATTER.format(Instant.ofEpochMilli(record.getMillis()));
        final StringBuilder expected = new StringBuilder()
                .append("[")
                .append(expectedTimestamp)
                .append("] [SEVERE] com.raindropcentral.sample.Exceptional: critical failure\n")
                .append(throwable)
                .append("\n")
                .append("\tat example.Service.handle(Service.java:27)\n")
                .append("\tat example.Service.invoke(Service.java:31)\n");

        assertEquals(expected.toString(), formatted);
    }
}
