package com.raindropcentral.rdq.perk.runtime;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CooldownService {

    private final Map<String, Long> cooldowns;

    public CooldownService() {
        this.cooldowns = new ConcurrentHashMap<>();
    }

    public void setCooldown(@NotNull Player player, @NotNull String perkId, long durationSeconds) {
        String key = getCooldownKey(player, perkId);
        long expiryTime = System.currentTimeMillis() + (durationSeconds * 1000);
        cooldowns.put(key, expiryTime);
    }

    public boolean isOnCooldown(@NotNull Player player, @NotNull String perkId) {
        String key = getCooldownKey(player, perkId);
        Long expiryTime = cooldowns.get(key);
        if (expiryTime == null) {
            return false;
        }
        if (System.currentTimeMillis() >= expiryTime) {
            cooldowns.remove(key);
            return false;
        }
        return true;
    }

    public long getRemainingCooldown(@NotNull Player player, @NotNull String perkId) {
        String key = getCooldownKey(player, perkId);
        Long expiryTime = cooldowns.get(key);
        if (expiryTime == null) {
            return 0;
        }
        long remaining = expiryTime - System.currentTimeMillis();
        if (remaining <= 0) {
            cooldowns.remove(key);
            return 0;
        }
        return remaining / 1000;
    }

    public void clearCooldown(@NotNull Player player, @NotNull String perkId) {
        cooldowns.remove(getCooldownKey(player, perkId));
    }

    public void clearAllCooldowns(@NotNull Player player) {
        clearAllCooldowns(player.getUniqueId());
    }

    public void clearAllCooldowns(@NotNull UUID playerId) {
        String prefix = playerId.toString() + ":";
        cooldowns.keySet().removeIf(key -> key.startsWith(prefix));
    }

    public void clearExpired() {
        long now = System.currentTimeMillis();
        cooldowns.entrySet().removeIf(entry -> entry.getValue() <= now);
    }

    private @NotNull String getCooldownKey(@NotNull Player player, @NotNull String perkId) {
        return player.getUniqueId().toString() + ":" + perkId;
    }
}
