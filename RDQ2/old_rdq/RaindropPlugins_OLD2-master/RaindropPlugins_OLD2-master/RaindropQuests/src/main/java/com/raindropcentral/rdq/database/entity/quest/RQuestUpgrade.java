package com.raindropcentral.rdq.database.entity.quest;

import de.jexcellence.hibernate.entity.AbstractEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Entity representing an upgrade stage for a quest in the RaindropQuests system.
 * <p>
 * Each quest upgrade is associated with a parent {@link RQuest} and contains a set of
 * {@link RQuestUpgradeRequirement} objects that define the requirements for each upgrade level.
 * This entity is mapped to the {@code r_quest_upgrade} table in the database.
 * </p>
 *
 * @author JExcellence
 * @version 1.0.0
 * @since TBD
 */
@Entity
@Table(name = "r_quest_upgrade")
public class RQuestUpgrade extends AbstractEntity {

    /**
     * The quest to which this upgrade belongs.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quest_id", nullable = false)
    private RQuest quest;

    /**
     * The set of requirements for this quest upgrade, grouped by upgrade level.
     */
    @OneToMany(mappedBy = "questUpgrade", cascade = CascadeType.ALL, orphanRemoval = true)
    private final Set<RQuestUpgradeRequirement> upgradeRequirements = new HashSet<>();

    /**
     * Protected no-argument constructor for JPA.
     */
    protected RQuestUpgrade() {
    }

    /**
     * Constructs a new {@code RQuestUpgrade} for the specified quest.
     *
     * @param quest the parent quest to which this upgrade belongs
     */
    public RQuestUpgrade(
            final @NotNull RQuest quest
    ) {
        this.quest = quest;
    }

    /**
     * Gets the parent quest associated with this upgrade.
     *
     * @return the parent {@link RQuest}
     */
    public RQuest getQuest() {
        return this.quest;
    }

    /**
     * Gets the set of requirements for this quest upgrade.
     *
     * @return a set of {@link RQuestUpgradeRequirement} objects
     */
    public Set<RQuestUpgradeRequirement> getUpgradeRequirements() {
        return this.upgradeRequirements;
    }

    /**
     * Retrieves a list of requirements for a specific upgrade level.
     *
     * @param level the upgrade level to filter requirements by
     * @return a list of {@link RQuestUpgradeRequirement} for the specified level
     */
    @NotNull
    public List<RQuestUpgradeRequirement> getRequirementsForLevel(
            final int level
    ) {
        return
                this.upgradeRequirements.stream().filter(requirement -> requirement.getUpgradeLevel() == level).toList();
    }

    /**
     * Adds a requirement to this quest upgrade.
     * <p>
     * If the requirement is successfully added and not already associated with this upgrade,
     * its {@code questUpgrade} reference is updated accordingly.
     * </p>
     *
     * @param requirement the requirement to add
     * @return {@code true} if the requirement was added, {@code false} if it was already present
     */
    public boolean addUpgradeRequirement(
            final @NotNull RQuestUpgradeRequirement requirement
    ) {
        boolean added = this.upgradeRequirements.add(requirement);
        if (
                added && requirement.getQuestUpgrade() != this
        ) requirement.setQuestUpgrade(this);

        return added;
    }

    /**
     * Retrieves all unique upgrade levels for which this upgrade has requirements.
     *
     * @return a sorted set of upgrade level integers
     */
    @NotNull
    public SortedSet<Integer> getAllUpgradeLevels() {
        return
                this.upgradeRequirements.stream().map(
                        RQuestUpgradeRequirement::getUpgradeLevel
                ).collect(Collectors.toCollection(TreeSet::new));
    }
}
