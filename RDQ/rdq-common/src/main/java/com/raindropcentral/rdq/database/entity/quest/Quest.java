/*
 * Copyright (c) 2021-2026 Antimatter Zone LLC. All rights reserved.
 *
 * This source code is proprietary and confidential to Antimatter Zone LLC.
 * Unauthorized copying, modification, distribution, display, performance,
 * publication, sublicensing, or creation of derivative works is prohibited
 * without prior written permission from Antimatter Zone LLC, except to the
 * extent permitted by applicable United States law.
 *
 * This notice is intended to preserve all rights and remedies available under
 * the laws of the State of Washington and the United States of America.
 */

package com.raindropcentral.rdq.database.entity.quest;

import com.raindropcentral.rplatform.config.icon.IconSection;
import com.raindropcentral.rdq.database.converter.IconSectionConverter;
import com.raindropcentral.rdq.model.quest.QuestDifficulty;
import com.raindropcentral.rplatform.progression.IProgressionNode;
import de.jexcellence.hibernate.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.io.Serial;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Entity representing a quest.
 * <p>
 * A quest is a collection of tasks that players must complete to earn rewards.
 * Quests can be repeatable, have cooldowns, time limits, and difficulty levels.
 * </p>
 *
 * @author RaindropCentral
 * @version 1.0.0
 */
@Getter
@Setter
@Entity
@Table(
        name = "rdq_quest",
        uniqueConstraints = @UniqueConstraint(columnNames = "identifier"),
        indexes = {
                @Index(name = "idx_quest_identifier", columnList = "identifier"),
                @Index(name = "idx_quest_category", columnList = "category_id"),
                @Index(name = "idx_quest_enabled", columnList = "enabled"),
                @Index(name = "idx_quest_difficulty", columnList = "difficulty")
        }
)
public class Quest extends BaseEntity implements IProgressionNode<Quest> {
    
    @Serial
    private static final long serialVersionUID = 1L;
    
    /**
     * Unique identifier for this quest (e.g., "zombie_slayer").
     */
    @Column(name = "identifier", nullable = false, unique = true, length = 64)
    private String identifier;
    
    /**
     * The category this quest belongs to.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private QuestCategory category;
    
    /**
     * The icon representing this quest in the UI.
     * Contains material, display name key, description key, and visual properties.
     */
    @Convert(converter = IconSectionConverter.class)
    @Column(name = "icon", nullable = false, columnDefinition = "LONGTEXT")
    private IconSection icon;
    
    /**
     * The difficulty level of this quest.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty", nullable = false, length = 32)
    private QuestDifficulty difficulty = QuestDifficulty.MEDIUM;
    
    /**
     * Whether this quest can be completed multiple times.
     */
    @Column(name = "repeatable", nullable = false)
    private boolean repeatable = false;
    
    /**
     * Maximum number of times this quest can be completed.
     * 0 means unlimited.
     */
    @Column(name = "max_completions", nullable = false)
    private int maxCompletions = 0;
    
    /**
     * Cooldown period in seconds between quest completions.
     * 0 means no cooldown.
     */
    @Column(name = "cooldown_seconds", nullable = false)
    private long cooldownSeconds = 0;
    
    /**
     * Time limit in seconds for completing the quest after starting.
     * 0 means no time limit.
     */
    @Column(name = "time_limit_seconds", nullable = false)
    private long timeLimitSeconds = 0;
    
    /**
     * Whether this quest is enabled and available to players.
     */
    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    /**
     * JSON-serialized reward configuration from the quest definition YAML.
     * Contains the Map&lt;String, RewardSection&gt; serialized via Gson.
     */
    @Column(name = "reward_data", columnDefinition = "LONGTEXT")
    private String rewardData;

    /**
     * JSON-serialized requirement configuration from the quest definition YAML.
     * Contains the Map&lt;String, BaseRequirementSection&gt; serialized via Gson.
     */
    @Column(name = "requirement_data", columnDefinition = "LONGTEXT")
    private String requirementData;

