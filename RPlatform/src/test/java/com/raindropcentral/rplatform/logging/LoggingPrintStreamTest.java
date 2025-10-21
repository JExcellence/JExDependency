package com.raindropcentral.rplatform.logging;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class LoggingPrintStreamTest {

    @Test
    void printFlushRoutesLineThroughLogger() {
        final Logger logger = mock(Logger.class);
        final TrackingPrintStream original = new TrackingPrintStream();
        final LoggingPrintStream stream = new LoggingPrintStream(logger, Level.INFO, original);

        stream.print("hello world");

        verify(logger, never()).log(eq(Level.INFO), eq("📝 hello world"));

        stream.flush();

        final ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(logger).log(eq(Level.INFO), messageCaptor.capture());
        assertEquals("📝 hello world", messageCaptor.getValue());
        assertTrue(original.wasFlushed(), "flush should be delegated to the original stream");
    }

    @Test
    void printlnRoutesLineThroughLoggerWithSeverityPrefix() {
        final Logger logger = mock(Logger.class);
        final LoggingPrintStream stream = new LoggingPrintStream(logger, Level.SEVERE, null);

        stream.println("danger ahead");

        final ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(logger).log(eq(Level.SEVERE), messageCaptor.capture());
        assertEquals("🚨 danger ahead", messageCaptor.getValue());
    }

    @Test
    void formatRoutesMessageThroughLogger() {
        final Logger logger = mock(Logger.class);
        final LoggingPrintStream stream = new LoggingPrintStream(logger, Level.INFO, null);

        stream.format("value %d%n", 42);

        final ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(logger).log(eq(Level.INFO), messageCaptor.capture());
        assertEquals("📝 value 42", messageCaptor.getValue());
    }

    @Test
    void closeFlushesPendingContentAndClosesOriginalStream() {
        final Logger logger = mock(Logger.class);
        final TrackingPrintStream original = new TrackingPrintStream();
        final LoggingPrintStream stream = new LoggingPrintStream(logger, Level.INFO, original);

        stream.print("final message");
        stream.close();

        final ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(logger).log(eq(Level.INFO), messageCaptor.capture());
        assertEquals("📝 final message", messageCaptor.getValue());
        assertTrue(original.wasFlushed(), "close should flush the original stream");
        assertTrue(original.wasClosed(), "close should close the original stream");
    }

    @Test
    void concurrentWritesProduceIsolatedLogRecords() throws InterruptedException {
        final RecordingLogger logger = new RecordingLogger();
        final ByteArrayOutputStream mirror = new ByteArrayOutputStream();
        final LoggingPrintStream stream = new LoggingPrintStream(
                logger,
                Level.INFO,
                new PrintStream(mirror, true, StandardCharsets.UTF_8)
        );

        final int messagesPerThread = 50;
        final ExecutorService executor = Executors.newFixedThreadPool(2);
        final CountDownLatch startLatch = new CountDownLatch(1);

        final Runnable writer = () -> {
            try {
                startLatch.await();
                for (int i = 0; i < messagesPerThread; i++) {
                    stream.println(Thread.currentThread().getName() + "-" + i);
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        };

        executor.submit(writer);
        executor.submit(writer);
        startLatch.countDown();
        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS), "writers should finish promptly");
        stream.flush();

        final List<LogRecord> records = logger.getRecords();
        assertEquals(messagesPerThread * 2, records.size(), "Each println should produce one log record");
        assertTrue(records.stream().allMatch(r -> r.getMessage().startsWith("📝 ")));
        assertTrue(records.stream().noneMatch(r -> r.getMessage().contains("\n")),
                "Messages should be isolated without embedded newlines");
    }

    private static final class TrackingPrintStream extends PrintStream {

        private boolean flushed;
        private boolean closed;

        private TrackingPrintStream() {
            super(new ByteArrayOutputStream(), true);
        }

        @Override
        public void flush() {
            flushed = true;
            super.flush();
        }

        @Override
        public void close() {
            closed = true;
            super.close();
        }

        private boolean wasFlushed() {
            return flushed;
        }

        private boolean wasClosed() {
            return closed;
        }
    }

    private static final class RecordingLogger extends Logger {

        private final List<LogRecord> records = Collections.synchronizedList(new ArrayList<>());

        private RecordingLogger() {
            super("recording", null);
            setLevel(Level.ALL);
        }

        @Override
        public void log(final LogRecord record) {
            records.add(record);
        }

        @Override
        public void log(final Level level, final String msg) {
            final LogRecord record = new LogRecord(level, msg);
            record.setLoggerName(getName());
            log(record);
        }

        private List<LogRecord> getRecords() {
            return records;
        }
    }
}
