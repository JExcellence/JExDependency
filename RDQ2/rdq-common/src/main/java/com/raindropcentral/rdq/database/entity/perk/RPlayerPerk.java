package com.raindropcentral.rdq.database.entity.perk;

import com.raindropcentral.rdq.database.entity.player.RDQPlayer;
import de.jexcellence.hibernate.entity.AbstractEntity;
import jakarta.persistence.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serial;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "r_player_perk", uniqueConstraints = {@UniqueConstraint(columnNames = {"player_id", "perk_id"})})
public final class RPlayerPerk extends AbstractEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "player_id", nullable = false)
    private RDQPlayer player;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "perk_id", nullable = false)
    private RPerk perk;

    @Column(name = "is_active", nullable = false)
    private boolean active = false;

    @Column(name = "is_enabled", nullable = false)
    private boolean enabled = false;

    @Column(name = "acquired_at", nullable = false)
    private LocalDateTime acquiredAt;

    @Column(name = "last_activated")
    private LocalDateTime lastActivated;

    @Column(name = "last_deactivated")
    private LocalDateTime lastDeactivated;

    @Column(name = "cooldown_expiry")
    private LocalDateTime cooldownExpiry;

    @Column(name = "activation_count", nullable = false)
    private long activationCount = 0L;

    @Column(name = "total_active_duration", nullable = false)
    private long totalActiveDuration = 0L;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "is_temporary", nullable = false)
    private boolean temporary = false;

    @Column(name = "assignment_source", length = 64)
    private String assignmentSource;

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    protected RPlayerPerk() {}

    public RPlayerPerk(@NotNull RDQPlayer player, @NotNull RPerk perk) {
        this.player = Objects.requireNonNull(player, "player cannot be null");
        this.perk = Objects.requireNonNull(perk, "perk cannot be null");
        this.acquiredAt = LocalDateTime.now();
        perk.addPlayerPerk(this);
    }

    public RPlayerPerk(@NotNull RDQPlayer player, @NotNull RPerk perk, boolean enabled) {
        this(player, perk);
        this.enabled = enabled;
    }

    public RPlayerPerk(@NotNull RDQPlayer player, @NotNull RPerk perk, @Nullable String assignmentSource) {
        this(player, perk);
        this.assignmentSource = assignmentSource;
    }

    public @NotNull RDQPlayer getPlayer() {
        return this.player;
    }

    public void setPlayer(@NotNull RDQPlayer player) {
        this.player = Objects.requireNonNull(player, "player cannot be null");
    }

    public @Nullable RPerk getPerk() {
        return this.perk;
    }

    public void setPerk(@NotNull RPerk perk) {
        Objects.requireNonNull(perk, "perk cannot be null");
        if (this.perk == perk) {
            return;
        }
        var previous = this.perk;
        this.perk = perk;
        perk.attachPlayerPerk(this);
        if (previous != null) {
            previous.detachPlayerPerk(this);
        }
    }

    void clearPerkAssociation() {
        this.perk = null;
    }

    /**
     * Indicates whether the perk is currently active for the player.
     *
     * @return {@code true} when the perk is active
     */
    public boolean isActive() {
        return this.active;
    }

    /**
     * Sets the active state of the perk.
     *
     * @param active {@code true} when the perk should be flagged as active
     */
    public void setActive(final boolean active) {
        this.active = active;
    }

    /**
     * Indicates whether the perk is enabled for activation.
     *
     * @return {@code true} when the perk is enabled
     */
    public boolean isEnabled() {
        return this.enabled;
    }

    /**
     * Updates the enabled state of the perk.
     *
     * @param enabled {@code true} to enable the perk
     */
    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Provides the timestamp when the perk was acquired.
     *
     * @return the acquisition time
     */
    public @NotNull LocalDateTime getAcquiredAt() {
        return this.acquiredAt;
    }

    /**
     * Provides the timestamp of the most recent activation.
     *
     * @return the last activation time or {@code null} when not previously activated
     */
    public @Nullable LocalDateTime getLastActivated() {
        return this.lastActivated;
    }

    /**
     * Provides the timestamp when the cooldown ends.
     *
     * @return the cooldown expiry time or {@code null} when no cooldown applies
     */
    public @Nullable LocalDateTime getCooldownExpiry() {
        return this.cooldownExpiry;
    }

    /**
     * Retrieves how many times the perk has been activated.
     *
     * @return the activation counter value
     */
    public long getActivationCount() {
        return this.activationCount;
    }

    /**
     * Retrieves the total number of seconds the perk has been active across all sessions.
     *
     * @return the cumulative active duration in seconds
     */
    public long getTotalActiveDuration() {
        return this.totalActiveDuration;
    }

    /**
     * Provides the timestamp when the perk expires.
     *
     * @return the expiration timestamp or {@code null} when the perk is permanent
     */
    public @Nullable LocalDateTime getExpiresAt() {
        return this.expiresAt;
    }

    /**
     * Updates the expiry timestamp for the perk.
     *
     * @param expiresAt the new expiration timestamp, or {@code null} for no expiry
     */
    public void setExpiresAt(final @Nullable LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    /**
     * Indicates whether the perk is temporary.
     *
     * @return {@code true} when the perk has an expiration
     */
    public boolean isTemporary() {
        return this.temporary;
    }

    /**
     * Flags the perk as temporary or permanent.
     *
     * @param temporary {@code true} to mark the perk as temporary
     */
    public void setTemporary(final boolean temporary) {
        this.temporary = temporary;
    }

    /**
     * Retrieves the identifier describing how the perk was assigned.
     *
     * @return the assignment source or {@code null} when unknown
     */
    public @Nullable String getAssignmentSource() {
        return this.assignmentSource;
    }

    /**
     * Retrieves any serialized metadata that supplements the perk association.
     *
     * @return the metadata blob or {@code null} when none is stored
     */
    public @Nullable String getMetadata() {
        return this.metadata;
    }

    /**
     * Stores serialized metadata alongside the perk association.
     *
     * @param metadata the metadata blob, or {@code null} to clear it
     */
    public void setMetadata(final @Nullable String metadata) {
        this.metadata = metadata;
    }

    /**
     * Determines whether the perk has reached its expiration timestamp.
     *
     * @return {@code true} when the perk expired
     */
    public boolean hasExpired() {
        return this.expiresAt != null && this.expiresAt.isBefore(LocalDateTime.now());
    }

    /**
     * Determines whether the perk is currently in a cooldown window.
     *
     * @return {@code true} when the cooldown has not yet elapsed
     */
    public boolean isOnCooldown() {
        return this.cooldownExpiry != null && this.cooldownExpiry.isAfter(LocalDateTime.now());
    }

    /**
     * Calculates the number of seconds remaining before the cooldown ends.
     *
     * @return the remaining cooldown in seconds, or {@code 0} when none remains
     */
    public long getRemainingCooldownSeconds() {
        if (this.cooldownExpiry == null || this.cooldownExpiry.isBefore(LocalDateTime.now())) {
            return 0L;
        }
        return Duration.between(LocalDateTime.now(), this.cooldownExpiry).getSeconds();
    }

    /**
     * Records a new activation event and updates tracking fields accordingly.
     */
    public void recordActivation() {
        this.activationCount++;
        this.lastActivated = LocalDateTime.now();
    }

    /**
     * Records a deactivation event and aggregates the session's active duration.
     */
    public void recordDeactivation() {
        this.lastDeactivated = LocalDateTime.now();
        if (this.lastActivated != null) {
            final long sessionDuration = Duration.between(this.lastActivated, this.lastDeactivated).getSeconds();
            this.totalActiveDuration += Math.max(0, sessionDuration);
        }
    }

    /**
     * Applies a cooldown duration measured in seconds.
     *
     * @param durationSeconds the number of seconds the perk should remain on cooldown
     */
    public void setCooldown(final long durationSeconds) {
        this.cooldownExpiry = durationSeconds > 0 ? LocalDateTime.now().plusSeconds(durationSeconds) : null;
    }

    /**
     * Clears any existing cooldown information.
     */
    public void clearCooldown() {
        this.cooldownExpiry = null;
    }

    /**
     * Determines whether the perk is eligible for activation.
     *
     * @return {@code true} when the perk is enabled, not expired, not on cooldown, and inactive
     */
    public boolean canBeActivated() {
        return this.enabled && !hasExpired() && !isOnCooldown() && !this.active;
    }

    /**
     * Compares this entity with another for equality based on player and perk identifiers.
     *
     * @param obj the object to compare against
     * @return {@code true} when both entities reference the same player and perk
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof final RPlayerPerk other)) return false;
        return Objects.equals(this.player, other.player) && Objects.equals(this.perk, other.perk);
    }

    /**
     * Generates a hash code consistent with {@link #equals(Object)} using the player and perk.
     *
     * @return the computed hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(this.player, this.perk);
    }

    /**
     * Creates a human-readable description including player and perk identifiers.
     *
     * @return the string representation of this entity
     */
    @Override
    public String toString() {
        return "RPlayerPerk[player=%s, perk=%s, enabled=%b, active=%b]"
                .formatted(player != null ? player.getPlayerName() : "null",
                        perk != null ? perk.getIdentifier() : "null", enabled, active);
    }
}
