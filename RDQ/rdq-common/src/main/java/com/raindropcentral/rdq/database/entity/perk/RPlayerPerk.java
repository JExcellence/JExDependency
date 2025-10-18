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

    public RPlayerPerk(final @NotNull RDQPlayer player, final @NotNull RPerk perk) {
        this.player = Objects.requireNonNull(player, "player cannot be null");
        this.perk = Objects.requireNonNull(perk, "perk cannot be null");
        this.acquiredAt = LocalDateTime.now();
    }

    public RPlayerPerk(final @NotNull RDQPlayer player, final @NotNull RPerk perk, final boolean enabled) {
        this(player, perk);
        this.enabled = enabled;
    }

    public RPlayerPerk(final @NotNull RDQPlayer player, final @NotNull RPerk perk, final @Nullable String assignmentSource) {
        this(player, perk);
        this.assignmentSource = assignmentSource;
    }

    public @NotNull RDQPlayer getPlayer() {
        return this.player;
    }

    public void setPlayer(final @NotNull RDQPlayer player) {
        this.player = Objects.requireNonNull(player, "player cannot be null");
    }

    public @NotNull RPerk getPerk() {
        return this.perk;
    }

    public void setPerk(final @NotNull RPerk perk) {
        this.perk = Objects.requireNonNull(perk, "perk cannot be null");
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

    public @Nullable LocalDateTime getLastActivated() {
        return this.lastActivated;
    }

    public @Nullable LocalDateTime getCooldownExpiry() {
        return this.cooldownExpiry;
    }

    public long getActivationCount() {
        return this.activationCount;
    }

    public long getTotalActiveDuration() {
        return this.totalActiveDuration;
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

    public @Nullable String getMetadata() {
        return this.metadata;
    }

    public void setMetadata(final @Nullable String metadata) {
        this.metadata = metadata;
    }

    public boolean hasExpired() {
        return this.expiresAt != null && this.expiresAt.isBefore(LocalDateTime.now());
    }

    public boolean isOnCooldown() {
        return this.cooldownExpiry != null && this.cooldownExpiry.isAfter(LocalDateTime.now());
    }

    public long getRemainingCooldownSeconds() {
        if (this.cooldownExpiry == null || this.cooldownExpiry.isBefore(LocalDateTime.now())) {
            return 0L;
        }
        return Duration.between(LocalDateTime.now(), this.cooldownExpiry).getSeconds();
    }

    public void recordActivation() {
        this.activationCount++;
        this.lastActivated = LocalDateTime.now();
    }

    public void recordDeactivation() {
        this.lastDeactivated = LocalDateTime.now();
        if (this.lastActivated != null) {
            final long sessionDuration = Duration.between(this.lastActivated, this.lastDeactivated).getSeconds();
            this.totalActiveDuration += Math.max(0, sessionDuration);
        }
    }

    public void setCooldown(final long durationSeconds) {
        this.cooldownExpiry = durationSeconds > 0 ? LocalDateTime.now().plusSeconds(durationSeconds) : null;
    }

    public void clearCooldown() {
        this.cooldownExpiry = null;
    }

    public boolean canBeActivated() {
        return this.enabled && !hasExpired() && !isOnCooldown() && !this.active;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof final RPlayerPerk other)) return false;
        return Objects.equals(this.player, other.player) && Objects.equals(this.perk, other.perk);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.player, this.perk);
    }

    @Override
    public String toString() {
        return "RPlayerPerk[player=%s, perk=%s, enabled=%b, active=%b]"
                .formatted(player != null ? player.getPlayerName() : "null",
                        perk != null ? perk.getIdentifier() : "null", enabled, active);
    }
}