/*
package com.raindropcentral.rdq2.perk;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public record PlayerPerkState(
    @NotNull UUID playerId,
    @NotNull String perkId,
    boolean unlocked,
    boolean active,
    @Nullable Instant cooldownExpiry
) {
    public PlayerPerkState {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(perkId, "perkId");
    }

    public boolean isOnCooldown() {
        return cooldownExpiry != null && Instant.now().isBefore(cooldownExpiry);
    }

    public Optional<Duration> remainingCooldown() {
        if (!isOnCooldown()) return Optional.empty();
        return Optional.of(Duration.between(Instant.now(), cooldownExpiry));
    }

    public PlayerPerkState withUnlocked(boolean unlocked) {
        return new PlayerPerkState(playerId, perkId, unlocked, active, cooldownExpiry);
    }

    public PlayerPerkState withActive(boolean active) {
        return new PlayerPerkState(playerId, perkId, unlocked, active, cooldownExpiry);
    }

    public PlayerPerkState withCooldown(@Nullable Instant cooldownExpiry) {
        return new PlayerPerkState(playerId, perkId, unlocked, active, cooldownExpiry);
    }

    public static PlayerPerkState initial(@NotNull UUID playerId, @NotNull String perkId) {
        return new PlayerPerkState(playerId, perkId, false, false, null);
    }
}
*/
