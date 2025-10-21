package com.raindropcentral.rplatform.logging;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import static org.junit.jupiter.api.Assertions.*;

class PlatformLogFormatterTest {

    private static final DateTimeFormatter FULL_TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    @Test
    void formatIncludesTimestampLevelContextAndMessage() {
        final PlatformLogFormatter formatter = new PlatformLogFormatter(false);
        final LogRecord record = new LogRecord(Level.WARNING, "System ready");
        record.setLoggerName("com.raindropcentral.rplatform.logging.PlatformLogFormatter");
        record.setMillis(Instant.parse("2024-05-14T10:15:30Z").toEpochMilli());

        final String formatted = formatter.format(record);

        final String expectedTimestamp = FULL_TIMESTAMP.format(Instant.ofEpochMilli(record.getMillis()));
        final String expected = "[" + expectedTimestamp + "] [WARNING] c.r.r.l.PlatformLogFormatter: System ready\n";

        assertEquals(expected, formatted);
    }

    @Test
    void formatSanitizesControlCharactersInMessages() {
        final PlatformLogFormatter formatter = new PlatformLogFormatter(false);
        final LogRecord record = new LogRecord(Level.INFO, "Line one\nLine two\t\u001B[31mAlert");
        record.setLoggerName("com.raindropcentral.test.MultiLine");
        record.setMillis(Instant.parse("2024-05-14T11:00:00Z").toEpochMilli());

        final String formatted = formatter.format(record);

        final String expectedTimestamp = FULL_TIMESTAMP.format(Instant.ofEpochMilli(record.getMillis()));
        final String expectedMessage = "Line oneLine twoAlert";
        final String expected = "[" + expectedTimestamp + "] [INFO] com.raindropcentral.test.MultiLine: "
                + expectedMessage + "\n";

        assertEquals(expected, formatted);
        assertFalse(formatted.contains("\nLine"));
        assertFalse(formatted.contains("\t"));
        assertFalse(formatted.contains("\u001B"));
    }

    @Test
    void formatAppendsSanitizedThrowableStackTrace() {
        final PlatformLogFormatter formatter = new PlatformLogFormatter(false);
        final LogRecord record = new LogRecord(Level.SEVERE, "Critical failure");
        record.setLoggerName("com.raindropcentral.test.Exceptional");
        record.setMillis(Instant.parse("2024-05-14T12:30:00Z").toEpochMilli());

        final IllegalStateException throwable = new IllegalStateException("boom");
        throwable.setStackTrace(new StackTraceElement[]{
                new StackTraceElement("example.Service", "handle", "Service.java", 27),
                new StackTraceElement("example.Service", "invoke", "Service.java", 31)
        });
        record.setThrown(throwable);

        final String formatted = formatter.format(record);

        final String expectedTimestamp = FULL_TIMESTAMP.format(Instant.ofEpochMilli(record.getMillis()));
        final String expectedPrefix = "[" + expectedTimestamp + "] [SEVERE] com.raindropcentral.test.Exceptional: Critical failure";

        assertTrue(formatted.startsWith(expectedPrefix));
        assertTrue(formatted.contains("Critical failure\n"));
        assertTrue(formatted.contains("java.lang.IllegalStateException: boom"));
        assertTrue(formatted.contains("at example.Service.handle(Service.java:27)"));
        assertTrue(formatted.contains("at example.Service.invoke(Service.java:31)"));
        assertFalse(formatted.contains("\t"));
        assertTrue(formatted.endsWith("\n"));
    }
}
