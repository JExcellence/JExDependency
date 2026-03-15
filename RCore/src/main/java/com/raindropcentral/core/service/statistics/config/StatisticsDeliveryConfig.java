package com.raindropcentral.core.service.statistics.config;

import com.raindropcentral.rplatform.type.EStatisticType.StatisticCategory;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Configuration for the Statistics Delivery System.
 * Manages all settings related to statistics collection, queuing, batching,
 * delivery, filtering, cross-server sync, and security.
 *
 * @author JExcellence
 * @since 1.0.0
 */
public class StatisticsDeliveryConfig {

    // Delivery intervals
    private int deliveryIntervalSeconds;
    private int nativeStatCollectionIntervalSeconds;

    // Queue settings
    private int maxQueueSize;
    private int backpressureWarningThreshold;
    private int backpressureCriticalThreshold;
    private int persistenceIntervalSeconds;

    // Batch settings
    private int maxBatchSizeHighPriority;
    private int maxBatchSizeNormal;
    private int compressionThresholdBytes;

    // Rate limiting
    private int maxRequestsPerMinute;
    private int maxRetries;
    private long initialBackoffMs;
    private long maxBackoffMs;

    // Filtering
    private Set<StatisticCategory> enabledCategories;
    private Set<String> excludedStatisticKeys;
    private Set<String> includedStatisticKeys;
    private List<Pattern> excludePatterns;
    private List<Pattern> includePatterns;
    private boolean collectNativeStatistics;
    private boolean collectBlockStatistics;
    private boolean collectItemStatistics;
    private boolean collectMobStatistics;
    private boolean collectTravelStatistics;
    private boolean collectGeneralStatistics;

    // Event thresholds
    private double significantChangeThresholdPercent;
    private double economyTransactionThreshold;
    private int eventConsolidationWindowMs;

    // Cross-server sync
    private boolean enableCrossServerSync;
    private long cacheValidityMs;
    private ConflictStrategy defaultConflictStrategy;

    // Security
    private boolean signPayloads;
    private boolean encryptSensitiveData;

    // System
    private boolean enabled;
    private boolean diagnosticMode;

    /**
     * Creates a new configuration with default values.
     */
    public StatisticsDeliveryConfig() {
        setDefaults();
    }

    /**
     * Creates a new configuration loaded from plugin config.
     *
     * @param plugin the plugin to load configuration from
     */
    public StatisticsDeliveryConfig(final @NotNull Plugin plugin) {
        setDefaults();
        load(plugin);
    }

    private void setDefaults() {
        // Delivery intervals
        this.deliveryIntervalSeconds = 300;
        this.nativeStatCollectionIntervalSeconds = 60;

        // Queue settings
        this.maxQueueSize = 50000;
        this.backpressureWarningThreshold = 5000;
        this.backpressureCriticalThreshold = 7500;
        this.persistenceIntervalSeconds = 60;

        // Batch settings
        this.maxBatchSizeHighPriority = 500;
        this.maxBatchSizeNormal = 2000;
        this.compressionThresholdBytes = 5120;

        // Rate limiting
        this.maxRequestsPerMinute = 60;
        this.maxRetries = 5;
        this.initialBackoffMs = 2000;
        this.maxBackoffMs = 60000;

        // Filtering - enable all categories except SYSTEM by default
        this.enabledCategories = EnumSet.allOf(StatisticCategory.class);
        this.enabledCategories.remove(StatisticCategory.SYSTEM);
        this.excludedStatisticKeys = new HashSet<>();
        this.includedStatisticKeys = new HashSet<>();
        this.excludePatterns = List.of();
        this.includePatterns = List.of();
        this.collectNativeStatistics = true;
        this.collectBlockStatistics = true;
        this.collectItemStatistics = true;
        this.collectMobStatistics = true;
        this.collectTravelStatistics = true;
        this.collectGeneralStatistics = true;

        // Event thresholds
        this.significantChangeThresholdPercent = 10.0;
        this.economyTransactionThreshold = 1000.0;
        this.eventConsolidationWindowMs = 5000;

        // Cross-server sync
        this.enableCrossServerSync = true;
        this.cacheValidityMs = 300000;
        this.defaultConflictStrategy = ConflictStrategy.LATEST_WINS;

        // Security
        this.signPayloads = true;
        this.encryptSensitiveData = true;

        // System
        this.enabled = true;
        this.diagnosticMode = false;
    }