    /**
     * Tasks that must be completed for this quest.
     */
    @OneToMany(mappedBy = "quest", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("orderIndex ASC")
    private List<QuestTask> tasks = new ArrayList<>();

    /**
     * Rewards granted upon quest completion.
     */
    @OneToMany(mappedBy = "quest", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("displayOrder ASC")
    private List<QuestReward> rewards = new ArrayList<>();

    /**
     * Requirements that must be met to start this quest.
     */
    @OneToMany(mappedBy = "quest", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<QuestRequirement> requirements = new ArrayList<>();

    /**
     * Identifiers of quests that must be completed before this quest becomes available.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "rdq_quest_prerequisites",
            joinColumns = @JoinColumn(name = "quest_id")
    )
    @Column(name = "prerequisite_quest_identifier")
    private List<String> previousQuestIdentifiers = new ArrayList<>();

    /**
     * Identifiers of quests that are unlocked when this quest is completed.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "rdq_quest_dependents",
            joinColumns = @JoinColumn(name = "quest_id")
    )
    @Column(name = "dependent_quest_identifier")
    private List<String> nextQuestIdentifiers = new ArrayList<>();

    /**
     * Protected no-argument constructor for JPA.
     */
    protected Quest() {
    }
    
    /**
     * Constructs a new quest.
     *
     * @param identifier the unique identifier
     * @param category   the quest category
     * @param icon       the icon section with display information
     * @param difficulty the quest difficulty
     */
    public Quest(
            @NotNull final String identifier,
            @NotNull final QuestCategory category,
            @NotNull final IconSection icon,
            @NotNull final QuestDifficulty difficulty
    ) {
        this.identifier = identifier;
        this.category = category;
        this.icon = icon;
        this.difficulty = difficulty;
    }
    
    /**
     * Checks if this quest is repeatable.
     *
     * @return true if repeatable, false otherwise
     */
    public boolean isRepeatable() {
        return repeatable;
    }
    
    /**
     * Checks if this quest has a time limit.
     *
     * @return true if time-limited, false otherwise
     */
    public boolean hasTimeLimit() {
        return timeLimitSeconds > 0;
    }
    
    /**
     * Gets the time limit as a Duration.
     *
     * @return the time limit duration, or Duration.ZERO if no limit
     */
    public Duration getTimeLimit() {
        return Duration.ofSeconds(timeLimitSeconds);
    }
    
    /**
     * Gets the cooldown period as a Duration.
     *
     * @return the cooldown duration, or Duration.ZERO if no cooldown
     */
    public Duration getCooldown() {
        return Duration.ofSeconds(cooldownSeconds);
    }
    
    /**
     * Adds a task to this quest.
     *
     * @param task the task to add
     */
    public void addTask(@NotNull final QuestTask task) {
        tasks.add(task);
        task.setQuest(this);
    }
    
    /**
     * Removes a task from this quest.
     *
     * @param task the task to remove
     */
    public void removeTask(@NotNull final QuestTask task) {
        tasks.remove(task);
        task.setQuest(null);
    }
    
    public void addReward(@NotNull final QuestReward reward) {
        rewards.add(reward);
    }

    public void addRequirement(@NotNull final QuestRequirement requirement) {
        requirements.add(requirement);
    }

    // -------------------------------------------------------------------------
    // IProgressionNode implementation
    // -------------------------------------------------------------------------

    @Override
    @NotNull
    public List<String> getPreviousNodeIdentifiers() {
        return this.previousQuestIdentifiers;
    }

    @Override
    @NotNull
    public List<String> getNextNodeIdentifiers() {
        return this.nextQuestIdentifiers;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Quest quest)) return false;
        
        if (this.getId() != null && quest.getId() != null) {
            return this.getId().equals(quest.getId());
        }
        
        return identifier != null && identifier.equals(quest.identifier);
    }
    
    @Override
    public int hashCode() {
        if (this.getId() != null) {
            return this.getId().hashCode();
        }
        
        return Objects.hash(identifier);
    }
    
    @Override
    public String toString() {
        return "Quest{" +
                "id=" + getId() +
                ", identifier='" + identifier + '\'' +
                ", difficulty=" + difficulty +
                ", repeatable=" + repeatable +
                ", enabled=" + enabled +
                '}';
    }
}
