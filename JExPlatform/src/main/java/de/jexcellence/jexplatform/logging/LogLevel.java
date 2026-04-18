package de.jexcellence.jexplatform.logging;

import java.util.logging.Level;

/**
 * Log severity levels ordered from least to most severe.
 *
 * <p>Maps cleanly to {@link java.util.logging.Level} internally while
 * keeping the public API free of JUL verbosity.
 *
 * @author JExcellence
 * @since 1.0.0
 */
public enum LogLevel {

    /** Fine-grained diagnostic output, disabled by default. */
    DEBUG(Level.FINE),

    /** Normal operational messages. */
    INFO(Level.INFO),

    /** Indicates a potential problem that does not prevent operation. */
    WARN(Level.WARNING),

    /** Indicates a serious failure that likely prevents correct operation. */
    ERROR(Level.SEVERE);

    private final Level julLevel;

    LogLevel(Level julLevel) {
        this.julLevel = julLevel;
    }

    /**
     * Returns the corresponding {@link java.util.logging.Level}.
     *
     * @return the JUL level
     */
    public Level toJulLevel() {
        return julLevel;
    }
}
