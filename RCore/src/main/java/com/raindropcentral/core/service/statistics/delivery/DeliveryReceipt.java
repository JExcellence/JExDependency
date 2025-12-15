package com.raindropcentral.core.service.statistics.delivery;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Receipt returned by the backend acknowledging successful statistic delivery.
 * Used for verification and audit purposes.
 *
 * @param success        whether the delivery was successful
 * @param batchId        the batch ID that was delivered
 * @param receivedCount  number of statistics received by the backend
 * @param processedCount number of statistics successfully processed
 * @param timestamp      when the backend processed this batch
 * @param signature      HMAC signature for receipt verification
 * @param errorMessage   error message if delivery failed
 *
 * @author JExcellence
 * @since 1.0.0
 */
public record DeliveryReceipt(
    boolean success,
    @NotNull String batchId,
    int receivedCount,
    int processedCount,
    long timestamp,
    @Nullable String signature,
    @Nullable String errorMessage
) {

    /**
     * Creates a successful receipt.
     *
     * @param batchId        the batch ID
     * @param receivedCount  statistics received
     * @param processedCount statistics processed
     * @return a successful receipt
     */
    public static DeliveryReceipt success(
        final @NotNull String batchId,
        final int receivedCount,
        final int processedCount
    ) {
        return new DeliveryReceipt(
            true, batchId, receivedCount, processedCount,
            System.currentTimeMillis(), null, null
        );
    }

    /**
     * Creates a failed receipt.
     *
     * @param batchId      the batch ID
     * @param errorMessage the error message
     * @return a failed receipt
     */
    public static DeliveryReceipt failure(
        final @NotNull String batchId,
        final @NotNull String errorMessage
    ) {
        return new DeliveryReceipt(
            false, batchId, 0, 0,
            System.currentTimeMillis(), null, errorMessage
        );
    }
}
