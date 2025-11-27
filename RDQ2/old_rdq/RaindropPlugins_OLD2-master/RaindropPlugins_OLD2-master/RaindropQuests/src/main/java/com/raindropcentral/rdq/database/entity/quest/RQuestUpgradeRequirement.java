package com.raindropcentral.rdq.database.entity.quest;

import com.raindropcentral.rdq.database.converter.RequirementConverter;
import com.raindropcentral.rdq.requirement.AbstractRequirement;
import com.raindropcentral.rplatform.database.converter.BasicMaterialConverter;
import de.jexcellence.hibernate.entity.AbstractEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

/**
 * Entity representing a specific requirement for a quest upgrade in the RaindropQuests system.
 * <p>
 * Each instance links a {@link RQuestUpgrade} to an {@link AbstractRequirement} at a specific upgrade level,
 * and provides a showcase material for UI representation. This entity is mapped to the
 * {@code r_quest_upgrade_requirement} table in the database.
 * </p>
 *
 * @author JExcellence
 * @version 1.0.0
 * @since TBD
 */
@Entity
@Table(name = "r_quest_upgrade_requirement")
public class RQuestUpgradeRequirement extends AbstractEntity {

    /**
     * The quest upgrade to which this requirement belongs.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quest_upgrade_id")
    private RQuestUpgrade questUpgrade;

    /**
     * The requirement data for this upgrade, stored as a JSON string in the database.
     */
    @Column(name = "requirement_data", nullable = false, columnDefinition = "LONGTEXT")
    @Convert(converter = RequirementConverter.class)
    private AbstractRequirement requirement;

    /**
     * The upgrade level at which this requirement applies.
     */
    @Column(name = "upgrade_level", nullable = false)
    private int upgradeLevel;

    /**
     * The material used to visually represent this requirement in the UI.
     */
    @Column(name = "showcase", nullable = false)
    @Convert(converter = BasicMaterialConverter.class)
    private Material showcase;

    /**
     * Protected no-argument constructor for JPA.
     */
    protected RQuestUpgradeRequirement() {
    }

    /**
     * Constructs a new {@code RQuestUpgradeRequirement} for the specified quest upgrade, requirement, and level.
     *
     * @param questUpgrade the quest upgrade to which this requirement belongs
     * @param requirement  the requirement data for this upgrade
     * @param upgradeLevel the upgrade level at which this requirement applies
     */
    public RQuestUpgradeRequirement(
            final @NotNull RQuestUpgrade questUpgrade,
            final @NotNull AbstractRequirement requirement,
            final int upgradeLevel
    ) {
        this.questUpgrade = questUpgrade;
        this.requirement = requirement;
        this.upgradeLevel = upgradeLevel;

        //TODO ADD REQ to quest upgrade
    }

    /**
     * Gets the quest upgrade to which this requirement belongs.
     *
     * @return the associated {@link RQuestUpgrade}
     */
    public RQuestUpgrade getQuestUpgrade() {
        return this.questUpgrade;
    }

    /**
     * Sets the quest upgrade to which this requirement belongs.
     *
     * @param questUpgrade the {@link RQuestUpgrade} to associate
     */
    public void setQuestUpgrade(RQuestUpgrade questUpgrade) {
        this.questUpgrade = questUpgrade;
    }

    /**
     * Gets the requirement data for this upgrade.
     *
     * @return the {@link AbstractRequirement} instance
     */
    public AbstractRequirement getRequirement() {
        return this.requirement;
    }

    /**
     * Gets the upgrade level at which this requirement applies.
     *
     * @return the upgrade level
     */
    public int getUpgradeLevel() {
        return this.upgradeLevel;
    }

    /**
     * Gets the material used to visually represent this requirement in the UI.
     *
     * @return the showcase {@link Material}
     */
    public Material getShowcase() {
        return this.showcase;
    }
}
