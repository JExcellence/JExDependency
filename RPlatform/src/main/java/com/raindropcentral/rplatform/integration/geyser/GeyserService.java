/*
 * Copyright (c) 2021-2026 Antimatter Zone LLC. All rights reserved.
 *
 * This source code is proprietary and confidential to Antimatter Zone LLC.
 * Unauthorized copying, modification, distribution, display, performance,
 * publication, sublicensing, or creation of derivative works is prohibited
 * without prior written permission from Antimatter Zone LLC, except to the
 * extent permitted by applicable United States law.
 *
 * This notice is intended to preserve all rights and remedies available under
 * the laws of the State of Washington and the United States of America.
 */

package com.raindropcentral.rplatform.integration.geyser;

import com.raindropcentral.rplatform.service.ServiceRegistry;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service for detecting and interacting with Bedrock Edition players via Floodgate/Geyser.
 *
 * <p>This service provides a clean, non-static API for Bedrock player detection that can be
 * registered with the {@link ServiceRegistry} and injected into consuming plugins.
 *
 * <p>Supports multiple detection methods:
 * <ul>
 *   <li>Floodgate API via ServiceRegistry (preferred)</li>
 *   <li>Direct Floodgate API class loading (fallback)</li>
 *   <li>UUID prefix detection (last resort - checks for Floodgate UUID prefix)</li>
 * </ul>
 *
 * @author JExcellence
 * @version 1.2.0
 */
public class GeyserService {

    private static final Logger LOGGER = Logger.getLogger(GeyserService.class.getName());

    /**
     * Floodgate API fully qualified class name.
     */
    private static final String FLOODGATE_API_CLASS = "org.geysermc.floodgate.api.FloodgateApi";

    /**
     * Floodgate uses a specific UUID version (0) with a prefix to identify Bedrock players.
     * The first 8 characters of a Floodgate UUID are always "00000000".
     */
    private static final String FLOODGATE_UUID_PREFIX = "00000000";

    private final AtomicBoolean floodgateAvailable = new AtomicBoolean(false);
    private final AtomicBoolean uuidPrefixFallback = new AtomicBoolean(false);
    private final AtomicReference<FloodgateAdapter> floodgateAdapter = new AtomicReference<>(null);

    /**
     * Creates a new GeyserService with immediate detection.
     */
    public GeyserService() {
        initialize(null);
    }

    /**
     * Creates a new GeyserService using the provided ServiceRegistry for async discovery.
     *
     * @param registry the service registry for async Floodgate discovery
     */
    public GeyserService(@Nullable ServiceRegistry registry) {
        initialize(registry);
    }

    private void initialize(@Nullable ServiceRegistry registry) {
        // First, check if Floodgate/Geyser plugins are present
        Plugin floodgatePlugin = Bukkit.getPluginManager().getPlugin("floodgate");
        Plugin geyserPlugin = Bukkit.getPluginManager().getPlugin("Geyser-Spigot");

        boolean pluginPresent = floodgatePlugin != null || geyserPlugin != null;

        if (!pluginPresent) {
            LOGGER.info("No Geyser/Floodgate detected - Bedrock player detection unavailable");
            return;
        }

        // Try to load Floodgate API using the plugin's classloader
        if (floodgatePlugin != null) {
            tryLoadFloodgateApi(floodgatePlugin.getClass().getClassLoader());
        }

        // If not loaded yet, try Geyser's classloader
        if (!floodgateAvailable.get() && geyserPlugin != null) {
            tryLoadFloodgateApi(geyserPlugin.getClass().getClassLoader());
        }

        // If still not loaded, try current classloader
        if (!floodgateAvailable.get()) {
            tryLoadFloodgateApi(getClass().getClassLoader());
        }

        // If API still not available but plugin is present, use UUID fallback
        if (!floodgateAvailable.get() && pluginPresent) {
            uuidPrefixFallback.set(true);
            LOGGER.info("Floodgate plugin detected but API not accessible - using UUID prefix detection");
        }

        // If registry provided, also try async discovery as backup
        if (registry != null && !floodgateAvailable.get()) {
            registry.register(FLOODGATE_API_CLASS, "floodgate")
                    .optional()
                    .maxAttempts(5)
                    .retryDelay(1000)
                    .onSuccess(api -> {
                        try {
                            floodgateAdapter.set(new FloodgateAdapter());
                            floodgateAvailable.set(true);
                            uuidPrefixFallback.set(false);
                            LOGGER.info("Floodgate API discovered via ServiceRegistry - Bedrock support enabled");
                        } catch (Exception e) {
                            LOGGER.log(Level.WARNING, "Failed to create FloodgateAdapter after discovery", e);
                        }
                    })
                    .load();
        }
    }

