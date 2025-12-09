package com.raindropcentral.rdq.service;

import com.raindropcentral.core.api.RCoreAdapter;
import com.raindropcentral.rplatform.RPlatform;
import com.raindropcentral.rplatform.logging.CentralLogger;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Logger;

/**
 * Service class for managing RCore API operations, such as player data retrieval,
 * statistics management, and player existence checks.
 * <p>
 * Provides utility methods for:
 * <ul>
 *     <li>Fetching RPlayer entities by UUID, OfflinePlayer, or name</li>
 *     <li>Checking player existence in the RCore system</li>
 *     <li>Retrieving player statistics</li>
 *     <li>Creating and updating player records</li>
 *     <li>Asynchronous operations for non-blocking data access</li>
 * </ul>
 *
 * @author JExcellence
 * @version 1.0.0
 * @since 1.0.0
 */
@SuppressWarnings("unused")
public class RCoreService {

    private static final Logger LOGGER = CentralLogger.getLogger(RCoreService.class.getName());

    private final RPlatform platform;
    private final RCoreAdapter rCoreAdapter;

    /**
     * Constructs a new RCoreService.
     *
     * @param platform     The RPlatform instance for accessing platform services
     * @param rCoreAdapter The RCoreAdapter instance for accessing RCore data
     */
    public RCoreService(
            final @NotNull RPlatform platform,
            final @NotNull RCoreAdapter rCoreAdapter
    ) {
        this.platform = platform;
        this.rCoreAdapter = rCoreAdapter;
        LOGGER.info("RCoreService initialized successfully");
    }

    /**
     * Gets the underlying RCoreAdapter instance.
     * <p>
     * Use this for advanced operations not covered by the service methods.
     * </p>
     *
     * @return The RCoreAdapter instance
     */
    @NotNull
    public RCoreAdapter getAdapter() {
        return this.rCoreAdapter;
    }

    /**
     * Gets the API version of the RCore adapter.
     *
     * @return The API version string
     */
    @NotNull
    public String getApiVersion() {
        return this.rCoreAdapter.getApiVersion();
    }

    /**
     * Gets the RPlatform instance.
     *
     * @return The RPlatform instance
     */
    @NotNull
    public RPlatform getPlatform() {
        return this.platform;
    }
}