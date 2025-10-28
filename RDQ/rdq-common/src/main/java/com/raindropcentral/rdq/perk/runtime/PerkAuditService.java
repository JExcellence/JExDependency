package com.raindropcentral.rdq.perk.runtime;

import com.raindropcentral.rplatform.logging.CentralLogger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * Emits structured audit entries for perk runtime actions without exposing player identifiers.
 *
 * <p>The service fingerprints player UUIDs using a deterministic hash to avoid leaking personal
 * data while still allowing correlation across audit events. Audit entries are emitted as
 * compact JSON records routed through {@link CentralLogger} so administrators can aggregate and
 * analyse perk behaviour across clustered environments.</p>
 *
 * @author JExcellence
 * @version 1.0.2
 * @since 3.2.0
 */
public final class PerkAuditService {

    private static final Logger AUDIT_LOGGER = CentralLogger.getLogger("com.raindropcentral.rdq.perk.audit");
    private static final int FINGERPRINT_LENGTH = 16;
    private static final int MAX_CONTEXT_ENTRIES = 16;
    private static final int MAX_KEY_LENGTH = 48;
    private static final int MAX_STRING_VALUE_LENGTH = 256;
    private static final int MAX_JSON_LENGTH = 4_096;

    private final ConcurrentMap<UUID, String> cachedFingerprints = new ConcurrentHashMap<>();

    /**
     * Records an activation outcome for the supplied perk and player.
     *
     * @param perkId   the perk identifier
     * @param playerId the player identifier
     * @param success  whether the activation completed successfully
     * @param detail   human-readable detail describing the outcome
     * @param context  optional structured context values to include in the record
     * @param cause    optional cause captured when activation failed due to an exception
     */
    public void recordActivation(
            @NotNull String perkId,
            @NotNull UUID playerId,
            boolean success,
            @NotNull String detail,
            @Nullable Map<String, Object> context,
            @Nullable Throwable cause
    ) {
        record("activation", perkId, playerId, success, detail, context, cause);
    }

    /**
     * Records a deactivation outcome for the supplied perk and player.
     *
     * @param perkId   the perk identifier
     * @param playerId the player identifier
     * @param success  whether the deactivation completed successfully
     * @param detail   human-readable detail describing the outcome
     * @param cause    optional cause captured when deactivation failed due to an exception
     */
    public void recordDeactivation(
            @NotNull String perkId,
            @NotNull UUID playerId,
            boolean success,
            @NotNull String detail,
            @Nullable Throwable cause
    ) {
        record("deactivation", perkId, playerId, success, detail, null, cause);
    }

    /**
     * Records a trigger outcome for the supplied perk and player.
     *
     * @param perkId   the perk identifier
     * @param playerId the player identifier
     * @param source   the trigger source, typically an event name
     * @param success  whether the trigger completed successfully
     * @param detail   human-readable detail describing the outcome
     * @param context  optional structured context values to include in the record
     * @param cause    optional cause captured when triggering failed due to an exception
     */
    public void recordTrigger(
            @NotNull String perkId,
            @NotNull UUID playerId,
            @NotNull String source,
            boolean success,
            @NotNull String detail,
            @Nullable Map<String, Object> context,
            @Nullable Throwable cause
    ) {
        final Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("source", source);
        if (context != null && !context.isEmpty()) {
            payload.putAll(context);
        }
        record("trigger", perkId, playerId, success, detail, payload, cause);
    }

    /**
     * Records the expiry of a perk activation for the supplied player.
     *
     * @param perkId   the perk identifier
     * @param playerId the player identifier
     * @param detail   human-readable detail describing the outcome
     */
    public void recordExpiry(@NotNull String perkId, @NotNull UUID playerId, @NotNull String detail) {
        record("expiry", perkId, playerId, true, detail, null, null);
    }

    /**
     * Records cleanup performed outside the normal deactivate flow, such as when a player
     * disconnects.
     *
     * @param perkId   the perk identifier
     * @param playerId the player identifier
     * @param detail   human-readable detail describing the outcome
     */
    public void recordCleanup(@NotNull String perkId, @NotNull UUID playerId, @NotNull String detail) {
        record("cleanup", perkId, playerId, true, detail, null, null);
    }

    /**
     * Records a suppression event when a player is temporarily blocked from
     * activating or triggering a perk due to repeated failures.
     *
     * @param perkId   the perk identifier
     * @param playerId the player identifier
     * @param detail   human-readable detail describing the suppression reason
     * @param context  optional structured context values related to the suppression
     */
    public void recordSuppression(
            @NotNull String perkId,
            @NotNull UUID playerId,
            @NotNull String detail,
            @Nullable Map<String, Object> context
    ) {
        record("suppression", perkId, playerId, true, detail, context, null);
    }

    /**
     * Provides a stable fingerprint for the supplied player identifier. The fingerprint can be
     * used in diagnostics without disclosing the actual UUID.
     *
     * @param playerId the player identifier to fingerprint
     * @return a stable fingerprint string
     */
    public @NotNull String fingerprint(@NotNull UUID playerId) {
        Objects.requireNonNull(playerId, "playerId");
        return cachedFingerprints.computeIfAbsent(playerId, this::computeFingerprint);
    }

