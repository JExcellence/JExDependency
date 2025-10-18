package com.raindropcentral.rplatform.logging;

import org.jetbrains.annotations.NotNull;

import java.io.OutputStream;
import java.util.logging.ConsoleHandler;
import java.util.logging.LogRecord;

/**
 * Console handler that always writes to the OutputStream provided at construction time.
 * - Null-safe during ConsoleHandler(super) construction to avoid NPEs (JUL calls setOutputStream in super()).
 * - Never closes the underlying System streams on close(); only flushes.
 * - Flushes after each publish to keep console output timely.
 */
public class PlatformConsoleHandler extends ConsoleHandler {

    private final OutputStream target;

    /**
     * Defaults to System.out.
     */
    public PlatformConsoleHandler() {
        this(System.out);
    }

    /**
     * @param out desired output stream; falls back to System.out if null
     */
    public PlatformConsoleHandler(final OutputStream out) {
        super();
        this.target = (out != null) ? out : System.out;
        // Ensure the underlying StreamHandler uses our intended stream after super() init.
        setOutputStream(this.target);
    }

    @Override
    protected synchronized void setOutputStream(final OutputStream out) throws SecurityException {
        // During super() construction, 'target' is still null. Use the provided 'out' (never null in JUL),
        // and once our constructor sets 'target', we call setOutputStream(this.target) again.
        final OutputStream effective = (this.target != null) ? this.target : (out != null ? out : System.out);
        super.setOutputStream(effective);
    }

    @Override
    public synchronized void publish(final LogRecord record) {
        if (!isLoggable(record)) {
            return;
        }
        super.publish(record);
        flush();
    }

    @Override
    public synchronized void close() throws SecurityException {
        // Do not close System.out/err; only flush.
        try {
            flush();
        } catch (final Exception ignored) {
        }
        // Intentionally not calling super.close() to avoid closing the underlying stream.
    }
}