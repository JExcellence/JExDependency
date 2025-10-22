package org.slf4j.impl;

import org.slf4j.ILoggerFactory;
import org.slf4j.spi.LoggerFactoryBinder;

/**
 * Simple SLF4J binder used in tests to capture log events for verification.
 */
public final class StaticLoggerBinder implements LoggerFactoryBinder {

    public static final StaticLoggerBinder SINGLETON = new StaticLoggerBinder();

    private final TestLoggerFactory loggerFactory = new TestLoggerFactory();

    private StaticLoggerBinder() {
    }

    public static StaticLoggerBinder getSingleton() {
        return SINGLETON;
    }

    @Override
    public ILoggerFactory getLoggerFactory() {
        return this.loggerFactory;
    }

    @Override
    public String getLoggerFactoryClassStr() {
        return TestLoggerFactory.class.getName();
    }

    public TestLoggerFactory getTestLoggerFactory() {
        return this.loggerFactory;
    }
}
