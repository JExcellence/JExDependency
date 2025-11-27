package com.raindropcentral.rdq.database.entity.perk;

import com.raindropcentral.rdq.database.entity.RDQPlayer;
import de.jexcellence.hibernate.entity.AbstractEntity;
import jakarta.persistence.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Entity representing the relationship between a player and a perk.
 * <p>
 * This entity tracks the ownership, activation state, and usage history of perks
 * for individual players. It includes information about when perks were acquired,
 * activated, deactivated, and any cooldown periods.
 * </p>
 *
 * <p>
 * Key features:
 * <ul>
 *   <li>Tracks perk ownership and activation state</li>
 *   <li>Manages cooldown periods and expiry times</li>
 *   <li>Records usage history and statistics</li>
 *   <li>Supports temporary and permanent perk assignments</li>
 * </ul>
 * </p>
 *
 * @author ItsRainingHP
 * @version 1.0.0
 * @since TBD
 */
@Entity
@Table(name = "r_player_perk", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"player_id", "perk_id"})
})
public class RPlayerPerk extends AbstractEntity {
    
    /**
     * The player who owns this perk.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "player_id", nullable = false)
    private RDQPlayer player;
    
    /**
     * The perk that is owned by the player.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "perk_id", nullable = false)
    private RPerk perk;
    
    /**
     * Whether this perk is currently active for the player.
     */
    @Column(name = "is_active", nullable = false)
    private boolean active = false;
    
    /**
     * Whether this perk is enabled by the player (can be activated).
     * This is different from active - enabled means the player has chosen to use this perk,
     * while active means it's currently running/providing effects.
     */
    @Column(name = "is_enabled", nullable = false)
    private boolean enabled = false;
    
    /**
     * When this perk was first acquired by the player.
     */
    @Column(name = "acquired_at", nullable = false)
    private LocalDateTime acquiredAt;
    
    /**
     * When this perk was last activated by the player.
     */
    @Column(name = "last_activated")
    private LocalDateTime lastActivated;
    
    /**
     * When this perk was last deactivated by the player.
     */
    @Column(name = "last_deactivated")
    private LocalDateTime lastDeactivated;
    
    /**
     * When the current cooldown period expires (null if no cooldown).
     */
    @Column(name = "cooldown_expiry")
    private LocalDateTime cooldownExpiry;
    
    /**
     * Total number of times this perk has been activated.
     */
    @Column(name = "activation_count", nullable = false)
    private long activationCount = 0L;
    
    /**
     * Total duration this perk has been active (in seconds).
     */
    @Column(name = "total_active_duration", nullable = false)
    private long totalActiveDuration = 0L;
    
    /**
     * When this perk assignment expires (null for permanent).
     */
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
    
    /**
     * Whether this perk assignment is temporary.
     */
    @Column(name = "is_temporary", nullable = false)
    private boolean temporary = false;
    
    /**
     * Source of this perk assignment (e.g., "purchase", "reward", "admin").
     */
    @Column(name = "assignment_source", length = 64)
    private String assignmentSource;
    
    /**
     * Additional metadata stored as JSON.
     */
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;
    
    /**
     * Protected no-argument constructor for JPA.
     */
    protected RPlayerPerk() {}
    
    /**
     * Creates a new player-perk relationship.
     *
     * @param player the player who owns the perk
     * @param perk   the perk being assigned
     */
    public RPlayerPerk(final @NotNull RDQPlayer player, final @NotNull RPerk perk) {
        this.player = Objects.requireNonNull(player, "Player cannot be null");
        this.perk = Objects.requireNonNull(perk, "Perk cannot be null");
        this.acquiredAt = LocalDateTime.now();
    }
    
    /**
     * Creates a new player-perk relationship with enabled state.
     *
     * @param player  the player who owns the perk
     * @param perk    the perk being assigned
     * @param enabled whether the perk should be initially enabled
     */
    public RPlayerPerk(
        final @NotNull RDQPlayer player,
        final @NotNull RPerk perk,
        final boolean enabled
    ) {
        this(player, perk);
        this.enabled = enabled;
    }
    
    /**
     * Creates a new player-perk relationship with assignment source.
     *
     * @param player           the player who owns the perk
     * @param perk             the perk being assigned
     * @param assignmentSource the source of this assignment
     */
    public RPlayerPerk(
        final @NotNull RDQPlayer player,
        final @NotNull RPerk perk,
        final @Nullable String assignmentSource
    ) {
        this(player, perk);
        this.assignmentSource = assignmentSource;
    }
    
    /**
     * Creates a new player-perk relationship with enabled state and assignment source.
     *
     * @param player           the player who owns the perk
     * @param perk             the perk being assigned
     * @param enabled          whether the perk should be initially enabled
     * @param assignmentSource the source of this assignment
     */
    public RPlayerPerk(
        final @NotNull RDQPlayer player,
        final @NotNull RPerk perk,
        final boolean enabled,
        final @Nullable String assignmentSource
    ) {
        this(player, perk, enabled);
        this.assignmentSource = assignmentSource;
    }
    
    // ========== Getters and Setters ==========
    
    public @NotNull RDQPlayer getPlayer() {
        return this.player;
    }
    
    public void setPlayer(final @NotNull RDQPlayer player) {
        this.player = Objects.requireNonNull(player, "Player cannot be null");
    }
    
    public @NotNull RPerk getPerk() {
        return this.perk;
    }
    
    public void setPerk(final @NotNull RPerk perk) {
        this.perk = Objects.requireNonNull(perk, "Perk cannot be null");
    }
    
    public boolean isActive() {
        return this.active;
    }
    
    public void setActive(final boolean active) {
        this.active = active;
    }
    
