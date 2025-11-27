package com.raindropcentral.rdq.database.entity.perk;

import de.jexcellence.hibernate.entity.AbstractEntity;
import jakarta.persistence.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serial;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Entity tracking progress toward perk requirements.
 *
 * @author JExcellence
 * @since 6.0.0
 */
@Entity
@Table(
    name = "rdq_perk_progress",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"player_id", "perk_id", "requirement_type", "requirement_key"})
    },
    indexes = {
        @Index(name = "idx_perk_progress_player", columnList = "player_id"),
        @Index(name = "idx_perk_progress_perk", columnList = "perk_id"),
        @Index(name = "idx_perk_progress_completed", columnList = "completed")
    }
)
public class PerkProgressEntity extends AbstractEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @Column(name = "player_id", nullable = false)
    private UUID playerId;

    @Column(name = "perk_id", nullable = false, length = 64)
    private String perkId;

    @Column(name = "requirement_type", nullable = false, length = 32)
    private String requirementType;

    @Column(name = "requirement_key", nullable = false, length = 128)
    private String requirementKey;

    @Column(name = "current_value", precision = 19, scale = 4)
    private BigDecimal currentValue;

    @Column(name = "target_value", precision = 19, scale = 4)
    private BigDecimal targetValue;

    @Column(name = "completed", nullable = false)
    private boolean completed;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected PerkProgressEntity() {
    }

    public PerkProgressEntity(
        @NotNull UUID playerId,
        @NotNull String perkId,
        @NotNull String requirementType,
        @NotNull String requirementKey,
        @Nullable BigDecimal currentValue,
        @Nullable BigDecimal targetValue,
        boolean completed,
        @Nullable Instant completedAt,
        @NotNull Instant updatedAt
    ) {
        this.playerId = Objects.requireNonNull(playerId, "playerId");
        this.perkId = Objects.requireNonNull(perkId, "perkId");
        this.requirementType = Objects.requireNonNull(requirementType, "requirementType");
        this.requirementKey = Objects.requireNonNull(requirementKey, "requirementKey");
        this.currentValue = currentValue;
        this.targetValue = targetValue;
        this.completed = completed;
        this.completedAt = completedAt;
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
    }

    @NotNull
    public static PerkProgressEntity create(
        @NotNull UUID playerId,
        @NotNull String perkId,
        @NotNull String requirementType,
        @NotNull String requirementKey,
        @NotNull BigDecimal targetValue
    ) {
        return new PerkProgressEntity(
            playerId, perkId, requirementType, requirementKey,
            BigDecimal.ZERO, targetValue, false, null, Instant.now()
        );
    }

    @NotNull
    public UUID playerId() {
        return playerId;
    }

    @NotNull
    public String perkId() {
        return perkId;
    }

    @NotNull
    public String requirementType() {
        return requirementType;
    }

    @NotNull
    public String requirementKey() {
        return requirementKey;
    }

    @Nullable
    public BigDecimal currentValue() {
        return currentValue;
    }

    @Nullable
    public BigDecimal targetValue() {
        return targetValue;
    }

    public boolean completed() {
        return completed;
    }

    @Nullable
    public Instant completedAt() {
        return completedAt;
    }

    @NotNull
    public Instant updatedAt() {
        return updatedAt;
    }

    public void setCurrentValue(@Nullable BigDecimal currentValue) {
        this.currentValue = currentValue;
        this.updatedAt = Instant.now();
        checkCompletion();
    }

    public void incrementValue(@NotNull BigDecimal amount) {
        if (this.currentValue == null) {
            this.currentValue = amount;
        } else {
            this.currentValue = this.currentValue.add(amount);
        }
        this.updatedAt = Instant.now();
        checkCompletion();
    }

    public void markCompleted() {
        if (!this.completed) {
            this.completed = true;
            this.completedAt = Instant.now();
            this.updatedAt = Instant.now();
        }
    }

    private void checkCompletion() {
        if (!completed && currentValue != null && targetValue != null) {
            if (currentValue.compareTo(targetValue) >= 0) {
                markCompleted();
            }
        }
    }

    public double getProgressPercentage() {
        if (completed) return 100.0;
        if (currentValue == null || targetValue == null || targetValue.compareTo(BigDecimal.ZERO) == 0) {
            return 0.0;
        }
        return currentValue.divide(targetValue, 4, java.math.RoundingMode.HALF_UP)
            .multiply(BigDecimal.valueOf(100))
            .doubleValue();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PerkProgressEntity that)) return false;
        return Objects.equals(getId(), that.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId());
    }

    @Override
    public String toString() {
        return "PerkProgressEntity[playerId=" + playerId + ", perkId=" + perkId + 
               ", type=" + requirementType + ", key=" + requirementKey + 
               ", progress=" + currentValue + "/" + targetValue + "]";
    }
}
