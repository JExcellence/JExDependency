package org.slf4j.impl;

import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple in-memory {@link ILoggerFactory} for capturing log events during tests.
 */
public final class TestLoggerFactory implements ILoggerFactory {

    private final Map<String, TestLogger> loggers = new ConcurrentHashMap<>();

    @Override
    public Logger getLogger(final String name) {
        return this.loggers.computeIfAbsent(name, TestLogger::new);
    }

    public TestLogger getLoggerFor(final String name) {
        return this.loggers.computeIfAbsent(name, TestLogger::new);
    }

    public void clear() {
        this.loggers.values().forEach(TestLogger::clear);
    }
}
