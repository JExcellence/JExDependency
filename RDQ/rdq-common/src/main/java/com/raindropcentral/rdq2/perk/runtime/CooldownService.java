package com.raindropcentral.rdq2.perk.runtime;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CooldownService {

    private final Map<String, Long> cooldowns = new ConcurrentHashMap<>();

    public void setCooldown(@NotNull Player player, @NotNull String perkId, long durationSeconds) {
        setCooldown(player.getUniqueId(), perkId, durationSeconds);
    }

    public void setCooldown(@NotNull UUID playerId, @NotNull String perkId, long durationSeconds) {
        var key = getCooldownKey(playerId, perkId);
        var expiryTime = System.currentTimeMillis() + (durationSeconds * 1000);
        cooldowns.put(key, expiryTime);
    }

    public boolean isOnCooldown(@NotNull Player player, @NotNull String perkId) {
        return isOnCooldown(player.getUniqueId(), perkId);
    }

    public boolean isOnCooldown(@NotNull UUID playerId, @NotNull String perkId) {
        var key = getCooldownKey(playerId, perkId);
        var expiryTime = cooldowns.get(key);
        if (expiryTime == null) return false;
        
        if (System.currentTimeMillis() >= expiryTime) {
            cooldowns.remove(key, expiryTime);
            return false;
        }
        return true;
    }

    public long getRemainingCooldown(@NotNull Player player, @NotNull String perkId) {
        return getRemainingCooldown(player.getUniqueId(), perkId);
    }

    public long getRemainingCooldown(@NotNull UUID playerId, @NotNull String perkId) {
        var key = getCooldownKey(playerId, perkId);
        var expiryTime = cooldowns.get(key);
        if (expiryTime == null) return 0L;
        
        var remaining = expiryTime - System.currentTimeMillis();
        if (remaining <= 0) {
            cooldowns.remove(key, expiryTime);
            return 0L;
        }
        return remaining / 1000;
    }

    public void clearCooldown(@NotNull Player player, @NotNull String perkId) {
        clearCooldown(player.getUniqueId(), perkId);
    }

    public void clearCooldown(@NotNull UUID playerId, @NotNull String perkId) {
        cooldowns.remove(getCooldownKey(playerId, perkId));
    }

    public void clearAllCooldowns(@NotNull Player player) {
        clearAllCooldowns(player.getUniqueId());
    }

    public void clearAllCooldowns(@NotNull UUID playerId) {
        var prefix = playerId.toString() + ":";
        cooldowns.keySet().removeIf(key -> key.startsWith(prefix));
    }

    public void clearExpired() {
        var now = System.currentTimeMillis();
        cooldowns.entrySet().removeIf(entry -> entry.getValue() <= now);
    }

    private @NotNull String getCooldownKey(@NotNull UUID playerId, @NotNull String perkId) {
        return playerId.toString() + ":" + perkId;
    }
}