    private void record(
            @NotNull String action,
            @NotNull String perkId,
            @NotNull UUID playerId,
            boolean success,
            @NotNull String detail,
            @Nullable Map<String, Object> context,
            @Nullable Throwable cause
    ) {
        Objects.requireNonNull(action, "action");
        Objects.requireNonNull(perkId, "perkId");
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(detail, "detail");

        final Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("timestamp", Instant.now().toString());
        payload.put("action", truncate(action, MAX_KEY_LENGTH));
        payload.put("perkId", truncate(perkId, MAX_KEY_LENGTH));
        payload.put("playerFingerprint", fingerprint(playerId));
        payload.put("success", success);
        payload.put("detail", truncate(detail, MAX_STRING_VALUE_LENGTH));
        final Map<String, Object> safeContext = sanitizeContext(context);
        if (!safeContext.isEmpty()) {
            payload.put("context", safeContext);
        }

        final Level level = success ? Level.INFO : Level.WARNING;
        final String json = toJson(payload);
        final String safeJson = json.length() > MAX_JSON_LENGTH
                ? json.substring(0, MAX_JSON_LENGTH)
                : json;
        final LogRecord record = new LogRecord(level, "PERK_AUDIT {0}");
        record.setLoggerName(AUDIT_LOGGER.getName());
        record.setParameters(new Object[]{safeJson});
        record.setThrown(cause);
        AUDIT_LOGGER.log(record);
    }

    private String computeFingerprint(UUID playerId) {
        try {
            final MessageDigest digest = MessageDigest.getInstance("SHA-256");
            final byte[] hashed = digest.digest(playerId.toString().getBytes(StandardCharsets.UTF_8));
            return toHex(hashed, FINGERPRINT_LENGTH);
        } catch (NoSuchAlgorithmException ignored) {
            final String raw = playerId.toString().replace("-", "");
            return raw.length() <= FINGERPRINT_LENGTH ? raw : raw.substring(0, FINGERPRINT_LENGTH);
        }
    }

    private Map<String, Object> sanitizeContext(@Nullable Map<String, Object> context) {
        if (context == null || context.isEmpty()) {
            return Map.of();
        }
        final Map<String, Object> sanitized = new LinkedHashMap<>();
        int count = 0;
        for (Map.Entry<String, Object> entry : context.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                continue;
            }
            if (count >= MAX_CONTEXT_ENTRIES) {
                break;
            }
            final String key = normalizeKey(entry.getKey());
            if (key.isEmpty()) {
                continue;
            }
            sanitized.put(key, normalizeValue(entry.getValue()));
            count++;
        }
        return Map.copyOf(sanitized);
    }

    private Object normalizeValue(Object value) {
        if (value instanceof Number || value instanceof Boolean) {
            return value;
        }
        if (value instanceof Map<?, ?> map) {
            final Map<String, Object> nested = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (entry.getKey() == null || entry.getValue() == null) {
                    continue;
                }
                final String key = normalizeKey(String.valueOf(entry.getKey()));
                if (key.isEmpty()) {
                    continue;
                }
                nested.put(key, normalizeValue(entry.getValue()));
                if (nested.size() >= MAX_CONTEXT_ENTRIES) {
                    break;
                }
            }
            return Map.copyOf(nested);
        }
        if (value instanceof Iterable<?> iterable) {
            final StringBuilder builder = new StringBuilder();
            builder.append('[');
            int processed = 0;
            for (Object element : iterable) {
                if (processed > 0) {
                    builder.append(',');
                }
                builder.append(truncate(String.valueOf(element), MAX_STRING_VALUE_LENGTH));
                if (++processed >= MAX_CONTEXT_ENTRIES) {
                    break;
                }
            }
            builder.append(']');
            return builder.toString();
        }
        return truncate(String.valueOf(value), MAX_STRING_VALUE_LENGTH);
    }

    private String normalizeKey(String rawKey) {
        final String trimmed = truncate(rawKey, MAX_KEY_LENGTH).trim();
        if (trimmed.isEmpty()) {
            return "";
        }
        final StringBuilder builder = new StringBuilder(trimmed.length());
        for (int i = 0; i < trimmed.length(); i++) {
            final char c = trimmed.charAt(i);
            if (Character.isLetterOrDigit(c) || c == '-' || c == '_' || c == '.') {
                builder.append(c);
            }
        }
        return builder.toString();
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, Math.max(0, maxLength));
    }

    private String toHex(byte[] data, int maxLength) {
        final StringBuilder builder = new StringBuilder();
        for (byte b : data) {
            if (builder.length() >= maxLength) {
                break;
            }
            final String hex = Integer.toHexString(b & 0xFF);
            if (hex.length() == 1) {
                builder.append('0');
            }
            builder.append(hex);
        }
        return builder.toString();
    }

    private String toJson(Map<String, Object> values) {
        final StringBuilder builder = new StringBuilder();
        builder.append('{');
        boolean first = true;
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            if (!first) {
                builder.append(',');
            }
            builder.append('"').append(escape(entry.getKey())).append('"').append(':');
            builder.append(format(entry.getValue()));
            first = false;
        }
        builder.append('}');
        return builder.toString();
    }

    private String format(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof Number || value instanceof Boolean) {
            return value.toString();
        }
        return '"' + escape(String.valueOf(value)) + '"';
    }

    private String escape(String input) {
        final StringBuilder builder = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            final char c = input.charAt(i);
            switch (c) {
                case '\\':
                    builder.append("\\\\");
                    break;
                case '"':
                    builder.append("\\\"");
                    break;
                case '\n':
                    builder.append("\\n");
                    break;
                case '\r':
                    builder.append("\\r");
                    break;
                case '\t':
                    builder.append("\\t");
                    break;
                default:
                    if (c < 0x20) {
                        builder.append(String.format("\\u%04x", (int) c));
                    } else {
                        builder.append(c);
                    }
                    break;
            }
        }
        return builder.toString();
    }
}

