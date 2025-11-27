package com.raindropcentral.rdq.database.entity.rank;

import com.raindropcentral.rdq.config.item.IconSection;
import com.raindropcentral.rdq.database.converter.IconSectionConverter;
import com.raindropcentral.rdq.database.converter.RewardConverter;
import com.raindropcentral.rdq.reward.AbstractReward;
import de.jexcellence.hibernate.entity.AbstractEntity;
import jakarta.persistence.*;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.Serial;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@Entity
@Table(name = "r_reward")
public final class RReward extends AbstractEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @Column(name = "reward_data", nullable = false, columnDefinition = "LONGTEXT")
    @Convert(converter = RewardConverter.class)
    private AbstractReward reward;

    @Convert(converter = IconSectionConverter.class)
    @Column(name = "reward_icon", nullable = false, columnDefinition = "LONGTEXT")
    private IconSection icon;

    protected RReward() {}

    public RReward(@NotNull AbstractReward reward, @NotNull IconSection icon) {
        this.reward = Objects.requireNonNull(reward, "reward cannot be null");
        this.icon = Objects.requireNonNull(icon, "icon cannot be null");
    }

    public @NotNull AbstractReward getReward() { return reward; }
    public void setReward(@NotNull AbstractReward reward) { 
        this.reward = Objects.requireNonNull(reward, "reward cannot be null"); 
    }

    public @NotNull IconSection getShowcase() { return icon; }
    public void setShowcase(@NotNull IconSection icon) { 
        this.icon = Objects.requireNonNull(icon, "icon cannot be null"); 
    }

    public @NotNull CompletableFuture<Boolean> grant(@NotNull Player player) {
        Objects.requireNonNull(player, "player cannot be null");
        return reward.grant(player);
    }

    public double getEstimatedValue() { return reward.getEstimatedValue(); }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof RReward other)) return false;
        return Objects.equals(getId(), other.getId());
    }

    @Override
    public int hashCode() { return Objects.hash(getId()); }

    @Override
    public String toString() {
        return "RReward[id=%d, reward=%s]".formatted(getId(), reward.getClass().getSimpleName());
    }
}
