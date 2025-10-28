package com.raindropcentral.rdq.database.entity.perk;

import de.jexcellence.hibernate.entity.AbstractEntity;
import jakarta.persistence.*;
import org.jetbrains.annotations.NotNull;

import java.io.Serial;
import java.util.Objects;

/**
 * Represents a reward granted to a player when they unlock or receive a perk.
 * Rewards can include currency, items, experience, or custom effects.
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.0
 */
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

    /**
     * Framework-required constructor for JPA.
     */
    protected RPerkReward() {}

    /**
     * Creates a new perk reward.
     *
     * @param perk the perk this reward is associated with
     * @param rewardType the type of reward (e.g., "CURRENCY", "ITEM", "EXPERIENCE")
     * @param rewardValue the value/data for the reward
     * @param descriptionKey the localization key for the reward description
     */
    public RPerkReward(
        final @NotNull RPerk perk,
        final @NotNull String rewardType,
        final @NotNull String rewardValue,
        final @NotNull String descriptionKey
    ) {
        this.perk = Objects.requireNonNull(perk, "perk cannot be null");
        this.rewardType = Objects.requireNonNull(rewardType, "rewardType cannot be null");
        this.rewardValue = Objects.requireNonNull(rewardValue, "rewardValue cannot be null");
        this.descriptionKey = Objects.requireNonNull(descriptionKey, "descriptionKey cannot be null");
    }

    public @NotNull RPerk getPerk() {
        return this.perk;
    }

    public void setPerk(final @NotNull RPerk perk) {
        this.perk = Objects.requireNonNull(perk, "perk cannot be null");
    }

    public @NotNull String getRewardType() {
        return this.rewardType;
    }

    public void setRewardType(final @NotNull String rewardType) {
        this.rewardType = Objects.requireNonNull(rewardType, "rewardType cannot be null");
    }

    public @NotNull String getRewardValue() {
        return this.rewardValue;
    }

    public void setRewardValue(final @NotNull String rewardValue) {
        this.rewardValue = Objects.requireNonNull(rewardValue, "rewardValue cannot be null");
    }

    public @NotNull String getDescriptionKey() {
        return this.descriptionKey;
    }

    public void setDescriptionKey(final @NotNull String descriptionKey) {
        this.descriptionKey = Objects.requireNonNull(descriptionKey, "descriptionKey cannot be null");
    }

    public int getPriority() {
        return this.priority;
    }

    public void setPriority(final int priority) {
        this.priority = priority;
    }

    public boolean isRepeatable() {
        return this.repeatable;
    }

    public void setRepeatable(final boolean repeatable) {
        this.repeatable = repeatable;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof final RPerkReward other)) return false;
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