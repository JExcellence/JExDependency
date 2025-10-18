package com.raindropcentral.core.service;

import com.raindropcentral.core.database.entity.player.RPlayer;
import com.raindropcentral.core.database.entity.statistic.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service class for managing player statistics with comprehensive CRUD operations.
 * This implementation aligns with the current entity APIs that expose Optional-based accessors
 * and immutable statistic collections.
 */
@SuppressWarnings("unused")
public final class RPlayerStatisticService {

    private RPlayerStatisticService() {}

    public static @NotNull RPlayerStatistic createPlayerStatistic(
            final @NotNull RPlayer player
    ) {
        return new RPlayerStatistic(player);
    }

    public static @NotNull RPlayerStatistic createPlayerStatisticWithData(
            final @NotNull RPlayer player,
            final @NotNull Map<String, Object> initialStatistics,
            final @NotNull String plugin
    ) {
        final RPlayerStatistic playerStatistic = new RPlayerStatistic(player);

        initialStatistics.forEach((identifier, value) -> {
            // Upsert, so initial loaders don't ever violate unique keys if double-called
            addOrUpdateStatistic(playerStatistic, identifier, plugin, value);
        });

        return playerStatistic;
    }

    /**
     * Adds or updates a statistic (in-place update if exists).
     * Important: We treat "identifier" as the uniqueness key (DB constraint),
     * so we search by identifier first to avoid duplicate inserts.
     * 
     * This method prioritizes updating existing statistics in-place to avoid
     * Hibernate flush ordering issues that can cause unique constraint violations.
     */
    public static boolean addOrUpdateStatistic(
            final @NotNull RPlayerStatistic playerStatistic,
            final @NotNull String identifier,
            final @NotNull String plugin,
            final @NotNull Object value
    ) {
        // Try update in-place by identifier to avoid insert+delete ordering issues
        final Optional<RAbstractStatistic> existingOpt = findByIdentifier(playerStatistic, identifier);

        if (existingOpt.isPresent()) {
            final RAbstractStatistic existing = existingOpt.get();
            
            // Try to update the existing statistic in-place
            if (updateExistingStatistic(existing, value)) {
                // Successfully updated in-place, no need to create new entity
                // Note: We keep the original plugin value to avoid unnecessary changes
                return true;
            }
            
            // Type mismatch: we need to replace the statistic with a new type
            // Use addOrReplaceStatistic which properly handles removal and addition
            final RAbstractStatistic newStatistic = createStatisticFromValue(identifier, plugin, value);
            playerStatistic.addOrReplaceStatistic(newStatistic);
            return true;
        }

        // No existing statistic found - create new entity
        final RAbstractStatistic statistic = createStatisticFromValue(identifier, plugin, value);
        playerStatistic.addOrReplaceStatistic(statistic);
        return true;
    }

    public static int addStatisticsBulk(
            final @NotNull RPlayerStatistic playerStatistic,
            final @NotNull Map<String, Object> statistics,
            final @NotNull String plugin
    ) {
        int successCount = 0;
        for (Map.Entry<String, Object> entry : statistics.entrySet()) {
            if (addOrUpdateStatistic(playerStatistic, entry.getKey(), plugin, entry.getValue())) {
                successCount++;
            }
        }
        return successCount;
    }

    public static boolean removeStatistic(
            final @NotNull RPlayerStatistic playerStatistic,
            final @NotNull String identifier,
            final @NotNull String plugin
    ) {
        // Prefer entity-level removal
        if (playerStatistic.removeStatistic(identifier, plugin)) {
            return true;
        }
        // Fallback: DB uniqueness is per identifier, ensure removal if plugin didn't match
        return removeByIdentifier(playerStatistic, identifier);
    }

    public static int removeStatisticsByPlugin(
            final @NotNull RPlayerStatistic playerStatistic,
            final @NotNull String plugin
    ) {
        final Set<String> idsForPlugin = playerStatistic.getStatistics().stream()
                .filter(stat -> stat.getPlugin().equals(plugin))
                .map(RAbstractStatistic::getIdentifier)
                .collect(Collectors.toSet());

        int removed = 0;
        for (final String id : idsForPlugin) {
            if (playerStatistic.removeStatistic(id, plugin)) {
                removed++;
            }
        }
        return removed;
    }

    @SuppressWarnings("unchecked")
    public static <T> Optional<T> getStatisticValue(
            final @NotNull RPlayerStatistic playerStatistic,
            final @NotNull String identifier,
            final @NotNull String plugin,
            final @NotNull Class<T> expectedType
    ) {
        return playerStatistic
                .getStatisticValue(identifier, plugin)
                .filter(expectedType::isInstance)
                .map(v -> (T) v);
    }

    public static @Nullable String getStatisticAsString(
            final @NotNull RPlayerStatistic playerStatistic,
            final @NotNull String identifier,
            final @NotNull String plugin
    ) {
        return playerStatistic
                .getStatisticValue(identifier, plugin)
                .map(Object::toString)
                .orElse(null);
    }

    public static @Nullable Double incrementNumericStatistic(
            final @NotNull RPlayerStatistic playerStatistic,
            final @NotNull String identifier,
            final @NotNull String plugin,
            final double increment
    ) {
        final Optional<Object> current = playerStatistic.getStatisticValue(identifier, plugin);
        if (current.isPresent() && current.get() instanceof Number num) {
            final double newValue = num.doubleValue() + increment;
            addOrUpdateStatistic(playerStatistic, identifier, plugin, newValue);
            return newValue;
        }
        return null;
    }

