package de.jexcellence.core.stats;

import org.jetbrains.annotations.NotNull;

import java.io.Closeable;

/**
 * Transport SPI. Default is {@link HttpCentralTransport}; tests supply an
 * in-memory fake. Close shuts down any background resources.
 */
public interface CentralTransport extends Closeable {

    @NotNull DeliveryResult send(@NotNull BatchPayload batch);

    @Override
    default void close() {
    }
}
