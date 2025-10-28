package com.raindropcentral.rdq.perk.runtime;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Default in-memory implementation of CooldownStore.
 * <p>
 * Stores cooldowns in memory keyed by player UUID and perk identifier.
 * Cooldowns are ephemeral and reset on server restart.
 * </p>
 *
 * @author qodo
 * @version 1.0.0
 * @since TBD
 */
public class DefaultCooldownStore implements CooldownStore {

    private static final Logger LOGGER = Logger.getLogger(DefaultCooldownStore.class.getName());
    private static final DefaultCooldownStore INSTANCE = new DefaultCooldownStore();

    /** Key format: playerUUID:perkId -> expiresAtMillis */
    private final Map<String, Long> cooldowns = new ConcurrentHashMap<>();

    private DefaultCooldownStore() {}

    public static DefaultCooldownStore getInstance() {
        return INSTANCE;
    }

    private String getKey(final @NotNull UUID playerId, final @NotNull String perkId) {
        return playerId + ":" + perkId;
    }

    @Override
    public boolean isOnCooldown(final @NotNull Player player, final @NotNull String perkId) {
        final String key = getKey(player.getUniqueId(), perkId);
        final Long expiresAt = this.cooldowns.get(key);
        if (expiresAt == null) return false;

        final long now = System.currentTimeMillis();
        if (now >= expiresAt) {
            this.cooldowns.remove(key);
            return false;
        }
        return true;
    }

    @Override
    public long getRemainingCooldown(final @NotNull Player player, final @NotNull String perkId) {
        final String key = getKey(player.getUniqueId(), perkId);
        final Long expiresAt = this.cooldowns.get(key);
        if (expiresAt == null) return 0L;

        final long now = System.currentTimeMillis();
        if (now >= expiresAt) {
            this.cooldowns.remove(key);
            return 0L;
        }
        return (expiresAt - now) / 1000L;
    }

    @Override
    public void setCooldown(final @NotNull Player player, final @NotNull String perkId, final long seconds) {
        if (seconds <= 0) {
            clearCooldown(player, perkId);
            return;
        }
        final String key = getKey(player.getUniqueId(), perkId);
        final long expiresAt = System.currentTimeMillis() + (seconds * 1000L);
        this.cooldowns.put(key, expiresAt);
    }

    @Override
    public void clearCooldown(final @NotNull Player player, final @NotNull String perkId) {
        final String key = getKey(player.getUniqueId(), perkId);
        this.cooldowns.remove(key);
    }

    @Override
    public void clearAllCooldowns(final @NotNull Player player) {
        final UUID playerId = player.getUniqueId();
        this.cooldowns.keySet().removeIf(key -> key.startsWith(playerId.toString() + ":"));
    }
}
