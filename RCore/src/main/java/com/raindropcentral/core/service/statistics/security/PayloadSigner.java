package com.raindropcentral.core.service.statistics.security;

import com.google.gson.Gson;
import com.raindropcentral.core.service.statistics.delivery.BatchPayload;
import com.raindropcentral.core.service.statistics.delivery.DeliveryReceipt;
import com.raindropcentral.rplatform.logging.CentralLogger;
import org.jetbrains.annotations.NotNull;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.logging.Logger;

/**
 * Signs and verifies payloads using HMAC-SHA256.
 * Ensures payload integrity and authenticity during transmission.
 *
 * @author JExcellence
 * @since 1.0.0
 */
public class PayloadSigner {

    private static final Logger LOGGER = CentralLogger.getLoggerByName("RCore");
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final Gson gson;

    public PayloadSigner() {
        this.gson = new Gson();
    }

    /**
     * Signs a batch payload using HMAC-SHA256.
     *
     * @param payload the payload to sign
     * @param apiKey  the API key to use as signing key
     * @return the signed payload with signature field populated
     */
    public BatchPayload sign(final @NotNull BatchPayload payload, final @NotNull String apiKey) {
        String signature = computeSignature(payload, apiKey);

        return BatchPayload.builder()
            .serverUuid(payload.serverUuid())
            .batchId(payload.batchId())
            .timestamp(payload.timestamp())
            .compressed(payload.compressed())
            .entries(payload.entries())
            .serverMetrics(payload.serverMetrics())
            .pluginMetrics(payload.pluginMetrics())
            .aggregates(payload.aggregates())
            .continuationToken(payload.continuationToken())
            .checksum(payload.checksum())
            .signature(signature)
            .build();
    }

    /**
     * Computes the HMAC-SHA256 signature for a payload.
     *
     * @param payload the payload to sign
     * @param apiKey  the API key
     * @return the hex-encoded signature
     */
    public String computeSignature(final @NotNull BatchPayload payload, final @NotNull String apiKey) {
        try {
            // Create canonical representation for signing
            String canonical = createCanonicalString(payload);

            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(apiKey.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
            mac.init(keySpec);

            byte[] signature = mac.doFinal(canonical.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(signature);

        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            LOGGER.severe("Failed to compute signature: " + e.getMessage());
            throw new RuntimeException("Signature computation failed", e);
        }
    }

    /**
     * Verifies a delivery receipt signature.
     *
     * @param receipt the receipt to verify
     * @param apiKey  the API key
     * @return true if the signature is valid
     */
    public boolean verifyReceipt(final @NotNull DeliveryReceipt receipt, final @NotNull String apiKey) {
        if (receipt.signature() == null || receipt.signature().isEmpty()) {
            LOGGER.warning("Receipt has no signature to verify");
            return false;
        }

        try {
            String canonical = createReceiptCanonicalString(receipt);

            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(apiKey.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
            mac.init(keySpec);

            byte[] expectedSignature = mac.doFinal(canonical.getBytes(StandardCharsets.UTF_8));
            String expectedHex = HexFormat.of().formatHex(expectedSignature);

            boolean valid = expectedHex.equalsIgnoreCase(receipt.signature());
            if (!valid) {
                LOGGER.warning("Receipt signature verification failed for batch " + receipt.batchId());
            }
            return valid;

        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            LOGGER.severe("Failed to verify receipt signature: " + e.getMessage());
            return false;
        }
    }

    /**
     * Creates a canonical string representation of a payload for signing.
     */
    private String createCanonicalString(final BatchPayload payload) {
        StringBuilder sb = new StringBuilder();
        sb.append(payload.serverUuid()).append("|");
        sb.append(payload.batchId()).append("|");
        sb.append(payload.timestamp()).append("|");
        sb.append(payload.entryCount()).append("|");
        if (payload.checksum() != null) {
            sb.append(payload.checksum());
        }
        return sb.toString();
    }

    /**
     * Creates a canonical string representation of a receipt for verification.
     */
    private String createReceiptCanonicalString(final DeliveryReceipt receipt) {
        StringBuilder sb = new StringBuilder();
        sb.append(receipt.batchId()).append("|");
        sb.append(receipt.timestamp()).append("|");
        sb.append(receipt.receivedCount()).append("|");
        sb.append(receipt.processedCount());
        return sb.toString();
    }

    /**
     * Computes HMAC-SHA256 for arbitrary data.
     *
     * @param data   the data to sign
     * @param apiKey the API key
     * @return the hex-encoded signature
     */
    public String computeHmac(final byte[] data, final @NotNull String apiKey) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(apiKey.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
            mac.init(keySpec);

            byte[] signature = mac.doFinal(data);
            return HexFormat.of().formatHex(signature);

        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("HMAC computation failed", e);
        }
    }
}
