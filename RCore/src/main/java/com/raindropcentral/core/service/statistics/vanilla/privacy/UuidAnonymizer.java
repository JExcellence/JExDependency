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

package com.raindropcentral.core.service.statistics.vanilla.privacy;

import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

/**
 * Utility class for anonymizing player UUIDs for privacy protection.
 *
 * <p>This class provides methods to convert player UUIDs into anonymized versions
 * using SHA-256 hashing. The anonymization is deterministic (same UUID always produces
 * the same anonymized UUID) but irreversible (cannot recover original UUID from
 * anonymized version).
 *
 * <p>Anonymization is useful for:
 * <ul>
 *   <li>Complying with privacy regulations (GDPR, CCPA)</li>
 *   <li>Protecting player identity in analytics</li>
 *   <li>Sharing statistics without exposing player identities</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * UUID playerUuid = player.getUniqueId();
 * UUID anonymized = UuidAnonymizer.anonymizeUuid(playerUuid);
 * 
 * // Use anonymized UUID in statistics
 * statistic.setPlayerUuid(anonymized);
 * }</pre>
 *
 * @author JExcellence
 * @since 1.0.0
 */
public final class UuidAnonymizer {

    private static final String HASH_ALGORITHM = "SHA-256";
    private static final String SALT = "RCore-VanillaStats-Privacy-Salt-v1";

    /**
     * Private constructor to prevent instantiation.
     */
    private UuidAnonymizer() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Anonymizes a player UUID using SHA-256 hashing.
     *
     * <p>The anonymization process:
     * <ol>
     *   <li>Converts UUID to string representation</li>
     *   <li>Appends a salt value for additional security</li>
     *   <li>Hashes the result using SHA-256</li>
     *   <li>Converts hash to a new UUID</li>
     * </ol>
     *
     * <p>The same input UUID will always produce the same anonymized UUID,
     * allowing for consistent tracking while protecting identity.
     *
     * @param uuid the UUID to anonymize
     * @return the anonymized UUID
     * @throws IllegalStateException if SHA-256 algorithm is not available
     */
    public static @NotNull UUID anonymizeUuid(final @NotNull UUID uuid) {
        try {
            // Create message digest
            final MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
            
            // Combine UUID with salt
            final String input = uuid.toString() + SALT;
            final byte[] inputBytes = input.getBytes(StandardCharsets.UTF_8);
            
            // Hash the input
            final byte[] hashBytes = digest.digest(inputBytes);
            
            // Convert hash to UUID
            // Use first 16 bytes of hash (SHA-256 produces 32 bytes)
            return bytesToUuid(hashBytes);
            
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Converts a byte array to a UUID.
     *
     * <p>Uses the first 16 bytes of the array to construct a UUID.
     * If the array is shorter than 16 bytes, it is padded with zeros.
     *
     * @param bytes the byte array (at least 16 bytes recommended)
     * @return the UUID constructed from the bytes
     */
    private static @NotNull UUID bytesToUuid(final byte[] bytes) {
        // Ensure we have at least 16 bytes
        final byte[] uuidBytes = new byte[16];
        System.arraycopy(bytes, 0, uuidBytes, 0, Math.min(bytes.length, 16));
        
        // Convert to long values for UUID constructor
        long mostSigBits = 0;
        long leastSigBits = 0;
        
        for (int i = 0; i < 8; i++) {
            mostSigBits = (mostSigBits << 8) | (uuidBytes[i] & 0xff);
        }
        
        for (int i = 8; i < 16; i++) {
            leastSigBits = (leastSigBits << 8) | (uuidBytes[i] & 0xff);
        }
        
        return new UUID(mostSigBits, leastSigBits);
    }

    /**
     * Checks if a UUID appears to be anonymized.
     *
     * <p>This is a heuristic check and cannot definitively determine if a UUID
     * was anonymized by this class. It checks if the UUID matches the pattern
     * of anonymized UUIDs (high entropy, no version/variant bits set).
     *
     * @param uuid the UUID to check
     * @return true if the UUID appears to be anonymized
     */
    public static boolean isAnonymized(final @NotNull UUID uuid) {
        // Anonymized UUIDs don't follow standard UUID version/variant patterns
        // This is a simple heuristic check
        final int version = uuid.version();
        final int variant = uuid.variant();
        
        // Standard UUIDs have version 1-5 and variant 2
        // Anonymized UUIDs will have random values
        return version < 1 || version > 5 || variant != 2;
    }
}
