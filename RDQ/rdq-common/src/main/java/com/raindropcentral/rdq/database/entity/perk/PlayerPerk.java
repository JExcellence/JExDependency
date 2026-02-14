package com.raindropcentral.rdq.database.entity.perk;

import com.raindropcentral.rdq.database.entity.player.RDQPlayer;
import de.jexcellence.hibernate.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Entity representing a player's association with a perk.
 * <p>
 * This entity tracks the ownership, enabled state, active state, cooldown status,
 * and usage statistics for a specific perk owned by a player.
 * </p>
 *
 * @author JExcellence
 * @version 1.0.0
 */
@Setter
@Getter
@Entity
@Table(
        name = "rdq_player_perk",
        uniqueConstraints = @UniqueConstraint(columnNames = {"player_id", "perk_id"})
)
public class PlayerPerk extends BaseEntity {

    /**
     * The player who owns this perk.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false)
    private RDQPlayer player;

    /**
     * The perk that is owned.
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "perk_id", nullable = false)
    private Perk perk;

    /**
     * Whether the player has unlocked this perk.
     */
    @Column(name = "unlocked", nullable = false)
    private boolean unlocked = false;

    /**
     * Whether the player has enabled this perk.
     * Enabled perks will be activated when the player logs in.
     */
    @Column(name = "enabled", nullable = false)
    private boolean enabled = false;

    /**
     * Whether this perk is currently active (effects are being applied).
     */
    @Column(name = "active", nullable = false)
    private boolean active = false;

    /**
     * Timestamp when the cooldown expires.
     * Null if the perk is not on cooldown.
     */
    @Column(name = "cooldown_expires_at")
    private LocalDateTime cooldownExpiresAt;

    /**
     * Number of times this perk has been activated.
     */
    @Column(name = "activation_count", nullable = false)
    private int activationCount = 0;

    /**
     * Total time in milliseconds that this perk has been active.
     */
    @Column(name = "total_usage_time_millis", nullable = false)
    private long totalUsageTimeMillis = 0;

    /**
     * Timestamp when this perk was last activated.
     */
    @Column(name = "last_activated")
    private LocalDateTime lastActivated;

    /**
     * Timestamp when this perk was last deactivated.
     */
    @Column(name = "last_deactivated")
    private LocalDateTime lastDeactivated;

    /**
     * Timestamp when this perk was unlocked.
     */
    @CreationTimestamp
    @Column(name = "unlocked_at", nullable = false, updatable = false)
    private LocalDateTime unlockedAt;

    /**
     * Timestamp when this record was last updated.
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Version for optimistic locking.
     */
    @Version
    @Column(name = "version")
    private int version;

    /**
     * Protected no-argument constructor for JPA.
     */
    protected PlayerPerk() {
    }

    /**
     * Constructs a new {@code PlayerPerk} association.
     *
     * @param player the player who owns the perk
     * @param perk   the perk being owned
     */
    public PlayerPerk(
            @NotNull final RDQPlayer player,
            @NotNull final Perk perk
    ) {
        this.player = player;
        this.perk = perk;
        this.unlocked = true;
    }

    /**
     * Checks if this perk is currently on cooldown.
     *
     * @return true if on cooldown, false otherwise
     */
    public boolean isOnCooldown() {
        return cooldownExpiresAt != null && LocalDateTime.now().isBefore(cooldownExpiresAt);
    }

    /**
     * Gets the remaining cooldown time in milliseconds.
     *
     * @return remaining cooldown in milliseconds, or 0 if not on cooldown
     */
    public long getRemainingCooldownMillis() {
        if (!isOnCooldown()) {
            return 0;
        }
        return java.time.Duration.between(LocalDateTime.now(), cooldownExpiresAt).toMillis();
    }

    /**
     * Starts a cooldown for this perk.
     *
     * @param durationMillis the cooldown duration in milliseconds
     */
    public void startCooldown(final long durationMillis) {
        this.cooldownExpiresAt = LocalDateTime.now().plusNanos(durationMillis * 1_000_000);
    }

    /**
     * Clears the cooldown for this perk.
     */
    public void clearCooldown() {
        this.cooldownExpiresAt = null;
    }

    /**
     * Records an activation of this perk.
     */
    public void recordActivation() {
        this.activationCount++;
        this.lastActivated = LocalDateTime.now();
        this.active = true;
    }

    /**
     * Records a deactivation of this perk.
     */
    public void recordDeactivation() {
        this.lastDeactivated = LocalDateTime.now();
        this.active = false;

        // Update total usage time if we have both activation and deactivation times
        if (this.lastActivated != null && this.lastDeactivated != null) {
            long usageMillis = java.time.Duration.between(this.lastActivated, this.lastDeactivated).toMillis();
            if (usageMillis > 0) {
                this.totalUsageTimeMillis += usageMillis;
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PlayerPerk that)) return false;

        if (this.getId() != null && that.getId() != null) {
            return this.getId().equals(that.getId());
        }

        return player != null && player.equals(that.player) &&
                perk != null && perk.equals(that.perk);
    }

    @Override
    public int hashCode() {
        if (this.getId() != null) {
            return this.getId().hashCode();
        }

        return Objects.hash(player, perk);
    }

    @Override
    public String toString() {
        return "PlayerPerk{" +
                "id=" + getId() +
                ", player=" + (player != null ? player.getUniqueId() : null) +
                ", perk=" + (perk != null ? perk.getIdentifier() : null) +
                ", unlocked=" + unlocked +
                ", enabled=" + enabled +
                ", active=" + active +
                '}';
    }
}
