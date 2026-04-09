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

package com.raindropcentral.core.service.statistics.vanilla.config;

import com.raindropcentral.core.service.statistics.vanilla.StatisticCategory;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Configuration for vanilla Minecraft statistics collection.
 * Manages settings for collection frequency, filtering, privacy, performance, and aggregation.
 *
 * <p>Configuration is loaded from the plugin's config.yml under the
 * {@code statistics-delivery.vanilla-statistics} section.
 *
 * @author JExcellence
 * @since 1.0.0
 */
public class VanillaStatisticConfig {

    // System
    private boolean enabled;

    // Collection frequency
    private int collectionFrequencySeconds;
    private final Map<StatisticCategory, Integer> categoryFrequencies;

    // Category settings
    private final Set<StatisticCategory> enabledCategories;

    // Delta computation
    private int deltaThreshold;

    // Performance
    private int batchSize;
    private int parallelThreads;
    private int afkThresholdSeconds;

    // TPS throttling
    private boolean tpsThrottlingEnabled;
    private double tpsPauseThreshold;
    private double tpsReduceThreshold;

    // Privacy
    private boolean privacyModeEnabled;
    private boolean anonymizeUuids;
    private final Set<String> excludedStatistics;

    // Filtering
    private final Set<Material> materialWhitelist;
    private final Set<Material> materialBlacklist;
    private final Set<EntityType> entityWhitelist;
    private final Set<EntityType> entityBlacklist;

    // Custom aggregates
    private final Map<String, CustomAggregate> customAggregates;

    // Cross-server sync
    private boolean enableCrossServerSync;
    private long syncCacheValidityMs;

    /**
     * Creates a new configuration with default values.
     */
    public VanillaStatisticConfig() {
        this.categoryFrequencies = new EnumMap<>(StatisticCategory.class);
        this.enabledCategories = EnumSet.allOf(StatisticCategory.class);
        this.excludedStatistics = new HashSet<>();
        this.materialWhitelist = new HashSet<>();
        this.materialBlacklist = new HashSet<>();
        this.entityWhitelist = new HashSet<>();
        this.entityBlacklist = new HashSet<>();
        this.customAggregates = new HashMap<>();
        setDefaults();
    }

    /**
     * Creates a new configuration loaded from plugin config.
     *
     * @param plugin the plugin to load configuration from
     */
    public VanillaStatisticConfig(final @NotNull Plugin plugin) {
        this();
        load(plugin);
    }

    private void setDefaults() {
        // System
        this.enabled = true;

        // Collection frequency
        this.collectionFrequencySeconds = 60;

        // Delta computation
        this.deltaThreshold = 5;

        // Performance
        this.batchSize = 100;
        this.parallelThreads = 4;
        this.afkThresholdSeconds = 600;

        // TPS throttling
        this.tpsThrottlingEnabled = true;
        this.tpsPauseThreshold = 15.0;
        this.tpsReduceThreshold = 18.0;

        // Privacy
        this.privacyModeEnabled = false;
        this.anonymizeUuids = false;
        this.excludedStatistics.add("LEAVE_GAME");
        this.excludedStatistics.add("TIME_SINCE_REST");

        // Cross-server sync
        this.enableCrossServerSync = true;
        this.syncCacheValidityMs = 5 * 60 * 1000; // 5 minutes
    }

    /**
     * Loads configuration from the plugin's config.yml.
     *
     * @param plugin the plugin to load configuration from
     */
    public void load(final @NotNull Plugin plugin) {
        var config = plugin.getConfig();
        var section = config.getConfigurationSection("statistics-delivery.vanilla-statistics");
        if (section == null) {
            return;
        }

        // System
        this.enabled = section.getBoolean("enabled", this.enabled);

        // Collection frequency
        int rawFrequency = section.getInt("collection-frequency", this.collectionFrequencySeconds);
        this.collectionFrequencySeconds = clamp(rawFrequency, 10, 600);
        if (rawFrequency != this.collectionFrequencySeconds) {
            plugin.getLogger().warning(String.format(
                "Vanilla statistics collection frequency %d is out of range (10-600), using %d",
                rawFrequency, this.collectionFrequencySeconds
            ));
        }

        // Category settings
        loadCategorySettings(section, plugin);

        // Delta computation
        this.deltaThreshold = section.getInt("delta-threshold", this.deltaThreshold);

        // Performance
        loadPerformanceSettings(section, plugin);

        // TPS throttling
        loadTpsThrottlingSettings(section, plugin);

        // Privacy
        loadPrivacySettings(section);

        // Filtering
        loadFilteringSettings(section);

        // Custom aggregates
        loadCustomAggregates(section);

        // Cross-server sync
        loadCrossServerSyncSettings(section);
        
        // Validate configuration
        validateConfiguration(plugin);
    }

