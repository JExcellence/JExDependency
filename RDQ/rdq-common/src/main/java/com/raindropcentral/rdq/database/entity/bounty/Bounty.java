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

import de.jexcellence.hibernate.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.io.Serial;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Stores a bounty contract and its attached rewards.
 */
@Entity
@Table(
        name = "r_bounty",
        indexes = {
                @Index(name = "idx_rbounty_target", columnList = "target_unique_id"),
                @Index(name = "idx_rbounty_commissioner", columnList = "commissioner_unique_id"),
                @Index(name = "idx_rbounty_active", columnList = "active"),
                @Index(name = "idx_rbounty_expires", columnList = "expires_at")
        }
)
@Getter
@Setter
public class Bounty extends BaseEntity {

        @Serial
        private static final long serialVersionUID = 1L;

        @Column(name = "target_unique_id", unique = true, nullable = false)
        private UUID targetUniqueId;

        @Column(name = "commissioner_unique_id", nullable = false)
        private UUID commissionerUniqueId;

        @Column(name = "expires_at")
        private LocalDateTime expiresAt;

        @Column(name = "active", nullable = false)
        private boolean active = true;

        @Column(name = "claimed_by")
        private UUID claimedBy;

        @Column(name = "claimed_at")
        private LocalDateTime claimedAt;

        @Column(name = "total_estimated_value", nullable = false)
        private double totalEstimatedValue;

        @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
        @JoinColumn(name = "bounty_id")
        private List<BountyReward> rewards = new ArrayList<>();

        @ElementCollection(targetClass = String.class, fetch = FetchType.EAGER)
        @CollectionTable(name = "r_reward_bounty_history", joinColumns = @JoinColumn(name = "entry_id"))
        @Column(name = "reward_history")
        private Set<String> rewardHistory = new HashSet<>();

        protected Bounty() {}

        /**
         * Executes Bounty.
         */
        public Bounty(@NotNull UUID targetUniqueId, @NotNull UUID commissionerUniqueId) {
                this.targetUniqueId = targetUniqueId;
                this.commissionerUniqueId = commissionerUniqueId;
                this.expiresAt = null; //expires never
        }

        /**
         * Executes Bounty.
         */
        public Bounty(
                @NotNull UUID targetUniqueId,
                @NotNull UUID commissionerUniqueId,
                @NotNull List<BountyReward> rewards
        ) {
                this(targetUniqueId, commissionerUniqueId);
                this.rewards = rewards;
        }

        /**
         * Executes addReward.
         */
        public void addReward(@NotNull BountyReward rewardItem) {

                rewards.add(rewardItem);
        }

        /**
         * Returns whether claimed.
         */
        public boolean isClaimed() {
                return claimedBy != null;
        }

        /**
         * Returns whether expired.
         */
        public boolean isExpired() {
                return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
        }

        /**
         * Executes claim.
         */
        public void claim(
                @NotNull UUID claimedBy
        ) {
                this.claimedBy = claimedBy;
                this.claimedAt = LocalDateTime.now();
                this.active = false;
        }

        /**
         * Executes expire.
         */
        public void expire() {
                this.active = false;
        }
}
