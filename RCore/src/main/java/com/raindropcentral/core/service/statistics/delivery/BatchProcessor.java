package com.raindropcentral.core.service.statistics.delivery;

import com.raindropcentral.core.service.statistics.config.StatisticsDeliveryConfig;
import com.raindropcentral.core.service.statistics.queue.DeliveryPriority;
import com.raindropcentral.core.service.statistics.queue.QueuedStatistic;
import com.raindropcentral.rplatform.logging.CentralLogger;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Processes queued statistics into batch payloads for delivery.
 * Handles deduplication, batch sizing, and splitting oversized batches.
 *
 * @author JExcellence
 * @since 1.0.0
 */
public class BatchProcessor {

    private static final Logger LOGGER = CentralLogger.getLogger(BatchProcessor.class);

    /** Batch size limits by priority */
    private static final int CRITICAL_HIGH_BATCH_SIZE = 500;
    private static final int NORMAL_LOW_BULK_BATCH_SIZE = 2000;

    private final StatisticsDeliveryConfig config;
    private final String serverUuid;

    public BatchProcessor(
        final @NotNull String serverUuid,
        final @NotNull StatisticsDeliveryConfig config
    ) {
        this.serverUuid = serverUuid;
        this.config = config;
    }

    /**
     * Processes a list of queued statistics into batch payloads.
     *
     * @param statistics the statistics to process
     * @param priority   the delivery priority
     * @return list of batch payloads ready for delivery
     */
    public List<BatchPayload> process(
        final @NotNull List<QueuedStatistic> statistics,
        final @NotNull DeliveryPriority priority
    ) {
        if (statistics.isEmpty()) {
            return List.of();
        }

        // Deduplicate statistics
        List<QueuedStatistic> deduplicated = deduplicate(statistics);

        // Determine batch size based on priority
        int batchSize = getBatchSizeForPriority(priority);

        // Split into batches
        List<BatchPayload> batches = new ArrayList<>();
        List<List<QueuedStatistic>> chunks = partition(deduplicated, batchSize);

        String continuationToken = null;
        for (int i = 0; i < chunks.size(); i++) {
            List<QueuedStatistic> chunk = chunks.get(i);
            boolean hasMore = i < chunks.size() - 1;

            BatchPayload batch = createBatch(chunk, priority, continuationToken);

            if (hasMore) {
                continuationToken = batch.batchId();
            }

            batches.add(batch);
        }

        LOGGER.fine("Processed " + statistics.size() + " statistics into " + batches.size() +
            " batches (after dedup: " + deduplicated.size() + ")");

        return batches;
    }

    /**
     * Deduplicates statistics, keeping the most recent value per player-statistic pair.
     *
     * @param statistics the statistics to deduplicate
     * @return deduplicated list
     */
    public List<QueuedStatistic> deduplicate(final @NotNull List<QueuedStatistic> statistics) {
        // Group by player UUID and statistic key, keep most recent
        Map<String, QueuedStatistic> deduped = new LinkedHashMap<>();

        for (QueuedStatistic stat : statistics) {
            String key = stat.playerUuid() + ":" + stat.statisticKey();
            QueuedStatistic existing = deduped.get(key);

            if (existing == null || stat.collectionTimestamp() > existing.collectionTimestamp()) {
                deduped.put(key, stat);
            }
        }

        return new ArrayList<>(deduped.values());
    }

    /**
     * Deduplicates a batch payload in place.
     *
     * @param batch the batch to deduplicate
     * @return deduplicated batch
     */
    public BatchPayload deduplicate(final @NotNull BatchPayload batch) {
        Map<String, StatisticEntry> deduped = new LinkedHashMap<>();

        for (StatisticEntry entry : batch.entries()) {
            String key = entry.playerUuid() + ":" + entry.statisticKey();
            StatisticEntry existing = deduped.get(key);

            if (existing == null || entry.collectionTimestamp() > existing.collectionTimestamp()) {
                deduped.put(key, entry);
            }
        }

        List<StatisticEntry> dedupedEntries = new ArrayList<>(deduped.values());

        return BatchPayload.builder()
            .serverUuid(batch.serverUuid())
            .batchId(batch.batchId())
            .timestamp(batch.timestamp())
            .compressed(batch.compressed())
            .entries(dedupedEntries)
            .serverMetrics(batch.serverMetrics())
            .pluginMetrics(batch.pluginMetrics())
            .aggregates(batch.aggregates())
            .continuationToken(batch.continuationToken())
            .checksum(batch.checksum())
            .signature(batch.signature())
            .build();
    }

