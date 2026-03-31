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
 * Entity representing a single requirement for a {@link QuestCategory} in the RaindropQuests system.
 * <p>
 * This entity encapsulates a single {@link BaseRequirement} that must be satisfied
 * to access the associated quest category. It also includes an icon for visual representation
 * and display order for UI presentation.
 * </p>
 *
 * <p>
 * Multiple instances of this entity can exist for a single category, representing different requirements
 * that all need to be completed for the category to become accessible.
 * </p>
 *
 * @author JExcellence
 * @version 2.0.0
 * @since TBD
 */
@Entity
@Table(name = "rdq_quest_category_requirement")
public class QuestCategoryRequirement extends BaseEntity {
    
    @Serial
    private static final long serialVersionUID = 1L;
    
    /**
     * The category to which this requirement belongs.
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "category_id", nullable = false)
    private QuestCategory category;
    
    /**
     * The requirement that must be satisfied for this category.
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
     * Optional display order for this requirement within the category's requirements.
     */
    @Column(name = "display_order", nullable = false)
    private int displayOrder = 0;
    
    /**
     * Protected no-argument constructor for JPA.
     */
    protected QuestCategoryRequirement() {}
    
    /**
     * Constructs a new {@code QuestCategoryRequirement} with the specified category, requirement, and icon.
     *
     * @param category    the {@link QuestCategory} to which this requirement belongs
     * @param requirement the {@link BaseRequirement} that must be satisfied
     * @param icon        the {@link IconSection} used as the icon for this requirement
     */
    public QuestCategoryRequirement(
            @Nullable final QuestCategory category,
            @NotNull final BaseRequirement requirement,
            @NotNull final IconSection icon
    ) {
        this.category = category;
        this.requirement = requirement;
        this.icon = icon;
        
        // NOTE: Do NOT call category.addRequirement(this) here!
        // Hibernate will manage the bidirectional relationship automatically.
    }
    
    /**
     * Returns the category to which this requirement belongs.
     *
     * @return the associated {@link QuestCategory}
     */
    @NotNull
    public QuestCategory getCategory() {
        return this.category;
    }
    
    /**
     * Sets the category for this requirement.
     * <p>
     * This method manages the bidirectional relationship between QuestCategory and QuestCategoryRequirement.
     * </p>
     *
     * @param category the category to set
     */
    public void setCategory(@Nullable final QuestCategory category) {
        if (this.category != null && this.category != category) {
            this.category.getRequirements().remove(this);
        }
        
        this.category = category;
        
        if (category != null && !category.getRequirements().contains(this)) {
            category.getRequirements().add(this);
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
     * Sets the requirement for this category requirement.
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
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof QuestCategoryRequirement that)) return false;
        
        if (this.getId() != null && that.getId() != null) {
            return this.getId().equals(that.getId());
        }
        
        if (this.requirement != null && that.requirement != null &&
                this.category != null && that.category != null) {
            return this.requirement.equals(that.requirement) &&
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
        
        if (this.requirement != null && this.category != null) {
            return Objects.hash(this.requirement, this.category, this.displayOrder);
        }
        
        return System.identityHashCode(this);
    }
}
