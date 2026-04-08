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

package com.raindropcentral.rdq.database.entity.reward;

import com.raindropcentral.rdq.database.converter.IconSectionConverter;
import com.raindropcentral.rplatform.config.icon.IconSection;
import com.raindropcentral.rplatform.reward.AbstractReward;
import com.raindropcentral.rplatform.reward.json.RewardParser;
import de.jexcellence.hibernate.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

/**
 * Entity representing a reward in the RaindropQuests system.
 *
 * <p>This entity encapsulates an {@link AbstractReward} from RPlatform and its visual icon,
 * providing convenience methods for reward granting.
 */
@Entity
@Table(name = "r_reward")
@Getter
@Setter
public class BaseReward extends BaseEntity {

    private static final Logger LOGGER = LoggerFactory.getLogger("RDQ");

    @Setter(lombok.AccessLevel.NONE)
    @Getter(lombok.AccessLevel.PUBLIC)
    @Column(name = "reward_data", nullable = false, columnDefinition = "LONGTEXT")
    private String rewardJson;

    @Setter(lombok.AccessLevel.NONE)
    @Transient
    private AbstractReward cachedReward;

    /**
     * Optional description for this reward.
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Convert(converter = IconSectionConverter.class)
    @Column(name = "reward_icon", nullable = false, columnDefinition = "LONGTEXT")
    private IconSection icon;

    /**
     * Whether this reward is currently active.
     */
    @Column(name = "active", nullable = false)
    private boolean active = true;

    /**
     * Optional category for grouping rewards.
     */
    @Column(name = "category", length = 100)
    private String category;

    protected BaseReward() {
    }

    /**
     * Executes BaseReward.
     */
    public BaseReward(
            @NotNull AbstractReward reward,
            @NotNull IconSection icon
    ) {
        if (reward == null) {
            throw new IllegalArgumentException("Reward cannot be null");
        }
        if (icon == null) {
            throw new IllegalArgumentException("Icon cannot be null");
        }
        setReward(reward);
        this.description = reward.getDescriptionKey();
        this.icon = icon;
    }

    /**
     * Gets reward.
     */
    public AbstractReward getReward() {
        if (cachedReward == null && rewardJson != null) {
            try {
                cachedReward = RewardParser.parse(rewardJson);
            } catch (Exception e) {
                LOGGER.error("Failed to parse reward JSON", e);
                throw new RuntimeException("Failed to parse reward", e);
            }
        }
        return cachedReward;
    }

    /**
     * Sets reward.
     */
    public void setReward(@NotNull AbstractReward reward) {
        this.cachedReward = reward;
        try {
            this.rewardJson = RewardParser.serialize(reward);
        } catch (Exception e) {
            LOGGER.error("Failed to serialize reward", e);
            throw new RuntimeException("Failed to serialize reward", e);
        }
    }

    /**
     * Executes grant.
     */
    public CompletableFuture<Boolean> grant(@NotNull Player player) {
        return getReward().grant(player);
    }

    /**
     * Gets estimatedValue.
     */
    public double getEstimatedValue() {
        return getReward().getEstimatedValue();
    }

    /**
     * Gets typeId.
     */
    public String getTypeId() {
        return getReward().getTypeId();
    }
}
