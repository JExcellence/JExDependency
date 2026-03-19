package de.jexcellence.glow.database.entity;

import de.jexcellence.hibernate.entity.BaseEntity;
import jakarta.persistence.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing a player's glow state.
 * <p>
 * This entity is mapped to the {@code jexglow_player_glow} table and stores
 * whether a player has the glowing effect enabled, along with metadata about
 * when and by whom the glow was enabled.
 * </p>
 *
 * @author JExcellence
 * @version 1.0.0
 */
@Entity
@Table(
        name = "jexglow_player_glow",
        indexes = {
                @Index(name = "idx_player_uuid", columnList = "player_uuid")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_player_uuid", columnNames = "player_uuid")
        }
)
public class PlayerGlow extends BaseEntity {

    @Column(name = "player_uuid", nullable = false, unique = true)
    private UUID playerUuid;

    @Column(name = "glow_enabled", nullable = false)
    private boolean glowEnabled = false;

    @Column(name = "enabled_at")
    private LocalDateTime enabledAt;

    @Column(name = "enabled_by")
    private UUID enabledBy;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    /**
     * Protected no-argument constructor for JPA/Hibernate.
     */
    protected PlayerGlow() {}

    /**
     * Constructs a new PlayerGlow entity with the specified player UUID.
     *
     * @param playerUuid the UUID of the player
     */
    public PlayerGlow(@NotNull UUID playerUuid) {
        this.playerUuid = playerUuid;
        this.glowEnabled = false;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Constructs a new PlayerGlow entity with the specified player UUID and glow state.
     *
     * @param playerUuid  the UUID of the player
     * @param glowEnabled whether glow is enabled
     * @param enabledBy   the UUID of the admin who enabled the glow (nullable)
     */
    public PlayerGlow(@NotNull UUID playerUuid, boolean glowEnabled, @Nullable UUID enabledBy) {
        this.playerUuid = playerUuid;
        this.glowEnabled = glowEnabled;
        this.enabledBy = enabledBy;
        this.enabledAt = glowEnabled ? LocalDateTime.now() : null;
        this.updatedAt = LocalDateTime.now();
    }

    // ==================== Getters/Setters ====================

    public @NotNull UUID getPlayerUuid() {
        return playerUuid;
    }

    public void setPlayerUuid(@NotNull UUID playerUuid) {
        this.playerUuid = playerUuid;
    }

    public boolean isGlowEnabled() {
        return glowEnabled;
    }

    public void setGlowEnabled(boolean glowEnabled) {
        this.glowEnabled = glowEnabled;
        this.updatedAt = LocalDateTime.now();
        if (glowEnabled && this.enabledAt == null) {
            this.enabledAt = LocalDateTime.now();
        }
    }

    public @Nullable LocalDateTime getEnabledAt() {
        return enabledAt;
    }

    public void setEnabledAt(@Nullable LocalDateTime enabledAt) {
        this.enabledAt = enabledAt;
    }

    public @Nullable UUID getEnabledBy() {
        return enabledBy;
    }

    public void setEnabledBy(@Nullable UUID enabledBy) {
        this.enabledBy = enabledBy;
    }

    public @NotNull LocalDateTime getUpdatedAt() {
        return updatedAt != null ? updatedAt : LocalDateTime.now();
    }

    public void setUpdatedAt(@NotNull LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    // ==================== Utility Methods ====================

    /**
     * Enables the glow effect for this player.
     *
     * @param adminUuid the UUID of the admin enabling the glow
     */
    public void enableGlow(@Nullable UUID adminUuid) {
        this.glowEnabled = true;
        this.enabledAt = LocalDateTime.now();
        this.enabledBy = adminUuid;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Disables the glow effect for this player.
     */
    public void disableGlow() {
        this.glowEnabled = false;
        this.updatedAt = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PlayerGlow that)) return false;
        return getId() != null && getId().equals(that.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "PlayerGlow{" +
                "playerUuid=" + playerUuid +
                ", glowEnabled=" + glowEnabled +
                ", enabledAt=" + enabledAt +
                ", enabledBy=" + enabledBy +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
