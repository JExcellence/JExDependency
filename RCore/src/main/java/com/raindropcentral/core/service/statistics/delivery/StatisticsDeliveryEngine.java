package com.raindropcentral.core.service.statistics.delivery;

import com.raindropcentral.core.service.statistics.config.StatisticsDeliveryConfig;
import com.raindropcentral.core.service.statistics.queue.DeliveryPriority;
import com.raindropcentral.core.service.statistics.queue.QueuedStatistic;
import com.raindropcentral.rplatform.logging.CentralLogger;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

/**
 * Orchestrates the statistics delivery process.
 * Coordinates rate limiting, batching, compression, and retry logic.
 *
 * @author JExcellence
 * @since 1.0.0
 */
public class StatisticsDeliveryEngine {

    private static final Logger LOGGER = CentralLogger.getLoggerByName("RCore");

    private final RateLimiter rateLimiter;
    private final RetryHandler retryHandler;
    private final BatchProcessor batchProcessor;
    private final PayloadCompressor payloadCompressor;
    private final StatisticsDeliveryConfig config;

    // Delivery metrics
    private final AtomicLong totalDeliveries = new AtomicLong(0);
    private final AtomicLong successfulDeliveries = new AtomicLong(0);
    private final AtomicLong failedDeliveries = new AtomicLong(0);
    private final AtomicLong totalStatisticsDelivered = new AtomicLong(0);
    private final AtomicLong totalBytesTransmitted = new AtomicLong(0);
    private final AtomicLong totalLatencyMs = new AtomicLong(0);

    // Delivery callback for actual transmission
    private DeliveryCallback deliveryCallback;

    public StatisticsDeliveryEngine(
        final @NotNull RateLimiter rateLimiter,
        final @NotNull RetryHandler retryHandler,
        final @NotNull BatchProcessor batchProcessor,
        final @NotNull PayloadCompressor payloadCompressor,
        final @NotNull StatisticsDeliveryConfig config
    ) {
        this.rateLimiter = rateLimiter;
        this.retryHandler = retryHandler;
        this.batchProcessor = batchProcessor;
        this.payloadCompressor = payloadCompressor;
        this.config = config;
    }


    /**
     * Sets the delivery callback for actual transmission.
     *
     * @param callback the callback to use for delivery
     */
    public void setDeliveryCallback(final @NotNull DeliveryCallback callback) {
        this.deliveryCallback = callback;
    }

    /**
     * Delivers a list of statistics with automatic priority detection.
     *
     * @param statistics the statistics to deliver
     * @return a future containing the delivery result
     */
    public CompletableFuture<DeliveryResult> deliver(final @NotNull List<QueuedStatistic> statistics) {
        if (statistics.isEmpty()) {
            return CompletableFuture.completedFuture(DeliveryResult.empty());
        }

        // Determine highest priority in the batch
        DeliveryPriority priority = statistics.stream()
            .map(QueuedStatistic::priority)
            .min((a, b) -> Integer.compare(a.getOrder(), b.getOrder()))
            .orElse(DeliveryPriority.NORMAL);

        return deliverWithPriority(statistics, priority);
    }

    /**
     * Delivers statistics with a specific priority.
     *
     * @param statistics the statistics to deliver
     * @param priority   the delivery priority
     * @return a future containing the delivery result
     */
    public CompletableFuture<DeliveryResult> deliverWithPriority(
        final @NotNull List<QueuedStatistic> statistics,
        final @NotNull DeliveryPriority priority
    ) {
        if (statistics.isEmpty()) {
            return CompletableFuture.completedFuture(DeliveryResult.empty());
        }

        totalDeliveries.incrementAndGet();
        long startTime = System.currentTimeMillis();

        // Check rate limit
        if (!rateLimiter.tryAcquire()) {
            LOGGER.fine("Rate limited, deferring delivery of " + statistics.size() + " statistics");
            return CompletableFuture.completedFuture(
                DeliveryResult.rateLimited(statistics.size(), rateLimiter.getRemainingPauseMs())
            );
        }

        // Process into batches
        List<BatchPayload> batches = batchProcessor.process(statistics, priority);
        if (batches.isEmpty()) {
            return CompletableFuture.completedFuture(DeliveryResult.empty());
        }

        // Deliver all batches
        return deliverBatches(batches, startTime);
    }

    /**
     * Delivers multiple batches sequentially.
     */
    private CompletableFuture<DeliveryResult> deliverBatches(
        final List<BatchPayload> batches,
        final long startTime
    ) {
        CompletableFuture<DeliveryResult> result = CompletableFuture.completedFuture(
            DeliveryResult.builder().build()
        );

        for (BatchPayload batch : batches) {
            result = result.thenCompose(prev -> deliverBatch(batch)
                .thenApply(batchResult -> mergeResults(prev, batchResult)));
        }

        return result.thenApply(finalResult -> {
            long latency = System.currentTimeMillis() - startTime;
            totalLatencyMs.addAndGet(latency);

            if (finalResult.success()) {
                successfulDeliveries.incrementAndGet();
                rateLimiter.recordSuccess();
            } else {
                failedDeliveries.incrementAndGet();
                rateLimiter.recordError();
            }

            return finalResult.withLatency(latency);
        });
    }