    private void loadCategorySettings(final ConfigurationSection section, final Plugin plugin) {
        var categoriesSection = section.getConfigurationSection("categories");
        if (categoriesSection == null) return;

        for (StatisticCategory category : StatisticCategory.values()) {
            String categoryKey = category.name().toLowerCase();
            var categorySection = categoriesSection.getConfigurationSection(categoryKey);
            if (categorySection == null) continue;

            boolean enabled = categorySection.getBoolean("enabled", true);
            if (!enabled) {
                this.enabledCategories.remove(category);
            }

            int rawFrequency = categorySection.getInt("frequency", -1);
            if (rawFrequency > 0) {
                int clampedFrequency = clamp(rawFrequency, 10, 600);
                this.categoryFrequencies.put(category, clampedFrequency);
                if (rawFrequency != clampedFrequency) {
                    plugin.getLogger().warning(String.format(
                        "Category %s frequency %d is out of range (10-600), using %d",
                        category.name(), rawFrequency, clampedFrequency
                    ));
                }
            }
        }
    }

    private void loadPerformanceSettings(final ConfigurationSection section, final Plugin plugin) {
        this.batchSize = section.getInt("batch-size", this.batchSize);
        
        int rawThreads = section.getInt("parallel-threads", this.parallelThreads);
        this.parallelThreads = clamp(rawThreads, 1, 16);
        if (rawThreads != this.parallelThreads) {
            plugin.getLogger().warning(String.format(
                "Parallel threads %d is out of range (1-16), using %d",
                rawThreads, this.parallelThreads
            ));
        }
        
        this.afkThresholdSeconds = section.getInt("afk-threshold", this.afkThresholdSeconds);
    }

    private void loadTpsThrottlingSettings(final ConfigurationSection section, final Plugin plugin) {
        var tpsSection = section.getConfigurationSection("tps-throttling");
        if (tpsSection == null) return;

        this.tpsThrottlingEnabled = tpsSection.getBoolean("enabled", this.tpsThrottlingEnabled);
        
        double pauseThreshold = tpsSection.getDouble("pause-below", this.tpsPauseThreshold);
        double reduceThreshold = tpsSection.getDouble("reduce-below", this.tpsReduceThreshold);
        
        // Validate TPS thresholds
        if (pauseThreshold < 0 || pauseThreshold > 20) {
            plugin.getLogger().warning(String.format(
                "TPS pause threshold %.1f is invalid (0-20), using default %.1f",
                pauseThreshold, this.tpsPauseThreshold
            ));
        } else {
            this.tpsPauseThreshold = pauseThreshold;
        }
        
        if (reduceThreshold < 0 || reduceThreshold > 20) {
            plugin.getLogger().warning(String.format(
                "TPS reduce threshold %.1f is invalid (0-20), using default %.1f",
                reduceThreshold, this.tpsReduceThreshold
            ));
        } else {
            this.tpsReduceThreshold = reduceThreshold;
        }
        
        // Ensure pause threshold is less than reduce threshold
        if (this.tpsPauseThreshold >= this.tpsReduceThreshold) {
            plugin.getLogger().warning(String.format(
                "TPS pause threshold (%.1f) must be less than reduce threshold (%.1f), adjusting",
                this.tpsPauseThreshold, this.tpsReduceThreshold
            ));
            this.tpsPauseThreshold = this.tpsReduceThreshold - 1.0;
        }
    }

    private void loadPrivacySettings(final ConfigurationSection section) {
        var privacySection = section.getConfigurationSection("privacy");
        if (privacySection == null) return;

        this.privacyModeEnabled = privacySection.getBoolean("enabled", this.privacyModeEnabled);
        this.anonymizeUuids = privacySection.getBoolean("anonymize-uuids", this.anonymizeUuids);

        var excludedList = privacySection.getStringList("excluded-statistics");
        if (!excludedList.isEmpty()) {
            this.excludedStatistics.clear();
            this.excludedStatistics.addAll(excludedList);
        }
    }

