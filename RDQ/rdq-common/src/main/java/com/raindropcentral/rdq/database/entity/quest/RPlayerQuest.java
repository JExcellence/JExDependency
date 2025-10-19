package com.raindropcentral.rdq.database.entity.quest;

import com.raindropcentral.rdq.database.entity.player.RDQPlayer;
import de.jexcellence.hibernate.entity.AbstractEntity;
import jakarta.persistence.*;
import org.jetbrains.annotations.NotNull;

import java.io.Serial;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Represents a quest instance assigned to a specific player, including the
 * current upgrade level and progress tracking for each requirement belonging
 * to the quest upgrades.
 *
 * <p>The entity eagerly loads the associated player, quest definition, and all
 * requirement progress records so that callers can reason about upgrade
 * eligibility without additional database fetches.</p>
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
@Entity
@Table(name = "r_player_quest")
public final class RPlayerQuest extends AbstractEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "player_id", nullable = false)
    private RDQPlayer player;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "quest_id", nullable = false)
    private RQuest quest;

    @OneToMany(fetch = FetchType.EAGER, mappedBy = "playerQuest", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RPlayerQuestRequirementProgress> requirementProgressRecords = new ArrayList<>();

    @Column(name = "current_upgrade_level", nullable = false)
    private int currentUpgradeLevel;

    /**
     * Required by JPA and Hibernate for entity reconstruction.
     */
    protected RPlayerQuest() {}

    /**
     * Creates a new player quest for the specified player and quest pairing at
     * an optional upgrade level.
     *
     * <p>All requirement progress entries for the quest upgrades are
     * initialized so callers can update progress without additional setup.</p>
     *
     * @param player the owning player
     * @param quest the quest definition assigned to the player
     * @param currentUpgradeLevel the desired upgrade level if it is within the
     *                            quest's allowed range
     * @throws NullPointerException if {@code player} or {@code quest} is {@code null}
     */
    public RPlayerQuest(final @NotNull RDQPlayer player, final @NotNull RQuest quest, final int currentUpgradeLevel) {
        this.player = Objects.requireNonNull(player, "player cannot be null");
        this.quest = Objects.requireNonNull(quest, "quest cannot be null");
        this.currentUpgradeLevel = this.quest.getInitialUpgradeLevel();

        if (currentUpgradeLevel >= this.quest.getInitialUpgradeLevel() &&
            currentUpgradeLevel <= this.quest.getMaximumUpgradeLevel()) {
            this.currentUpgradeLevel = currentUpgradeLevel;
        }

        this.quest.getUpgrades().forEach(questUpgrade ->
                questUpgrade.getUpgradeRequirements().forEach(upgradeRequirement ->
                        this.requirementProgressRecords.add(
                                new RPlayerQuestRequirementProgress(this, upgradeRequirement)
                        )
                )
        );
    }

    /**
     * Retrieves the player that owns this quest instance.
     *
     * @return the owning {@link RDQPlayer}
     */
    public @NotNull RDQPlayer getPlayer() {
        return this.player;
    }

    /**
     * Sets the owning player for this quest instance.
     *
     * @param player the new owning {@link RDQPlayer}
     * @throws NullPointerException if {@code player} is {@code null}
     */
    public void setPlayer(final @NotNull RDQPlayer player) {
        this.player = Objects.requireNonNull(player, "player cannot be null");
    }

    /**
     * Retrieves the quest definition backing this player quest instance.
     *
     * @return the underlying {@link RQuest}
     */
    public @NotNull RQuest getQuest() {
        return this.quest;
    }

    /**
     * Sets the quest definition for this instance.
     *
     * @param quest the new {@link RQuest}
     * @throws NullPointerException if {@code quest} is {@code null}
     */
    public void setQuest(final @NotNull RQuest quest) {
        this.quest = Objects.requireNonNull(quest, "quest cannot be null");
    }

    /**
     * Provides an immutable view of the requirement progress records related to
     * this quest instance.
     *
     * @return an unmodifiable list of requirement progress records
     */
    public @NotNull List<RPlayerQuestRequirementProgress> getRequirementProgressRecords() {
        return Collections.unmodifiableList(this.requirementProgressRecords);
    }

    /**
     * Retrieves the current upgrade level achieved by the player for this quest.
     *
     * @return the current upgrade level
     */
    public int getCurrentUpgradeLevel() {
        return this.currentUpgradeLevel;
    }

    /**
     * Updates the current upgrade level for this quest instance.
     *
     * @param currentUpgradeLevel the new upgrade level value
     */
    public void setCurrentUpgradeLevel(final int currentUpgradeLevel) {
        this.currentUpgradeLevel = currentUpgradeLevel;
    }

    /**
     * Determines whether the quest can be upgraded to the next level based on
     * the quest's configured maximum upgrade level.
     *
     * @return {@code true} if another upgrade level is available; otherwise
     *         {@code false}
     */
    public boolean canUpgrade() {
        return this.currentUpgradeLevel < this.quest.getMaximumUpgradeLevel();
    }

    /**
     * Advances the upgrade level by one if the quest is eligible for an upgrade.
     */
    public void incrementUpgradeLevel() {
        if (canUpgrade()) {
            this.currentUpgradeLevel++;
        }
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof final RPlayerQuest other)) return false;
        return Objects.equals(this.player, other.player) && Objects.equals(this.quest, other.quest);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.player, this.quest);
    }

    @Override
    public String toString() {
        return "RPlayerQuest[id=%d, player=%s, quest=%s, level=%d]"
                .formatted(getId(), player != null ? player.getPlayerName() : "null",
                        quest != null ? quest.getIdentifier() : "null", currentUpgradeLevel);
    }
}