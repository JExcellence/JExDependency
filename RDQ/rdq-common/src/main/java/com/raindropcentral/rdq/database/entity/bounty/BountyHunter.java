package com.raindropcentral.rdq.database.entity.bounty;

import com.raindropcentral.rdq.database.entity.player.RDQPlayer;
import de.jexcellence.hibernate.entity.AbstractEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.io.Serial;

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
public class BountyHunter extends AbstractEntity {

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

    public BountyHunter(final @NotNull RDQPlayer player) {
        this.player = player;
    }

    public void recordClaim(final double rewardValue) {
        incrementBountiesClaimed();
        addRewardValue(rewardValue);
    }

    public void incrementBountiesClaimed() {
        this.bountiesClaimed++;
    }

    public void addRewardValue(final double value) {
        if (value > 0) {
            this.totalRewardValue += value;
            if (value > this.highestBountyValue) {
                this.highestBountyValue = value;
            }
        }
    }
}
