package com.raindropcentral.rdq.database.entity.perk;

import com.raindropcentral.rdq.config.utility.IconSection;
import com.raindropcentral.rdq.database.converter.IconSectionConverter;
import com.raindropcentral.rplatform.database.converter.RewardConverter;
import com.raindropcentral.rplatform.reward.AbstractReward;
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
 * Entity representing a reward granted when a perk is unlocked.
 * <p>
 * This entity encapsulates an {@link AbstractReward} from RPlatform and its visual icon,
 * providing convenience methods for reward granting.
 * </p>
 *
 * @author JExcellence
 * @version 1.0.0
 */
@Setter
@Getter
@Entity
@Table(name = "rdq_perk_unlock_reward")
public class PerkUnlockReward extends BaseEntity {

    /**
     * The perk to which this reward belongs.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "perk_id", nullable = false)
    private Perk perk;

    /**
     * Display order for this reward within the perk's unlock rewards.
     */
    @Column(name = "display_order", nullable = false)
    private int displayOrder = 0;

    /**
     * The reward definition stored as JSON.
     */
    @Convert(converter = RewardConverter.class)
    @Column(name = "reward_data", nullable = false, columnDefinition = "LONGTEXT")
    private AbstractReward reward;

    /**
     * The icon representing this reward in the UI.
     */
    @Convert(converter = IconSectionConverter.class)
    @Column(name = "icon", columnDefinition = "LONGTEXT")
    private IconSection icon;

    /**
     * Version for optimistic locking.
     */
    @Version
    @Column(name = "version")
    private int version;

    /**
     * Protected no-argument constructor for JPA.
     */
    protected PerkUnlockReward() {
    }

    /**
     * Constructs a new {@code PerkUnlockReward} with the specified perk, reward, and icon.
     *
     * @param perk   the perk to which this reward belongs
     * @param reward the reward definition
     * @param icon   the icon for UI display
     */
    public PerkUnlockReward(
            @Nullable final Perk perk,
            @NotNull final AbstractReward reward,
            @Nullable final IconSection icon
    ) {
        this.perk = perk;
        this.reward = reward;
        this.icon = icon;

        if (perk != null) {
            perk.addUnlockReward(this);
        }
    }

    /**
     * Grants this reward to the specified player.
     *
     * @param player the player to grant the reward to
     * @return a CompletableFuture indicating success
     */
    public CompletableFuture<Boolean> grant(@NotNull final Player player) {
        return reward.grant(player);
    }

    /**
     * Gets the estimated value of this reward.
     *
     * @return the estimated value
     */
    public double getEstimatedValue() {
        return reward.getEstimatedValue();
    }

    /**
     * Gets the type identifier of this reward.
     *
     * @return the reward type ID
     */
    public String getTypeId() {
        return reward.getTypeId();
    }

    /**
     * Gets the description key for this reward.
     *
     * @return the i18n description key
     */
    public String getDescriptionKey() {
        return reward.getDescriptionKey();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PerkUnlockReward that)) return false;

        if (this.getId() != null && that.getId() != null) {
            return this.getId().equals(that.getId());
        }

        return perk != null && perk.equals(that.perk) &&
                reward != null && reward.equals(that.reward) &&
                displayOrder == that.displayOrder;
    }

    @Override
    public int hashCode() {
        if (this.getId() != null) {
            return this.getId().hashCode();
        }

        return Objects.hash(perk, reward, displayOrder);
    }

    @Override
    public String toString() {
        return "PerkUnlockReward{" +
                "id=" + getId() +
                ", perk=" + (perk != null ? perk.getIdentifier() : null) +
                ", displayOrder=" + displayOrder +
                ", rewardType=" + (reward != null ? reward.getTypeId() : null) +
                '}';
    }
}
