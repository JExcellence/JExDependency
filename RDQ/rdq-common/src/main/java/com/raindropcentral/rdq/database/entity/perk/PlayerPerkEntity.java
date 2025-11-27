package com.raindropcentral.rdq.database.entity.perk;

import de.jexcellence.hibernate.entity.AbstractEntity;
import jakarta.persistence.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serial;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Entity representing a player's perk state.
 *
 * @author JExcellence
 * @since 6.0.0
 */
@Entity
@Table(
    name = "rdq_player_perks",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"player_id", "perk_id"})
    },
    indexes = {
        @Index(name = "idx_player_perk_player", columnList = "player_id"),
        @Index(name = "idx_player_perk_perk", columnList = "perk_id"),
        @Index(name = "idx_player_perk_active", columnList = "active")
    }
)
public class PlayerPerkEntity extends AbstractEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @Column(name = "player_id", nullable = false)
    private UUID playerId;

    @Column(name = "perk_id", nullable = false, length = 64)
    private String perkId;

    @Column(name = "unlocked", nullable = false)
    private boolean unlocked;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "cooldown_expiry")
    private Instant cooldownExpiry;

    @Column(name = "unlocked_at")
    private Instant unlockedAt;

    @Column(name = "last_activated_at")
    private Instant lastActivatedAt;

    protected PlayerPerkEntity() {
    }

    public PlayerPerkEntity(
        @NotNull UUID playerId,
        @NotNull String perkId,
        boolean unlocked,
        boolean active,
        @Nullable Instant cooldownExpiry,
        @Nullable Instant unlockedAt,
        @Nullable Instant lastActivatedAt
    ) {
        this.playerId = Objects.requireNonNull(playerId, "playerId");
        this.perkId = Objects.requireNonNull(perkId, "perkId");
        this.unlocked = unlocked;
        this.active = active;
        this.cooldownExpiry = cooldownExpiry;
        this.unlockedAt = unlockedAt;
        this.lastActivatedAt = lastActivatedAt;
    }

    @NotNull
    public static PlayerPerkEntity create(@NotNull UUID playerId, @NotNull String perkId) {
        return new PlayerPerkEntity(playerId, perkId, false, false, null, null, null);
    }

    @NotNull
    public static PlayerPerkEntity unlocked(@NotNull UUID playerId, @NotNull String perkId) {
        return new PlayerPerkEntity(playerId, perkId, true, false, null, Instant.now(), null);
    }

    @NotNull
    public UUID playerId() {
        return playerId;
    }

    @NotNull
    public String perkId() {
        return perkId;
    }

    public boolean unlocked() {
        return unlocked;
    }

    public boolean active() {
        return active;
    }

    @Nullable
    public Instant cooldownExpiry() {
        return cooldownExpiry;
    }

    @Nullable
    public Instant unlockedAt() {
        return unlockedAt;
    }

    @Nullable
    public Instant lastActivatedAt() {
        return lastActivatedAt;
    }

    public void unlock() {
        this.unlocked = true;
        if (this.unlockedAt == null) {
            this.unlockedAt = Instant.now();
        }
    }

    public void setUnlocked(boolean unlocked) {
        this.unlocked = unlocked;
        if (unlocked && this.unlockedAt == null) {
            this.unlockedAt = Instant.now();
        }
    }

    public void activate() {
        this.active = true;
        this.lastActivatedAt = Instant.now();
    }

    public void setActive(boolean active) {
        this.active = active;
        if (active) {
            this.lastActivatedAt = Instant.now();
        }
    }

    public void deactivate() {
        this.active = false;
    }

    public void setCooldownExpiry(@Nullable Instant cooldownExpiry) {
        this.cooldownExpiry = cooldownExpiry;
    }

    public boolean isOnCooldown() {
        return cooldownExpiry != null && Instant.now().isBefore(cooldownExpiry);
    }

    public boolean isActive() {
        return active;
    }

    @NotNull
    public com.raindropcentral.rdq.perk.PlayerPerkState toState() {
        return new com.raindropcentral.rdq.perk.PlayerPerkState(playerId, perkId, unlocked, active, cooldownExpiry);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PlayerPerkEntity that)) return false;
        return Objects.equals(getId(), that.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId());
    }

    @Override
    public String toString() {
        return "PlayerPerkEntity[playerId=" + playerId + ", perkId=" + perkId + ", unlocked=" + unlocked + ", active=" + active + "]";
    }
}