    private void loadFilteringSettings(final ConfigurationSection section) {
        var filterSection = section.getConfigurationSection("filtering");
        if (filterSection == null) return;

        // Material filtering
        loadMaterialFilters(filterSection);

        // Entity filtering
        loadEntityFilters(filterSection);
    }

    private void loadMaterialFilters(final ConfigurationSection section) {
        var materialSection = section.getConfigurationSection("materials");
        if (materialSection == null) return;

        var whitelist = materialSection.getStringList("whitelist");
        for (String materialName : whitelist) {
            try {
                this.materialWhitelist.add(Material.valueOf(materialName.toUpperCase()));
            } catch (IllegalArgumentException ignored) {
                // Invalid material name, skip
            }
        }

        var blacklist = materialSection.getStringList("blacklist");
        for (String materialName : blacklist) {
            try {
                this.materialBlacklist.add(Material.valueOf(materialName.toUpperCase()));
            } catch (IllegalArgumentException ignored) {
                // Invalid material name, skip
            }
        }
    }

    private void loadEntityFilters(final ConfigurationSection section) {
        var entitySection = section.getConfigurationSection("entities");
        if (entitySection == null) return;

        var whitelist = entitySection.getStringList("whitelist");
        for (String entityName : whitelist) {
            try {
                this.entityWhitelist.add(EntityType.valueOf(entityName.toUpperCase()));
            } catch (IllegalArgumentException ignored) {
                // Invalid entity name, skip
            }
        }

        var blacklist = entitySection.getStringList("blacklist");
        for (String entityName : blacklist) {
            try {
                this.entityBlacklist.add(EntityType.valueOf(entityName.toUpperCase()));
            } catch (IllegalArgumentException ignored) {
                // Invalid entity name, skip
            }
        }
    }

    private void loadCustomAggregates(final ConfigurationSection section) {
        var aggregatesSection = section.getConfigurationSection("custom-aggregates");
        if (aggregatesSection == null) return;

        for (String aggregateName : aggregatesSection.getKeys(false)) {
            var aggregateSection = aggregatesSection.getConfigurationSection(aggregateName);
            if (aggregateSection == null) continue;

            String type = aggregateSection.getString("type", "sum");
            List<String> statistics = aggregateSection.getStringList("statistics");
            String formula = aggregateSection.getString("formula");

            this.customAggregates.put(
                aggregateName,
                new CustomAggregate(aggregateName, type, statistics, formula)
            );
        }
    }

    private void loadCrossServerSyncSettings(final ConfigurationSection section) {
        var syncSection = section.getConfigurationSection("cross-server-sync");
        if (syncSection == null) return;

        this.enableCrossServerSync = syncSection.getBoolean("enabled", this.enableCrossServerSync);
        
        int cacheValidityMinutes = syncSection.getInt("cache-validity-minutes", 5);
        this.syncCacheValidityMs = cacheValidityMinutes * 60 * 1000L;
    }

