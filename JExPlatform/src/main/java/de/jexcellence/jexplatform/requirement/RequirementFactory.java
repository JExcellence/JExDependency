package de.jexcellence.jexplatform.requirement;

import de.jexcellence.jexplatform.requirement.impl.*;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Factory for creating requirement instances from map-based configurations.
 *
 * @author JExcellence
 * @since 1.0.0
 */
public final class RequirementFactory {

    private RequirementFactory() {
    }

    /**
     * Creates a requirement from a configuration map.
     *
     * @param config the configuration containing at minimum a {@code "type"} key
     * @return the constructed requirement
     * @throws IllegalArgumentException if the type is unknown or configuration is invalid
     */
    @SuppressWarnings("unchecked")
    public static @NotNull AbstractRequirement fromMap(@NotNull Map<String, Object> config) {
        var type = String.valueOf(config.getOrDefault("type", "")).toUpperCase();

        return switch (type) {
            case "ITEM" -> new ItemRequirement(
                    getString(config, "material", "STONE"),
                    getInt(config, "amount", 1),
                    getBool(config, "exactMatch", false),
                    getBool(config, "consumeOnComplete", false));

            case "CURRENCY" -> new CurrencyRequirement(
                    getDouble(config, "amount", 0.0),
                    getString(config, "currency", null),
                    getBool(config, "consumeOnComplete", false));

            case "EXPERIENCE_LEVEL" -> new ExperienceLevelRequirement(
                    getInt(config, "level", 1),
                    getBool(config, "consumeOnComplete", false));

            case "PERMISSION" -> new PermissionRequirement(
                    getStringList(config, "permissions"),
                    parseEnum(config, "mode",
                            PermissionRequirement.PermissionMode.class,
                            PermissionRequirement.PermissionMode.ALL),
                    getBool(config, "negated", false));

            case "LOCATION" -> new LocationRequirement(
                    getString(config, "world", null),
                    getDouble(config, "x", 0.0),
                    getDouble(config, "y", 0.0),
                    getDouble(config, "z", 0.0),
                    getDouble(config, "radius", 10.0));

            case "PLAYTIME" -> new PlaytimeRequirement(
                    getLong(config, "seconds", 0L));

            default -> throw new IllegalArgumentException(
                    "Unknown requirement type: " + type);
        };
    }

    /**
     * Attempts to create a requirement from a configuration map.
     *
     * @param config the configuration
     * @return the requirement, or empty if creation fails
     */
    public static @NotNull Optional<AbstractRequirement> tryFromMap(
            @NotNull Map<String, Object> config) {
        try {
            return Optional.of(fromMap(config));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static String getString(Map<String, Object> m, String key, String def) {
        var v = m.get(key);
        return v != null ? v.toString() : def;
    }

    private static int getInt(Map<String, Object> m, String key, int def) {
        var v = m.get(key);
        if (v instanceof Number n) return n.intValue();
        if (v instanceof String s) {
            try { return Integer.parseInt(s); } catch (NumberFormatException ignored) { }
        }
        return def;
    }

    private static long getLong(Map<String, Object> m, String key, long def) {
        var v = m.get(key);
        if (v instanceof Number n) return n.longValue();
        if (v instanceof String s) {
            try { return Long.parseLong(s); } catch (NumberFormatException ignored) { }
        }
        return def;
    }

    private static double getDouble(Map<String, Object> m, String key, double def) {
        var v = m.get(key);
        if (v instanceof Number n) return n.doubleValue();
        if (v instanceof String s) {
            try { return Double.parseDouble(s); } catch (NumberFormatException ignored) { }
        }
        return def;
    }

    private static boolean getBool(Map<String, Object> m, String key, boolean def) {
        var v = m.get(key);
        if (v instanceof Boolean b) return b;
        if (v instanceof String s) return Boolean.parseBoolean(s);
        return def;
    }

    @SuppressWarnings("unchecked")
    private static List<String> getStringList(Map<String, Object> m, String key) {
        var v = m.get(key);
        if (v instanceof List<?> list) {
            return list.stream().map(Object::toString).toList();
        }
        return List.of();
    }

    private static <E extends Enum<E>> E parseEnum(Map<String, Object> m,
                                                    String key, Class<E> cls, E def) {
        var v = m.get(key);
        if (v == null) return def;
        try {
            return Enum.valueOf(cls, v.toString().toUpperCase());
        } catch (IllegalArgumentException e) {
            return def;
        }
    }
}
