package com.raindropcentral.rdq.perk.runtime;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Normalises perk metadata derived from configuration sections before it is consumed by
 * runtime services.
 *
 * <p>The original implementation embedded sanitisation logic directly within the perk registry,
 * making the code difficult to test and reuse. Extracting the behaviour into this utility keeps
 * the registry focused on orchestration while providing a single, well-documented location for
 * bounds checks and input trimming.</p>
 *
 * <p>All methods are thread-safe and avoid mutating the supplied inputs. Where results are cached
 * internally the caches are bounded and safe for concurrent access.</p>
 *
 * @author JExcellence
 * @since 3.2.0
 * @version 1.0.0
 */
public final class PerkMetadataSanitizer {

    private static final int DEFAULT_TEXT_LIMIT = 256;
    private static final int MATERIAL_LIMIT = 64;
    private static final int PERMISSION_KEY_LIMIT = 128;

    private static final Map<String, String> MATERIAL_CACHE = new ConcurrentHashMap<>();

    private PerkMetadataSanitizer() {
    }

    /**
     * Produces a sanitized copy of the supplied metadata map, trimming keys, clamping string
     * lengths, and stripping ISO control characters.
     *
     * @param metadata the raw metadata map
     * @return an immutable sanitized view of the metadata
     */
    public static @NotNull Map<String, Object> sanitizeMetadata(@NotNull Map<String, Object> metadata) {
        final Map<String, Object> sanitized = new LinkedHashMap<>();
        metadata.forEach((key, value) -> {
            if (key == null || value == null) {
                return;
            }
            final String normalizedKey = key.trim();
            if (normalizedKey.isEmpty()) {
                return;
            }
            if (value instanceof Number || value instanceof Boolean) {
                sanitized.put(normalizedKey, value);
            } else {
                sanitized.put(normalizedKey, sanitizeText(String.valueOf(value)));
            }
        });
        return Map.copyOf(sanitized);
    }

    /**
     * Sanitizes a material identifier, constraining the value to an uppercase alphanumeric token
     * recognised by the Bukkit API.
     *
     * @param material the raw material identifier
     * @return the sanitized identifier, defaulting to {@code STONE}
     */
    public static @NotNull String sanitizeMaterial(@Nullable String material) {
        if (material == null) {
            return "STONE";
        }
        return MATERIAL_CACHE.computeIfAbsent(material, key -> {
            final String sanitized = sanitizeText(key, MATERIAL_LIMIT)
                    .toUpperCase(Locale.ROOT)
                    .replaceAll("[^A-Z0-9_]+", "");
            return sanitized.isEmpty() ? "STONE" : sanitized;
        });
    }

    /**
     * Normalises permission keyed cooldowns, removing negative or empty entries.
     *
     * @param source the raw cooldown map
     * @return a sanitized immutable map
     */
    public static @NotNull Map<String, Long> sanitizeCooldownMap(@NotNull Map<String, Long> source) {
        final Map<String, Long> sanitized = new LinkedHashMap<>();
        source.forEach((key, value) -> {
            if (value == null || value < 0) {
                return;
            }
            final String sanitizedKey = sanitizePermissionKey(key);
            if (sanitizedKey.isEmpty()) {
                return;
            }
            sanitized.merge(sanitizedKey, value, Math::min);
        });
        return Map.copyOf(sanitized);
    }

    /**
     * Normalises permission keyed durations, ignoring negative values and blank keys.
     *
     * @param source the raw duration map
     * @return a sanitized immutable map
     */
    public static @NotNull Map<String, Long> sanitizeDurationMap(@NotNull Map<String, Long> source) {
        final Map<String, Long> sanitized = new LinkedHashMap<>();
        source.forEach((key, value) -> {
            if (value == null || value < 0) {
                return;
            }
            final String sanitizedKey = sanitizePermissionKey(key);
            if (sanitizedKey.isEmpty()) {
                return;
            }
            sanitized.merge(sanitizedKey, value, Math::max);
        });
        return Map.copyOf(sanitized);
    }

