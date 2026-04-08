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

import com.raindropcentral.rdq.database.entity.player.RDQPlayer;
import de.jexcellence.hibernate.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.io.Serial;

/**
 * Tracks bounty-claim statistics for a player.
 */
@Entity
@Table(
        name = "r_bounty_hunter",
        uniqueConstraints = @UniqueConstraint(columnNames = {"player_id"}),
        indexes = {
                @Index(name = "idx_bounties_claimed", columnList = "bounties_claimed DESC"),
                @Index(name = "idx_total_reward_value", columnList = "total_reward_value DESC")
        }
)
@Getter
@Setter
public class BountyHunter extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "player_id", nullable = false, unique = true)
    private RDQPlayer player;

    @Column(name = "bounties_claimed", nullable = false)
    private int bountiesClaimed = 0;

    @Column(name = "total_reward_value", nullable = false)
    private double totalRewardValue = 0.0;

    @Column(name = "highest_bounty_value", nullable = false)
    private double highestBountyValue = 0.0;

    @Column(name = "last_claim_timestamp")
    private Long lastClaimTimestamp;

    protected BountyHunter() {}

    /**
     * Executes BountyHunter.
     */
    public BountyHunter(final @NotNull RDQPlayer player) {
        this.player = player;
    }

    /**
     * Executes recordClaim.
     */
    public void recordClaim(final double rewardValue) {
        incrementBountiesClaimed();
        addRewardValue(rewardValue);
    }

    /**
     * Executes incrementBountiesClaimed.
     */
    public void incrementBountiesClaimed() {
        this.bountiesClaimed++;
    }

    /**
     * Executes addRewardValue.
     */
    public void addRewardValue(final double value) {
        if (value > 0) {
            this.totalRewardValue += value;
            if (value > this.highestBountyValue) {
                this.highestBountyValue = value;
            }
        }
    }
}
