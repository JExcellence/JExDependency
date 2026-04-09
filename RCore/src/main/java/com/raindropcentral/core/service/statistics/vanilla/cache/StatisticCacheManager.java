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

package com.raindropcentral.core.service.statistics.vanilla.cache;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.raindropcentral.core.service.statistics.vanilla.config.VanillaStatisticConfig;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages in-memory caching of vanilla statistic values for delta computation.
 * Provides persistence to disk for server restart survival.
 *
 * <p>The cache stores the last known value for each statistic per player,
 * enabling efficient delta-based transmission by only sending changed values.
 *
 * <p>Cache structure:
 * <pre>
 * UUID (player) -> Map&lt;String (statistic key), Integer (value)&gt;
 * </pre>
 *
 * <p>Persistence format (JSON):
 * <pre>
 * {
 *   "version": 1,
 *   "lastSaved": 1234567890,
 *   "players": {
 *     "uuid-1": {
 *       "minecraft.blocks.mined.stone": 1500,
 *       "minecraft.travel.walk": 50000
 *     }
 *   }
 * }
 * </pre>
 *
 * @author JExcellence
 * @since 1.0.0
 */
public class StatisticCacheManager {

    private static final int CACHE_VERSION = 1;
    private static final String CACHE_FILENAME = "vanilla-stats-cache.json";

    private final Plugin plugin;
    private final VanillaStatisticConfig config;
    private final Logger logger;
    private final Path cacheFile;
    private final Gson gson;

    /**
     * In-memory cache: UUID -> (StatisticKey -> Value).
     */
    private final ConcurrentHashMap<UUID, Map<String, Integer>> cache;

    /**
     * Creates a new statistic cache manager.
     *
     * @param plugin the plugin instance
     * @param config the vanilla statistic configuration
     */
    public StatisticCacheManager(
        final @NotNull Plugin plugin,
        final @NotNull VanillaStatisticConfig config
    ) {
        this.plugin = plugin;
        this.config = config;
        this.logger = plugin.getLogger();
        this.cacheFile = Paths.get(plugin.getDataFolder().getPath(), CACHE_FILENAME);
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.cache = new ConcurrentHashMap<>();
    }

    /**
     * Computes the delta between current statistic values and cached values.
     * Only includes statistics where the absolute delta meets or exceeds the configured threshold.
     *
     * <p>Delta computation:
     * <ul>
     *   <li>If statistic exists in cache: delta = current - cached</li>
     *   <li>If statistic not in cache: delta = current (first collection)</li>
     *   <li>Only include if |delta| >= threshold</li>
     * </ul>
     *
     * @param playerId      the player UUID
     * @param currentValues the current statistic values
     * @return map of statistic keys to delta values that meet the threshold
     */
    public @NotNull Map<String, Integer> getDelta(
        final @NotNull UUID playerId,
        final @NotNull Map<String, Integer> currentValues
    ) {
        Map<String, Integer> cached = cache.getOrDefault(playerId, Map.of());
        Map<String, Integer> deltas = new HashMap<>();
        int threshold = config.getDeltaThreshold();

        for (Map.Entry<String, Integer> entry : currentValues.entrySet()) {
            String key = entry.getKey();
            int currentValue = entry.getValue();
            int cachedValue = cached.getOrDefault(key, 0);
            int delta = currentValue - cachedValue;

            // Apply delta threshold filtering
            if (Math.abs(delta) >= threshold) {
                deltas.put(key, delta);
            }
        }

        return deltas;
    }

    /**
     * Updates the cache with new statistic values for a player.
     * Replaces the entire cached map for the player with the provided values.
     *
     * @param playerId the player UUID
     * @param values   the new statistic values to cache
     */
    public void updateCache(
        final @NotNull UUID playerId,
        final @NotNull Map<String, Integer> values
    ) {
        // Create a defensive copy to prevent external modifications
        Map<String, Integer> cachedValues = new HashMap<>(values);
        cache.put(playerId, cachedValues);
    }

    /**
     * Clears all cached statistics for a player.
     * Typically called when a player disconnects to free memory.
     *
     * @param playerId the player UUID
     */
    public void clearPlayer(final @NotNull UUID playerId) {
        cache.remove(playerId);
    }

