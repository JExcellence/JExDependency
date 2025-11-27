package com.raindropcentral.rdq.database.entity.quest;

import com.raindropcentral.rdq.database.entity.RDQPlayer;
import de.jexcellence.hibernate.entity.AbstractEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing a player's progress on a specific quest.
 * <p>
 * This entity links a {@link RDQPlayer} to a {@link RQuest}, tracking the player's current upgrade level
 * and progress on all requirements for the quest's upgrades. Requirement progress is managed via
 * {@link RPlayerQuestRequirementProgress} records, which are initialized for each upgrade requirement
 * of the quest upon construction.
 * </p>
 *
 * <p>
 * Mapped to the {@code r_player_quest} table in the database.
 * </p>
 *
 * @author JExcellence
 * @version 1.0.0
 * @since TBD
 */
@Entity
@Table(name = "r_player_quest")
public class RPlayerQuest extends AbstractEntity {

    /**
     * The player who owns this quest progress record.
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "player_id", nullable = false)
    private RDQPlayer player;

    /**
     * The quest associated with this progress record.
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "quest_id", nullable = false)
    private RQuest quest;

    /**
     * List of progress records for each requirement of the quest's upgrades.
     */
    @OneToMany(fetch = FetchType.EAGER, mappedBy = "playerQuest", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RPlayerQuestRequirementProgress> requirementProgressRecords = new ArrayList<>();

    /**
     * The player's current upgrade level for this quest.
     */
    @Column(name = "current_upgrade_level", nullable = false)
    private int currentUpgradeLevel;

    /**
     * Protected no-argument constructor for JPA.
     */
    protected RPlayerQuest() {
    }

    /**
     * Constructs a new {@code RPlayerQuest} for the given player and quest, with the specified upgrade level.
     * <p>
     * Initializes the current upgrade level to the quest's initial upgrade level, unless the provided
     * {@code currentUpgradeLevel} is within the valid range (inclusive), in which case it is used.
     * Also initializes progress records for all requirements of all quest upgrades.
     * </p>
     *
     * @param player           the player who owns this quest progress
     * @param quest               the quest being tracked
     * @param currentUpgradeLevel the player's current upgrade level for this quest
     */
    public RPlayerQuest(
            final @NotNull RDQPlayer player,
            final @NotNull RQuest quest,
            final int currentUpgradeLevel
    ) {
        this.player = player;
        this.quest = quest;
        this.currentUpgradeLevel = this.quest.getInitialUpgradeLevel();

        if (
                currentUpgradeLevel >= this.quest.getInitialUpgradeLevel() &&
                        currentUpgradeLevel <= this.quest.getMaximumUpgradeLevel()
        ) this.currentUpgradeLevel = currentUpgradeLevel;

        // Initialize progress records for all requirements of all quest upgrades
        this.quest.getUpgrades().forEach(
                questUpgrade -> questUpgrade.getUpgradeRequirements().forEach(
                        upgradeRequirement -> this.requirementProgressRecords.add(
                                new RPlayerQuestRequirementProgress(this, upgradeRequirement)
                        )
                )
        );
    }
}
