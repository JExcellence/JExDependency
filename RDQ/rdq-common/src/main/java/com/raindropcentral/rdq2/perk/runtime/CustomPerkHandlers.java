package com.raindropcentral.rdq2.perk.runtime;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class CustomPerkHandlers {

    private static final Map<String, CustomPerkHandler> HANDLERS = new ConcurrentHashMap<>();

    private CustomPerkHandlers() {
    }

    public static void register(@NotNull String id, @NotNull CustomPerkHandler handler) {
        HANDLERS.put(id.toLowerCase(), handler);
    }

    public static void unregister(@NotNull String id) {
        HANDLERS.remove(id.toLowerCase());
    }

    @NotNull
    public static Optional<CustomPerkHandler> get(@NotNull String id) {
        return Optional.ofNullable(HANDLERS.get(id.toLowerCase()));
    }

    public static boolean exists(@NotNull String id) {
        return HANDLERS.containsKey(id.toLowerCase());
    }

    public static void clear() {
        HANDLERS.clear();
    }

    public interface CustomPerkHandler {
        void apply(@NotNull Player player, @NotNull Map<String, Object> config);
        void remove(@NotNull Player player, @NotNull Map<String, Object> config);
    }
}
