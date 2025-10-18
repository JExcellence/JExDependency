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

    protected RPlayerQuest() {}

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

    public @NotNull RDQPlayer getPlayer() {
        return this.player;
    }

    public void setPlayer(final @NotNull RDQPlayer player) {
        this.player = Objects.requireNonNull(player, "player cannot be null");
    }

    public @NotNull RQuest getQuest() {
        return this.quest;
    }

    public void setQuest(final @NotNull RQuest quest) {
        this.quest = Objects.requireNonNull(quest, "quest cannot be null");
    }

    public @NotNull List<RPlayerQuestRequirementProgress> getRequirementProgressRecords() {
        return Collections.unmodifiableList(this.requirementProgressRecords);
    }

    public int getCurrentUpgradeLevel() {
        return this.currentUpgradeLevel;
    }

    public void setCurrentUpgradeLevel(final int currentUpgradeLevel) {
        this.currentUpgradeLevel = currentUpgradeLevel;
    }

    public boolean canUpgrade() {
        return this.currentUpgradeLevel < this.quest.getMaximumUpgradeLevel();
    }

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