    /**
     * Loads configuration from the plugin's config.yml.
     *
     * @param plugin the plugin to load configuration from
     */
    public void load(final @NotNull Plugin plugin) {
        var config = plugin.getConfig();
        var section = config.getConfigurationSection("statistics-delivery");
        if (section == null) {
            return;
        }

        // System
        this.enabled = section.getBoolean("enabled", this.enabled);
        this.diagnosticMode = section.getBoolean("diagnostic-mode", this.diagnosticMode);

        // Delivery intervals
        this.deliveryIntervalSeconds = clamp(
            section.getInt("delivery-interval-seconds", this.deliveryIntervalSeconds),
            30, 3600
        );
        this.nativeStatCollectionIntervalSeconds = clamp(
            section.getInt("native-stat-collection-interval-seconds", this.nativeStatCollectionIntervalSeconds),
            30, 300
        );

        // Queue settings
        loadQueueSettings(section);

        // Batch settings
        loadBatchSettings(section);

        // Rate limiting
        loadRateLimitSettings(section);

        // Filtering
        loadFilterSettings(section);

        // Event thresholds
        loadEventThresholds(section);

        // Cross-server sync
        loadSyncSettings(section);

        // Security
        loadSecuritySettings(section);
    }

    private void loadQueueSettings(final ConfigurationSection section) {
        var queueSection = section.getConfigurationSection("queue");
        if (queueSection == null) return;

        this.maxQueueSize = queueSection.getInt("max-size", this.maxQueueSize);
        this.backpressureWarningThreshold = queueSection.getInt("backpressure-warning-threshold", this.backpressureWarningThreshold);
        this.backpressureCriticalThreshold = queueSection.getInt("backpressure-critical-threshold", this.backpressureCriticalThreshold);
        this.persistenceIntervalSeconds = queueSection.getInt("persistence-interval-seconds", this.persistenceIntervalSeconds);
    }

    private void loadBatchSettings(final ConfigurationSection section) {
        var batchSection = section.getConfigurationSection("batch");
        if (batchSection == null) return;

        this.maxBatchSizeHighPriority = batchSection.getInt("max-size-high-priority", this.maxBatchSizeHighPriority);
        this.maxBatchSizeNormal = batchSection.getInt("max-size-normal", this.maxBatchSizeNormal);
        this.compressionThresholdBytes = batchSection.getInt("compression-threshold-bytes", this.compressionThresholdBytes);
    }

    private void loadRateLimitSettings(final ConfigurationSection section) {
        var rateLimitSection = section.getConfigurationSection("rate-limit");
        if (rateLimitSection == null) return;

        this.maxRequestsPerMinute = rateLimitSection.getInt("max-requests-per-minute", this.maxRequestsPerMinute);
        this.maxRetries = rateLimitSection.getInt("max-retries", this.maxRetries);
        this.initialBackoffMs = rateLimitSection.getLong("initial-backoff-ms", this.initialBackoffMs);
        this.maxBackoffMs = rateLimitSection.getLong("max-backoff-ms", this.maxBackoffMs);
    }

    private void loadFilterSettings(final ConfigurationSection section) {
        var filterSection = section.getConfigurationSection("filter");
        if (filterSection == null) return;

        // Categories
        var categoriesList = filterSection.getStringList("enabled-categories");
        if (!categoriesList.isEmpty()) {
            this.enabledCategories = EnumSet.noneOf(StatisticCategory.class);
            for (String categoryName : categoriesList) {
                try {
                    this.enabledCategories.add(StatisticCategory.valueOf(categoryName.toUpperCase()));
                } catch (IllegalArgumentException ignored) {
                    // Invalid category name, skip
                }
            }
        }

        // Excluded/included keys
        this.excludedStatisticKeys = new HashSet<>(filterSection.getStringList("excluded-keys"));
        this.includedStatisticKeys = new HashSet<>(filterSection.getStringList("included-keys"));

        // Regex patterns
        this.excludePatterns = filterSection.getStringList("exclude-patterns").stream()
            .map(Pattern::compile)
            .toList();
        this.includePatterns = filterSection.getStringList("include-patterns").stream()
            .map(Pattern::compile)
            .toList();

        // Native statistics
        var nativeSection = filterSection.getConfigurationSection("native-statistics");
        if (nativeSection != null) {
            this.collectNativeStatistics = nativeSection.getBoolean("enabled", this.collectNativeStatistics);
            this.collectBlockStatistics = nativeSection.getBoolean("blocks", this.collectBlockStatistics);
            this.collectItemStatistics = nativeSection.getBoolean("items", this.collectItemStatistics);
            this.collectMobStatistics = nativeSection.getBoolean("mobs", this.collectMobStatistics);
            this.collectTravelStatistics = nativeSection.getBoolean("travel", this.collectTravelStatistics);
            this.collectGeneralStatistics = nativeSection.getBoolean("general", this.collectGeneralStatistics);
        }
    }

