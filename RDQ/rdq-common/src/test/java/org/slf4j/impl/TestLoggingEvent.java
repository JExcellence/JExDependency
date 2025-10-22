package org.slf4j.impl;

import org.slf4j.event.Level;

/**
 * Value object describing a captured logging event.
 *
 * @param level     the event level
 * @param message   the raw log message or format string
 * @param arguments formatting arguments supplied with the log call
 * @param throwable optional throwable associated with the event
 */
public record TestLoggingEvent(Level level, String message, Object[] arguments, Throwable throwable) {
}
