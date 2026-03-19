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
 * Entity representing a single reward for a {@link QuestTask} in the RaindropQuests system.
 * <p>
 * This entity encapsulates a single {@link BaseReward} that is granted when completing the associated quest task.
 * It also includes an icon for visual representation, display order, and auto-grant configuration.
 * </p>
 *
 * <p>
 * Multiple instances of this entity can exist for a single task, representing different rewards
 * that are granted when the task is completed.
 * </p>
 *
 * @author JExcellence
 * @version 2.0.0
 * @since TBD
 */
@Entity
@Table(name = "rdq_quest_task_reward")
public class QuestTaskReward extends BaseEntity {
    
    @Serial
    private static final long serialVersionUID = 1L;
    
    /**
     * The task to which this reward belongs.
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "task_id", nullable = false)
    private QuestTask task;
    
    /**
     * The reward that is granted for this task.
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
     * Optional display order for this reward within the task's rewards.
     */
    @Column(name = "display_order", nullable = false)
    private int displayOrder = 0;
    
    /**
     * Whether this reward should be automatically granted when the task is completed.
     */
    @Column(name = "auto_grant", nullable = false)
    private boolean autoGrant = true;
    
    /**
     * Protected no-argument constructor for JPA.
     */
    protected QuestTaskReward() {}
    
    /**
     * Constructs a new {@code QuestTaskReward} with the specified task, reward, and icon.
     *
     * @param task   the {@link QuestTask} to which this reward belongs
     * @param reward the {@link BaseReward} that is granted
     * @param icon   the {@link IconSection} used as the icon for this reward
     */
    public QuestTaskReward(
            @Nullable final QuestTask task,
            @NotNull final BaseReward reward,
            @NotNull final IconSection icon
    ) {
        this.task = task;
        this.reward = reward;
        this.icon = icon;
        
        if (task != null) {
            task.addReward(this);
        }
    }
    
    /**
     * Returns the task to which this reward belongs.
     *
     * @return the associated {@link QuestTask}
     */
    @NotNull
    public QuestTask getTask() {
        return this.task;
    }
    
    /**
     * Sets the task for this reward.
     * <p>
     * This method manages the bidirectional relationship between QuestTask and QuestTaskReward.
     * </p>
     *
     * @param task the task to set
     */
    public void setTask(@Nullable final QuestTask task) {
        if (this.task != null && this.task != task) {
            this.task.getRewards().remove(this);
        }
        
        this.task = task;
        
        if (task != null && !task.getRewards().contains(this)) {
            task.getRewards().add(this);
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
     * Sets the reward for this task reward.
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
        if (!(o instanceof QuestTaskReward that)) return false;
        
        if (this.getId() != null && that.getId() != null) {
            return this.getId().equals(that.getId());
        }
        
        if (this.reward != null && that.reward != null &&
                this.task != null && that.task != null) {
            return this.reward.equals(that.reward) &&
                    this.task.equals(that.task) &&
                    this.displayOrder == that.displayOrder;
        }
        
        return false;
    }
    
    @Override
    public int hashCode() {
        if (this.getId() != null) {
            return this.getId().hashCode();
        }
        
        if (this.reward != null && this.task != null) {
            return Objects.hash(this.reward, this.task, this.displayOrder);
        }
        
        return System.identityHashCode(this);
    }
}
