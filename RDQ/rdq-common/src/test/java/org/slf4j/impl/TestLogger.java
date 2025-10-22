package org.slf4j.impl;

import org.slf4j.Logger;
import org.slf4j.Marker;
import org.slf4j.event.Level;

import java.util.Arrays;

/**
 * Minimal {@link Logger} implementation that captures the last log event.
 */
public final class TestLogger implements Logger {

    private final String name;
    private volatile TestLoggingEvent lastEvent;

    public TestLogger(final String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return this.name;
    }

    public TestLoggingEvent getLastEvent() {
        return this.lastEvent;
    }

    public void clear() {
        this.lastEvent = null;
    }

    private void record(final Level level, final String message) {
        this.record(level, message, new Object[0], null);
    }

    private void record(final Level level, final String message, final Object argument) {
        this.record(level, message, new Object[]{argument}, null);
    }

    private void record(final Level level, final String message, final Object argument1, final Object argument2) {
        this.record(level, message, new Object[]{argument1, argument2}, null);
    }

    private void recordVarargs(final Level level, final String message, final Object... arguments) {
        this.record(level, message, arguments, null);
    }

    private void recordWithThrowable(final Level level, final String message, final Throwable throwable) {
        this.record(level, message, new Object[0], throwable);
    }

    private void record(final Level level, final String message, final Object[] arguments, final Throwable throwable) {
        final Object[] copy = arguments == null ? new Object[0] : Arrays.copyOf(arguments, arguments.length);
        this.lastEvent = new TestLoggingEvent(level, message, copy, throwable);
    }

    // TRACE

    @Override
    public boolean isTraceEnabled() {
        return true;
    }

    @Override
    public void trace(final String msg) {
        this.record(Level.TRACE, msg);
    }

    @Override
    public void trace(final String format, final Object arg) {
        this.record(Level.TRACE, format, arg);
    }

    @Override
    public void trace(final String format, final Object arg1, final Object arg2) {
        this.record(Level.TRACE, format, arg1, arg2);
    }

    @Override
    public void trace(final String format, final Object... arguments) {
        this.recordVarargs(Level.TRACE, format, arguments);
    }

    @Override
    public void trace(final String msg, final Throwable t) {
        this.recordWithThrowable(Level.TRACE, msg, t);
    }

    @Override
    public boolean isTraceEnabled(final Marker marker) {
        return true;
    }

    @Override
    public void trace(final Marker marker, final String msg) {
        this.trace(msg);
    }

    @Override
    public void trace(final Marker marker, final String format, final Object arg) {
        this.trace(format, arg);
    }

    @Override
    public void trace(final Marker marker, final String format, final Object arg1, final Object arg2) {
        this.trace(format, arg1, arg2);
    }

    @Override
    public void trace(final Marker marker, final String format, final Object... argArray) {
        this.trace(format, argArray);
    }

    @Override
    public void trace(final Marker marker, final String msg, final Throwable t) {
        this.trace(msg, t);
    }

    // DEBUG

    @Override
    public boolean isDebugEnabled() {
        return true;
    }

    @Override
    public void debug(final String msg) {
        this.record(Level.DEBUG, msg);
    }

    @Override
    public void debug(final String format, final Object arg) {
        this.record(Level.DEBUG, format, arg);
    }

    @Override
    public void debug(final String format, final Object arg1, final Object arg2) {
        this.record(Level.DEBUG, format, arg1, arg2);
    }

    @Override
    public void debug(final String format, final Object... arguments) {
        this.recordVarargs(Level.DEBUG, format, arguments);
    }

    @Override
    public void debug(final String msg, final Throwable t) {
        this.recordWithThrowable(Level.DEBUG, msg, t);
    }

    @Override
    public boolean isDebugEnabled(final Marker marker) {
        return true;
    }

    @Override
    public void debug(final Marker marker, final String msg) {
        this.debug(msg);
    }

    @Override
    public void debug(final Marker marker, final String format, final Object arg) {
        this.debug(format, arg);
    }

    @Override
    public void debug(final Marker marker, final String format, final Object arg1, final Object arg2) {
        this.debug(format, arg1, arg2);
    }

    @Override
    public void debug(final Marker marker, final String format, final Object... arguments) {
        this.debug(format, arguments);
    }

