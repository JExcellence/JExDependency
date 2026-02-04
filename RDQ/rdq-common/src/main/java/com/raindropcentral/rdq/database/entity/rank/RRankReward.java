package com.raindropcentral.rdq.database.entity.rank;

import com.raindropcentral.rplatform.reward.AbstractReward;
import de.jexcellence.hibernate.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

@Setter
@Getter
@Entity
@Table(name = "r_rank_reward")
public class RRankReward extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rank_id", nullable = false)
    private RRank rank;

    @Convert(converter = com.raindropcentral.rplatform.database.converter.RewardConverter.class)
    @Column(name = "reward_data", columnDefinition = "TEXT", nullable = false)
    private AbstractReward reward;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(name = "auto_grant", nullable = false)
    private boolean autoGrant = true;

    public RRankReward() {}

    public RRankReward(@NotNull RRank rank, @NotNull AbstractReward reward) {
        this.rank = rank;
        this.reward = reward;
        this.displayOrder = 0;
    }

    public @NotNull RRank getRank() {
        return rank;
    }

    public void setRank(@NotNull RRank rank) {
        this.rank = rank;
    }

    public @NotNull AbstractReward getReward() {
        return reward;
    }

    public void setReward(@NotNull AbstractReward reward) {
        this.reward = reward;
    }

}
