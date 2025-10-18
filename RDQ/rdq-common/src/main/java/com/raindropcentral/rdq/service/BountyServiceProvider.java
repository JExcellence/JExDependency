package com.raindropcentral.rdq.service;

import org.jetbrains.annotations.NotNull;

/**
 * Provider for bounty service instances.
 * Automatically provides the correct implementation based on the loaded module.
 *
 * @author JExcellence
 * @version 2.0.0
 * @since 2.0.0
 */
public final class BountyServiceProvider {

    private static BountyService instance;

    private BountyServiceProvider() {}

    public static void setInstance(final @NotNull BountyService service) {
        if (instance != null) {
            throw new IllegalStateException("BountyService already initialized");
        }
        instance = service;
    }

    public static @NotNull BountyService getInstance() {
        if (instance == null) {
            throw new IllegalStateException("BountyService not initialized");
        }
        return instance;
    }

    public static boolean isInitialized() {
        return instance != null;
    }

    public static void reset() {
        instance = null;
    }
}