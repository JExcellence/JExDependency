package com.raindropcentral.rdq.perk.runtime;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe cooldown registry shared across all perk runtimes.
 * <p>
 * The implementation stores cooldown expiries using a {@code playerId:perkId}
 * composite key, allowing both online and offline player state to be managed
 * consistently.
 * </p>
 *
 * @author JExcellence
 * @version 1.0.1
 * @since 1.0.0
 */
public class CooldownService {

    private final Map<String, Long> cooldowns;

    public CooldownService() {
        this.cooldowns = new ConcurrentHashMap<>();
    }

    public void setCooldown(@NotNull Player player, @NotNull String perkId, long durationSeconds) {
        Objects.requireNonNull(player, "player");
        setCooldown(player.getUniqueId(), perkId, durationSeconds);
    }

    public void setCooldown(@NotNull UUID playerId, @NotNull String perkId, long durationSeconds) {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(perkId, "perkId");
        final String key = getCooldownKey(playerId, perkId);
        final long expiryTime = System.currentTimeMillis() + (durationSeconds * 1000);
        cooldowns.put(key, expiryTime);
    }

    public boolean isOnCooldown(@NotNull Player player, @NotNull String perkId) {
        Objects.requireNonNull(player, "player");
        return isOnCooldown(player.getUniqueId(), perkId);
    }

    public boolean isOnCooldown(@NotNull UUID playerId, @NotNull String perkId) {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(perkId, "perkId");
        final String key = getCooldownKey(playerId, perkId);
        final Long expiryTime = cooldowns.get(key);
        if (expiryTime == null) {
            return false;
        }
        if (System.currentTimeMillis() >= expiryTime) {
            cooldowns.remove(key, expiryTime);
            return false;
        }
        return true;
    }

    public long getRemainingCooldown(@NotNull Player player, @NotNull String perkId) {
        Objects.requireNonNull(player, "player");
        return getRemainingCooldown(player.getUniqueId(), perkId);
    }

    public long getRemainingCooldown(@NotNull UUID playerId, @NotNull String perkId) {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(perkId, "perkId");
        final String key = getCooldownKey(playerId, perkId);
        final Long expiryTime = cooldowns.get(key);
        if (expiryTime == null) {
            return 0L;
        }
        final long remaining = expiryTime - System.currentTimeMillis();
        if (remaining <= 0) {
            cooldowns.remove(key, expiryTime);
            return 0L;
        }
        return remaining / 1000;
    }

    public void clearCooldown(@NotNull Player player, @NotNull String perkId) {
        Objects.requireNonNull(player, "player");
        clearCooldown(player.getUniqueId(), perkId);
    }

    public void clearCooldown(@NotNull UUID playerId, @NotNull String perkId) {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(perkId, "perkId");
        cooldowns.remove(getCooldownKey(playerId, perkId));
    }

    public void clearAllCooldowns(@NotNull Player player) {
        Objects.requireNonNull(player, "player");
        clearAllCooldowns(player.getUniqueId());
    }

    public void clearAllCooldowns(@NotNull UUID playerId) {
        Objects.requireNonNull(playerId, "playerId");
        final String prefix = playerId.toString() + ":";
        cooldowns.keySet().removeIf(key -> key.startsWith(prefix));
    }

    public void clearExpired() {
        final long now = System.currentTimeMillis();
        cooldowns.entrySet().removeIf(entry -> entry.getValue() <= now);
    }

    private @NotNull String getCooldownKey(@NotNull Player player, @NotNull String perkId) {
        return getCooldownKey(player.getUniqueId(), perkId);
    }

    private @NotNull String getCooldownKey(@NotNull UUID playerId, @NotNull String perkId) {
        return playerId.toString() + ":" + perkId;
    }
}
