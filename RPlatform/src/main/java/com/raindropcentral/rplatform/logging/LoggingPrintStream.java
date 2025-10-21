package com.raindropcentral.rplatform.logging;

import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * LoggingPrintStream captures output written to redirected {@link System#out} and
 * {@link System#err}, forwarding distinct lines into {@link CentralLogger}'s routing without
 * producing feedback loops. The stream tags severity levels so that {@link PlatformLogger}
 * consumers receive consistent formatting.
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
public class LoggingPrintStream extends PrintStream {

    private static final Pattern ALREADY_FORMATTED_LINE = Pattern.compile(
            "^\\[\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}] \\[(SEVERE|WARNING|WARN|INFO|CONFIG|DEBUG|TRACE|FINE|FINER|FINEST)].*"
    );

    /**
     * Target logger used to emit redirected console lines.
     */
    private final Logger logger;

    /**
     * Severity applied to redirected lines when publishing to {@link #logger}.
     */
    private final Level level;

    /**
     * Original stream preserved for pass-through flushing and optional mirroring.
     */
    private final PrintStream originalStream;

    /**
     * Line buffer used to aggregate writes until a newline delimiter is encountered.
     */
    private final ByteArrayOutputStream buffer;

    /**
     * Creates a new logging print stream that forwards complete lines into the supplied logger.
     *
     * @param logger          destination logger managed by {@link CentralLogger}
     * @param level           severity to apply when forwarding
     * @param originalStream  optional original stream to flush/close alongside the redirect
     */
    public LoggingPrintStream(@NotNull final Logger logger,
                              @NotNull final Level level,
                              final PrintStream originalStream) {
        super(new ByteArrayOutputStream());
        this.logger = logger;
        this.level = level;
        this.originalStream = originalStream;
        this.buffer = (ByteArrayOutputStream) out;
    }

    @Override
    public void write(final int b) {
        buffer.write(b);
        if (b == '\n') {
            flush();
        }
    }

    @Override
    public void write(final byte[] buf, final int off, final int len) {
        buffer.write(buf, off, len);
        for (int i = off; i < off + len; i++) {
            if (buf[i] == '\n') {
                flush();
                break;
            }
        }
    }

    /**
     * Flushes buffered content into the {@link #logger}, applying duplicate suppression heuristics
     * and forwarding to the preserved stream when present.
     */
    @Override
    public void flush() {
        synchronized (this) {
            if (buffer.size() > 0) {
                final String message = buffer.toString().trim();
                buffer.reset();
                if (!message.isEmpty()) {
                    // Skip obvious init/status banners to avoid noise
                    if (!message.startsWith("[INIT]") &&
                        !message.startsWith("[DEBUG]") &&
                        !message.startsWith("[EMERGENCY]") &&
                        !message.startsWith("╔") &&
                        !message.startsWith("║") &&
                        !message.startsWith("╚")) {

                        // If already formatted, don't re-log
                        if (!ALREADY_FORMATTED_LINE.matcher(message).matches()) {
                            final String prefix = level == Level.SEVERE ? "🚨 " : "📝 ";
                            logger.log(level, prefix + message);
                        }
                    }
                }
            }
        }
        if (originalStream != null) {
            originalStream.flush();
        }
    }

    /**
     * Flushes pending content and closes the original stream when provided before releasing the
     * buffer resources.
     */
    @Override
    public void close() {
        flush();
        if (originalStream != null) {
            originalStream.close();
        }
        super.close();
    }
}