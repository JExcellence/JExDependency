/*
 * Copyright (c) 2021-2026 Antimatter Zone LLC. All rights reserved.
 *
 * This source code is proprietary and confidential to Antimatter Zone LLC.
 * Unauthorized copying, modification, distribution, display, performance,
 * publication, sublicensing, or creation of derivative works is prohibited
 * without prior written permission from Antimatter Zone LLC, except to the
 * extent permitted by applicable United States law.
 *
 * This notice is intended to preserve all rights and remedies available under
 * the laws of the State of Washington and the United States of America.
 */

package com.raindropcentral.rdq.database.entity.rank;

import com.raindropcentral.rdq.config.utility.IconSection;
import com.raindropcentral.rdq.database.converter.IconSectionConverter;
import com.raindropcentral.rdq.database.entity.reward.BaseReward;
import de.jexcellence.hibernate.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Entity representing a single reward for a {@link RRank} in the RaindropQuests system.
 *
 * <p>This entity encapsulates a single {@link BaseReward} that is granted when achieving the associated rank.
 * It also includes an icon for visual representation and display order.
 *
 *
 * <p>Multiple instances of this entity can exist for a single rank, representing different rewards
 * that are granted when the rank is achieved.
 */
@Setter
@Getter
@Entity
@Table(name = "r_rank_reward")
public class RRankReward extends BaseEntity {

    /**
     * The rank to which this reward belongs.
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "rank_id", nullable = false)
    private RRank rank;

    /**
     * The reward that is granted for this rank.
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "reward_id", nullable = false)
    private BaseReward reward;

    /**
     * The icon representing this reward.
     */
    @Column(name = "icon", nullable = false, columnDefinition = "LONGTEXT")
    @Convert(converter = IconSectionConverter.class)
    private IconSection icon;

    /**
     * Optional display order for this reward within the rank's rewards.
     */
    @Column(name = "display_order", nullable = false)
    private int displayOrder = 0;

    /**
     * Whether this reward should be automatically granted when the rank is achieved.
     */
    @Column(name = "auto_grant", nullable = false)
    private boolean autoGrant = true;

    @Version
    @Column(name = "version")
    private int version;

    /**
     * Protected no-argument constructor for JPA.
     */
    public RRankReward() {}

    /**
     * Constructs a new {@code RRankReward} with the specified rank, reward, and icon.
     *
     * @param rank   the {@link RRank} to which this reward belongs
     * @param reward the {@link BaseReward} that is granted
     * @param icon   the icon for this reward
     */
    public RRankReward(
            @Nullable final RRank rank,
            @NotNull final BaseReward reward,
            @NotNull final IconSection icon
    ) {
        this.rank = rank;
        this.reward = reward;
        this.icon = icon;
        this.displayOrder = 0;

        if (rank != null) {
            rank.addReward(this);
        }
    }

    /**
     * Returns the rank to which this reward belongs.
     *
     * @return the associated {@link RRank}
     */
    @NotNull
    public RRank getRank() {
        return this.rank;
    }

    /**
     * Returns the reward that is granted.
     *
     * @return the {@link BaseReward} object
     */
    @NotNull
    public BaseReward getReward() {
        return this.reward;
    }

    /**
     * Sets the reward for this rank reward.
     *
     * @param reward the reward
     */
    public void setReward(@NotNull final BaseReward reward) {
        this.reward = reward;
    }

    /**
     * Convenience method to grant this reward to a player.
     *
     * @param player the player to grant the reward to
     * @return a CompletableFuture indicating success
     */
    public CompletableFuture<Boolean> grant(@NotNull final Player player) {
        return this.reward.grant(player);
    }

    /**
     * Convenience method to get the estimated value of this reward.
     *
     * @return the estimated value
     */
    public double getEstimatedValue() {
        return this.reward.getEstimatedValue();
    }

    /**
     * Executes equals.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RRankReward that)) return false;

        if (this.getId() != null && that.getId() != null) {
            return this.getId().equals(that.getId());
        }

        if (this.reward != null && that.reward != null &&
                this.rank != null && that.rank != null) {
            return this.reward.equals(that.reward) &&
                    this.rank.equals(that.rank) &&
                    this.displayOrder == that.displayOrder;
        }

        return false;
    }

    /**
     * Returns whether hCode.
     */
    @Override
    public int hashCode() {
        if (this.getId() != null) {
            return this.getId().hashCode();
        }

        if (this.reward != null && this.rank != null) {
            return Objects.hash(this.reward, this.rank, this.displayOrder);
        }

        return System.identityHashCode(this);
    }

    /**
     * Enhanced setRank method with better relationship management.
     */
    public void setRank(@Nullable final RRank rank) {
        if (this.rank != null && this.rank != rank) {
            this.rank.getRewards().remove(this);
        }

        this.rank = rank;

        if (rank != null) {
            rank.getRewards().add(this);
        }
    }
}