    /**
     * Validates the loaded configuration and logs warnings for invalid settings.
     *
     * @param plugin the plugin for logging
     */
    private void validateConfiguration(final @NotNull Plugin plugin) {
        var logger = plugin.getLogger();
        
        // Validate collection frequency
        if (collectionFrequencySeconds < 10 || collectionFrequencySeconds > 600) {
            logger.warning(String.format(
                "Collection frequency %d seconds is out of valid range (10-600)",
                collectionFrequencySeconds
            ));
        }
        
        // Validate batch size
        if (batchSize < 1 || batchSize > 1000) {
            logger.warning(String.format(
                "Batch size %d is out of recommended range (1-1000)",
                batchSize
            ));
        }
        
        // Validate parallel threads
        if (parallelThreads < 1 || parallelThreads > 16) {
            logger.warning(String.format(
                "Parallel threads %d is out of valid range (1-16)",
                parallelThreads
            ));
        }
        
        // Validate TPS thresholds
        if (tpsThrottlingEnabled) {
            if (tpsPauseThreshold < 0 || tpsPauseThreshold > 20) {
                logger.warning(String.format(
                    "TPS pause threshold %.1f is invalid (must be 0-20)",
                    tpsPauseThreshold
                ));
            }
            if (tpsReduceThreshold < 0 || tpsReduceThreshold > 20) {
                logger.warning(String.format(
                    "TPS reduce threshold %.1f is invalid (must be 0-20)",
                    tpsReduceThreshold
                ));
            }
            if (tpsPauseThreshold >= tpsReduceThreshold) {
                logger.warning(String.format(
                    "TPS pause threshold (%.1f) should be less than reduce threshold (%.1f)",
                    tpsPauseThreshold, tpsReduceThreshold
                ));
            }
        }
        
        // Validate enabled categories
        if (enabledCategories.isEmpty()) {
            logger.warning("No statistic categories are enabled - vanilla statistics will not be collected");
        }
        
        // Log configuration summary
        logger.info(String.format(
            "Vanilla statistics configuration loaded - Enabled: %s, Frequency: %ds, Categories: %d/%d, TPS Throttling: %s",
            enabled,
            collectionFrequencySeconds,
            enabledCategories.size(),
            StatisticCategory.values().length,
            tpsThrottlingEnabled ? "enabled" : "disabled"
        ));
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    // ==================== Getters ====================

    /**
     * Returns whether vanilla statistics collection is enabled.
     *
     * @return true if enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Gets the default collection frequency in seconds.
     *
     * @return the collection frequency
     */
    public int getCollectionFrequencySeconds() {
        return collectionFrequencySeconds;
    }

    /**
     * Gets the default collection frequency in seconds.
     * Alias for getCollectionFrequencySeconds() for backward compatibility.
     *
     * @return the collection frequency
     */
    public int getCollectionFrequency() {
        return collectionFrequencySeconds;
    }

    /**
     * Gets the collection frequency for a specific category.
     * Returns the default frequency if no category-specific frequency is configured.
     *
     * @param category the statistic category
     * @return the collection frequency in seconds
     */
    public int getCategoryFrequency(final @NotNull StatisticCategory category) {
        return categoryFrequencies.getOrDefault(category, collectionFrequencySeconds);
    }

    /**
     * Gets the set of enabled statistic categories.
     *
     * @return the enabled categories
     */
    public Set<StatisticCategory> getEnabledCategories() {
        return EnumSet.copyOf(enabledCategories);
    }

    /**
     * Checks if a statistic category is enabled.
     *
     * @param category the category to check
     * @return true if the category is enabled
     */
    public boolean isCategoryEnabled(final @NotNull StatisticCategory category) {
        return enabledCategories.contains(category);
    }

    /**
     * Gets the minimum delta threshold for transmitting statistics.
     * Statistics with changes below this threshold are filtered out.
     *
     * @return the delta threshold
     */
    public int getDeltaThreshold() {
        return deltaThreshold;
    }

    /**
     * Gets the batch size for player collection.
     *
     * @return the batch size
     */
    public int getBatchSize() {
        return batchSize;
    }

    /**
     * Gets the number of parallel threads for collection.
     *
     * @return the thread count
     */
    public int getParallelThreads() {
        return parallelThreads;
    }

    /**
     * Gets the AFK threshold in seconds.
     * Players AFK longer than this are skipped during collection.
     *
     * @return the AFK threshold
     */
    public int getAfkThresholdSeconds() {
        return afkThresholdSeconds;
    }

    /**
     * Returns whether TPS-based throttling is enabled.
     *
     * @return true if TPS throttling is enabled
     */
    public boolean isTpsThrottlingEnabled() {
        return tpsThrottlingEnabled;
    }

    /**
     * Gets the TPS threshold below which collection is paused.
     *
     * @return the pause threshold
     */
    public double getTpsPauseThreshold() {
        return tpsPauseThreshold;
    }

    /**
     * Gets the TPS threshold below which collection frequency is reduced.
     *
     * @return the reduce threshold
     */
    public double getTpsReduceThreshold() {
        return tpsReduceThreshold;
    }

    /**
     * Returns whether privacy mode is enabled.
     *
     * @return true if privacy mode is enabled
     */
    public boolean isPrivacyModeEnabled() {
        return privacyModeEnabled;
    }

    /**
     * Returns whether UUIDs should be anonymized.
     *
     * @return true if UUIDs should be anonymized
     */
    public boolean isAnonymizeUuids() {
        return anonymizeUuids;
    }

    /**
     * Gets the set of excluded statistic names.
     *
     * @return the excluded statistics
     */
    public Set<String> getExcludedStatistics() {
        return new HashSet<>(excludedStatistics);
    }

    /**
     * Checks if a statistic should be excluded.
     *
     * @param statisticName the statistic name
     * @return true if the statistic should be excluded
     */
    public boolean isStatisticExcluded(final @NotNull String statisticName) {
        return excludedStatistics.contains(statisticName);
    }

    /**
     * Gets the material whitelist.
     * If empty, all materials are allowed.
     *
     * @return the material whitelist
     */
    public Set<Material> getMaterialWhitelist() {
        return new HashSet<>(materialWhitelist);
    }

    /**
     * Gets the material blacklist.
     *
     * @return the material blacklist
     */
    public Set<Material> getMaterialBlacklist() {
        return new HashSet<>(materialBlacklist);
    }

    /**
     * Checks if a material should be collected.
     *
     * @param material the material to check
     * @return true if the material should be collected
     */
    public boolean shouldCollectMaterial(final @NotNull Material material) {
        if (materialBlacklist.contains(material)) {
            return false;
        }
        if (!materialWhitelist.isEmpty()) {
            return materialWhitelist.contains(material);
        }
        return true;
    }

    /**
     * Gets the entity whitelist.
     * If empty, all entities are allowed.
     *
     * @return the entity whitelist
     */
    public Set<EntityType> getEntityWhitelist() {
        return new HashSet<>(entityWhitelist);
    }

    /**
     * Gets the entity blacklist.
     *
     * @return the entity blacklist
     */
    public Set<EntityType> getEntityBlacklist() {
        return new HashSet<>(entityBlacklist);
    }

    /**
     * Checks if an entity type should be collected.
     *
     * @param entityType the entity type to check
     * @return true if the entity type should be collected
     */
    public boolean shouldCollectEntity(final @NotNull EntityType entityType) {
        if (entityBlacklist.contains(entityType)) {
            return false;
        }
        if (!entityWhitelist.isEmpty()) {
            return entityWhitelist.contains(entityType);
        }
        return true;
    }

    /**
     * Gets the map of custom aggregates.
     *
     * @return the custom aggregates
     */
    public Map<String, CustomAggregate> getCustomAggregates() {
        return new HashMap<>(customAggregates);
    }

    /**
     * Returns whether cross-server synchronization is enabled.
     *
     * @return true if cross-server sync is enabled
     */
    public boolean isEnableCrossServerSync() {
        return enableCrossServerSync;
    }

    /**
     * Gets the sync cache validity duration in milliseconds.
     *
     * @return the cache validity duration
     */
    public long getSyncCacheValidityMs() {
        return syncCacheValidityMs;
    }

    /**
     * Represents a custom aggregate definition.
     */
    public static class CustomAggregate {
        private final String name;
        private final String type;
        private final List<String> statistics;
        private final String formula;

        /**
         * Creates a new custom aggregate.
         *
         * @param name       the aggregate name
         * @param type       the aggregate type (sum, formula)
         * @param statistics the list of statistic keys to aggregate
         * @param formula    the formula for formula-type aggregates
         */
        public CustomAggregate(
            final @NotNull String name,
            final @NotNull String type,
            final @NotNull List<String> statistics,
            final String formula
        ) {
            this.name = name;
            this.type = type;
            this.statistics = List.copyOf(statistics);
            this.formula = formula;
        }

        /**
         * Gets the aggregate name.
         *
         * @return the name
         */
        public String getName() {
            return name;
        }

        /**
         * Gets the aggregate type.
         *
         * @return the type
         */
        public String getType() {
            return type;
        }

        /**
         * Gets the list of statistics to aggregate.
         *
         * @return the statistics
         */
        public List<String> getStatistics() {
            return statistics;
        }

        /**
         * Gets the formula for formula-type aggregates.
         *
         * @return the formula, or null if not a formula type
         */
        public String getFormula() {
            return formula;
        }
    }
}
