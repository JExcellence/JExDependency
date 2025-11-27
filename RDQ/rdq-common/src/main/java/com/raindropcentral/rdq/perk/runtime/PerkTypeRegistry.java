package com.raindropcentral.rdq.perk.runtime;

import com.raindropcentral.rdq.perk.PerkType;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public final class PerkTypeRegistry {

    private static final Map<String, PerkTypeDefinition> DEFINITIONS = new ConcurrentHashMap<>();

    static {
        registerDefaults();
    }

    private PerkTypeRegistry() {
    }

    public static void register(@NotNull String id, @NotNull PerkTypeDefinition definition) {
        DEFINITIONS.put(id.toLowerCase(), definition);
    }

    public static void unregister(@NotNull String id) {
        DEFINITIONS.remove(id.toLowerCase());
    }

    @NotNull
    public static Optional<PerkTypeDefinition> get(@NotNull String id) {
        return Optional.ofNullable(DEFINITIONS.get(id.toLowerCase()));
    }

    @NotNull
    public static List<String> getRegisteredTypes() {
        return List.copyOf(DEFINITIONS.keySet());
    }

    public static boolean exists(@NotNull String id) {
        return DEFINITIONS.containsKey(id.toLowerCase());
    }

    @NotNull
    public static PerkType parse(@NotNull String typeString, @NotNull Map<String, Object> config) {
        var parts = typeString.split(":", 2);
        var typeId = parts[0].toLowerCase();

        return switch (typeId) {
            case "toggleable" -> new PerkType.Toggleable();
            case "event", "event_based" -> {
                var eventType = parts.length > 1 ? parts[1] : config.getOrDefault("eventType", "").toString();
                yield new PerkType.EventBased(eventType);
            }
            case "passive" -> new PerkType.Passive();
            default -> new PerkType.Toggleable();
        };
    }

    private static void registerDefaults() {
        register("toggleable", new PerkTypeDefinition(
            "toggleable",
            "Toggleable Perk",
            "A perk that can be turned on and off",
            config -> new PerkType.Toggleable()
        ));

        register("event_based", new PerkTypeDefinition(
            "event_based",
            "Event-Based Perk",
            "A perk that triggers on specific events",
            config -> {
                var eventType = config.getOrDefault("eventType", "").toString();
                return new PerkType.EventBased(eventType);
            }
        ));

        register("passive", new PerkTypeDefinition(
            "passive",
            "Passive Perk",
            "A perk that is always active when unlocked",
            config -> new PerkType.Passive()
        ));
    }

    public record PerkTypeDefinition(
        @NotNull String id,
        @NotNull String displayName,
        @NotNull String description,
        @NotNull Function<Map<String, Object>, PerkType> factory
    ) {
        @NotNull
        public PerkType create(@NotNull Map<String, Object> config) {
            return factory.apply(config);
        }
    }
}
