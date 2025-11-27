package com.raindropcentral.rdq.database.entity.quest;

import de.jexcellence.hibernate.entity.AbstractEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.jetbrains.annotations.NotNull;

/**
 * Entity representing a player's progress towards a specific quest upgrade requirement.
 * <p>
 * This entity links a {@link RPlayerQuest} to a {@link RQuestUpgradeRequirement}, tracking the
 * current progress value for that requirement. It is mapped to the {@code r_player_quest_requirement_progress}
 * table in the database and is used to persist incremental progress for each requirement of a quest upgrade.
 * </p>
 *
 * @author JExcellence
 * @version 1.0.0
 * @since TBD
 */
@Entity
@Table(name = "r_player_quest_requirement_progress")
public class RPlayerQuestRequirementProgress extends AbstractEntity {

    /**
     * The player's quest progress record to which this requirement progress belongs.
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "player_quest_id", nullable = false)
    private RPlayerQuest playerQuest;

    /**
     * The specific quest upgrade requirement being tracked.
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "upgrade_requirement_id", nullable = false)
    private RQuestUpgradeRequirement upgradeRequirement;

    /**
     * The current progress value for this requirement.
     */
    @Column(name = "progress_value", nullable = false)
    private double progress;

    /**
     * Protected no-argument constructor for JPA.
     */
    protected RPlayerQuestRequirementProgress() {
    }

    /**
     * Constructs a new {@code RPlayerQuestRequirementProgress} for the given player quest and upgrade requirement.
     * <p>
     * Initializes the progress value to {@code 0.00}.
     * </p>
     *
     * @param playerQuest        the player's quest progress record
     * @param upgradeRequirement the specific quest upgrade requirement being tracked
     */
    public RPlayerQuestRequirementProgress(
            final @NotNull RPlayerQuest playerQuest,
            final @NotNull RQuestUpgradeRequirement upgradeRequirement
    ) {
        this.playerQuest = playerQuest;
        this.upgradeRequirement = upgradeRequirement;
        this.progress = 0.00;
    }
}
