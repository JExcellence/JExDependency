package com.raindropcentral.rdq2.perk.runtime;

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


public final class PerkAuditService {

    private static final Logger AUDIT_LOGGER = CentralLogger.getLogger("com.raindropcentral.rdq.perk.audit");
    private static final int FINGERPRINT_LENGTH = 16;
    private static final int MAX_CONTEXT_ENTRIES = 16;
    private static final int MAX_KEY_LENGTH = 48;
    private static final int MAX_STRING_VALUE_LENGTH = 256;
    private static final int MAX_JSON_LENGTH = 4_096;

    private final ConcurrentMap<UUID, String> cachedFingerprints = new ConcurrentHashMap<>();

    public void recordActivation(@NotNull String perkId, @NotNull UUID playerId, boolean success, @NotNull String detail, @Nullable Map<String, Object> context, @Nullable Throwable cause) {
        record("activation", perkId, playerId, success, detail, context, cause);
    }

    public void recordDeactivation(@NotNull String perkId, @NotNull UUID playerId, boolean success, @NotNull String detail, @Nullable Throwable cause) {
        record("deactivation", perkId, playerId, success, detail, null, cause);
    }

    public void recordTrigger(@NotNull String perkId, @NotNull UUID playerId, @NotNull String source, boolean success, @NotNull String detail, @Nullable Map<String, Object> context, @Nullable Throwable cause) {
        var payload = new LinkedHashMap<String, Object>();
        payload.put("source", source);
        if (context != null && !context.isEmpty()) {
            payload.putAll(context);
        }
        record("trigger", perkId, playerId, success, detail, payload, cause);
    }

    public void recordExpiry(@NotNull String perkId, @NotNull UUID playerId, @NotNull String detail) {
        record("expiry", perkId, playerId, true, detail, null, null);
    }

    public void recordCleanup(@NotNull String perkId, @NotNull UUID playerId, @NotNull String detail) {
        record("cleanup", perkId, playerId, true, detail, null, null);
    }

    public void recordSuppression(@NotNull String perkId, @NotNull UUID playerId, @NotNull String detail, @Nullable Map<String, Object> context) {
        record("suppression", perkId, playerId, true, detail, context, null);
    }

    public @NotNull String fingerprint(@NotNull UUID playerId) {
        Objects.requireNonNull(playerId);
        return cachedFingerprints.computeIfAbsent(playerId, this::computeFingerprint);
    }

    private void record(@NotNull String action, @NotNull String perkId, @NotNull UUID playerId, boolean success, @NotNull String detail, @Nullable Map<String, Object> context, @Nullable Throwable cause) {
        Objects.requireNonNull(action);
        Objects.requireNonNull(perkId);
        Objects.requireNonNull(playerId);
        Objects.requireNonNull(detail);

        var payload = new LinkedHashMap<String, Object>();
        payload.put("timestamp", Instant.now().toString());
        payload.put("action", truncate(action, MAX_KEY_LENGTH));
        payload.put("perkId", truncate(perkId, MAX_KEY_LENGTH));
        payload.put("playerFingerprint", fingerprint(playerId));
        payload.put("success", success);
        payload.put("detail", truncate(detail, MAX_STRING_VALUE_LENGTH));
        
        var safeContext = sanitizeContext(context);
        if (!safeContext.isEmpty()) {
            payload.put("context", safeContext);
        }

        var level = success ? Level.INFO : Level.WARNING;
        var json = toJson(payload);
        var safeJson = json.length() > MAX_JSON_LENGTH ? json.substring(0, MAX_JSON_LENGTH) : json;
        
        var record = new LogRecord(level, "PERK_AUDIT {0}");
        record.setLoggerName(AUDIT_LOGGER.getName());
        record.setParameters(new Object[]{safeJson});
        record.setThrown(cause);
        AUDIT_LOGGER.log(record);
    }

    private String computeFingerprint(UUID playerId) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            var hashed = digest.digest(playerId.toString().getBytes(StandardCharsets.UTF_8));
            return toHex(hashed, FINGERPRINT_LENGTH);
        } catch (NoSuchAlgorithmException ignored) {
            var raw = playerId.toString().replace("-", "");
            return raw.length() <= FINGERPRINT_LENGTH ? raw : raw.substring(0, FINGERPRINT_LENGTH);
        }
    }

    private Map<String, Object> sanitizeContext(@Nullable Map<String, Object> context) {
        if (context == null || context.isEmpty()) return Map.of();
        
        var sanitized = new LinkedHashMap<String, Object>();
        var count = 0;
        
        for (var entry : context.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) continue;
            if (count >= MAX_CONTEXT_ENTRIES) break;
            
            var key = normalizeKey(entry.getKey());
            if (key.isEmpty()) continue;
            
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