    /**
     * Sets a timestamp statistic to the current time.
     * Uses LocalDateTime so it is stored as a TIMESTAMP statistic, not a NUMBER.
     *
     * @return epoch millis that was set
     */
    public static long setCurrentTimestamp(
            final @NotNull RPlayerStatistic playerStatistic,
            final @NotNull String identifier,
            final @NotNull String plugin
    ) {
        final LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        addOrUpdateStatistic(playerStatistic, identifier, plugin, now);
        return now.toEpochSecond(ZoneOffset.UTC) * 1000L;
    }

    public static @NotNull Map<String, Map<String, Object>> exportStatistics(
            final @NotNull RPlayerStatistic playerStatistic
    ) {
        final Map<String, Map<String, Object>> export = new HashMap<>();

        for (RAbstractStatistic statistic : playerStatistic.getStatistics()) {
            export
                    .computeIfAbsent(statistic.getPlugin(), k -> new HashMap<>())
                    .put(statistic.getIdentifier(), statistic.getValue());
        }

        return export;
    }

    public static int importStatistics(
            final @NotNull RPlayerStatistic playerStatistic,
            final @NotNull Map<String, Map<String, Object>> importData
    ) {
        int importCount = 0;

        for (Map.Entry<String, Map<String, Object>> pluginEntry : importData.entrySet()) {
            final String plugin = pluginEntry.getKey();
            importCount += addStatisticsBulk(playerStatistic, pluginEntry.getValue(), plugin);
        }

        return importCount;
    }

    public static @NotNull Map<String, Long> getStatisticCountByPlugin(
            final @NotNull RPlayerStatistic playerStatistic
    ) {
        return playerStatistic.getStatistics().stream()
                .collect(Collectors.groupingBy(
                        RAbstractStatistic::getPlugin,
                        Collectors.counting()
                ));
    }

    public static boolean hasStatistic(
            final @NotNull RPlayerStatistic playerStatistic,
            final @NotNull String identifier,
            final @NotNull String plugin
    ) {
        return playerStatistic.getStatisticValue(identifier, plugin).isPresent();
    }

    private static @NotNull RAbstractStatistic createStatisticFromValue(
            final @NotNull String identifier,
            final @NotNull String plugin,
            final @NotNull Object value
    ) {
        if (value instanceof Boolean b) {
            return new RBooleanStatistic(identifier, plugin, b);
        }
        if (value instanceof Number number) {
            return new RNumberStatistic(identifier, plugin, number.doubleValue());
        }
        if (value instanceof String s) {
            return new RStringStatistic(identifier, plugin, s);
        }
        if (value instanceof LocalDateTime localDateTime) {
            final long timestamp = localDateTime.toEpochSecond(ZoneOffset.UTC) * 1000;
            return new RDateStatistic(identifier, plugin, timestamp);
        }
        // Fallback: store as string representation
        return new RStringStatistic(identifier, plugin, value.toString());
    }

    // ---- helpers ----

    private static Optional<RAbstractStatistic> findByIdentifier(
            final @NotNull RPlayerStatistic playerStatistic,
            final @NotNull String identifier
    ) {
        // CRITICAL: Use the internal mutable collection to ensure we get the actual Hibernate-managed entities
        // This is essential for in-place updates to be properly tracked by Hibernate
        final Set<RAbstractStatistic> stats = playerStatistic.getStatisticsInternal();
        return stats.stream()
                .filter(s -> s.getIdentifier().equals(identifier))
                .findFirst();
    }

    private static boolean removeByIdentifier(
            final @NotNull RPlayerStatistic playerStatistic,
            final @NotNull String identifier
    ) {
        // There is no direct API; simulate via known plugins present
        final Set<String> plugins = playerStatistic.getStatistics().stream()
                .filter(s -> s.getIdentifier().equals(identifier))
                .map(RAbstractStatistic::getPlugin)
                .collect(Collectors.toSet());

        boolean removedAny = false;
        for (final String plugin : plugins) {
            removedAny |= playerStatistic.removeStatistic(identifier, plugin);
        }
        return removedAny;
    }

    /**
     * Update the existing statistic entity in place if the value type matches.
     * Returns true if updated, false if type mismatch (caller may replace instead).
     */
    private static boolean updateExistingStatistic(
            final @NotNull RAbstractStatistic existing,
            final @NotNull Object value
    ) {
        try {
            if (existing instanceof RBooleanStatistic bStat) {
                if (value instanceof Boolean b) {
                    bStat.setValue(b);
                    return true;
                }
                return false;
            }
            if (existing instanceof RNumberStatistic nStat) {
                if (value instanceof Number n) {
                    nStat.setValue(n.doubleValue());
                    return true;
                }
                return false;
            }
            if (existing instanceof RStringStatistic sStat) {
                if (value instanceof String s) {
                    sStat.setValue(s);
                    return true;
                }
                // allow non-string overwrite by toString() to avoid churn
                sStat.setValue(value.toString());
                return true;
            }
            if (existing instanceof RDateStatistic dStat) {
                if (value instanceof LocalDateTime ldt) {
                    final long ts = ldt.toEpochSecond(ZoneOffset.UTC) * 1000L;
                    dStat.setValue(ts);
                    return true;
                }
                if (value instanceof Number n) {
                    dStat.setValue(n.longValue());
                    return true;
                }
                return false;
            }
        } catch (final Exception ignored) {
            return false;
        }
        return false;
    }
}