    /**
     * Attempts to load the Floodgate API using the specified classloader.
     *
     * @param classLoader the classloader to use
     */
    private void tryLoadFloodgateApi(@NotNull ClassLoader classLoader) {
        try {
            Class.forName(FLOODGATE_API_CLASS, true, classLoader);
            floodgateAdapter.set(new FloodgateAdapter());
            floodgateAvailable.set(true);
            LOGGER.info("Floodgate API detected - Bedrock player support enabled");
        } catch (ClassNotFoundException | NoClassDefFoundError e) {
            // API not available with this classloader
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error initializing Floodgate adapter", e);
        }
    }

    /**
     * Checks if Floodgate/Geyser is available on this server.
     *
     * @return true if Bedrock player detection is available
     */
    public boolean isFloodgateAvailable() {
        return floodgateAvailable.get() || uuidPrefixFallback.get();
    }

    /**
     * Checks if the full Floodgate API is available (not just UUID fallback).
     *
     * @return true if the Floodgate API is available
     */
    public boolean hasFloodgateApi() {
        return floodgateAvailable.get();
    }

    /**
     * Checks if a player is connecting via Bedrock Edition through Floodgate.
     *
     * @param player the player to check
     * @return true if the player is a Bedrock player, false otherwise
     */
    public boolean isBedrockPlayer(@NotNull Player player) {
        return isBedrockPlayer(player.getUniqueId());
    }

    /**
     * Checks if a UUID belongs to a Bedrock Edition player.
     *
     * @param uuid the UUID to check
     * @return true if the UUID belongs to a Bedrock player, false otherwise
     */
    public boolean isBedrockPlayer(@NotNull UUID uuid) {

        // Try Floodgate API first
        FloodgateAdapter adapter = floodgateAdapter.get();
        if (floodgateAvailable.get() && adapter != null) {
            try {
                return adapter.isFloodgatePlayer(uuid);
            } catch (Exception e) {
                LOGGER.log(Level.FINE, "Error checking Bedrock player via Floodgate API, falling back to UUID check", e);
            }
        }

        // Fallback to UUID prefix detection
        if (uuidPrefixFallback.get() || floodgateAvailable.get()) {
            return isFloodgateUuid(uuid);
        }

        return false;
    }

    /**
     * Checks if a UUID has the Floodgate prefix (starts with 00000000).
 *
 * <p>Floodgate assigns UUIDs with version 0 to Bedrock players, which always
     * start with "00000000-" in string form.
     *
     * @param uuid the UUID to check
     * @return true if the UUID has the Floodgate prefix
     */
    private boolean isFloodgateUuid(@NotNull UUID uuid) {
        String uuidString = uuid.toString();
        return uuidString.startsWith(FLOODGATE_UUID_PREFIX);
    }

    /**
     * Gets the Bedrock username for a Floodgate player.
     *
     * @param uuid the player's UUID
     * @return the Bedrock username, or null if not a Bedrock player or API unavailable
     */
    @Nullable
    public String getBedrockUsername(@NotNull UUID uuid) {

        FloodgateAdapter adapter = floodgateAdapter.get();
        if (floodgateAvailable.get() && adapter != null) {
            try {
                return adapter.getBedrockUsername(uuid);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error getting Bedrock username", e);
            }
        }

        return null;
    }
}
