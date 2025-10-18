package com.raindropcentral.rplatform.logging;

import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class LoggingPrintStream extends PrintStream {

    private static final Pattern ALREADY_FORMATTED_LINE = Pattern.compile(
            "^\\[\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}] \\[(SEVERE|WARNING|WARN|INFO|CONFIG|DEBUG|TRACE|FINE|FINER|FINEST)].*"
    );

    private final Logger logger;
    private final Level level;
    private final PrintStream originalStream;
    private final ByteArrayOutputStream buffer;

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

    @Override
    public void close() {
        flush();
        if (originalStream != null) {
            originalStream.close();
        }
        super.close();
    }
}