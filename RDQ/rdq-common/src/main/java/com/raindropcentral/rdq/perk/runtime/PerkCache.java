package com.raindropcentral.rdq.perk.runtime;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class PerkCache {

    private final Cache<String, PlayerPerkState> playerStates;

    public PerkCache() {
        this.playerStates = Caffeine.newBuilder()
            .maximumSize(10000)
            .expireAfterAccess(30, TimeUnit.MINUTES)
            .build();
    }

    public @NotNull PlayerPerkState getOrCreate(@NotNull Player player) {
        String key = player.getUniqueId().toString();
        return playerStates.get(key, k -> new PlayerPerkState(player));
    }

    public @Nullable PlayerPerkState get(@NotNull Player player) {
        return playerStates.getIfPresent(player.getUniqueId().toString());
    }

    public void invalidate(@NotNull Player player) {
        playerStates.invalidate(player.getUniqueId().toString());
    }

    public void invalidate(@NotNull UUID playerId) {
        playerStates.invalidate(playerId.toString());
    }

    public void invalidateAll() {
        playerStates.invalidateAll();
    }

    public long size() {
        return playerStates.estimatedSize();
    }
}
