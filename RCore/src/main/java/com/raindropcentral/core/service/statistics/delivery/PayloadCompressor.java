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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.raindropcentral.rplatform.logging.CentralLogger;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;
import java.util.zip.GZIPOutputStream;

/**
 * Compresses batch payloads using GZIP for efficient transmission.
 * Only compresses payloads exceeding a configurable size threshold.
 *
 * @author JExcellence
 * @since 1.0.0
 */
public class PayloadCompressor {

    private static final Logger LOGGER = CentralLogger.getLoggerByName("RCore");

/** Represents this API member. */
    private static final int DEFAULT_COMPRESSION_THRESHOLD = 5 * 1024;

    private final Gson gson;
    private final int compressionThreshold;

    /**
     * Executes PayloadCompressor.
     */
    public PayloadCompressor() {
        this(DEFAULT_COMPRESSION_THRESHOLD);
    }

    /**
     * Executes PayloadCompressor.
     */
    public PayloadCompressor(final int compressionThreshold) {
        this.gson = new GsonBuilder().create();
        this.compressionThreshold = compressionThreshold;
    }

    /**
     * Executes PayloadCompressor.
     */
    public PayloadCompressor(final @NotNull com.raindropcentral.core.service.statistics.config.StatisticsDeliveryConfig config) {
        this.gson = new GsonBuilder().create();
        this.compressionThreshold = config.getCompressionThresholdBytes();
    }

    /**
     * Checks if a batch payload should be compressed based on size threshold.
     *
     * @param batch the batch to check
     * @return true if the batch should be compressed
     */
    public boolean shouldCompress(final @NotNull BatchPayload batch) {
        String json = gson.toJson(batch);
        return json.getBytes(StandardCharsets.UTF_8).length > compressionThreshold;
    }

    /**
     * Compresses a batch payload if it exceeds the threshold.
     * Returns uncompressed data if below threshold or compression fails.
     *
     * @param batch the batch to potentially compress
     * @return compression result with data and metadata
     */
    public CompressionResult compressIfNeeded(final @NotNull BatchPayload batch) {
        String json = gson.toJson(batch);
        byte[] uncompressedBytes = json.getBytes(StandardCharsets.UTF_8);
        int originalSize = uncompressedBytes.length;

        if (originalSize <= compressionThreshold) {
            return new CompressionResult(
                uncompressedBytes,
                originalSize,
                originalSize,
                false,
                1.0
            );
        }

        return compress(batch);
    }

    /**
     * Compresses a batch payload using GZIP.
     *
     * @param batch the batch to compress
     * @return compression result
     */
    public CompressionResult compress(final @NotNull BatchPayload batch) {
        String json = gson.toJson(batch);
        byte[] uncompressedBytes = json.getBytes(StandardCharsets.UTF_8);
        int originalSize = uncompressedBytes.length;

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             GZIPOutputStream gzipOut = new GZIPOutputStream(baos)) {

            gzipOut.write(uncompressedBytes);
            gzipOut.finish();

            byte[] compressedBytes = baos.toByteArray();
            int compressedSize = compressedBytes.length;
            double ratio = (double) compressedSize / originalSize;

            LOGGER.fine("Compressed payload from " + originalSize + " to " + compressedSize +
                " bytes (ratio: " + String.format("%.2f", ratio) + ")");

            return new CompressionResult(
                compressedBytes,
                originalSize,
                compressedSize,
                true,
                ratio
            );

        } catch (IOException e) {
            LOGGER.warning("Compression failed, using uncompressed payload: " + e.getMessage());
            return new CompressionResult(
                uncompressedBytes,
                originalSize,
                originalSize,
                false,
                1.0
            );
        }
    }

    /**
     * Compresses raw bytes using GZIP.
     *
     * @param data the data to compress
     * @return compressed bytes
     * @throws IOException if compression fails
     */
    public byte[] compressBytes(final byte[] data) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             GZIPOutputStream gzipOut = new GZIPOutputStream(baos)) {

            gzipOut.write(data);
            gzipOut.finish();

            return baos.toByteArray();
        }
    }

    /**
     * Gets the compression threshold in bytes.
     *
     * @return compression threshold
     */
    public int getCompressionThreshold() {
        return compressionThreshold;
    }

    /**
     * Result of a compression operation.
     *
     * @param data             the resulting data (compressed or uncompressed)
     * @param originalSize     original size in bytes
     * @param compressedSize   compressed size in bytes
     * @param compressed       whether compression was applied
     * @param compressionRatio ratio of compressed to original size
     */
    public record CompressionResult(
        byte[] data,
        int originalSize,
        int compressedSize,
        boolean compressed,
        double compressionRatio
    ) {
        /**
         * Gets the space savings as a percentage.
         *
         * @return savings percentage (0-100)
         */
        public double savingsPercent() {
            if (!compressed || originalSize == 0) {
                return 0.0;
            }
            return (1.0 - compressionRatio) * 100;
        }
    }
}
