package com.raindropcentral.rdq.database.entity.quest;

import com.raindropcentral.rplatform.config.icon.IconSection;
import com.raindropcentral.rdq.database.converter.IconSectionConverter;
import com.raindropcentral.rdq.database.entity.requirement.BaseRequirement;
import de.jexcellence.hibernate.entity.BaseEntity;
import jakarta.persistence.*;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serial;
import java.util.Objects;

/**
 * Entity representing a single requirement for a {@link QuestTask} in the RaindropQuests system.
 * <p>
 * This entity encapsulates a single {@link BaseRequirement} that must be satisfied
 * to complete the associated quest task. It also includes an icon for visual representation
 * and display order for UI presentation.
 * </p>
 *
 * <p>
 * Multiple instances of this entity can exist for a single task, representing different requirements
 * that all need to be completed for the task to be marked as complete.
 * </p>
 *
 * @author JExcellence
 * @version 2.0.0
 * @since TBD
 */
@Entity
@Table(name = "rdq_quest_task_requirement")
public class QuestTaskRequirement extends BaseEntity {
    
    @Serial
    private static final long serialVersionUID = 1L;
    
    /**
     * The task to which this requirement belongs.
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "task_id", nullable = false)
    private QuestTask task;
    
    /**
     * The requirement that must be satisfied for this task.
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "requirement_id", nullable = false)
    private BaseRequirement requirement;
    
    /**
     * The icon representing this requirement, stored as a serialized {@link IconSection}.
     */
    @Column(name = "icon", nullable = false, columnDefinition = "LONGTEXT")
    @Convert(converter = IconSectionConverter.class)
    private IconSection icon;
    
    /**
     * Optional display order for this requirement within the task's requirements.
     */
    @Column(name = "display_order", nullable = false)
    private int displayOrder = 0;
    
    /**
     * Protected no-argument constructor for JPA.
     */
    protected QuestTaskRequirement() {}
    
    /**
     * Constructs a new {@code QuestTaskRequirement} with the specified task, requirement, and icon.
     *
     * @param task        the {@link QuestTask} to which this requirement belongs
     * @param requirement the {@link BaseRequirement} that must be satisfied
     * @param icon        the {@link IconSection} used as the icon for this requirement
     */
    public QuestTaskRequirement(
            @Nullable final QuestTask task,
            @NotNull final BaseRequirement requirement,
            @NotNull final IconSection icon
    ) {
        this.task = task;
        this.requirement = requirement;
        this.icon = icon;
        
        // NOTE: Do NOT call task.addRequirement(this) here!
        // Hibernate will manage the bidirectional relationship automatically.
    }
    
    /**
     * Returns the task to which this requirement belongs.
     *
     * @return the associated {@link QuestTask}
     */
    @NotNull
    public QuestTask getTask() {
        return this.task;
    }
    
    /**
     * Sets the task for this requirement.
     * <p>
     * This method manages the bidirectional relationship between QuestTask and QuestTaskRequirement.
     * </p>
     *
     * @param task the task to set
     */
    public void setTask(@Nullable final QuestTask task) {
        if (this.task != null && this.task != task) {
            this.task.getRequirements().remove(this);
        }
        
        this.task = task;
        
        if (task != null && !task.getRequirements().contains(this)) {
            task.getRequirements().add(this);
        }
    }
    
    /**
     * Returns the requirement that must be satisfied.
     *
     * @return the {@link BaseRequirement} object
     */
    @NotNull
    public BaseRequirement getRequirement() {
        return this.requirement;
    }
    
    /**
     * Sets the requirement for this task requirement.
     *
     * @param requirement the requirement
     */
    public void setRequirement(@NotNull final BaseRequirement requirement) {
        this.requirement = requirement;
    }
    
    /**
     * Returns the icon for this requirement.
     *
     * @return the {@link IconSection}
     */
    @NotNull
    public IconSection getIcon() {
        return this.icon;
    }
    
    /**
     * Sets the icon for this requirement.
     *
     * @param icon the icon section
     */
    public void setIcon(@NotNull final IconSection icon) {
        this.icon = icon;
    }
    
    /**
     * Returns the display order for this requirement.
     *
     * @return the display order
     */
    public int getDisplayOrder() {
        return this.displayOrder;
    }
    
    /**
     * Sets the display order for this requirement.
     *
     * @param displayOrder the display order
     */
    public void setDisplayOrder(final int displayOrder) {
        this.displayOrder = displayOrder;
    }
    
    /**
     * Convenience method to check if this requirement is met for a player.
     *
     * @param player the player to check against
     * @return {@code true} if the requirement is met, {@code false} otherwise
     */
    public boolean isMet(@NotNull final Player player) {
        return this.requirement.isMet(player);
    }
    
    /**
     * Convenience method to calculate the progress for this requirement.
     *
     * @param player the player to calculate progress for
     * @return the progress value between 0.0 and 1.0
     */
    public double calculateProgress(@NotNull final Player player) {
        return this.requirement.calculateProgress(player);
    }
    
    /**
     * Convenience method to consume resources for this requirement.
     *
     * @param player the player from whom to consume resources
     */
    public void consume(@NotNull final Player player) {
        this.requirement.consume(player);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof QuestTaskRequirement that)) return false;
        
        if (this.getId() != null && that.getId() != null) {
            return this.getId().equals(that.getId());
        }
        
        if (this.requirement != null && that.requirement != null &&
                this.task != null && that.task != null) {
            return this.requirement.equals(that.requirement) &&
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
        
        if (this.requirement != null && this.task != null) {
            return Objects.hash(this.requirement, this.task, this.displayOrder);
        }
        
        return System.identityHashCode(this);
    }
}
