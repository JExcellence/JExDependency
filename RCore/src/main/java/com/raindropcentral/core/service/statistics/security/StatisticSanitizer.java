package com.raindropcentral.core.service.statistics.security;

import com.raindropcentral.rplatform.logging.CentralLogger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Sanitizes statistic keys and values to prevent injection attacks.
 * Validates keys against allowed patterns and removes dangerous content from values.
 *
 * @author JExcellence
 * @since 1.0.0
 */
public class StatisticSanitizer {

    private static final Logger LOGGER = CentralLogger.getLogger(StatisticSanitizer.class);

    /** Pattern for valid statistic keys: alphanumeric, underscores, dots, hyphens */
    private static final Pattern VALID_KEY_PATTERN = Pattern.compile("^[a-zA-Z0-9_.\\-:]+$");

    /** Maximum key length */
    private static final int MAX_KEY_LENGTH = 128;

    /** Maximum string value length */
    private static final int MAX_VALUE_LENGTH = 4096;

    /** Dangerous patterns to remove from string values */
    private static final Set<String> DANGEROUS_PATTERNS = Set.of(
        "<script", "</script>", "javascript:", "data:", "vbscript:",
        "onclick", "onerror", "onload", "onmouseover",
        "expression(", "eval(", "document.", "window."
    );

    /** SQL injection patterns */
    private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile(
        "(?i)(\\b(SELECT|INSERT|UPDATE|DELETE|DROP|UNION|ALTER|CREATE|TRUNCATE)\\b.*\\b(FROM|INTO|TABLE|WHERE)\\b)"
    );

    public StatisticSanitizer() {
    }

    /**
     * Validates a statistic key against allowed patterns.
     *
     * @param key the key to validate
     * @return true if the key is valid
     */
    public boolean isValidKey(final @NotNull String key) {
        if (key.isEmpty() || key.length() > MAX_KEY_LENGTH) {
            return false;
        }
        return VALID_KEY_PATTERN.matcher(key).matches();
    }

    /**
     * Sanitizes a statistic key, returning null if invalid.
     *
     * @param key the key to sanitize
     * @return sanitized key or null if invalid
     */
    public @Nullable String sanitizeKey(final @NotNull String key) {
        if (!isValidKey(key)) {
            LOGGER.warning("Invalid statistic key rejected: " + truncate(key, 50));
            return null;
        }
        return key;
    }

    /**
     * Sanitizes a statistic value, removing potentially dangerous content.
     *
     * @param value the value to sanitize
     * @return sanitized value
     */
    public @Nullable Object sanitize(final @Nullable Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof String stringValue) {
            return sanitizeString(stringValue);
        }

        if (value instanceof Number || value instanceof Boolean) {
            return value;
        }

        // For other types, convert to string and sanitize
        return sanitizeString(value.toString());
    }

    /**
     * Sanitizes a string value.
     *
     * @param value the string to sanitize
     * @return sanitized string
     */
    public @NotNull String sanitizeString(final @NotNull String value) {
        String sanitized = value;

        // Truncate if too long
        if (sanitized.length() > MAX_VALUE_LENGTH) {
            sanitized = sanitized.substring(0, MAX_VALUE_LENGTH);
            LOGGER.fine("Truncated oversized value");
        }

        // Remove dangerous HTML/JS patterns
        for (String pattern : DANGEROUS_PATTERNS) {
            if (sanitized.toLowerCase().contains(pattern.toLowerCase())) {
                sanitized = sanitized.replaceAll("(?i)" + Pattern.quote(pattern), "");
                LOGGER.fine("Removed dangerous pattern from value");
            }
        }

        // Check for SQL injection patterns
        if (SQL_INJECTION_PATTERN.matcher(sanitized).find()) {
            LOGGER.warning("Potential SQL injection detected and sanitized");
            sanitized = SQL_INJECTION_PATTERN.matcher(sanitized).replaceAll("");
        }

        // Escape special characters
        sanitized = escapeSpecialChars(sanitized);

        return sanitized;
    }

    /**
     * Escapes special characters that could be used for injection.
     */
    private String escapeSpecialChars(final String value) {
        return value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("'", "\\'")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
            .replace("\0", "");
    }

    /**
     * Validates that a value is safe for storage/transmission.
     *
     * @param key   the statistic key
     * @param value the value
     * @return true if the value is safe
     */
    public boolean isSafe(final @NotNull String key, final @Nullable Object value) {
        if (!isValidKey(key)) {
            return false;
        }

        if (value == null) {
            return true;
        }

        if (value instanceof String stringValue) {
            return !containsDangerousContent(stringValue);
        }

        return true;
    }

    /**
     * Checks if a string contains dangerous content.
     */
    private boolean containsDangerousContent(final String value) {
        String lower = value.toLowerCase();

        for (String pattern : DANGEROUS_PATTERNS) {
            if (lower.contains(pattern.toLowerCase())) {
                return true;
            }
        }

        return SQL_INJECTION_PATTERN.matcher(value).find();
    }

    /**
     * Truncates a string for logging purposes.
     */
    private String truncate(final String value, final int maxLength) {
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength) + "...";
    }

    /**
     * Result of a sanitization operation.
     */
    public record SanitizationResult(
        boolean valid,
        @Nullable String sanitizedKey,
        @Nullable Object sanitizedValue,
        @Nullable String warning
    ) {
        public static SanitizationResult valid(String key, Object value) {
            return new SanitizationResult(true, key, value, null);
        }

        public static SanitizationResult invalid(String warning) {
            return new SanitizationResult(false, null, null, warning);
        }
    }

    /**
     * Performs full sanitization of a key-value pair.
     *
     * @param key   the statistic key
     * @param value the value
     * @return sanitization result
     */
    public SanitizationResult sanitizePair(final @NotNull String key, final @Nullable Object value) {
        String sanitizedKey = sanitizeKey(key);
        if (sanitizedKey == null) {
            return SanitizationResult.invalid("Invalid key format: " + truncate(key, 50));
        }

        Object sanitizedValue = sanitize(value);
        return SanitizationResult.valid(sanitizedKey, sanitizedValue);
    }
}
