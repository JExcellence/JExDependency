package com.raindropcentral.rdq.database.entity.bounty;

import de.jexcellence.hibernate.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.io.Serial;
import java.time.LocalDateTime;
import java.util.*;

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

        public Bounty(@NotNull UUID targetUniqueId, @NotNull UUID commissionerUniqueId) {
                this.targetUniqueId = targetUniqueId;
                this.commissionerUniqueId = commissionerUniqueId;
                this.expiresAt = null; //expires never
        }

        public Bounty(
                @NotNull UUID targetUniqueId,
                @NotNull UUID commissionerUniqueId,
                @NotNull List<BountyReward> rewards
        ) {
                this(targetUniqueId, commissionerUniqueId);
                this.rewards = rewards;
        }

        public void addReward(@NotNull BountyReward rewardItem) {

                rewards.add(rewardItem);
        }

        public boolean isClaimed() {
                return claimedBy != null;
        }

        public boolean isExpired() {
                return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
        }

        public void claim(
                @NotNull UUID claimedBy
        ) {
                this.claimedBy = claimedBy;
                this.claimedAt = LocalDateTime.now();
                this.active = false;
        }

        public void expire() {
                this.active = false;
        }
}
