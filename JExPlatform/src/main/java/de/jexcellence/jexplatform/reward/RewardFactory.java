package de.jexcellence.jexplatform.reward;

import de.jexcellence.jexplatform.reward.impl.*;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Factory for creating reward instances from map-based configurations.
 *
 * @author JExcellence
 * @since 1.0.0
 */
public final class RewardFactory {

    private RewardFactory() {
    }

    /**
     * Creates a reward from a configuration map.
     *
     * @param config the configuration containing at minimum a {@code "type"} key
     * @return the constructed reward
     * @throws IllegalArgumentException if the type is unknown or configuration is invalid
     */
    @SuppressWarnings("unchecked")
    public static @NotNull AbstractReward fromMap(@NotNull Map<String, Object> config) {
        var type = String.valueOf(config.getOrDefault("type", "")).toUpperCase();

        return switch (type) {
            case "ITEM" -> new ItemReward(
                    getString(config, "material", "STONE"),
                    getInt(config, "amount", 1));

            case "CURRENCY" -> new CurrencyReward(
                    getDouble(config, "amount", 0.0),
                    getString(config, "currency", null));

            case "EXPERIENCE" -> new ExperienceReward(
                    getInt(config, "amount", 0),
                    parseEnum(config, "mode",
                            ExperienceReward.ExperienceMode.class,
                            ExperienceReward.ExperienceMode.POINTS));

            case "COMMAND" -> new CommandReward(
                    getString(config, "command", ""));

            case "PERMISSION" -> new PermissionReward(
                    getString(config, "permission", ""),
                    getLong(config, "duration", 0L));

            case "SOUND" -> new SoundReward(
                    getString(config, "sound", "ENTITY_PLAYER_LEVELUP"),
                    getFloat(config, "volume", 1.0f),
                    getFloat(config, "pitch", 1.0f));

            case "PARTICLE" -> new ParticleReward(
                    getString(config, "particle", "HEART"),
                    getInt(config, "count", 10),
                    getDouble(config, "offsetX", 0.5),
                    getDouble(config, "offsetY", 0.5),
                    getDouble(config, "offsetZ", 0.5));

            case "TELEPORT" -> new TeleportReward(
                    getString(config, "world", null),
                    getDouble(config, "x", 0.0),
                    getDouble(config, "y", 64.0),
                    getDouble(config, "z", 0.0),
                    getFloat(config, "yaw", 0.0f),
                    getFloat(config, "pitch", 0.0f));

            case "TITLE" -> new TitleReward(
                    getString(config, "title", ""),
                    getString(config, "subtitle", ""),
                    getInt(config, "fadeIn", 10),
                    getInt(config, "stay", 70),
                    getInt(config, "fadeOut", 20));

            case "VANISHING_CHEST" -> new VanishingChestReward(
                    getString(config, "title", "Reward Chest"),
                    getStringList(config, "items"),
                    getBool(config, "dropOnVanish", true),
                    getInt(config, "vanishDelayTicks", 600));

            default -> throw new IllegalArgumentException(
                    "Unknown reward type: " + type);
        };
    }

    /**
     * Attempts to create a reward from a configuration map.
     *
     * @param config the configuration
     * @return the reward, or empty if creation fails
     */
    public static @NotNull Optional<AbstractReward> tryFromMap(
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
            try {
                return Integer.parseInt(s);
            } catch (NumberFormatException ignored) { }
        }
        return def;
    }

    private static long getLong(Map<String, Object> m, String key, long def) {
        var v = m.get(key);
        if (v instanceof Number n) return n.longValue();
        if (v instanceof String s) {
            try {
                return Long.parseLong(s);
            } catch (NumberFormatException ignored) { }
        }
        return def;
    }

    private static float getFloat(Map<String, Object> m, String key, float def) {
        var v = m.get(key);
        if (v instanceof Number n) return n.floatValue();
        if (v instanceof String s) {
            try {
                return Float.parseFloat(s);
            } catch (NumberFormatException ignored) { }
        }
        return def;
    }

    private static double getDouble(Map<String, Object> m, String key, double def) {
        var v = m.get(key);
        if (v instanceof Number n) return n.doubleValue();
        if (v instanceof String s) {
            try {
                return Double.parseDouble(s);
            } catch (NumberFormatException ignored) { }
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
