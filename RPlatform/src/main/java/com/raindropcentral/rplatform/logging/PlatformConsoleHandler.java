package com.raindropcentral.rplatform.logging;

import org.jetbrains.annotations.NotNull;

import java.io.OutputStream;
import java.util.logging.ConsoleHandler;
import java.util.logging.LogRecord;

/**
 * PlatformConsoleHandler writes {@link CentralLogger} output to a predetermined stream so stdout
 * and stderr redirection can avoid recursive publication. The handler never closes the supplied
 * stream, ensuring {@link LoggingPrintStream} can continue to mirror output during shutdown.
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
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
     * Constructs a handler that writes to the provided stream and integrates with
     * {@link CentralLogger}'s handler rotation.
     *
     * @param out desired output stream; falls back to {@link System#out} if {@code null}
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

    /**
     * Publishes a log record and flushes immediately to keep console output responsive.
     *
     * @param record record to write
     */
    @Override
    public synchronized void publish(final LogRecord record) {
        if (!isLoggable(record)) {
            return;
        }
        super.publish(record);
        flush();
    }

    /**
     * Flushes buffered data without closing the underlying system stream.
     */
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