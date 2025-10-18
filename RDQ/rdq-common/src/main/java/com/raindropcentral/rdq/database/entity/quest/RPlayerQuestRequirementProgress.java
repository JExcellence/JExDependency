package com.raindropcentral.rdq.database.entity.quest;

import de.jexcellence.hibernate.entity.AbstractEntity;
import jakarta.persistence.*;
import org.jetbrains.annotations.NotNull;

import java.io.Serial;
import java.util.Objects;

@Entity
@Table(name = "r_player_quest_requirement_progress")
public final class RPlayerQuestRequirementProgress extends AbstractEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "player_quest_id", nullable = false)
    private RPlayerQuest playerQuest;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "upgrade_requirement_id", nullable = false)
    private RQuestUpgradeRequirement upgradeRequirement;

    @Column(name = "progress_value", nullable = false)
    private double progress;

    protected RPlayerQuestRequirementProgress() {}

    public RPlayerQuestRequirementProgress(final @NotNull RPlayerQuest playerQuest,
                                           final @NotNull RQuestUpgradeRequirement upgradeRequirement) {
        this.playerQuest = Objects.requireNonNull(playerQuest, "playerQuest cannot be null");
        this.upgradeRequirement = Objects.requireNonNull(upgradeRequirement, "upgradeRequirement cannot be null");
        this.progress = 0.0;
    }

    public @NotNull RPlayerQuest getPlayerQuest() {
        return this.playerQuest;
    }

    public void setPlayerQuest(final @NotNull RPlayerQuest playerQuest) {
        this.playerQuest = Objects.requireNonNull(playerQuest, "playerQuest cannot be null");
    }

    public @NotNull RQuestUpgradeRequirement getUpgradeRequirement() {
        return this.upgradeRequirement;
    }

    public void setUpgradeRequirement(final @NotNull RQuestUpgradeRequirement upgradeRequirement) {
        this.upgradeRequirement = Objects.requireNonNull(upgradeRequirement, "upgradeRequirement cannot be null");
    }

    public double getProgress() {
        return this.progress;
    }

    public void setProgress(final double progress) {
        this.progress = Math.max(0.0, Math.min(1.0, progress));
    }

    public void incrementProgress(final double amount) {
        setProgress(this.progress + amount);
    }

    public void resetProgress() {
        this.progress = 0.0;
    }

    public boolean isCompleted() {
        return this.progress >= 1.0;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof final RPlayerQuestRequirementProgress other)) return false;
        return Objects.equals(this.playerQuest, other.playerQuest) &&
               Objects.equals(this.upgradeRequirement, other.upgradeRequirement);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.playerQuest, this.upgradeRequirement);
    }

    @Override
    public String toString() {
        return "RPlayerQuestRequirementProgress[id=%d, progress=%.2f, completed=%b]"
                .formatted(getId(), progress, isCompleted());
    }
}