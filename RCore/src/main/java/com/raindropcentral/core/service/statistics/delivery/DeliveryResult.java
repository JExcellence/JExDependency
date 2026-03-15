package com.raindropcentral.core.service.statistics.delivery;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Result of a statistics delivery attempt.
 * Contains success/failure status, counts, and performance metrics.
 *
 * @param success             whether the delivery was successful
 * @param batchId             the batch ID that was delivered
 * @param statisticsDelivered number of statistics successfully delivered
 * @param statisticsFailed    number of statistics that failed to deliver
 * @param errorMessage        error message if delivery failed
 * @param receipt             the delivery receipt from the backend
 * @param latencyMs           time taken for the delivery in milliseconds
 * @param rateLimited         whether the delivery was rate limited
 * @param retryAfterMs        milliseconds to wait before retrying (if rate limited)
 *
 * @author JExcellence
 * @since 1.0.0
 */
public record DeliveryResult(
    boolean success,
    @Nullable String batchId,
    int statisticsDelivered,
    int statisticsFailed,
    @Nullable String errorMessage,
    @Nullable DeliveryReceipt receipt,
    long latencyMs,
    boolean rateLimited,
    long retryAfterMs
) {

    /**
     * Creates an empty result (no statistics to deliver).
     */
    public static DeliveryResult empty() {
        return new DeliveryResult(true, null, 0, 0, null, null, 0, false, 0);
    }

    /**
     * Creates a rate-limited result.
     */
    public static DeliveryResult rateLimited(final int statisticsCount, final long retryAfterMs) {
        return new DeliveryResult(false, null, 0, statisticsCount,
            "Rate limited", null, 0, true, retryAfterMs);
    }

    /**
     * Creates a successful result.
     */
    public static DeliveryResult success(final int statisticsDelivered, final DeliveryReceipt receipt) {
        return new DeliveryResult(true, receipt != null ? receipt.batchId() : null,
            statisticsDelivered, 0, null, receipt, 0, false, 0);
    }

    /**
     * Creates a failure result.
     */
    public static DeliveryResult failure(final int statisticsFailed, final String errorMessage) {
        return new DeliveryResult(false, null, 0, statisticsFailed, errorMessage, null, 0, false, 0);
    }

    /**
     * Creates a builder for constructing results.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns a copy with updated latency.
     */
    public DeliveryResult withLatency(final long latencyMs) {
        return new DeliveryResult(success, batchId, statisticsDelivered, statisticsFailed,
            errorMessage, receipt, latencyMs, rateLimited, retryAfterMs);
    }

    // Legacy compatibility methods
    /**
     * Executes deliveredCount.
     */
    public int deliveredCount() { return statisticsDelivered; }
    /**
     * Executes failedCount.
     */
    public int failedCount() { return statisticsFailed; }

    /**
     * Creates a successful delivery result with full parameters.
     *
     * @param batchId        the batch ID
     * @param deliveredCount statistics delivered
     * @param receipt        the delivery receipt
     * @param latencyMs      delivery latency
     * @return a successful result
     */
    public static DeliveryResult success(
        final @NotNull String batchId,
        final int deliveredCount,
        final @NotNull DeliveryReceipt receipt,
        final long latencyMs
    ) {
        return new DeliveryResult(
            true, batchId, deliveredCount, 0,
            null, receipt, latencyMs, false, 0
        );
    }

    /**
     * Creates a failed delivery result with full parameters.
     *
     * @param batchId      the batch ID
     * @param failedCount  statistics that failed
     * @param errorMessage the error message
     * @param latencyMs    delivery latency
     * @return a failed result
     */
    public static DeliveryResult failure(
        final @NotNull String batchId,
        final int failedCount,
        final @NotNull String errorMessage,
        final long latencyMs
    ) {
        return new DeliveryResult(
            false, batchId, 0, failedCount,
            errorMessage, null, latencyMs, false, 0
        );
    }

    /**
     * Creates a partial success result (some statistics delivered, some failed).
     *
     * @param batchId        the batch ID
     * @param deliveredCount statistics delivered
     * @param failedCount    statistics that failed
     * @param receipt        the delivery receipt
     * @param latencyMs      delivery latency
     * @return a partial success result
     */
    public static DeliveryResult partial(
        final @NotNull String batchId,
        final int deliveredCount,
        final int failedCount,
        final @Nullable DeliveryReceipt receipt,
        final long latencyMs
    ) {
        return new DeliveryResult(
            deliveredCount > 0, batchId, deliveredCount, failedCount,
            failedCount > 0 ? "Partial delivery: " + failedCount + " statistics failed" : null,
            receipt, latencyMs, false, 0
        );
    }

    /**
     * Gets the total number of statistics in this delivery attempt.
     *
     * @return total statistics count
     */
    public int totalCount() {
        return statisticsDelivered + statisticsFailed;
    }

    /**
     * Gets the success rate as a percentage.
     *
     * @return success rate (0-100)
     */
    public double successRate() {
        int total = totalCount();
        return total > 0 ? (statisticsDelivered * 100.0) / total : 0.0;
    }

    /**
     * Builder for DeliveryResult.
     */
    public static class Builder {
        private boolean success = true;
        private String batchId;
        private int statisticsDelivered = 0;
        private int statisticsFailed = 0;
        private String errorMessage;
        private DeliveryReceipt receipt;
        private long latencyMs = 0;
        private boolean rateLimited = false;
        private long retryAfterMs = 0;

        /**
         * Executes success.
         */
        public Builder success(boolean success) { this.success = success; return this; }
        /**
         * Executes method.
         */
        /**
         * Executes this member.
         */
        /**
         * Executes batchId.
         */
        public Builder batchId(String batchId) { this.batchId = batchId; return this; }
        /**
         * Executes statisticsDelivered.
         */
        public Builder statisticsDelivered(int count) { this.statisticsDelivered = count; return this; }
        /**
         * Executes statisticsFailed.
         */
        public Builder statisticsFailed(int count) { this.statisticsFailed = count; return this; }
        /**
         * Executes errorMessage.
         */
        public Builder errorMessage(String msg) { this.errorMessage = msg; return this; }
        /**
         * Executes receipt.
         */
        public Builder receipt(DeliveryReceipt receipt) { this.receipt = receipt; return this; }
        /**
         * Executes latencyMs.
         */
        public Builder latencyMs(long ms) { this.latencyMs = ms; return this; }
        /**
         * Executes rateLimited.
         */
        public Builder rateLimited(boolean limited) { this.rateLimited = limited; return this; }
        /**
         * Executes retryAfterMs.
         */
        public Builder retryAfterMs(long ms) { this.retryAfterMs = ms; return this; }

        /**
         * Executes build.
         */
        public DeliveryResult build() {
            return new DeliveryResult(success, batchId, statisticsDelivered, statisticsFailed,
                errorMessage, receipt, latencyMs, rateLimited, retryAfterMs);
        }
    }
}
