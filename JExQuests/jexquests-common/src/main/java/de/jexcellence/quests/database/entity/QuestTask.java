package de.jexcellence.quests.database.entity;

import de.jexcellence.jehibernate.entity.base.LongIdEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A task inside a {@link Quest}. Tasks are ordered; if {@link #sequential}
 * is set on the quest, tasks must be completed in {@code orderIndex}
 * order. Icon, requirement, and reward configurations are JSON blobs
 * parsed by the service layer.
 */
@Entity
@Table(
        name = "jexquests_quest_task",
        indexes = {
                @Index(name = "idx_jexquests_task_quest", columnList = "quest_id"),
                @Index(name = "idx_jexquests_task_identifier", columnList = "task_identifier"),
                @Index(name = "idx_jexquests_task_order", columnList = "order_index")
        }
)
public class QuestTask extends LongIdEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quest_id", nullable = false)
    private Quest quest;

    @Column(name = "task_identifier", nullable = false, length = 64)
    private String taskIdentifier;

    @Column(name = "display_name", nullable = false, length = 128)
    private String displayName;

    @Lob
    @Column(name = "icon_data")
    private String iconData;

    @Column(name = "order_index", nullable = false)
    private int orderIndex;

    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty", nullable = false, length = 16)
    private QuestDifficulty difficulty = QuestDifficulty.MEDIUM;

    @Column(name = "sequential", nullable = false)
    private boolean sequential;

    @Lob
    @Column(name = "requirement_data")
    private String requirementData;

    @Lob
    @Column(name = "reward_data")
    private String rewardData;

    @Lob
    @Column(name = "objective_data")
    private String objectiveData;

    protected QuestTask() {
    }

    public QuestTask(
            @NotNull Quest quest,
            @NotNull String taskIdentifier,
            @NotNull String displayName,
            int orderIndex
    ) {
        this.quest = quest;
        this.taskIdentifier = taskIdentifier;
        this.displayName = displayName;
        this.orderIndex = orderIndex;
    }

    public @NotNull Quest getQuest() { return this.quest; }
    public void setQuest(@NotNull Quest quest) { this.quest = quest; }
    public @NotNull String getTaskIdentifier() { return this.taskIdentifier; }
    public @NotNull String getDisplayName() { return this.displayName; }
    public void setDisplayName(@NotNull String displayName) { this.displayName = displayName; }
    public @Nullable String getIconData() { return this.iconData; }
    public void setIconData(@Nullable String iconData) { this.iconData = iconData; }
    public int getOrderIndex() { return this.orderIndex; }
    public void setOrderIndex(int orderIndex) { this.orderIndex = orderIndex; }
    public @NotNull QuestDifficulty getDifficulty() { return this.difficulty; }
    public void setDifficulty(@NotNull QuestDifficulty difficulty) { this.difficulty = difficulty; }
    public boolean isSequential() { return this.sequential; }
    public void setSequential(boolean sequential) { this.sequential = sequential; }
    public @Nullable String getRequirementData() { return this.requirementData; }
    public void setRequirementData(@Nullable String requirementData) { this.requirementData = requirementData; }
    public @Nullable String getRewardData() { return this.rewardData; }
    public void setRewardData(@Nullable String rewardData) { this.rewardData = rewardData; }
    public @Nullable String getObjectiveData() { return this.objectiveData; }
    public void setObjectiveData(@Nullable String objectiveData) { this.objectiveData = objectiveData; }

    @Override
    public String toString() {
        return "QuestTask[" + this.taskIdentifier + "@" + this.orderIndex + "]";
    }
}