    private void loadEventThresholds(final ConfigurationSection section) {
        var eventSection = section.getConfigurationSection("events");
        if (eventSection == null) return;

        this.significantChangeThresholdPercent = eventSection.getDouble("significant-change-threshold-percent", this.significantChangeThresholdPercent);
        this.economyTransactionThreshold = eventSection.getDouble("economy-transaction-threshold", this.economyTransactionThreshold);
        this.eventConsolidationWindowMs = eventSection.getInt("consolidation-window-ms", this.eventConsolidationWindowMs);
    }

    private void loadSyncSettings(final ConfigurationSection section) {
        var syncSection = section.getConfigurationSection("cross-server-sync");
        if (syncSection == null) return;

        this.enableCrossServerSync = syncSection.getBoolean("enabled", this.enableCrossServerSync);
        this.cacheValidityMs = syncSection.getLong("cache-validity-ms", this.cacheValidityMs);

        var strategyName = syncSection.getString("conflict-strategy", this.defaultConflictStrategy.name());
        try {
            this.defaultConflictStrategy = ConflictStrategy.valueOf(strategyName.toUpperCase());
        } catch (IllegalArgumentException ignored) {
            // Keep default
        }
    }

    private void loadSecuritySettings(final ConfigurationSection section) {
        var securitySection = section.getConfigurationSection("security");
        if (securitySection == null) return;

        this.signPayloads = securitySection.getBoolean("sign-payloads", this.signPayloads);
        this.encryptSensitiveData = securitySection.getBoolean("encrypt-sensitive-data", this.encryptSensitiveData);
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    // ==================== Getters ====================

    /**
     * Returns whether enabled.
     */
    public boolean isEnabled() { return enabled; }
    /**
     * Returns whether diagnosticMode.
     */
    public boolean isDiagnosticMode() { return diagnosticMode; }
    /**
     * Gets deliveryIntervalSeconds.
     */
    public int getDeliveryIntervalSeconds() { return deliveryIntervalSeconds; }
    /**
     * Gets nativeStatCollectionIntervalSeconds.
     */
    public int getNativeStatCollectionIntervalSeconds() { return nativeStatCollectionIntervalSeconds; }
    /**
     * Gets maxQueueSize.
     */
    public int getMaxQueueSize() { return maxQueueSize; }
    /**
     * Gets backpressureWarningThreshold.
     */
    public int getBackpressureWarningThreshold() { return backpressureWarningThreshold; }
    /**
     * Gets backpressureCriticalThreshold.
     */
    public int getBackpressureCriticalThreshold() { return backpressureCriticalThreshold; }
    /**
     * Gets persistenceIntervalSeconds.
     */
    public int getPersistenceIntervalSeconds() { return persistenceIntervalSeconds; }
    /**
     * Gets maxBatchSizeHighPriority.
     */
    public int getMaxBatchSizeHighPriority() { return maxBatchSizeHighPriority; }
    /**
     * Gets maxBatchSizeNormal.
     */
    public int getMaxBatchSizeNormal() { return maxBatchSizeNormal; }
    /**
     * Gets compressionThresholdBytes.
     */
    public int getCompressionThresholdBytes() { return compressionThresholdBytes; }
    /**
     * Gets maxRequestsPerMinute.
     */
    public int getMaxRequestsPerMinute() { return maxRequestsPerMinute; }
    /**
     * Gets maxRetries.
     */
    public int getMaxRetries() { return maxRetries; }
    /**
     * Gets initialBackoffMs.
     */
    public long getInitialBackoffMs() { return initialBackoffMs; }
    /**
     * Gets maxBackoffMs.
     */
    public long getMaxBackoffMs() { return maxBackoffMs; }
    /**
     * Gets enabledCategories.
     */
    public Set<StatisticCategory> getEnabledCategories() { return enabledCategories; }
    /**
     * Gets excludedStatisticKeys.
     */
    public Set<String> getExcludedStatisticKeys() { return excludedStatisticKeys; }
    /**
     * Gets includedStatisticKeys.
     */
    public Set<String> getIncludedStatisticKeys() { return includedStatisticKeys; }
    /**
     * Gets excludePatterns.
     */
    public List<Pattern> getExcludePatterns() { return excludePatterns; }
    /**
     * Gets includePatterns.
     */
    public List<Pattern> getIncludePatterns() { return includePatterns; }
    /**
     * Returns whether collectNativeStatistics.
     */
    public boolean isCollectNativeStatistics() { return collectNativeStatistics; }
    public boolean isCollectBlockStatistics() { return collectBlockStatistics; }
    public boolean isCollectItemStatistics() { return collectItemStatistics; }
    public boolean isCollectMobStatistics() { return collectMobStatistics; }
    /**
     * Returns whether collectTravelStatistics.
     */
    public boolean isCollectTravelStatistics() { return collectTravelStatistics; }
    /**
     * Returns whether collectGeneralStatistics.
     */
    public boolean isCollectGeneralStatistics() { return collectGeneralStatistics; }
    /**
     * Gets significantChangeThresholdPercent.
     */
    public double getSignificantChangeThresholdPercent() { return significantChangeThresholdPercent; }
    /**
     * Gets economyTransactionThreshold.
     */
    public double getEconomyTransactionThreshold() { return economyTransactionThreshold; }
    /**
     * Gets eventConsolidationWindowMs.
     */
    public int getEventConsolidationWindowMs() { return eventConsolidationWindowMs; }
    /**
     * Returns whether enableCrossServerSync.
     */
    public boolean isEnableCrossServerSync() { return enableCrossServerSync; }
    /**
     * Gets cacheValidityMs.
     */
    public long getCacheValidityMs() { return cacheValidityMs; }
    /**
     * Gets defaultConflictStrategy.
     */
    public ConflictStrategy getDefaultConflictStrategy() { return defaultConflictStrategy; }
    /**
     * Returns whether signPayloads.
     */
    public boolean isSignPayloads() { return signPayloads; }
    /**
     * Returns whether encryptSensitiveData.
     */
    public boolean isEncryptSensitiveData() { return encryptSensitiveData; }

    /**
     * Checks if a statistic category is enabled for delivery.
     *
     * @param category the category to check
     * @return true if the category is enabled
     */
    public boolean isCategoryEnabled(final @NotNull StatisticCategory category) {
        return enabledCategories.contains(category);
    }

    /**
     * Checks if a statistic key should be collected based on filtering rules.
     *
     * @param key the statistic key to check
     * @return true if the key should be collected
     */
    public boolean shouldCollectKey(final @NotNull String key) {
        // Explicit exclusion takes precedence
        if (excludedStatisticKeys.contains(key)) {
            return false;
        }

        // Check exclude patterns
        for (Pattern pattern : excludePatterns) {
            if (pattern.matcher(key).matches()) {
                return false;
            }
        }

        // If include lists are specified, key must match
        if (!includedStatisticKeys.isEmpty() || !includePatterns.isEmpty()) {
            if (includedStatisticKeys.contains(key)) {
                return true;
            }
            for (Pattern pattern : includePatterns) {
                if (pattern.matcher(key).matches()) {
                    return true;
                }
            }
            return false;
        }

        return true;
    }

    /**
     * Conflict resolution strategies for cross-server synchronization.
     */
    public enum ConflictStrategy {
        /** Most recent timestamp wins. */
        LATEST_WINS,
        /** Highest numeric value wins. */
        HIGHEST_WINS,
        /** Lowest numeric value wins. */
        LOWEST_WINS,
        /** Values are summed together. */
        SUM_MERGE,
        /** Local value always wins. */
        LOCAL_WINS,
        /** Remote value always wins. */
        REMOTE_WINS
    }
}
