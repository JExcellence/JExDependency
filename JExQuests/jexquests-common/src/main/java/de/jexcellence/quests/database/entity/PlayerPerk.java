package de.jexcellence.quests.database.entity;

import de.jexcellence.jehibernate.entity.base.LongIdEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Per-player ownership + toggle state for a {@link Perk}. Cooldowns
 * for {@link PerkKind#ACTIVE} perks gate on
 * {@code lastActivatedAt + perk.cooldownSeconds}.
 */
@Entity
@Table(
        name = "jexquests_player_perk",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_jexquests_player_perk",
                columnNames = {"player_uuid", "perk_identifier"}
        ),
        indexes = {
                @Index(name = "idx_jexquests_player_perk_player", columnList = "player_uuid"),
                @Index(name = "idx_jexquests_player_perk_identifier", columnList = "perk_identifier")
        }
)
public class PlayerPerk extends LongIdEntity {

    @Column(name = "player_uuid", nullable = false)
    private UUID playerUuid;

    @Column(name = "perk_identifier", nullable = false, length = 64)
    private String perkIdentifier;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Column(name = "unlocked_at", nullable = false)
    private LocalDateTime unlockedAt;

    @Column(name = "last_activated_at")
    private LocalDateTime lastActivatedAt;

    @Column(name = "activation_count", nullable = false)
    private long activationCount;

    protected PlayerPerk() {
    }

    public PlayerPerk(@NotNull UUID playerUuid, @NotNull String perkIdentifier) {
        this.playerUuid = playerUuid;
        this.perkIdentifier = perkIdentifier;
        this.unlockedAt = LocalDateTime.now();
    }

    public @NotNull UUID getPlayerUuid() { return this.playerUuid; }
    public @NotNull String getPerkIdentifier() { return this.perkIdentifier; }
    public boolean isEnabled() { return this.enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public @NotNull LocalDateTime getUnlockedAt() { return this.unlockedAt; }
    public @Nullable LocalDateTime getLastActivatedAt() { return this.lastActivatedAt; }
    public void setLastActivatedAt(@Nullable LocalDateTime lastActivatedAt) { this.lastActivatedAt = lastActivatedAt; }
    public long getActivationCount() { return this.activationCount; }
    public void setActivationCount(long activationCount) { this.activationCount = activationCount; }

    @Override
    public String toString() {
        return "PlayerPerk[" + this.playerUuid + "/" + this.perkIdentifier + "/" + (this.enabled ? "on" : "off") + "]";
    }
}
