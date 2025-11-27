package com.raindropcentral.rdq.database.entity.perk;

import de.jexcellence.hibernate.entity.AbstractEntity;
import jakarta.persistence.*;
import org.jetbrains.annotations.NotNull;

import java.io.Serial;
import java.util.Objects;

@Entity
@Table(name = "r_perk_reward")
public final class RPerkReward extends AbstractEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "perk_id", nullable = false)
    private RPerk perk;

    @Column(name = "reward_type", nullable = false, length = 64)
    private String rewardType;

    @Column(name = "reward_value", nullable = false)
    private String rewardValue;

    @Column(name = "description_key", nullable = false)
    private String descriptionKey;

    @Column(name = "priority", nullable = false)
    private int priority = 0;

    @Column(name = "is_repeatable", nullable = false)
    private boolean repeatable = false;

    protected RPerkReward() {}

    public RPerkReward(
        @NotNull RPerk perk,
        @NotNull String rewardType,
        @NotNull String rewardValue,
        @NotNull String descriptionKey
    ) {
        this.perk = Objects.requireNonNull(perk, "perk cannot be null");
        this.rewardType = Objects.requireNonNull(rewardType, "rewardType cannot be null");
        this.rewardValue = Objects.requireNonNull(rewardValue, "rewardValue cannot be null");
        this.descriptionKey = Objects.requireNonNull(descriptionKey, "descriptionKey cannot be null");
    }

    public @NotNull RPerk getPerk() {
        return this.perk;
    }

    public void setPerk(@NotNull RPerk perk) {
        this.perk = Objects.requireNonNull(perk, "perk cannot be null");
    }

    public @NotNull String getRewardType() {
        return this.rewardType;
    }

    public void setRewardType(@NotNull String rewardType) {
        this.rewardType = Objects.requireNonNull(rewardType, "rewardType cannot be null");
    }

    public @NotNull String getRewardValue() {
        return this.rewardValue;
    }

    public void setRewardValue(@NotNull String rewardValue) {
        this.rewardValue = Objects.requireNonNull(rewardValue, "rewardValue cannot be null");
    }

    public @NotNull String getDescriptionKey() {
        return this.descriptionKey;
    }

    public void setDescriptionKey(@NotNull String descriptionKey) {
        this.descriptionKey = Objects.requireNonNull(descriptionKey, "descriptionKey cannot be null");
    }

    public int getPriority() {
        return this.priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public boolean isRepeatable() {
        return this.repeatable;
    }

    public void setRepeatable(boolean repeatable) {
        this.repeatable = repeatable;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof RPerkReward other)) return false;
        return Objects.equals(this.perk, other.perk) && 
               Objects.equals(this.rewardType, other.rewardType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.perk, this.rewardType);
    }

    @Override
    public String toString() {
        return "RPerkReward[perk=%s, type=%s, repeatable=%b]"
            .formatted(perk != null ? perk.getIdentifier() : "null", rewardType, repeatable);
    }
}