    /**
     * Normalises permission keyed amplifiers, coercing negative values to zero.
     *
     * @param source the raw amplifier map
     * @return a sanitized immutable map
     */
    public static @NotNull Map<String, Integer> sanitizeAmplifierMap(@NotNull Map<String, Integer> source) {
        final Map<String, Integer> sanitized = new LinkedHashMap<>();
        source.forEach((key, value) -> {
            if (value == null) {
                return;
            }
            final String sanitizedKey = sanitizePermissionKey(key);
            if (sanitizedKey.isEmpty()) {
                return;
            }
            sanitized.merge(sanitizedKey, Math.max(0, value), Math::max);
        });
        return Map.copyOf(sanitized);
    }

    /**
     * Extracts and normalises event names defined within metadata, allowing runtimes to filter
     * trigger invocations precisely.
     *
     * @param metadata the sanitized metadata
     * @return an immutable set of supported event identifiers
     */
    public static @NotNull Set<String> determineSupportedEvents(@NotNull Map<String, Object> metadata) {
        final Set<String> events = new java.util.HashSet<>();
        Optional.ofNullable(metadata.get("triggerEvent"))
                .map(Object::toString)
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(PerkMetadataSanitizer::normaliseEventKey)
                .filter(value -> !value.isEmpty())
                .ifPresent(events::add);
        Optional.ofNullable(metadata.get("triggerEvents"))
                .filter(Collection.class::isInstance)
                .map(Collection.class::cast)
                .ifPresent(collection -> collection.forEach(item -> {
                    final String normalised = normaliseEventKey(String.valueOf(item));
                    if (!normalised.isEmpty()) {
                        events.add(normalised);
                    }
                }));
        return Set.copyOf(events);
    }

    /**
     * Normalises an arbitrary event key to an uppercase alphanumeric token without the trailing
     * {@code Event} suffix.
     *
     * @param raw the raw event key
     * @return the normalised key or an empty string when no valid token remains
     */
    public static @NotNull String normaliseEventKey(@Nullable String raw) {
        final String trimmed = Optional.ofNullable(raw)
                .map(String::trim)
                .orElse("");
        if (trimmed.isEmpty()) {
            return "";
        }
        String upper = trimmed.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
        if (upper.endsWith("EVENT")) {
            upper = upper.substring(0, upper.length() - 5);
        }
        return upper;
    }

    /**
     * Sanitizes a permission key, limiting the character set and maximum length.
     *
     * @param key the raw permission key
     * @return the sanitized key or an empty string when no valid characters remain
     */
    public static @NotNull String sanitizePermissionKey(@Nullable String key) {
        if (key == null) {
            return "";
        }
        final String trimmed = key.trim();
        if (trimmed.isEmpty()) {
            return "";
        }
        final StringBuilder builder = new StringBuilder(trimmed.length());
        for (int i = 0; i < trimmed.length() && builder.length() < PERMISSION_KEY_LIMIT; i++) {
            final char c = trimmed.charAt(i);
            if (Character.isLetterOrDigit(c) || c == '.' || c == '-' || c == '_' || c == '*') {
                builder.append(Character.toLowerCase(c));
            }
        }
        return builder.toString();
    }

    /**
     * Sanitizes arbitrary text, removing ISO control characters and constraining the length to a
     * sensible default.
     *
     * @param value the text to sanitize
     * @return the sanitized text
     */
    public static @NotNull String sanitizeText(@Nullable String value) {
        return sanitizeText(value, DEFAULT_TEXT_LIMIT);
    }

    /**
     * Sanitizes arbitrary text, removing ISO control characters and constraining the length.
     *
     * @param value the text to sanitize
     * @param maxLength the maximum length of the result
     * @return the sanitized text
     */
    public static @NotNull String sanitizeText(@Nullable String value, int maxLength) {
        if (value == null) {
            return "";
        }
        final StringBuilder builder = new StringBuilder();
        final int limit = Math.min(value.length(), Math.max(0, maxLength));
        for (int i = 0; i < limit; i++) {
            final char c = value.charAt(i);
            if (Character.isISOControl(c)) {
                continue;
            }
            builder.append(c);
        }
        return builder.toString().trim();
    }
}
