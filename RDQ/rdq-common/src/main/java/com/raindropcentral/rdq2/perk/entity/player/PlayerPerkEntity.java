package com.raindropcentral.rdq2.perk.entity.player;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "player_perk",
    uniqueConstraints = @UniqueConstraint(columnNames = {"player_id", "perk_id"})
)
public class PlayerPerkEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "player_id", nullable = false)
    private UUID playerId;

    @Column(name = "perk_id", nullable = false)
    private String perkId;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "is_unlocked", nullable = false)
    private boolean unlocked;

    @Column(name = "activation_time")
    private LocalDateTime activationTime;

    @Column(name = "last_triggered")
    private LocalDateTime lastTriggered;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected PlayerPerkEntity() {
    }

    public PlayerPerkEntity(
        @NotNull UUID playerId,
        @NotNull String perkId
    ) {
        this.playerId = playerId;
        this.perkId = perkId;
        this.active = false;
        this.unlocked = false;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public @NotNull UUID getPlayerId() {
        return playerId;
    }

    public void setPlayerId(@NotNull UUID playerId) {
        this.playerId = playerId;
    }

    public @NotNull String getPerkId() {
        return perkId;
    }

    public void setPerkId(@NotNull String perkId) {
        this.perkId = perkId;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isUnlocked() {
        return unlocked;
    }

    public void setUnlocked(boolean unlocked) {
        this.unlocked = unlocked;
    }

    public LocalDateTime getActivationTime() {
        return activationTime;
    }

    public void setActivationTime(LocalDateTime activationTime) {
        this.activationTime = activationTime;
    }

    public LocalDateTime getLastTriggered() {
        return lastTriggered;
    }

    public void setLastTriggered(LocalDateTime lastTriggered) {
        this.lastTriggered = lastTriggered;
    }

    public @NotNull LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(@NotNull LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public @NotNull LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(@NotNull LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