    /**
     * Splits an oversized batch into smaller batches with continuation tokens.
     *
     * @param batch    the batch to split
     * @param maxSize  maximum entries per batch
     * @return list of split batches
     */
    public List<BatchPayload> split(final @NotNull BatchPayload batch, final int maxSize) {
        if (batch.entryCount() <= maxSize) {
            return List.of(batch);
        }

        List<BatchPayload> result = new ArrayList<>();
        List<StatisticEntry> entries = batch.entries();
        List<List<StatisticEntry>> chunks = partitionEntries(entries, maxSize);

        String continuationToken = null;
        for (int i = 0; i < chunks.size(); i++) {
            List<StatisticEntry> chunk = chunks.get(i);
            boolean hasMore = i < chunks.size() - 1;

            String batchId = batch.batchId() + "-" + (i + 1);
            String nextToken = hasMore ? batchId : null;

            BatchPayload splitBatch = BatchPayload.builder()
                .serverUuid(batch.serverUuid())
                .batchId(batchId)
                .timestamp(batch.timestamp())
                .compressed(false)
                .entries(chunk)
                .serverMetrics(i == 0 ? batch.serverMetrics() : null)
                .pluginMetrics(i == 0 ? batch.pluginMetrics() : null)
                .aggregates(i == 0 ? batch.aggregates() : null)
                .continuationToken(nextToken)
                .checksum(null)
                .signature(null)
                .build();

            result.add(splitBatch);
            continuationToken = batchId;
        }

        LOGGER.fine("Split batch " + batch.batchId() + " into " + result.size() + " parts");
        return result;
    }

    /**
     * Creates a batch payload from queued statistics.
     */
    private BatchPayload createBatch(
        final List<QueuedStatistic> statistics,
        final DeliveryPriority priority,
        final String previousBatchId
    ) {
        String batchId = generateBatchId();
        long timestamp = System.currentTimeMillis();

        List<StatisticEntry> entries = statistics.stream()
            .map(this::toStatisticEntry)
            .collect(Collectors.toList());

        String continuationToken = previousBatchId != null ? previousBatchId : null;

        return BatchPayload.builder()
            .serverUuid(serverUuid)
            .batchId(batchId)
            .timestamp(timestamp)
            .compressed(false)
            .entries(entries)
            .serverMetrics(null)
            .pluginMetrics(null)
            .aggregates(null)
            .continuationToken(continuationToken)
            .checksum(null)
            .signature(null)
            .build();
    }

    /**
     * Converts a QueuedStatistic to a StatisticEntry.
     */
    private StatisticEntry toStatisticEntry(final QueuedStatistic stat) {
        return StatisticEntry.fromQueued(stat);
    }

    /**
     * Gets the batch size limit for a priority level.
     */
    private int getBatchSizeForPriority(final DeliveryPriority priority) {
        return switch (priority) {
            case CRITICAL, HIGH -> CRITICAL_HIGH_BATCH_SIZE;
            case NORMAL, LOW, BULK -> NORMAL_LOW_BULK_BATCH_SIZE;
        };
    }

    /**
     * Partitions a list into chunks of specified size.
     */
    private <T> List<List<T>> partition(final List<T> list, final int size) {
        List<List<T>> partitions = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            partitions.add(list.subList(i, Math.min(i + size, list.size())));
        }
        return partitions;
    }

    /**
     * Partitions statistic entries into chunks.
     */
    private List<List<StatisticEntry>> partitionEntries(final List<StatisticEntry> entries, final int size) {
        List<List<StatisticEntry>> partitions = new ArrayList<>();
        for (int i = 0; i < entries.size(); i += size) {
            partitions.add(new ArrayList<>(entries.subList(i, Math.min(i + size, entries.size()))));
        }
        return partitions;
    }

    /**
     * Generates a unique batch ID.
     */
    private String generateBatchId() {
        return serverUuid.substring(0, 8) + "-" + System.currentTimeMillis() + "-" +
            UUID.randomUUID().toString().substring(0, 8);
    }
}