    /**
     * Clears the entire cache for all players.
     * Use with caution - this will cause all statistics to be treated as new on next collection.
     */
    public void clearAll() {
        logger.info("Clearing all cached statistics");
        cache.clear();
    }

    /**
     * Gets the current cache size (number of players with cached statistics).
     *
     * @return the number of cached players
     */
    public int getCacheSize() {
        return cache.size();
    }

    /**
     * Checks if a player has cached statistics.
     *
     * @param playerId the player UUID
     * @return true if the player has cached statistics
     */
    public boolean hasCachedData(final @NotNull UUID playerId) {
        return cache.containsKey(playerId);
    }

    /**
     * Gets the cached statistics for a player.
     * Returns an empty map if no cache exists for the player.
     *
     * @param playerId the player UUID
     * @return the cached statistics, or empty map if none exist
     */
    public @NotNull Map<String, Integer> getCachedValues(final @NotNull UUID playerId) {
        Map<String, Integer> cached = cache.get(playerId);
        return cached != null ? new HashMap<>(cached) : Map.of();
    }

    /**
     * Persists the cache to disk using JSON serialization.
     * Saves to {@code plugins/RCore/vanilla-stats-cache.json}.
     *
     * <p>This method is thread-safe and can be called from async tasks.
     * Failures are logged but do not throw exceptions.
     */
    public void persistCache() {
        try {
            // Ensure parent directory exists
            Files.createDirectories(cacheFile.getParent());

            // Build JSON structure
            JsonObject root = new JsonObject();
            root.addProperty("version", CACHE_VERSION);
            root.addProperty("lastSaved", Instant.now().getEpochSecond());

            JsonObject playersObject = new JsonObject();
            for (Map.Entry<UUID, Map<String, Integer>> playerEntry : cache.entrySet()) {
                String uuidString = playerEntry.getKey().toString();
                JsonObject statsObject = new JsonObject();

                for (Map.Entry<String, Integer> statEntry : playerEntry.getValue().entrySet()) {
                    statsObject.addProperty(statEntry.getKey(), statEntry.getValue());
                }

                playersObject.add(uuidString, statsObject);
            }
            root.add("players", playersObject);

            // Write to file
            String json = gson.toJson(root);
            Files.writeString(cacheFile, json);
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to persist vanilla statistics cache", e);
        }
    }

    /**
     * Loads the cache from disk.
     * Called on plugin startup to restore cached values from previous session.
     *
     * <p>If the cache file doesn't exist or is invalid, starts with an empty cache.
     * Failures are logged but do not throw exceptions.
     */
    public void loadCache() {
        if (!Files.exists(cacheFile)) {
            logger.info("No vanilla statistics cache file found, starting fresh");
            return;
        }

        try {
            String json = Files.readString(cacheFile);
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();

            // Validate version
            int version = root.get("version").getAsInt();
            if (version != CACHE_VERSION) {
                logger.warning("Cache version mismatch (expected " + CACHE_VERSION +
                    ", found " + version + "), starting fresh");
                return;
            }

            // Load player data
            JsonObject playersObject = root.getAsJsonObject("players");
            int loadedCount = 0;

            for (String uuidString : playersObject.keySet()) {
                try {
                    UUID playerId = UUID.fromString(uuidString);
                    JsonObject statsObject = playersObject.getAsJsonObject(uuidString);
                    Map<String, Integer> stats = new HashMap<>();

                    for (String statKey : statsObject.keySet()) {
                        int value = statsObject.get(statKey).getAsInt();
                        stats.put(statKey, value);
                    }

                    cache.put(playerId, stats);
                    loadedCount++;

                } catch (IllegalArgumentException e) {
                    logger.warning("Invalid UUID in cache: " + uuidString);
                }
            }

            long lastSaved = root.get("lastSaved").getAsLong();
            Instant lastSavedTime = Instant.ofEpochSecond(lastSaved);

            logger.info("Loaded vanilla statistics cache: " + loadedCount +
                " players (last saved: " + lastSavedTime + ")");

        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to load vanilla statistics cache", e);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to parse vanilla statistics cache", e);
        }
    }

    /**
     * Gets the cache file path.
     *
     * @return the cache file path
     */
    public @NotNull Path getCacheFilePath() {
        return cacheFile;
    }
}
