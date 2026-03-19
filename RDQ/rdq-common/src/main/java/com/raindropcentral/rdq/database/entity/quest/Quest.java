package com.raindropcentral.rdq.database.entity.quest;

import com.raindropcentral.rdq.model.quest.QuestDifficulty;
import com.raindropcentral.rplatform.progression.IProgressionNode;
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
 * Database entity representing a quest definition.
 * <p>
 * This entity stores the core quest metadata including identifier, display information,
 * difficulty, and relationships to tasks, requirements, and rewards.
 * </p>
 * <p>
 * Implements {@link IProgressionNode} to support prerequisite validation and sequential
 * progression through the quest system.
 * </p>
 *
 * @author RaindropCentral
 * @version 1.0.0
 */
@Entity
@Table(name = "rdq_quests", indexes = {
    @Index(name = "idx_quest_identifier", columnList = "identifier", unique = true),
    @Index(name = "idx_quest_category", columnList = "category_id")
})
public class Quest implements IProgressionNode<Quest> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "identifier", nullable = false, unique = true, length = 100)
    private String identifier;

    @Column(name = "display_name", nullable = false, length = 255)
    private String displayName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private QuestCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty", nullable = false, length = 20)
    private QuestDifficulty difficulty;

    @Column(name = "repeatable", nullable = false)
    private boolean repeatable;

    @Column(name = "cooldown_minutes")
    private Integer cooldownMinutes;

    @Column(name = "time_limit_minutes")
    private Integer timeLimitMinutes;

    @Column(name = "max_concurrent_tasks")
    private Integer maxConcurrentTasks;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "effects_json", columnDefinition = "TEXT")
    private String effectsJson;

    @Column(name = "metadata_json", columnDefinition = "TEXT")
    private String metadataJson;

    @Column(name = "failure_conditions_json", columnDefinition = "TEXT")
    private String failureConditionsJson;

    @Column(name = "chain_id", length = 100)
    private String chainId;

    @Column(name = "chain_order")
    private Integer chainOrder;

    @Column(name = "quest_type", length = 50)
    private String questType;

    @Column(name = "hidden", nullable = false)
    private boolean hidden;

    @Column(name = "auto_start", nullable = false)
    private boolean autoStart;

    @Column(name = "show_in_log", nullable = false)
    private boolean showInLog;

    @OneToMany(mappedBy = "quest", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<QuestTask> tasks = new ArrayList<>();

    @OneToMany(mappedBy = "quest", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<QuestRequirement> requirements = new ArrayList<>();

    @OneToMany(mappedBy = "quest", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<QuestReward> rewards = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "rdq_quest_prerequisites", joinColumns = @JoinColumn(name = "quest_id"))
    @Column(name = "prerequisite_quest_id", length = 100)
    private List<String> prerequisiteQuestIds = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "rdq_quest_unlocks", joinColumns = @JoinColumn(name = "quest_id"))
    @Column(name = "unlocked_quest_id", length = 100)
    private List<String> unlockedQuestIds = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * Default constructor for JPA.
     */
    public Quest() {
    }

    /**
     * Creates a new quest with the specified identifier and category.
     *
     * @param identifier the unique quest identifier
     * @param category   the quest category
     */
    public Quest(@NotNull final String identifier, @NotNull final QuestCategory category) {
        this.identifier = Objects.requireNonNull(identifier, "identifier cannot be null");
        this.category = Objects.requireNonNull(category, "category cannot be null");
        this.difficulty = QuestDifficulty.EASY;
        this.repeatable = false;
        this.enabled = true;
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
     * Gets the unique quest identifier.
     *
     * @return the identifier
     */
    @Override
    @NotNull
    public String getIdentifier() {
        return identifier;
    }

    /**
     * Sets the unique quest identifier.
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
     * Gets the quest category.
     *
     * @return the category
     */
    @NotNull
    public QuestCategory getCategory() {
        return category;
    }

    /**
     * Sets the quest category.
     *
     * @param category the category
     */
    public void setCategory(@NotNull final QuestCategory category) {
        this.category = Objects.requireNonNull(category, "category cannot be null");
    }

    /**
     * Gets the quest difficulty.
     *
     * @return the difficulty
     */
    @NotNull
    public QuestDifficulty getDifficulty() {
        return difficulty;
    }

    /**
     * Sets the quest difficulty.
     *
     * @param difficulty the difficulty
     */
    public void setDifficulty(@NotNull final QuestDifficulty difficulty) {
        this.difficulty = Objects.requireNonNull(difficulty, "difficulty cannot be null");
    }

    /**
     * Checks if the quest is repeatable.
     *
     * @return true if repeatable
     */
    public boolean isRepeatable() {
        return repeatable;
    }

    /**
     * Sets whether the quest is repeatable.
     *
     * @param repeatable true if repeatable
     */
    public void setRepeatable(final boolean repeatable) {
        this.repeatable = repeatable;
    }

    /**
     * Gets the cooldown in minutes.
     *
     * @return the cooldown minutes, or null if no cooldown
     */
    @Nullable
    public Integer getCooldownMinutes() {
        return cooldownMinutes;
    }

    /**
     * Sets the cooldown in minutes.
     *
     * @param cooldownMinutes the cooldown minutes
     */
    public void setCooldownMinutes(@Nullable final Integer cooldownMinutes) {
        this.cooldownMinutes = cooldownMinutes;
    }

    /**
     * Gets the time limit in minutes.
     *
     * @return the time limit minutes, or null if no time limit
     */
    @Nullable
    public Integer getTimeLimitMinutes() {
        return timeLimitMinutes;
    }

    /**
     * Sets the time limit in minutes.
     *
     * @param timeLimitMinutes the time limit minutes
     */
    public void setTimeLimitMinutes(@Nullable final Integer timeLimitMinutes) {
        this.timeLimitMinutes = timeLimitMinutes;
    }

    /**
     * Gets the maximum concurrent tasks.
     *
     * @return the max concurrent tasks, or null for unlimited
     */
    @Nullable
    public Integer getMaxConcurrentTasks() {
        return maxConcurrentTasks;
    }

    /**
     * Sets the maximum concurrent tasks.
     *
     * @param maxConcurrentTasks the max concurrent tasks
     */
    public void setMaxConcurrentTasks(@Nullable final Integer maxConcurrentTasks) {
        this.maxConcurrentTasks = maxConcurrentTasks;
    }

    /**
     * Checks if the quest is enabled.
     *
     * @return true if enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Sets whether the quest is enabled.
     *
     * @param enabled true if enabled
     */
    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Gets the effects JSON data.
     *
     * @return the effects JSON
     */
    @Nullable
    public String getEffectsJson() {
        return effectsJson;
    }

    /**
     * Sets the effects JSON data.
     *
     * @param effectsJson the effects JSON
     */
    public void setEffectsJson(@Nullable final String effectsJson) {
        this.effectsJson = effectsJson;
    }

    /**
     * Gets the metadata JSON data.
     *
     * @return the metadata JSON
     */
    @Nullable
    public String getMetadataJson() {
        return metadataJson;
    }

    /**
     * Sets the metadata JSON data.
     *
     * @param metadataJson the metadata JSON
     */
    public void setMetadataJson(@Nullable final String metadataJson) {
        this.metadataJson = metadataJson;
    }

    /**
     * Gets the failure conditions JSON data.
     *
     * @return the failure conditions JSON
     */
    @Nullable
    public String getFailureConditionsJson() {
        return failureConditionsJson;
    }

    /**
     * Sets the failure conditions JSON data.
     *
     * @param failureConditionsJson the failure conditions JSON
     */
    public void setFailureConditionsJson(@Nullable final String failureConditionsJson) {
        this.failureConditionsJson = failureConditionsJson;
    }

    /**
     * Gets the chain ID.
     *
     * @return the chain ID
     */
    @Nullable
    public String getChainId() {
        return chainId;
    }

    /**
     * Sets the chain ID.
     *
     * @param chainId the chain ID
     */
    public void setChainId(@Nullable final String chainId) {
        this.chainId = chainId;
    }

    /**
     * Gets the chain order.
     *
     * @return the chain order
     */
    @Nullable
    public Integer getChainOrder() {
        return chainOrder;
    }

    /**
     * Sets the chain order.
     *
     * @param chainOrder the chain order
     */
    public void setChainOrder(@Nullable final Integer chainOrder) {
        this.chainOrder = chainOrder;
    }

    /**
     * Gets the quest type.
     *
     * @return the quest type
     */
    @Nullable
    public String getQuestType() {
        return questType;
    }

    /**
     * Sets the quest type.
     *
     * @param questType the quest type
     */
    public void setQuestType(@Nullable final String questType) {
        this.questType = questType;
    }

    /**
     * Checks if the quest is hidden.
     *
     * @return true if hidden
     */
    public boolean isHidden() {
        return hidden;
    }

    /**
     * Sets whether the quest is hidden.
     *
     * @param hidden true if hidden
     */
    public void setHidden(final boolean hidden) {
        this.hidden = hidden;
    }

    /**
     * Checks if the quest auto-starts.
     *
     * @return true if auto-start
     */
    public boolean isAutoStart() {
        return autoStart;
    }

    /**
     * Sets whether the quest auto-starts.
     *
     * @param autoStart true if auto-start
     */
    public void setAutoStart(final boolean autoStart) {
        this.autoStart = autoStart;
    }

    /**
     * Checks if the quest shows in log.
     *
     * @return true if shows in log
     */
    public boolean isShowInLog() {
        return showInLog;
    }

    /**
     * Sets whether the quest shows in log.
     *
     * @param showInLog true if shows in log
     */
    public void setShowInLog(final boolean showInLog) {
        this.showInLog = showInLog;
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
     * Gets the quest tasks.
     *
     * @return the tasks
     */
    @NotNull
    public List<QuestTask> getTasks() {
        return tasks;
    }

    /**
     * Sets the quest tasks.
     *
     * @param tasks the tasks
     */
    public void setTasks(@NotNull final List<QuestTask> tasks) {
        this.tasks = Objects.requireNonNull(tasks, "tasks cannot be null");
    }

    /**
     * Gets the quest requirements.
     *
     * @return the requirements
     */
    @NotNull
    public List<QuestRequirement> getRequirements() {
        return requirements;
    }

    /**
     * Sets the quest requirements.
     *
     * @param requirements the requirements
     */
    public void setRequirements(@NotNull final List<QuestRequirement> requirements) {
        this.requirements = Objects.requireNonNull(requirements, "requirements cannot be null");
    }

    /**
     * Gets the quest rewards.
     *
     * @return the rewards
     */
    @NotNull
    public List<QuestReward> getRewards() {
        return rewards;
    }

    /**
     * Sets the quest rewards.
     *
     * @param rewards the rewards
     */
    public void setRewards(@NotNull final List<QuestReward> rewards) {
        this.rewards = Objects.requireNonNull(rewards, "rewards cannot be null");
    }

    /**
     * Gets the prerequisite quest identifiers.
     * <p>
     * These are quests that must be completed before this quest can be started.
     * </p>
     *
     * @return the list of prerequisite quest identifiers
     */
    @NotNull
    public List<String> getPrerequisiteQuestIds() {
        return prerequisiteQuestIds;
    }

    /**
     * Sets the prerequisite quest identifiers.
     *
     * @param prerequisiteQuestIds the prerequisite quest identifiers
     */
    public void setPrerequisiteQuestIds(@NotNull final List<String> prerequisiteQuestIds) {
        this.prerequisiteQuestIds = Objects.requireNonNull(prerequisiteQuestIds, "prerequisiteQuestIds cannot be null");
    }

    /**
     * Gets the unlocked quest identifiers.
     * <p>
     * These are quests that become available after this quest is completed.
     * </p>
     *
     * @return the list of unlocked quest identifiers
     */
    @NotNull
    public List<String> getUnlockedQuestIds() {
        return unlockedQuestIds;
    }

    /**
     * Sets the unlocked quest identifiers.
     *
     * @param unlockedQuestIds the unlocked quest identifiers
     */
    public void setUnlockedQuestIds(@NotNull final List<String> unlockedQuestIds) {
        this.unlockedQuestIds = Objects.requireNonNull(unlockedQuestIds, "unlockedQuestIds cannot be null");
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
     * Gets the display name key for i18n.
     *
     * @return the display name key
     */
    @NotNull
    public String getDisplayNameKey() {
        return "quest." + identifier + ".name";
    }

    /**
     * Checks if the quest has a time limit.
     *
     * @return true if the quest has a time limit
     */
    public boolean hasTimeLimit() {
        return timeLimitMinutes != null && timeLimitMinutes > 0;
    }

    /**
     * Gets the time limit as a Duration.
     *
     * @return the time limit duration
     */
    @NotNull
    public java.time.Duration getTimeLimit() {
        if (!hasTimeLimit()) {
            return java.time.Duration.ZERO;
        }
        return java.time.Duration.ofMinutes(timeLimitMinutes);
    }

    /**
     * Gets the maximum number of completions allowed.
     *
     * @return the max completions, or -1 for unlimited
     */
    public int getMaxCompletions() {
        return repeatable ? -1 : 1;
    }

    /**
     * Gets the cooldown in seconds.
     *
     * @return the cooldown seconds, or 0 if no cooldown
     */
    public long getCooldownSeconds() {
        if (cooldownMinutes == null || cooldownMinutes <= 0) {
            return 0;
        }
        return cooldownMinutes * 60L;
    }

    /**
     * Gets the time limit in seconds.
     *
     * @return the time limit seconds, or 0 if no time limit
     */
    public long getTimeLimitSeconds() {
        if (timeLimitMinutes == null || timeLimitMinutes <= 0) {
            return 0;
        }
        return timeLimitMinutes * 60L;
    }

    /**
     * Adds a requirement to this quest.
     *
     * @param requirement the requirement to add
     */
    public void addRequirement(@NotNull final QuestRequirement requirement) {
        Objects.requireNonNull(requirement, "requirement cannot be null");
        if (!requirements.contains(requirement)) {
            requirements.add(requirement);
        }
    }

    /**
     * Adds a reward to this quest.
     *
     * @param reward the reward to add
     */
    public void addReward(@NotNull final QuestReward reward) {
        Objects.requireNonNull(reward, "reward cannot be null");
        if (!rewards.contains(reward)) {
            rewards.add(reward);
        }
    }

    /**
     * Adds a task to this quest.
     *
     * @param task the task to add
     */
    public void addTask(@NotNull final QuestTask task) {
        Objects.requireNonNull(task, "task cannot be null");
        if (!tasks.contains(task)) {
            tasks.add(task);
        }
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof Quest)) return false;
        final Quest quest = (Quest) o;
        return Objects.equals(identifier, quest.identifier);
    }

    @Override
    public int hashCode() {
        return Objects.hash(identifier);
    }

    @Override
    public String toString() {
        return "Quest{" +
                "id=" + id +
                ", identifier='" + identifier + '\'' +
                ", displayName='" + displayName + '\'' +
                ", difficulty=" + difficulty +
                ", repeatable=" + repeatable +
                ", enabled=" + enabled +
                '}';
    }

    // IProgressionNode implementation

    /**
     * Gets the unique identifier for this quest node.
     * <p>
     * This identifier is used by the progression system to track prerequisites
     * and dependencies between quests.
     * </p>
     *
     * @return the quest identifier (never null or empty)
     */
    @Override
    @NotNull
    public List<String> getPreviousNodeIdentifiers() {
        return new ArrayList<>(prerequisiteQuestIds);
    }

    /**
     * Gets the identifiers of quests that depend on this quest as a prerequisite.
     * <p>
     * When this quest is completed, the progression system will check these dependent
     * quests to see if they can be automatically unlocked.
     * </p>
     *
     * @return list of dependent quest identifiers (never null, may be empty)
     */
    @Override
    @NotNull
    public List<String> getNextNodeIdentifiers() {
        return new ArrayList<>(unlockedQuestIds);
    }
}
