package com.raindropcentral.rdq.database.entity.bounty;

import com.raindropcentral.rdq.config.utility.IconSection;
import com.raindropcentral.rdq.database.converter.IconSectionConverter;
import com.raindropcentral.rdq.database.converter.RewardConverter;
import com.raindropcentral.rdq.reward.AbstractReward;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import de.jexcellence.hibernate.entity.AbstractEntity;
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
public class BountyReward extends AbstractEntity {

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
