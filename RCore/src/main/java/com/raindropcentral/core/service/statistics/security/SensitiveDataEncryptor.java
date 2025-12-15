package com.raindropcentral.core.service.statistics.security;

import com.raindropcentral.rplatform.logging.CentralLogger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.logging.Logger;

/**
 * Encrypts sensitive data fields using AES-256-GCM.
 * Used for protecting IP addresses, location data, and other PII.
 *
 * @author JExcellence
 * @since 1.0.0
 */
public class SensitiveDataEncryptor {

    private static final Logger LOGGER = CentralLogger.getLogger(SensitiveDataEncryptor.class);

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;
    private static final int AES_KEY_SIZE = 256;

    private final SecretKey secretKey;
    private final SecureRandom secureRandom;

    /**
     * Creates an encryptor with a derived key from the server UUID and API key.
     *
     * @param serverUuid the server UUID
     * @param apiKey     the API key
     */
    public SensitiveDataEncryptor(
        final @NotNull String serverUuid,
        final @NotNull String apiKey
    ) {
        this.secretKey = deriveKey(serverUuid, apiKey);
        this.secureRandom = new SecureRandom();
    }

    /**
     * Creates an encryptor with a provided secret key.
     *
     * @param secretKey the secret key to use
     */
    public SensitiveDataEncryptor(final @NotNull SecretKey secretKey) {
        this.secretKey = secretKey;
        this.secureRandom = new SecureRandom();
    }


    /**
     * Encrypts a string value using AES-256-GCM.
     *
     * @param value the value to encrypt
     * @return Base64-encoded encrypted value (IV + ciphertext), or null if encryption fails
     */
    public @Nullable String encrypt(final @NotNull String value) {
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);

            byte[] cipherText = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));

            // Prepend IV to ciphertext
            byte[] encryptedData = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, encryptedData, 0, iv.length);
            System.arraycopy(cipherText, 0, encryptedData, iv.length, cipherText.length);

            return Base64.getEncoder().encodeToString(encryptedData);

        } catch (Exception e) {
            LOGGER.warning("Encryption failed: " + e.getMessage());
            return null;
        }
    }

    /**
     * Decrypts a Base64-encoded encrypted value.
     *
     * @param encryptedValue the encrypted value
     * @return the decrypted string, or null if decryption fails
     */
    public @Nullable String decrypt(final @NotNull String encryptedValue) {
        try {
            byte[] encryptedData = Base64.getDecoder().decode(encryptedValue);

            // Extract IV and ciphertext
            byte[] iv = Arrays.copyOfRange(encryptedData, 0, GCM_IV_LENGTH);
            byte[] cipherText = Arrays.copyOfRange(encryptedData, GCM_IV_LENGTH, encryptedData.length);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);

            byte[] decryptedData = cipher.doFinal(cipherText);
            return new String(decryptedData, StandardCharsets.UTF_8);

        } catch (Exception e) {
            LOGGER.warning("Decryption failed: " + e.getMessage());
            return null;
        }
    }

    /**
     * Encrypts an IP address, masking the last octet for privacy.
     *
     * @param ipAddress the IP address to encrypt
     * @return encrypted IP with masked last octet
     */
    public @Nullable String encryptIpAddress(final @NotNull String ipAddress) {
        // Mask last octet for additional privacy
        String maskedIp = maskIpAddress(ipAddress);
        return encrypt(maskedIp);
    }

    /**
     * Masks the last octet of an IP address.
     *
     * @param ipAddress the IP address
     * @return masked IP address
     */
    public String maskIpAddress(final @NotNull String ipAddress) {
        int lastDot = ipAddress.lastIndexOf('.');
        if (lastDot > 0) {
            return ipAddress.substring(0, lastDot) + ".xxx";
        }
        return ipAddress;
    }

    /**
     * Checks if a value appears to be sensitive data that should be encrypted.
     *
     * @param key   the statistic key
     * @param value the value
     * @return true if the value should be encrypted
     */
    public boolean isSensitive(final @NotNull String key, final @Nullable Object value) {
        if (value == null) {
            return false;
        }

        String keyLower = key.toLowerCase();
        return keyLower.contains("ip") ||
               keyLower.contains("address") ||
               keyLower.contains("location") ||
               keyLower.contains("email") ||
               keyLower.contains("password") ||
               keyLower.contains("token");
    }

    /**
     * Derives an AES-256 key from server UUID and API key using SHA-256.
     */
    private SecretKey deriveKey(final String serverUuid, final String apiKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(serverUuid.getBytes(StandardCharsets.UTF_8));
            digest.update(apiKey.getBytes(StandardCharsets.UTF_8));
            byte[] keyBytes = digest.digest();

            return new SecretKeySpec(keyBytes, "AES");

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available for key derivation", e);
        }
    }

    /**
     * Generates a new random AES-256 key.
     *
     * @return a new secret key
     */
    public static SecretKey generateKey() {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(AES_KEY_SIZE);
            return keyGen.generateKey();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("AES key generation failed", e);
        }
    }
}
