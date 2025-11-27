package com.raindropcentral.rdq.perk.runtime;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class DefaultCooldownStore {

    private static final DefaultCooldownStore INSTANCE = new DefaultCooldownStore();
    private final Map<UUID, Map<String, Long>> store = new ConcurrentHashMap<>();

    private DefaultCooldownStore() {}

    public static DefaultCooldownStore getInstance() {
        return INSTANCE;
    }

    public void setCooldown(@NotNull Player player, @NotNull String perkId, long seconds) {
        if (seconds <= 0) {
            clearCooldown(player, perkId);
            return;
        }
        var expiry = System.currentTimeMillis() + (seconds * 1000L);
        store.computeIfAbsent(player.getUniqueId(), id -> new ConcurrentHashMap<>()).put(perkId, expiry);
    }

    public boolean isOnCooldown(@NotNull Player player, @NotNull String perkId) {
        var map = store.get(player.getUniqueId());
        if (map == null) return false;
        var expiry = map.get(perkId);
        if (expiry == null) return false;
        if (expiry <= System.currentTimeMillis()) {
            map.remove(perkId);
            return false;
        }
        return true;
    }

    public long getRemainingCooldown(@NotNull Player player, @NotNull String perkId) {
        var map = store.get(player.getUniqueId());
        if (map == null) return 0L;
        var expiry = map.get(perkId);
        if (expiry == null) return 0L;
        var remaining = (expiry - System.currentTimeMillis()) / 1000L;
        if (remaining <= 0) {
            map.remove(perkId);
            return 0L;
        }
        return remaining;
    }

    public void clearCooldown(@NotNull Player player, @NotNull String perkId) {
        var map = store.get(player.getUniqueId());
        if (map != null) {
            map.remove(perkId);
            if (map.isEmpty()) store.remove(player.getUniqueId());
        }
    }

    public void clearCooldown(@NotNull UUID playerId, @NotNull String perkId) {
        var map = store.get(playerId);
        if (map != null) {
            map.remove(perkId);
            if (map.isEmpty()) store.remove(playerId);
        }
    }

    public void clearAllCooldowns(@NotNull UUID playerId) {
        store.remove(playerId);
    }

    public void clearExpired() {
        var now = System.currentTimeMillis();
        store.entrySet().forEach(entry -> {
            var inner = entry.getValue();
            inner.entrySet().removeIf(e -> e.getValue() <= now);
            if (inner.isEmpty()) store.remove(entry.getKey());
        });
    }
}
