package com.raindropcentral.rdq.database.entity.quest;

import com.raindropcentral.rdq.config.utility.IconSection;
import com.raindropcentral.rdq.database.converter.IconSectionConverter;
import com.raindropcentral.rdq.database.entity.reward.BaseReward;
import de.jexcellence.hibernate.entity.BaseEntity;
import jakarta.persistence.*;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serial;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Entity representing a single reward for a {@link QuestCategory} in the RaindropQuests system.
 * <p>
 * This entity encapsulates a single {@link BaseReward} that is granted when completing all quests
 * in the associated quest category. It also includes an icon for visual representation, display order,
 * and auto-grant configuration.
 * </p>
 *
 * <p>
 * Multiple instances of this entity can exist for a single category, representing different rewards
 * that are granted when the category is completed.
 * </p>
 *
 * @author JExcellence
 * @version 2.0.0
 * @since TBD
 */
@Entity
@Table(name = "rdq_quest_category_reward")
public class QuestCategoryReward extends BaseEntity {
    
    @Serial
    private static final long serialVersionUID = 1L;
    
    /**
     * The category to which this reward belongs.
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "category_id", nullable = false)
    private QuestCategory category;
    
    /**
     * The reward that is granted for this category.
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
     * Optional display order for this reward within the category's rewards.
     */
    @Column(name = "display_order", nullable = false)
    private int displayOrder = 0;
    
    /**
     * Whether this reward should be automatically granted when the category is completed.
     */
    @Column(name = "auto_grant", nullable = false)
    private boolean autoGrant = true;
    
    /**
     * Protected no-argument constructor for JPA.
     */
    protected QuestCategoryReward() {}
    
    /**
     * Constructs a new {@code QuestCategoryReward} with the specified category, reward, and icon.
     *
     * @param category the {@link QuestCategory} to which this reward belongs
     * @param reward   the {@link BaseReward} that is granted
     * @param icon     the {@link IconSection} used as the icon for this reward
     */
    public QuestCategoryReward(
            @Nullable final QuestCategory category,
            @NotNull final BaseReward reward,
            @NotNull final IconSection icon
    ) {
        this.category = category;
        this.reward = reward;
        this.icon = icon;
        
        if (category != null) {
            category.addReward(this);
        }
    }
    
    /**
     * Returns the category to which this reward belongs.
     *
     * @return the associated {@link QuestCategory}
     */
    @NotNull
    public QuestCategory getCategory() {
        return this.category;
    }
    
    /**
     * Sets the category for this reward.
     * <p>
     * This method manages the bidirectional relationship between QuestCategory and QuestCategoryReward.
     * </p>
     *
     * @param category the category to set
     */
    public void setCategory(@Nullable final QuestCategory category) {
        if (this.category != null && this.category != category) {
            this.category.getRewards().remove(this);
        }
        
        this.category = category;
        
        if (category != null && !category.getRewards().contains(this)) {
            category.getRewards().add(this);
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
     * Sets the reward for this category reward.
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
        if (!(o instanceof QuestCategoryReward that)) return false;
        
        if (this.getId() != null && that.getId() != null) {
            return this.getId().equals(that.getId());
        }
        
        if (this.reward != null && that.reward != null &&
                this.category != null && that.category != null) {
            return this.reward.equals(that.reward) &&
                    this.category.equals(that.category) &&
                    this.displayOrder == that.displayOrder;
        }
        
        return false;
    }
    
    @Override
    public int hashCode() {
        if (this.getId() != null) {
            return this.getId().hashCode();
        }
        
        if (this.reward != null && this.category != null) {
            return Objects.hash(this.reward, this.category, this.displayOrder);
        }
        
        return System.identityHashCode(this);
    }
}
