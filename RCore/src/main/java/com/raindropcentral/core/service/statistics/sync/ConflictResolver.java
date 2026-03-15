package com.raindropcentral.core.service.statistics.sync;

import com.raindropcentral.core.service.statistics.config.StatisticsDeliveryConfig.ConflictStrategy;
import com.raindropcentral.rplatform.logging.CentralLogger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Resolves conflicts between local and remote statistic values during cross-server sync.
 * Supports multiple resolution strategies configurable per statistic key.
 *
 * @author JExcellence
 * @since 1.0.0
 */
public class ConflictResolver {

    private static final Logger LOGGER = CentralLogger.getLoggerByName("RCore");

    private final ConflictStrategy defaultStrategy;
    private final Map<String, ConflictStrategy> perKeyStrategies;

    /**
     * Executes ConflictResolver.
     */
    public ConflictResolver(final @NotNull ConflictStrategy defaultStrategy) {
        this.defaultStrategy = defaultStrategy;
        this.perKeyStrategies = new ConcurrentHashMap<>();
    }

    /**
     * Configures a specific strategy for a statistic key.
     *
     * @param statisticKey the statistic key
     * @param strategy     the strategy to use
     */
    public void setStrategyForKey(
        final @NotNull String statisticKey,
        final @NotNull ConflictStrategy strategy
    ) {
        perKeyStrategies.put(statisticKey, strategy);
    }

    /**
     * Gets the strategy for a statistic key.
     *
     * @param statisticKey the statistic key
     * @return the configured strategy or default
     */
    public ConflictStrategy getStrategyForKey(final @NotNull String statisticKey) {
        return perKeyStrategies.getOrDefault(statisticKey, defaultStrategy);
    }

    /**
     * Resolves a conflict between local and remote values.
     *
     * @param statisticKey the statistic key
     * @param localValue   the local value
     * @param remoteValue  the remote value
     * @param strategy     the resolution strategy
     * @return the resolved value
     */
    public Object resolve(
        final @NotNull String statisticKey,
        final @Nullable Object localValue,
        final @Nullable Object remoteValue,
        final @NotNull ConflictStrategy strategy
    ) {
        // Handle null cases
        if (localValue == null && remoteValue == null) {
            return null;
        }
        if (localValue == null) {
            return remoteValue;
        }
        if (remoteValue == null) {
            return localValue;
        }

        Object resolved = switch (strategy) {
            case LATEST_WINS -> resolveLatestWins(localValue, remoteValue);
            case HIGHEST_WINS -> resolveHighestWins(localValue, remoteValue);
            case LOWEST_WINS -> resolveLowestWins(localValue, remoteValue);
            case SUM_MERGE -> resolveSumMerge(localValue, remoteValue);
            case LOCAL_WINS -> localValue;
            case REMOTE_WINS -> remoteValue;
        };

        LOGGER.fine("Resolved conflict for '" + statisticKey + "' using " + strategy +
            ": local=" + localValue + ", remote=" + remoteValue + " -> " + resolved);

        return resolved;
    }

    /**
     * Resolves using the configured strategy for the key.
     */
    public Object resolve(
        final @NotNull String statisticKey,
        final @Nullable Object localValue,
        final @Nullable Object remoteValue
    ) {
        return resolve(statisticKey, localValue, remoteValue, getStrategyForKey(statisticKey));
    }

    /**
     * Resolves with timestamp information (for LATEST_WINS).
     */
    public Object resolveWithTimestamp(
        final @NotNull String statisticKey,
        final @Nullable Object localValue,
        final long localTimestamp,
        final @Nullable Object remoteValue,
        final long remoteTimestamp,
        final @NotNull ConflictStrategy strategy
    ) {
        if (strategy == ConflictStrategy.LATEST_WINS) {
            return localTimestamp >= remoteTimestamp ? localValue : remoteValue;
        }
        return resolve(statisticKey, localValue, remoteValue, strategy);
    }

    private Object resolveLatestWins(final Object local, final Object remote) {
        // Without timestamp info, prefer remote (assumed more recent from backend)
        return remote;
    }

    private Object resolveHighestWins(final Object local, final Object remote) {
        if (local instanceof Number localNum && remote instanceof Number remoteNum) {
            return localNum.doubleValue() >= remoteNum.doubleValue() ? local : remote;
        }
        // For non-numeric, prefer remote
        return remote;
    }

    private Object resolveLowestWins(final Object local, final Object remote) {
        if (local instanceof Number localNum && remote instanceof Number remoteNum) {
            return localNum.doubleValue() <= remoteNum.doubleValue() ? local : remote;
        }
        return remote;
    }

    private Object resolveSumMerge(final Object local, final Object remote) {
        if (local instanceof Number localNum && remote instanceof Number remoteNum) {
            if (local instanceof Integer && remote instanceof Integer) {
                return localNum.intValue() + remoteNum.intValue();
            } else if (local instanceof Long && remote instanceof Long) {
                return localNum.longValue() + remoteNum.longValue();
            } else {
                return localNum.doubleValue() + remoteNum.doubleValue();
            }
        }
        // For non-numeric, prefer remote
        return remote;
    }

    /**
     * Creates a resolution result with metadata.
     */
    public ResolutionResult resolveWithMetadata(
        final @NotNull String statisticKey,
        final @Nullable Object localValue,
        final @Nullable Object remoteValue,
        final @NotNull ConflictStrategy strategy
    ) {
        Object resolved = resolve(statisticKey, localValue, remoteValue, strategy);
        boolean hadConflict = localValue != null && remoteValue != null && !localValue.equals(remoteValue);

        return new ResolutionResult(
            statisticKey,
            localValue,
            remoteValue,
            resolved,
            strategy,
            hadConflict
        );
    }

    /**
     * Result of a conflict resolution.
     */
    public record ResolutionResult(
        String statisticKey,
        Object localValue,
        Object remoteValue,
        Object resolvedValue,
        ConflictStrategy strategyUsed,
        boolean hadConflict
    ) {}
}
