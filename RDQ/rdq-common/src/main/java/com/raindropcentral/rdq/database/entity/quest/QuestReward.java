package com.raindropcentral.rdq.database.entity.quest;

import com.raindropcentral.rdq.database.converter.IconSectionConverter;
import com.raindropcentral.rdq.database.entity.reward.BaseReward;
import com.raindropcentral.rplatform.config.icon.IconSection;
import de.jexcellence.hibernate.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serial;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Entity representing a single reward for a {@link Quest} in the RaindropQuests system.
 * <p>
 * This entity encapsulates a single {@link BaseReward} that is granted when completing the associated quest.
 * It also includes an icon for visual representation, display order, and auto-grant configuration.
 *
 * <p>
 * Multiple instances of this entity can exist for a single quest, representing different rewards
 * that are granted when the quest is completed.
 *
 * @author JExcellence
 * @version 2.0.0
 * @since TBD
 */
@Entity
@Table(name = "rdq_quest_reward")
public class QuestReward extends BaseEntity {
    
    @Serial
    private static final long serialVersionUID = 1L;
    
    /**
     * The quest to which this reward belongs.
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "quest_id", nullable = false)
    private Quest quest;
    
    /**
     * The reward that is granted for this quest.
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "reward_id", nullable = false)
    private BaseReward reward;
    
    /**
     * The icon representing this reward.
     */
    @Column(name = "icon", nullable = false, columnDefinition = "LONGTEXT")
    @Convert(converter = IconSectionConverter.class)
    private IconSection icon;
    
    /**
     * Optional display order for this reward within the quest's rewards.
     */
    @Column(name = "display_order", nullable = false)
    private int displayOrder = 0;
    
    /**
     * Whether this reward should be automatically granted when the quest is completed.
     */
    @Column(name = "auto_grant", nullable = false)
    private boolean autoGrant = true;
    
    /**
     * Optimistic locking version field.
     */
    @Version
    @Column(name = "version")
    private int version;
    
    /**
     * Protected no-argument constructor for JPA.
     */
    protected QuestReward() {}
    
    /**
     * Constructs a new {@code QuestReward} with the specified quest, reward, and icon.
     *
     * @param quest  the {@link Quest} to which this reward belongs
     * @param reward the {@link BaseReward} that is granted
     * @param icon   the {@link IconSection} used as the icon for this reward
     */
    public QuestReward(
            @Nullable final Quest quest,
            @NotNull final BaseReward reward,
            @NotNull final IconSection icon
    ) {
        this.quest = quest;
        this.reward = reward;
        this.icon = icon;
        
        // NOTE: Do NOT call quest.addReward(this) here!
        // Hibernate will manage the bidirectional relationship automatically
        // when this entity is persisted. Calling addReward() would trigger
        // lazy initialization of the rewards collection outside of a session.
    }
    
    /**
     * Returns the quest to which this reward belongs.
     *
     * @return the associated {@link Quest}
     */
    @NotNull
    public Quest getQuest() {
        return this.quest;
    }
    
    /**
     * Sets the quest for this reward.
     * <p>
     * This method manages the bidirectional relationship between Quest and QuestReward.
     *
     * @param quest the quest to set
     */
    public void setQuest(@Nullable final Quest quest) {
        if (this.quest != null && this.quest != quest) {
            this.quest.getRewards().remove(this);
        }
        
        this.quest = quest;
        
        if (quest != null && !quest.getRewards().contains(this)) {
            quest.getRewards().add(this);
        }
    }
    
    /**
     * Returns the reward that is granted.
     *
     * @return the {@link BaseReward} object
     */
    @NotNull
    public BaseReward getReward() {
        return this.reward;
    }
    
    /**
     * Sets the reward for this quest reward.
     *
     * @param reward the reward
     */
    public void setReward(@NotNull final BaseReward reward) {
        this.reward = reward;
    }
    
    /**
     * Returns the icon for this reward.
     *
     * @return the {@link IconSection}
     */
    @NotNull
    public IconSection getIcon() {
        return this.icon;
    }
    
    /**
     * Sets the icon for this reward.
     *
     * @param icon the icon section
     */
    public void setIcon(@NotNull final IconSection icon) {
        this.icon = icon;
    }
    
    /**
     * Returns the display order for this reward.
     *
     * @return the display order
     */
    public int getDisplayOrder() {
        return this.displayOrder;
    }
    
    /**
     * Sets the display order for this reward.
     *
     * @param displayOrder the display order
     */
    public void setDisplayOrder(final int displayOrder) {
        this.displayOrder = displayOrder;
    }
    
    /**
     * Returns whether this reward should be automatically granted.
     *
     * @return true if auto-grant is enabled, false otherwise
     */
    public boolean isAutoGrant() {
        return this.autoGrant;
    }
    
    /**
     * Sets whether this reward should be automatically granted.
     *
     * @param autoGrant the auto-grant flag
     */
    public void setAutoGrant(final boolean autoGrant) {
        this.autoGrant = autoGrant;
    }
    
    /**
     * Returns the optimistic locking version.
     *
     * @return the version
     */
    public int getVersion() {
        return this.version;
    }
    
    /**
     * Convenience method to grant this reward to a player.
     *
     * @param player the player to grant the reward to
     * @return a CompletableFuture indicating success
     */
    public CompletableFuture<Boolean> grant(@NotNull final Player player) {
        return this.reward.grant(player);
    }
    
    /**
     * Convenience method to get the estimated value of this reward.
     *
     * @return the estimated value
     */
    public double getEstimatedValue() {
        return this.reward.getEstimatedValue();
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof QuestReward that)) return false;
        
        if (this.getId() != null && that.getId() != null) {
            return this.getId().equals(that.getId());
        }
        
        if (this.reward != null && that.reward != null &&
                this.quest != null && that.quest != null) {
            return this.reward.equals(that.reward) &&
                    this.quest.equals(that.quest) &&
                    this.displayOrder == that.displayOrder;
        }
        
        return false;
    }
    
    @Override
    public int hashCode() {
        if (this.getId() != null) {
            return this.getId().hashCode();
        }
        
        if (this.reward != null && this.quest != null) {
            return Objects.hash(this.reward, this.quest, this.displayOrder);
        }
        
        return System.identityHashCode(this);
    }
}
