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

package com.raindropcentral.core.service.statistics.delivery;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * Represents a batch of statistics ready for transmission to the RaindropCentral backend.
 * This is the top-level wire format for the statistics delivery API.
 *
 * @param serverUuid        the UUID of the server sending this batch
 * @param batchId           unique identifier for this batch
 * @param timestamp         when this batch was created
 * @param compressed        whether the payload is GZIP compressed
 * @param entryCount        number of statistic entries in this batch
 * @param entries           the list of statistic entries
 * @param serverMetrics     current server performance metrics
 * @param pluginMetrics     plugin-specific activity metrics
 * @param aggregates        pre-computed aggregate statistics
 * @param continuationToken token for retrieving the next batch (if split)
 * @param checksum          SHA-256 checksum of the payload for integrity verification
 * @param signature         HMAC-SHA256 signature for authentication
 *
 * @author JExcellence
 * @since 1.0.0
 */
public record BatchPayload(
    @NotNull String serverUuid,
    @NotNull String batchId,
    long timestamp,
    boolean compressed,
    int entryCount,
    @NotNull List<StatisticEntry> entries,
    @Nullable ServerMetrics serverMetrics,
    @Nullable PluginMetrics pluginMetrics,
    @Nullable AggregatedStatistics aggregates,
    @Nullable String continuationToken,
    @Nullable String checksum,
    @Nullable String signature
) {

    /**
     * Creates a builder for BatchPayload.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a copy of this payload with a checksum.
     *
     * @param checksum the checksum to add
     * @return a new BatchPayload with the checksum
     */
    public BatchPayload withChecksum(final @NotNull String checksum) {
        return new BatchPayload(
            serverUuid, batchId, timestamp, compressed, entryCount,
            entries, serverMetrics, pluginMetrics, aggregates,
            continuationToken, checksum, signature
        );
    }

    /**
     * Creates a copy of this payload with a signature.
     *
     * @param signature the signature to add
     * @return a new BatchPayload with the signature
     */
    public BatchPayload withSignature(final @NotNull String signature) {
        return new BatchPayload(
            serverUuid, batchId, timestamp, compressed, entryCount,
            entries, serverMetrics, pluginMetrics, aggregates,
            continuationToken, checksum, signature
        );
    }

    /**
     * Builder for BatchPayload.
     */
    public static class Builder {
        private String serverUuid;
        private String batchId;
        private long timestamp = System.currentTimeMillis();
        private boolean compressed = false;
        private List<StatisticEntry> entries = List.of();
        private ServerMetrics serverMetrics;
        private PluginMetrics pluginMetrics;
        private AggregatedStatistics aggregates;
        private String continuationToken;
        private String checksum;
        private String signature;

        /**
         * Performs serverUuid.
         */
        public Builder serverUuid(final @NotNull String serverUuid) {
            this.serverUuid = serverUuid;
            return this;
        }

        /**
         * Performs serverUuid.
         */
        public Builder serverUuid(final @NotNull UUID serverUuid) {
            this.serverUuid = serverUuid.toString();
            return this;
        }

        /**
         * Performs batchId.
         */
        public Builder batchId(final @NotNull String batchId) {
            this.batchId = batchId;
            return this;
        }

        /**
         * Performs timestamp.
         */
        public Builder timestamp(final long timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        /**
         * Performs compressed.
         */
        public Builder compressed(final boolean compressed) {
            this.compressed = compressed;
            return this;
        }

        /**
         * Performs entries.
         */
        public Builder entries(final @NotNull List<StatisticEntry> entries) {
            this.entries = entries;
            return this;
        }

        /**
         * Performs serverMetrics.
         */
        public Builder serverMetrics(final @Nullable ServerMetrics serverMetrics) {
            this.serverMetrics = serverMetrics;
            return this;
        }

        /**
         * Performs pluginMetrics.
         */
        public Builder pluginMetrics(final @Nullable PluginMetrics pluginMetrics) {
            this.pluginMetrics = pluginMetrics;
            return this;
        }

        /**
         * Performs aggregates.
         */
        public Builder aggregates(final @Nullable AggregatedStatistics aggregates) {
            this.aggregates = aggregates;
            return this;
        }

        /**
         * Performs continuationToken.
         */
        public Builder continuationToken(final @Nullable String continuationToken) {
            this.continuationToken = continuationToken;
            return this;
        }

        /**
         * Performs checksum.
         */
        public Builder checksum(final @Nullable String checksum) {
            this.checksum = checksum;
            return this;
        }

        /**
         * Performs signature.
         */
        public Builder signature(final @Nullable String signature) {
            this.signature = signature;
            return this;
        }

        /**
         * Performs build.
         */
        public BatchPayload build() {
            if (serverUuid == null) throw new IllegalStateException("serverUuid is required");
            if (batchId == null) {
                batchId = "batch-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8);
            }
            return new BatchPayload(
                serverUuid, batchId, timestamp, compressed,
                entries.size(), entries, serverMetrics, pluginMetrics,
                aggregates, continuationToken, checksum, signature
            );
        }
    }
}