    /**
     * Delivers a single batch with retry logic.
     */
    private CompletableFuture<DeliveryResult> deliverBatch(final @NotNull BatchPayload batch) {
        // Calculate checksum
        BatchPayload batchWithChecksum = addChecksum(batch);

        // Compress if needed
        PayloadCompressor.CompressionResult compressionResult =
            payloadCompressor.compressIfNeeded(batchWithChecksum);

        totalBytesTransmitted.addAndGet(compressionResult.compressedSize());

        // Record the request
        rateLimiter.recordRequest();

        // Execute with retry
        return retryHandler.executeWithRetry(() -> transmitBatch(batchWithChecksum, compressionResult))
            .thenApply(receipt -> {
                totalStatisticsDelivered.addAndGet(batch.entryCount());
                return DeliveryResult.success(batch.entryCount(), receipt);
            })
            .exceptionally(error -> {
                LOGGER.warning("Batch delivery failed: " + error.getMessage());
                return DeliveryResult.failure(batch.entryCount(), error.getMessage());
            });
    }

    /**
     * Transmits a batch to the backend.
     */
    private CompletableFuture<DeliveryReceipt> transmitBatch(
        final BatchPayload batch,
        final PayloadCompressor.CompressionResult compressionResult
    ) {
        if (deliveryCallback == null) {
            return CompletableFuture.failedFuture(
                new IllegalStateException("No delivery callback configured")
            );
        }

        return deliveryCallback.deliver(batch, compressionResult);
    }

    /**
     * Adds a SHA-256 checksum to the batch.
     */
    private BatchPayload addChecksum(final @NotNull BatchPayload batch) {
        String checksum = calculateChecksum(batch);
        return BatchPayload.builder()
            .serverUuid(batch.serverUuid())
            .batchId(batch.batchId())
            .timestamp(batch.timestamp())
            .compressed(batch.compressed())
            .entries(batch.entries())
            .serverMetrics(batch.serverMetrics())
            .pluginMetrics(batch.pluginMetrics())
            .aggregates(batch.aggregates())
            .continuationToken(batch.continuationToken())
            .checksum(checksum)
            .signature(batch.signature())
            .build();
    }

    /**
     * Calculates SHA-256 checksum for a batch.
     */
    public String calculateChecksum(final @NotNull BatchPayload batch) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            // Include key fields in checksum
            digest.update(batch.serverUuid().getBytes(StandardCharsets.UTF_8));
            digest.update(batch.batchId().getBytes(StandardCharsets.UTF_8));
            digest.update(String.valueOf(batch.timestamp()).getBytes(StandardCharsets.UTF_8));
            digest.update(String.valueOf(batch.entryCount()).getBytes(StandardCharsets.UTF_8));

            // Include entry data
            for (StatisticEntry entry : batch.entries()) {
                digest.update(entry.playerUuid().toString().getBytes(StandardCharsets.UTF_8));
                digest.update(entry.statisticKey().getBytes(StandardCharsets.UTF_8));
                digest.update(String.valueOf(entry.value()).getBytes(StandardCharsets.UTF_8));
            }

            byte[] hash = digest.digest();
            return HexFormat.of().formatHex(hash);

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    /**
     * Merges two delivery results.
     */
    private DeliveryResult mergeResults(final DeliveryResult a, final DeliveryResult b) {
        return DeliveryResult.builder()
            .success(a.success() && b.success())
            .statisticsDelivered(a.statisticsDelivered() + b.statisticsDelivered())
            .statisticsFailed(a.statisticsFailed() + b.statisticsFailed())
            .errorMessage(a.errorMessage() != null ? a.errorMessage() : b.errorMessage())
            .receipt(b.receipt() != null ? b.receipt() : a.receipt())
            .build();
    }

    // ==================== Metrics ====================

    public DeliveryMetrics getMetrics() {
        long total = totalDeliveries.get();
        long successful = successfulDeliveries.get();
        long failed = failedDeliveries.get();
        long avgLatency = total > 0 ? totalLatencyMs.get() / total : 0;

        return new DeliveryMetrics(
            total,
            successful,
            failed,
            totalStatisticsDelivered.get(),
            totalBytesTransmitted.get(),
            avgLatency,
            total > 0 ? (double) successful / total : 1.0
        );
    }

    public void resetMetrics() {
        totalDeliveries.set(0);
        successfulDeliveries.set(0);
        failedDeliveries.set(0);
        totalStatisticsDelivered.set(0);
        totalBytesTransmitted.set(0);
        totalLatencyMs.set(0);
    }

    /**
     * Delivery metrics record.
     */
    public record DeliveryMetrics(
        long totalDeliveries,
        long successfulDeliveries,
        long failedDeliveries,
        long totalStatisticsDelivered,
        long totalBytesTransmitted,
        long averageLatencyMs,
        double successRate
    ) {}

    /**
     * Callback interface for actual delivery transmission.
     */
    @FunctionalInterface
    public interface DeliveryCallback {
        CompletableFuture<DeliveryReceipt> deliver(
            BatchPayload batch,
            PayloadCompressor.CompressionResult compressionResult
        );
    }
}
