package com.raindropcentral.rdq.database.entity.quest;

import com.raindropcentral.rdq.database.converter.IconSectionConverter;
import com.raindropcentral.rdq.database.entity.requirement.BaseRequirement;
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

/**
 * Entity representing a single requirement for a {@link Quest} in the RaindropQuests system.
 * <p>
 * This entity encapsulates a single {@link BaseRequirement} that must be satisfied
 * to start or complete the associated quest. It also includes an icon for visual representation
 * and display order for UI presentation.
 *
 * <p>
 * Multiple instances of this entity can exist for a single quest, representing different requirements
 * that all need to be completed for the quest to be accessible or completable.
 *
 * @author JExcellence
 * @version 2.0.0
 * @since TBD
 */
@Entity
@Table(name = "rdq_quest_requirement")
public class QuestRequirement extends BaseEntity {
    
    @Serial
    private static final long serialVersionUID = 1L;
    
    /**
     * The quest to which this requirement belongs.
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "quest_id", nullable = false)
    private Quest quest;
    
    /**
     * The requirement that must be satisfied for this quest.
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
     * Optional display order for this requirement within the quest's requirements.
     */
    @Column(name = "display_order", nullable = false)
    private int displayOrder = 0;
    
    /**
     * Optimistic locking version field.
     */
    @Version
    @Column(name = "version")
    private int version;
    
    /**
     * Protected no-argument constructor for JPA.
     */
    protected QuestRequirement() {}
    
    /**
     * Constructs a new {@code QuestRequirement} with the specified quest, requirement, and icon.
     *
     * @param quest       the {@link Quest} to which this requirement belongs
     * @param requirement the {@link BaseRequirement} that must be satisfied
     * @param icon        the {@link IconSection} used as the icon for this requirement
     */
    public QuestRequirement(
            @Nullable final Quest quest,
            @NotNull final BaseRequirement requirement,
            @NotNull final IconSection icon
    ) {
        this.quest = quest;
        this.requirement = requirement;
        this.icon = icon;
        
        // NOTE: Do NOT call quest.addRequirement(this) here!
        // Hibernate will manage the bidirectional relationship automatically.
    }
    
    /**
     * Returns the quest to which this requirement belongs.
     *
     * @return the associated {@link Quest}
     */
    @NotNull
    public Quest getQuest() {
        return this.quest;
    }
    
    /**
     * Sets the quest for this requirement.
     * <p>
     * This method manages the bidirectional relationship between Quest and QuestRequirement.
     *
     * @param quest the quest to set
     */
    public void setQuest(@Nullable final Quest quest) {
        if (this.quest != null && this.quest != quest) {
            this.quest.getRequirements().remove(this);
        }
        
        this.quest = quest;
        
        if (quest != null && !quest.getRequirements().contains(this)) {
            quest.getRequirements().add(this);
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
     * Sets the requirement for this quest requirement.
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
     * Returns the optimistic locking version.
     *
     * @return the version
     */
    public int getVersion() {
        return this.version;
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
        if (!(o instanceof QuestRequirement that)) return false;
        
        if (this.getId() != null && that.getId() != null) {
            return this.getId().equals(that.getId());
        }
        
        if (this.requirement != null && that.requirement != null &&
                this.quest != null && that.quest != null) {
            return this.requirement.equals(that.requirement) &&
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
        
        if (this.requirement != null && this.quest != null) {
            return Objects.hash(this.requirement, this.quest, this.displayOrder);
        }
        
        return System.identityHashCode(this);
    }
}
