package com.raindropcentral.rdq.perk.runtime;

import com.raindropcentral.rdq.perk.Perk;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public final class PerkRegistry {

    private static final Logger LOGGER = Logger.getLogger(PerkRegistry.class.getName());

    private final Plugin plugin;
    private final Map<String, PerkRuntime> runtimes = new ConcurrentHashMap<>();

    public PerkRegistry(@NotNull Plugin plugin) {
        this.plugin = plugin;
    }

    public void register(@NotNull Perk perk) {
        var runtime = new PerkRuntime(perk, plugin);
        runtimes.put(perk.id(), runtime);
        LOGGER.fine(() -> "Registered perk runtime: " + perk.id());
    }

    public void registerAll(@NotNull List<Perk> perks) {
        perks.forEach(this::register);
    }

    public void unregister(@NotNull String perkId) {
        var runtime = runtimes.remove(perkId);
        if (runtime != null) {
            runtime.getActiveUsers().forEach(runtime::cleanup);
            LOGGER.fine(() -> "Unregistered perk runtime: " + perkId);
        }
    }

    @NotNull
    public Optional<PerkRuntime> get(@NotNull String perkId) {
        return Optional.ofNullable(runtimes.get(perkId));
    }

    @NotNull
    public List<PerkRuntime> getAll() {
        return List.copyOf(runtimes.values());
    }

    @NotNull
    public List<PerkRuntime> getActiveForPlayer(@NotNull UUID playerId) {
        return runtimes.values().stream()
            .filter(r -> r.isActive(playerId))
            .toList();
    }

    public boolean isActive(@NotNull UUID playerId, @NotNull String perkId) {
        var runtime = runtimes.get(perkId);
        return runtime != null && runtime.isActive(playerId);
    }

    public void deactivateAllForPlayer(@NotNull Player player) {
        var playerId = player.getUniqueId();
        runtimes.values().stream()
            .filter(r -> r.isActive(playerId))
            .forEach(r -> r.deactivate(player));
    }

    public void cleanupPlayer(@NotNull UUID playerId) {
        runtimes.values().forEach(r -> r.cleanup(playerId));
    }

    public void clear() {
        runtimes.values().forEach(r -> r.getActiveUsers().forEach(r::cleanup));
        runtimes.clear();
    }

    public void reload(@NotNull List<Perk> perks) {
        clear();
        registerAll(perks);
    }

    public int count() {
        return runtimes.size();
    }

    public boolean exists(@NotNull String perkId) {
        return runtimes.containsKey(perkId);
    }
}