    @Override
    public void debug(final Marker marker, final String msg, final Throwable t) {
        this.debug(msg, t);
    }

    // INFO

    @Override
    public boolean isInfoEnabled() {
        return true;
    }

    @Override
    public void info(final String msg) {
        this.record(Level.INFO, msg);
    }

    @Override
    public void info(final String format, final Object arg) {
        this.record(Level.INFO, format, arg);
    }

    @Override
    public void info(final String format, final Object arg1, final Object arg2) {
        this.record(Level.INFO, format, arg1, arg2);
    }

    @Override
    public void info(final String format, final Object... arguments) {
        this.recordVarargs(Level.INFO, format, arguments);
    }

    @Override
    public void info(final String msg, final Throwable t) {
        this.recordWithThrowable(Level.INFO, msg, t);
    }

    @Override
    public boolean isInfoEnabled(final Marker marker) {
        return true;
    }

    @Override
    public void info(final Marker marker, final String msg) {
        this.info(msg);
    }

    @Override
    public void info(final Marker marker, final String format, final Object arg) {
        this.info(format, arg);
    }

    @Override
    public void info(final Marker marker, final String format, final Object arg1, final Object arg2) {
        this.info(format, arg1, arg2);
    }

    @Override
    public void info(final Marker marker, final String format, final Object... arguments) {
        this.info(format, arguments);
    }

    @Override
    public void info(final Marker marker, final String msg, final Throwable t) {
        this.info(msg, t);
    }

    // WARN

    @Override
    public boolean isWarnEnabled() {
        return true;
    }

    @Override
    public void warn(final String msg) {
        this.record(Level.WARN, msg);
    }

    @Override
    public void warn(final String format, final Object arg) {
        this.record(Level.WARN, format, arg);
    }

    @Override
    public void warn(final String format, final Object arg1, final Object arg2) {
        this.record(Level.WARN, format, arg1, arg2);
    }

    @Override
    public void warn(final String format, final Object... arguments) {
        this.recordVarargs(Level.WARN, format, arguments);
    }

    @Override
    public void warn(final String msg, final Throwable t) {
        this.recordWithThrowable(Level.WARN, msg, t);
    }

    @Override
    public boolean isWarnEnabled(final Marker marker) {
        return true;
    }

    @Override
    public void warn(final Marker marker, final String msg) {
        this.warn(msg);
    }

    @Override
    public void warn(final Marker marker, final String format, final Object arg) {
        this.warn(format, arg);
    }

    @Override
    public void warn(final Marker marker, final String format, final Object arg1, final Object arg2) {
        this.warn(format, arg1, arg2);
    }

    @Override
    public void warn(final Marker marker, final String format, final Object... arguments) {
        this.warn(format, arguments);
    }

    @Override
    public void warn(final Marker marker, final String msg, final Throwable t) {
        this.warn(msg, t);
    }

    // ERROR

    @Override
    public boolean isErrorEnabled() {
        return true;
    }

    @Override
    public void error(final String msg) {
        this.record(Level.ERROR, msg);
    }

    @Override
    public void error(final String format, final Object arg) {
        this.record(Level.ERROR, format, arg);
    }

    @Override
    public void error(final String format, final Object arg1, final Object arg2) {
        this.record(Level.ERROR, format, arg1, arg2);
    }

    @Override
    public void error(final String format, final Object... arguments) {
        this.recordVarargs(Level.ERROR, format, arguments);
    }

    @Override
    public void error(final String msg, final Throwable t) {
        this.recordWithThrowable(Level.ERROR, msg, t);
    }

    @Override
    public boolean isErrorEnabled(final Marker marker) {
        return true;
    }

    @Override
    public void error(final Marker marker, final String msg) {
        this.error(msg);
    }

    @Override
    public void error(final Marker marker, final String format, final Object arg) {
        this.error(format, arg);
    }

    @Override
    public void error(final Marker marker, final String format, final Object arg1, final Object arg2) {
        this.error(format, arg1, arg2);
    }

    @Override
    public void error(final Marker marker, final String format, final Object... arguments) {
        this.error(format, arguments);
    }

    @Override
    public void error(final Marker marker, final String msg, final Throwable t) {
        this.error(msg, t);
    }
}
