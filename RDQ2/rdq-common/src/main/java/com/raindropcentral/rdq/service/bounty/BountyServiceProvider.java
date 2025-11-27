package com.raindropcentral.rdq.service.bounty;

import org.jetbrains.annotations.NotNull;

/**
 * Provides access to the lazily supplied {@link BountyService} implementation that is
 * loaded for the active RDQ module.
 * <p>
 * The provider exposes a very small API that mirrors a classic service locator: the
 * service is registered once during module bootstrap and subsequently retrieved by
 * consumers. Callers should treat this class as <strong>not thread-safe</strong> and ensure
 * that registration occurs during single-threaded initialization.
 * </p>
 *
 * @author JExcellence
 * @version 2.0.0
 * @since 2.0.0
 */
public final class BountyServiceProvider {

    private static BountyService instance;

    /**
     * Prevents instantiation of this static utility class.
     */
    private BountyServiceProvider() {}

    /**
     * Registers the shared {@link BountyService} instance that will be returned by
     * {@link #getInstance()}.
     *
     * @param service the concrete service implementation provided by the active module
     * @throws IllegalStateException if a service has already been registered
     */
    public static void setInstance(final @NotNull BountyService service) {
        if (instance != null) {
            throw new IllegalStateException("BountyService already initialized");
        }
        instance = service;
    }

    /**
     * Retrieves the previously registered {@link BountyService} instance.
     *
     * @return the active bounty service implementation
     * @throws IllegalStateException if the service has not been registered yet
     */
    public static @NotNull BountyService getInstance() {
        if (instance == null) {
            throw new IllegalStateException("BountyService not initialized");
        }
        return instance;
    }

    /**
     * Indicates whether the provider currently has a registered service.
     *
     * @return {@code true} if a service has been registered, {@code false} otherwise
     */
    public static boolean isInitialized() {
        return instance != null;
    }

    /**
     * Clears the registered {@link BountyService} instance. This is primarily intended for
     * unit tests that need to simulate re-initialization scenarios.
     */
    public static void reset() {
        instance = null;
    }
}