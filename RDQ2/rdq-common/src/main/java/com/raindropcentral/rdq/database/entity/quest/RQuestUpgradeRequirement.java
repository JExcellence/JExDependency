package com.raindropcentral.rdq.database.entity.quest;

import com.raindropcentral.rdq.database.converter.RequirementConverter;
import com.raindropcentral.rdq.requirement.AbstractRequirement;
import com.raindropcentral.rplatform.database.converter.BasicMaterialConverter;
import de.jexcellence.hibernate.entity.AbstractEntity;
import jakarta.persistence.*;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

import java.io.Serial;
import java.util.Objects;

/**
 * Represents a persisted requirement needed to unlock a quest upgrade. The entity stores
 * the requirement payload, the owning quest upgrade reference, and presentation details
 * such as the showcase material for GUI displays.
 *
 * <p>The requirement payload itself is serialized using {@link RequirementConverter} to
 * ensure complex requirement implementations can be stored inside the database. The
 * {@link #showcase} material defaults to {@link Material#PAPER} but can be customized for
 * richer visual feedback.</p>
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
@Entity
@Table(name = "r_quest_upgrade_requirement")
public final class RQuestUpgradeRequirement extends AbstractEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quest_upgrade_id")
    private RQuestUpgrade questUpgrade;

    @Column(name = "requirement_data", nullable = false, columnDefinition = "LONGTEXT")
    @Convert(converter = RequirementConverter.class)
    private AbstractRequirement requirement;

    @Column(name = "upgrade_level", nullable = false)
    private int upgradeLevel;

    @Column(name = "showcase", nullable = false)
    @Convert(converter = BasicMaterialConverter.class)
    private Material showcase;

    /**
     * Protected no-args constructor for JPA serialization.
     */
    protected RQuestUpgradeRequirement() {}

    /**
     * Creates a requirement associated with a specific quest upgrade and level using the
     * given requirement payload. The showcase material defaults to {@link Material#PAPER}.
     *
     * @param questUpgrade the quest upgrade this requirement belongs to
     * @param requirement  the requirement payload that must be satisfied
     * @param upgradeLevel the upgrade level the requirement applies to
     */
    public RQuestUpgradeRequirement(final @NotNull RQuestUpgrade questUpgrade,
                                    final @NotNull AbstractRequirement requirement,
                                    final int upgradeLevel) {
        this.questUpgrade = Objects.requireNonNull(questUpgrade, "questUpgrade cannot be null");
        this.requirement = Objects.requireNonNull(requirement, "requirement cannot be null");
        this.upgradeLevel = upgradeLevel;
        this.showcase = Material.PAPER;
    }

    /**
     * Retrieves the quest upgrade that owns this requirement.
     *
     * @return the parent quest upgrade
     */
    public @NotNull RQuestUpgrade getQuestUpgrade() {
        return this.questUpgrade;
    }

    /**
     * Updates the quest upgrade reference that owns this requirement.
     *
     * @param questUpgrade the new quest upgrade association
     */
    public void setQuestUpgrade(final @NotNull RQuestUpgrade questUpgrade) {
        this.questUpgrade = Objects.requireNonNull(questUpgrade, "questUpgrade cannot be null");
    }

    /**
     * Retrieves the requirement payload that must be satisfied for the upgrade.
     *
     * @return the upgrade requirement payload
     */
    public @NotNull AbstractRequirement getRequirement() {
        return this.requirement;
    }

    /**
     * Replaces the underlying requirement payload with a new instance.
     *
     * @param requirement the new requirement payload
     */
    public void setRequirement(final @NotNull AbstractRequirement requirement) {
        this.requirement = Objects.requireNonNull(requirement, "requirement cannot be null");
    }

    /**
     * Obtains the upgrade level where this requirement applies.
     *
     * @return the associated upgrade level
     */
    public int getUpgradeLevel() {
        return this.upgradeLevel;
    }

    /**
     * Sets the upgrade level this requirement should be evaluated against.
     *
     * @param upgradeLevel the upgrade level to assign
     */
    public void setUpgradeLevel(final int upgradeLevel) {
        this.upgradeLevel = upgradeLevel;
    }

    /**
     * Retrieves the showcase material used when presenting this requirement to players.
     *
     * @return the showcase material
     */
    public @NotNull Material getShowcase() {
        return this.showcase;
    }

    /**
     * Updates the showcase material that represents this requirement visually.
     *
     * @param showcase the new showcase material
     */
    public void setShowcase(final @NotNull Material showcase) {
        this.showcase = Objects.requireNonNull(showcase, "showcase cannot be null");
    }

    /**
     * Compares this requirement with another object using the persistent identifier.
     *
     * @param obj the object to compare against
     * @return {@code true} if both entities share the same identifier; {@code false} otherwise
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof final RQuestUpgradeRequirement other)) return false;
        return Objects.equals(getId(), other.getId());
    }

    /**
     * Generates a hash code based on the entity identifier.
     *
     * @return the hash code of this requirement
     */
    @Override
    public int hashCode() {
        return Objects.hash(getId());
    }

    /**
     * Provides a debug-friendly representation of the requirement, including its identifier,
     * level, and requirement type.
     *
     * @return the formatted string representation of this requirement
     */
    @Override
    public String toString() {
        return "RQuestUpgradeRequirement[id=%d, level=%d, requirement=%s]"
                .formatted(getId(), upgradeLevel, requirement.getClass().getSimpleName());
    }
}