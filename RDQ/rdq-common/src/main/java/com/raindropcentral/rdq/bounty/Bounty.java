package com.raindropcentral.rdq.bounty;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Immutable record representing a bounty for API use.
 *
 * <p>This is the API-facing representation of a bounty. The actual persistence
 * is handled by {@link com.raindropcentral.rdq.database.entity.bounty.BountyEntity}.
 *
 * @author JExcellence
 * @since 6.0.0
 * @see BountyStatus
 * @see BountyRequest
 */
public record Bounty(
    @Nullable Long id,
    @NotNull UUID placerId,
    @NotNull UUID targetId,
    @NotNull BigDecimal amount,
    @NotNull String currency,
    @NotNull BountyStatus status,
    @NotNull Instant createdAt,
    @Nullable Instant expiresAt,
    @Nullable UUID claimedBy,
    @Nullable Instant claimedAt
) {
    public Bounty {
        Objects.requireNonNull(placerId, "placerId");
        Objects.requireNonNull(targetId, "targetId");
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(currency, "currency");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(createdAt, "createdAt");
    }

    /**
     * Creates a new active bounty.
     */
    public static Bounty create(
        @NotNull UUID placerId,
        @NotNull UUID targetId,
        @NotNull BigDecimal amount,
        @NotNull String currency,
        @Nullable Instant expiresAt
    ) {
        return new Bounty(
            null,
            placerId,
            targetId,
            amount,
            currency,
            BountyStatus.ACTIVE,
            Instant.now(),
            expiresAt,
            null,
            null
        );
    }

    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }

    public boolean isActive() {
        return status == BountyStatus.ACTIVE && !isExpired();
    }

    /**
     * Returns player info for the target.
     */
    @NotNull
    public PlayerInfo target() {
        return new PlayerInfo(targetId);
    }

    /**
     * Returns player info for the placer.
     */
    @NotNull
    public PlayerInfo placer() {
        return new PlayerInfo(placerId);
    }

    /**
     * Simple player info holder for compatibility with views.
     */
    public record PlayerInfo(@NotNull UUID uniqueId) {
        @NotNull
        public String name() {
            var player = org.bukkit.Bukkit.getPlayer(uniqueId);
            if (player != null) {
                return player.getName();
            }
            var offline = org.bukkit.Bukkit.getOfflinePlayer(uniqueId);
            return offline.getName() != null ? offline.getName() : "Unknown";
        }
    }
}
