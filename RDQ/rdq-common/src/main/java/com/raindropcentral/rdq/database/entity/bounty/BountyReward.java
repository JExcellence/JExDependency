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

package com.raindropcentral.rdq.database.entity.bounty;

import com.raindropcentral.rdq.config.utility.IconSection;
import com.raindropcentral.rdq.database.converter.IconSectionConverter;
import com.raindropcentral.rplatform.database.converter.RewardConverter;
import com.raindropcentral.rplatform.reward.AbstractReward;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import de.jexcellence.hibernate.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serial;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Entity
@Table(name = "rdq_bounty_reward")
@Getter
@Setter
public class BountyReward extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @Column(name = "reward_data", nullable = false, columnDefinition = "LONGTEXT")
    @Convert(converter = RewardConverter.class)
    private AbstractReward reward;

    @Convert(converter = IconSectionConverter.class)
    @Column(name = "reward_icon", columnDefinition = "LONGTEXT")
    private IconSection icon;

    @Column(name = "contributor_unique_id")
    private UUID contributorUniqueId;

    @Column(name = "estimated_value")
    private double estimatedValue;

    protected BountyReward() {}

    public BountyReward(@NotNull AbstractReward reward) {
        this.reward = reward;
        this.contributorUniqueId = null;
        this.icon = new IconSection(new EvaluationEnvironmentBuilder());
    }

    public BountyReward(@NotNull AbstractReward reward, @NotNull UUID contributorUniqueId) {
        this.reward = reward;
        this.contributorUniqueId = contributorUniqueId;
        this.icon = new IconSection(new EvaluationEnvironmentBuilder());
    }

    public BountyReward(@NotNull AbstractReward reward, @NotNull IconSection icon, @Nullable UUID contributorUniqueId) {
        this.reward = reward;
        this.icon = icon;
        this.contributorUniqueId = contributorUniqueId;
    }

    public @NotNull CompletableFuture<Boolean> grant(@NotNull Player player) {
        return reward.grant(player);
    }

    /**
     * Gets the reward instance.
     *
     * @return the abstract reward
     */
    public AbstractReward getReward() {
        return reward;
    }

    /**
     * Gets the contributor's unique ID.
     *
     * @return the contributor UUID, or null if no contributor
     */
    public UUID getContributorUniqueId() {
        return contributorUniqueId;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof BountyReward other)) return false;
        return Objects.equals(getId(), other.getId());
    }

    @Override
    public int hashCode() { 
        return Objects.hash(getId()); 
    }

    @Override
    public String toString() {
        return "BountyReward[id=%d, rewardData=%s]".formatted(getId(), reward.toString());
    }
}
