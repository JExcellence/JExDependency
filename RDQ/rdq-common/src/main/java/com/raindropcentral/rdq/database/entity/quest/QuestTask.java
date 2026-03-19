package com.raindropcentral.rdq.database.entity.quest;

import com.raindropcentral.rdq.model.quest.TaskDifficulty;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Database entity representing a quest task.
 * <p>
 * Tasks are individual objectives within a quest that players must complete.
 * Each task can have its own requirements and rewards.
 * </p>
 *
 * @author RaindropCentral
 * @version 1.0.0
 */
@Entity
@Table(name = "rdq_quest_tasks", indexes = {
    @Index(name = "idx_task_quest", columnList = "quest_id"),
    @Index(name = "idx_task_identifier", columnList = "quest_id,identifier", unique = true)
})
public class QuestTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quest_id", nullable = false)
    private Quest quest;

    @Column(name = "identifier", nullable = false, length = 100)
    private String identifier;

    @Column(name = "display_name", nullable = false, length = 255)
    private String displayName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty", nullable = false, length = 20)
    private TaskDifficulty difficulty;

    @Column(name = "optional", nullable = false)
    private boolean optional;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "task_type", length = 50)
    private String taskType;

    @Column(name = "target", length = 100)
    private String target;

    @Column(name = "amount")
    private Integer amount;

    @Column(name = "task_data_json", columnDefinition = "TEXT")
    private String taskDataJson;

    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<QuestTaskRequirement> requirements = new ArrayList<>();

    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<QuestTaskReward> rewards = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * Default constructor for JPA.
     */
    public QuestTask() {
    }

    /**
     * Creates a new quest task with the specified identifier and quest.
     *
     * @param identifier the task identifier (unique within the quest)
     * @param quest      the parent quest
     */
    public QuestTask(@NotNull final String identifier, @NotNull final Quest quest) {
        this.identifier = Objects.requireNonNull(identifier, "identifier cannot be null");
        this.quest = Objects.requireNonNull(quest, "quest cannot be null");
        this.difficulty = TaskDifficulty.MEDIUM;
        this.optional = false;
        this.sortOrder = 0;
    }

    /**
     * Gets the database ID.
     *
     * @return the ID
     */
    @Nullable
    public Long getId() {
        return id;
    }

    /**
     * Gets the parent quest.
     *
     * @return the quest
     */
    @NotNull
    public Quest getQuest() {
        return quest;
    }

    /**
     * Sets the parent quest.
     *
     * @param quest the quest
     */
    public void setQuest(@NotNull final Quest quest) {
        this.quest = Objects.requireNonNull(quest, "quest cannot be null");
    }

    /**
     * Gets the task identifier.
     *
     * @return the identifier
     */
    @NotNull
    public String getIdentifier() {
        return identifier;
    }

    /**
     * Sets the task identifier.
     *
     * @param identifier the identifier
     */
    public void setIdentifier(@NotNull final String identifier) {
        this.identifier = Objects.requireNonNull(identifier, "identifier cannot be null");
    }

    /**
     * Gets the display name.
     *
     * @return the display name
     */
    @NotNull
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Sets the display name.
     *
     * @param displayName the display name
     */
    public void setDisplayName(@NotNull final String displayName) {
        this.displayName = Objects.requireNonNull(displayName, "displayName cannot be null");
    }

    /**
     * Gets the description.
     *
     * @return the description
     */
    @Nullable
    public String getDescription() {
        return description;
    }

    /**
     * Sets the description.
     *
     * @param description the description
     */
    public void setDescription(@Nullable final String description) {
        this.description = description;
    }

    /**
     * Gets the task difficulty.
     *
     * @return the difficulty
     */
    @NotNull
    public TaskDifficulty getDifficulty() {
        return difficulty;
    }

    /**
     * Sets the task difficulty.
     *
     * @param difficulty the difficulty
     */
    public void setDifficulty(@NotNull final TaskDifficulty difficulty) {
        this.difficulty = Objects.requireNonNull(difficulty, "difficulty cannot be null");
    }

    /**
     * Checks if the task is optional.
     *
     * @return true if optional
     */
    public boolean isOptional() {
        return optional;
    }

    /**
     * Sets whether the task is optional.
     *
     * @param optional true if optional
     */
    public void setOptional(final boolean optional) {
        this.optional = optional;
    }

    /**
     * Gets the sort order.
     *
     * @return the sort order
     */
    public int getSortOrder() {
        return sortOrder;
    }

    /**
     * Sets the sort order.
     *
     * @param sortOrder the sort order
     */
    public void setSortOrder(final int sortOrder) {
        this.sortOrder = sortOrder;
    }

    /**
     * Gets the task type (e.g., KILL_MOBS, CRAFT_ITEMS, BREAK_BLOCKS).
     *
     * @return the task type
     */
    @Nullable
    public String getTaskType() {
        return taskType;
    }

    /**
     * Sets the task type.
     *
     * @param taskType the task type
     */
    public void setTaskType(@Nullable final String taskType) {
        this.taskType = taskType;
    }

    /**
     * Gets the task target (entity/item/block type).
     *
     * @return the target
     */
    @Nullable
    public String getTarget() {
        return target;
    }

    /**
     * Sets the task target.
     *
     * @param target the target
     */
    public void setTarget(@Nullable final String target) {
        this.target = target;
    }

    /**
     * Gets the required amount for task completion.
     *
     * @return the amount
     */
    @Nullable
    public Integer getAmount() {
        return amount;
    }

    /**
     * Sets the required amount for task completion.
     *
     * @param amount the amount
     */
    public void setAmount(@Nullable final Integer amount) {
        this.amount = amount;
    }

    /**
     * Gets the task-specific data as JSON.
     *
     * @return the task data JSON
     */
    @Nullable
    public String getTaskDataJson() {
        return taskDataJson;
    }

    /**
     * Sets the task-specific data as JSON.
     *
     * @param taskDataJson the task data JSON
     */
    public void setTaskDataJson(@Nullable final String taskDataJson) {
        this.taskDataJson = taskDataJson;
    }

    /**
     * Gets the task requirements.
     *
     * @return the requirements
     */
    @NotNull
    public List<QuestTaskRequirement> getRequirements() {
        return requirements;
    }

    /**
     * Sets the task requirements.
     *
     * @param requirements the requirements
     */
    public void setRequirements(@NotNull final List<QuestTaskRequirement> requirements) {
        this.requirements = Objects.requireNonNull(requirements, "requirements cannot be null");
    }

    /**
     * Gets the task rewards.
     *
     * @return the rewards
     */
    @NotNull
    public List<QuestTaskReward> getRewards() {
        return rewards;
    }

    /**
     * Sets the task rewards.
     *
     * @param rewards the rewards
     */
    public void setRewards(@NotNull final List<QuestTaskReward> rewards) {
        this.rewards = Objects.requireNonNull(rewards, "rewards cannot be null");
    }

    /**
     * Gets the creation timestamp.
     *
     * @return the creation timestamp
     */
    @NotNull
    public Instant getCreatedAt() {
        return createdAt;
    }

    /**
     * Gets the last update timestamp.
     *
     * @return the update timestamp
     */
    @NotNull
    public Instant getUpdatedAt() {
        return updatedAt;
    }

    /**
     * Gets the full task identifier including quest prefix.
     *
     * @return the full identifier (e.g., "zombie_hunter_i.kill_zombies")
     */
    @NotNull
    public String getFullIdentifier() {
        return quest.getIdentifier() + "." + identifier;
    }

    /**
     * Adds a requirement to this task.
     *
     * @param requirement the requirement to add
     */
    public void addRequirement(@NotNull final QuestTaskRequirement requirement) {
        Objects.requireNonNull(requirement, "requirement cannot be null");
        if (!requirements.contains(requirement)) {
            requirements.add(requirement);
        }
    }

    /**
     * Adds a reward to this task.
     *
     * @param reward the reward to add
     */
    public void addReward(@NotNull final QuestTaskReward reward) {
        Objects.requireNonNull(reward, "reward cannot be null");
        if (!rewards.contains(reward)) {
            rewards.add(reward);
        }
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof QuestTask)) return false;
        final QuestTask questTask = (QuestTask) o;
        return Objects.equals(quest, questTask.quest) &&
                Objects.equals(identifier, questTask.identifier);
    }

    @Override
    public int hashCode() {
        return Objects.hash(quest, identifier);
    }

    @Override
    public String toString() {
        return "QuestTask{" +
                "id=" + id +
                ", identifier='" + identifier + '\'' +
                ", displayName='" + displayName + '\'' +
                ", difficulty=" + difficulty +
                ", optional=" + optional +
                ", sortOrder=" + sortOrder +
                '}';
    }
}