    public boolean isEnabled() {
        return this.enabled;
    }
    
    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;
    }
    
    public @NotNull LocalDateTime getAcquiredAt() {
        return this.acquiredAt;
    }
    
    public void setAcquiredAt(final @NotNull LocalDateTime acquiredAt) {
        this.acquiredAt = Objects.requireNonNull(acquiredAt, "Acquired at cannot be null");
    }
    
    public @Nullable LocalDateTime getLastActivated() {
        return this.lastActivated;
    }
    
    public void setLastActivated(final @Nullable LocalDateTime lastActivated) {
        this.lastActivated = lastActivated;
    }
    
    public @Nullable LocalDateTime getLastDeactivated() {
        return this.lastDeactivated;
    }
    
    public void setLastDeactivated(final @Nullable LocalDateTime lastDeactivated) {
        this.lastDeactivated = lastDeactivated;
    }
    
    public @Nullable LocalDateTime getCooldownExpiry() {
        return this.cooldownExpiry;
    }
    
    public void setCooldownExpiry(final @Nullable LocalDateTime cooldownExpiry) {
        this.cooldownExpiry = cooldownExpiry;
    }
    
    public long getActivationCount() {
        return this.activationCount;
    }
    
    public void setActivationCount(final long activationCount) {
        this.activationCount = Math.max(0, activationCount);
    }
    
    public long getTotalActiveDuration() {
        return this.totalActiveDuration;
    }
    
    public void setTotalActiveDuration(final long totalActiveDuration) {
        this.totalActiveDuration = Math.max(0, totalActiveDuration);
    }
    
    public @Nullable LocalDateTime getExpiresAt() {
        return this.expiresAt;
    }
    
    public void setExpiresAt(final @Nullable LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }
    
    public boolean isTemporary() {
        return this.temporary;
    }
    
    public void setTemporary(final boolean temporary) {
        this.temporary = temporary;
    }
    
    public @Nullable String getAssignmentSource() {
        return this.assignmentSource;
    }
    
    public void setAssignmentSource(final @Nullable String assignmentSource) {
        this.assignmentSource = assignmentSource;
    }
    
    public @Nullable String getMetadata() {
        return this.metadata;
    }
    
    public void setMetadata(final @Nullable String metadata) {
        this.metadata = metadata;
    }
    
    // ========== Utility Methods ==========
    
    /**
     * Checks if this perk assignment has expired.
     *
     * @return true if the perk has expired, false otherwise
     */
    public boolean hasExpired() {
        return this.expiresAt != null && this.expiresAt.isBefore(LocalDateTime.now());
    }
    
    /**
     * Checks if this perk is currently on cooldown.
     *
     * @return true if the perk is on cooldown, false otherwise
     */
    public boolean isOnCooldown() {
        return this.cooldownExpiry != null && this.cooldownExpiry.isAfter(LocalDateTime.now());
    }
    
    /**
     * Gets the remaining cooldown time in seconds.
     *
     * @return remaining cooldown in seconds, 0 if no cooldown
     */
    public long getRemainingCooldownSeconds() {
        if (this.cooldownExpiry == null || this.cooldownExpiry.isBefore(LocalDateTime.now())) {
            return 0L;
        }
        
        return java.time.Duration.between(LocalDateTime.now(), this.cooldownExpiry).getSeconds();
    }
    
    /**
     * Increments the activation count and updates the last activated timestamp.
     */
    public void recordActivation() {
        this.activationCount++;
        this.lastActivated = LocalDateTime.now();
    }
    
    /**
     * Records deactivation and updates total active duration.
     */
    public void recordDeactivation() {
        this.lastDeactivated = LocalDateTime.now();
        
        // Calculate and add to total active duration if we have an activation time
        if (this.lastActivated != null) {
            final long sessionDuration = java.time.Duration.between(this.lastActivated, this.lastDeactivated).getSeconds();
            this.totalActiveDuration += Math.max(0, sessionDuration);
        }
    }
    
    /**
     * Sets a cooldown period starting from now.
     *
     * @param durationSeconds the cooldown duration in seconds
     */
    public void setCooldown(final long durationSeconds) {
        if (durationSeconds > 0) {
            this.cooldownExpiry = LocalDateTime.now().plusSeconds(durationSeconds);
        } else {
            this.cooldownExpiry = null;
        }
    }
    
    /**
     * Clears the current cooldown.
     */
    public void clearCooldown() {
        this.cooldownExpiry = null;
    }
    
    /**
     * Checks if this perk can be activated.
     * A perk can be activated if it's enabled, not expired, not on cooldown, and not already active.
     *
     * @return true if the perk can be activated, false otherwise
     */
    public boolean canBeActivated() {
        return this.enabled && !this.hasExpired() && !this.isOnCooldown() && !this.active;
    }
    
    /**
     * Increments the activation count by one.
     * This is a convenience method for {@link #setActivationCount(long)}.
     */
    public void incrementActivationCount() {
        this.activationCount++;
    }
    
    /**
     * Adds usage time to the total active duration.
     *
     * @param additionalSeconds the additional seconds to add to the total
     */
    public void addUsageTime(final long additionalSeconds) {
        this.totalActiveDuration += Math.max(0, additionalSeconds);
    }
    
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof final RPlayerPerk that)) return false;
        
        return Objects.equals(this.player, that.player) &&
               Objects.equals(this.perk, that.perk);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(this.player, this.perk);
    }
    
    @Override
    public String toString() {
        return "RDQPlayerPerk{" +
               "player=" + (this.player != null ? this.player.getPlayerName() : "null") +
               ", perk=" + (this.perk != null ? this.perk.getIdentifier() : "null") +
               ", enabled=" + this.enabled +
               ", active=" + this.active +
               ", acquiredAt=" + this.acquiredAt +
               ", onCooldown=" + this.isOnCooldown() +
               '}';
    }
}