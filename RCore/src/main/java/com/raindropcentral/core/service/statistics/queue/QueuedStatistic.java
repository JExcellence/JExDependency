package com.raindropcentral.core.service.statistics.queue;

import com.raindropcentral.rplatform.type.EStatisticType.StatisticDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Represents a statistic queued for delivery to the RaindropCentral backend.
 * Immutable record containing all information needed for batching and transmission.
 *
 * @param playerUuid          the UUID of the player this statistic belongs to
 * @param statisticKey        the unique key identifying the statistic type
 * @param value               the statistic value (Number, String, Boolean, or Long for timestamps)
 * @param dataType            the data type of the value
 * @param collectionTimestamp the timestamp when this statistic was collected
 * @param priority            the delivery priority for this statistic
 * @param isDelta             true if this value represents a change since last delivery
 * @param sourcePlugin        the plugin that generated this statistic
 *
 * @author JExcellence
 * @since 1.0.0
 */
public record QueuedStatistic(
    @NotNull UUID playerUuid,
    @NotNull String statisticKey,
    @NotNull Object value,
    @NotNull StatisticDataType dataType,
    long collectionTimestamp,
    @NotNull DeliveryPriority priority,
    boolean isDelta,
    @NotNull String sourcePlugin
) {

    /**
     * Creates a new QueuedStatistic with validation.
     */
    public QueuedStatistic {
        if (playerUuid == null) throw new IllegalArgumentException("playerUuid cannot be null");
        if (statisticKey == null || statisticKey.isBlank()) throw new IllegalArgumentException("statisticKey cannot be null or blank");
        if (value == null) throw new IllegalArgumentException("value cannot be null");
        if (dataType == null) throw new IllegalArgumentException("dataType cannot be null");
        if (priority == null) throw new IllegalArgumentException("priority cannot be null");
        if (sourcePlugin == null || sourcePlugin.isBlank()) throw new IllegalArgumentException("sourcePlugin cannot be null or blank");
    }

    /**
     * Creates a builder for constructing QueuedStatistic instances.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a copy of this statistic with a different priority.
     *
     * @param newPriority the new priority
     * @return a new QueuedStatistic with the updated priority
     */
    public QueuedStatistic withPriority(final @NotNull DeliveryPriority newPriority) {
        return new QueuedStatistic(
            playerUuid, statisticKey, value, dataType,
            collectionTimestamp, newPriority, isDelta, sourcePlugin
        );
    }

    /**
     * Creates a unique key for deduplication purposes.
     * Combines player UUID and statistic key.
     *
     * @return the deduplication key
     */
    public String getDeduplicationKey() {
        return playerUuid.toString() + ":" + statisticKey;
    }

    /**
     * Builder for creating QueuedStatistic instances.
     */
    public static class Builder {
        private UUID playerUuid;
        private String statisticKey;
        private Object value;
        private StatisticDataType dataType;
        private long collectionTimestamp = System.currentTimeMillis();
        private DeliveryPriority priority = DeliveryPriority.NORMAL;
        private boolean isDelta = false;
        private String sourcePlugin = "RCore";

        /**
         * Performs playerUuid.
         */
        public Builder playerUuid(final @NotNull UUID playerUuid) {
            this.playerUuid = playerUuid;
            return this;
        }

        /**
         * Performs statisticKey.
         */
        public Builder statisticKey(final @NotNull String statisticKey) {
            this.statisticKey = statisticKey;
            return this;
        }

        /**
         * Performs value.
         */
        public Builder value(final @NotNull Object value) {
            this.value = value;
            return this;
        }

        /**
         * Performs dataType.
         */
        public Builder dataType(final @NotNull StatisticDataType dataType) {
            this.dataType = dataType;
            return this;
        }

        /**
         * Performs collectionTimestamp.
         */
        public Builder collectionTimestamp(final long collectionTimestamp) {
            this.collectionTimestamp = collectionTimestamp;
            return this;
        }

        /**
         * Performs priority.
         */
        public Builder priority(final @NotNull DeliveryPriority priority) {
            this.priority = priority;
            return this;
        }

        /**
         * Returns whether delta.
         */
        public Builder isDelta(final boolean isDelta) {
            this.isDelta = isDelta;
            return this;
        }

        /**
         * Performs sourcePlugin.
         */
        public Builder sourcePlugin(final @NotNull String sourcePlugin) {
            this.sourcePlugin = sourcePlugin;
            return this;
        }

        /**
         * Performs build.
         */
        public QueuedStatistic build() {
            return new QueuedStatistic(
                playerUuid, statisticKey, value, dataType,
                collectionTimestamp, priority, isDelta, sourcePlugin
            );
        }
    }
}
