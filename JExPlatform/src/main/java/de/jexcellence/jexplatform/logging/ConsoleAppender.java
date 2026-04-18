package de.jexcellence.jexplatform.logging;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.logging.LogRecord;
import java.util.logging.StreamHandler;

/**
 * Console handler that writes to {@link System#out} instead of {@link System#err}.
 *
 * <p>Paper renders {@code stderr} output in red — this handler avoids that by
 * capturing the original {@code stdout} stream at class-load time and writing
 * exclusively to it. Every record is flushed immediately so output appears
 * without delay.
 *
 * @author JExcellence
 * @since 1.0.0
 */
final class ConsoleAppender extends StreamHandler {

    /** Captured at class-load time before any plugin can redirect streams. */
    private static final PrintStream ORIGINAL_OUT = System.out;

    ConsoleAppender(String name, LogLevel level) {
        super(safePrintStream(), new LogFormatter(name));
        setLevel(level.toJulLevel());
    }

    @Override
    public synchronized void publish(LogRecord record) {
        super.publish(record);
        flush();
    }

    @Override
    public synchronized void close() {
        flush();
        // Do NOT close stdout — other loggers share it.
    }

    /**
     * Updates the minimum level shown in the console.
     *
     * @param level the new minimum level
     */
    void setConsoleLevel(LogLevel level) {
        setLevel(level.toJulLevel());
    }

    private static OutputStream safePrintStream() {
        return ORIGINAL_OUT;
    }
}
