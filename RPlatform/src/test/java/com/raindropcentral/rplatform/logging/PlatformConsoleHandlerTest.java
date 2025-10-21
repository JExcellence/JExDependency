package com.raindropcentral.rplatform.logging;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class PlatformConsoleHandlerTest {

    private Logger logger;
    private RecordingOutputStream output;

    @BeforeEach
    void setUp() {
        logger = Logger.getLogger(PlatformConsoleHandlerTest.class.getName());
        logger.setUseParentHandlers(false);
        for (Handler handler : logger.getHandlers()) {
            logger.removeHandler(handler);
        }
        output = new RecordingOutputStream();
    }

    @AfterEach
    void tearDown() {
        for (Handler handler : logger.getHandlers()) {
            logger.removeHandler(handler);
        }
    }

    @Test
    void publishWritesFormattedOutputAndFlushes() {
        final PlatformConsoleHandler handler = handler(Level.INFO);
        logger.addHandler(handler);
        try {
            logger.log(Level.INFO, "console-line");

            final String written = output.content();
            assertFalse(written.isEmpty(), "Expected formatted output to be written");
            assertTrue(written.contains("[INFO]"));
            assertTrue(written.contains("console-line"));
            assertTrue(output.getFlushCount() >= 1, "Publish should flush the output stream");
        } finally {
            logger.removeHandler(handler);
            handler.close();
        }
    }

    @Test
    void levelFilteringPreventsLowerLevelPublishing() {
        final PlatformConsoleHandler handler = handler(Level.WARNING);
        logger.addHandler(handler);
        try {
            logger.log(Level.INFO, "ignored");
            assertEquals("", output.content(), "INFO should be filtered when handler level is WARNING");

            logger.log(Level.SEVERE, "accepted");
            final String written = output.content();
            assertTrue(written.contains("[SEVERE]"));
            assertTrue(written.contains("accepted"));
        } finally {
            logger.removeHandler(handler);
            handler.close();
        }
    }

    @Test
    void closeFlushesWithoutClosingUnderlyingStream() {
        final PlatformConsoleHandler handler = handler(Level.INFO);
        logger.addHandler(handler);
        try {
            logger.log(Level.INFO, "before-remove");
            assertFalse(output.content().isEmpty(), "Initial write should reach the output stream");
            output.reset();

            logger.removeHandler(handler);
            logger.log(Level.INFO, "after-remove");
            assertEquals("", output.content(), "Handler removal should prevent further writes");

            final int flushBeforeClose = output.getFlushCount();
            handler.close();
            assertEquals(flushBeforeClose + 1, output.getFlushCount(), "close() should flush");
            assertFalse(output.isClosed(), "Underlying stream must remain open");
        } finally {
            handler.close();
        }
    }

    private PlatformConsoleHandler handler(final Level level) {
        final PlatformConsoleHandler handler = new PlatformConsoleHandler(output);
        handler.setFormatter(new EnhancedLogFormatter(false));
        handler.setLevel(level);
        return handler;
    }

    private static final class RecordingOutputStream extends OutputStream {
        private final ByteArrayOutputStream delegate = new ByteArrayOutputStream();
        private int flushCount;
        private boolean closed;

        @Override
        public void write(final int b) {
            delegate.write(b);
        }

        @Override
        public void flush() throws IOException {
            flushCount++;
            delegate.flush();
        }

        @Override
        public void close() throws IOException {
            closed = true;
            delegate.close();
        }

        String content() {
            return delegate.toString(StandardCharsets.UTF_8);
        }

        void reset() {
            delegate.reset();
        }

        int getFlushCount() {
            return flushCount;
        }

        boolean isClosed() {
            return closed;
        }
    }
}
