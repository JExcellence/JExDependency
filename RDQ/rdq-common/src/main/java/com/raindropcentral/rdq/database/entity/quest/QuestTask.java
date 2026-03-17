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

import com.raindropcentral.rdq.config.utility.IconSection;
import com.raindropcentral.rdq.database.converter.IconSectionConverter;
import com.raindropcentral.rdq.quest.model.TaskDifficulty;
import de.jexcellence.hibernate.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.io.Serial;
import java.util.Objects;

/**
 * Entity representing a task within a quest.
 *
 * <p>Quest tasks are individual objectives that must be completed as part of a quest.
 * Each task has requirements (stored as JSON) and rewards that are granted upon completion.
 *
 * @author RaindropCentral
 * @version 1.0.0
 */
@Getter
@Setter
@Entity
@Table(
        name = "rdq_quest_task",
        indexes = {
                @Index(name = "idx_quest_task_quest", columnList = "quest_id"),
                @Index(name = "idx_quest_task_identifier", columnList = "task_identifier"),
                @Index(name = "idx_quest_task_order", columnList = "order_index")
        }
)
/**
 * Represents the QuestTask API type.
 */
public class QuestTask extends BaseEntity {
    
    @Serial
    private static final long serialVersionUID = 1L;
    
    /**
     * The quest this task belongs to.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quest_id", nullable = false)
    private Quest quest;
    
    /**
     * Unique identifier for this task within its quest (e.g., "kill_zombies").
     */
    @Column(name = "task_identifier", nullable = false, length = 64)
    private String taskIdentifier;
    
    /**
     * The icon representing this task in the UI.
     * Contains material, display name key, description key, and visual properties.
     */
    @Convert(converter = IconSectionConverter.class)
    @Column(name = "icon", nullable = false, columnDefinition = "LONGTEXT")
    private IconSection icon;
    
    /**
     * Order index for displaying tasks in sequence.
     * Lower values appear first.
     */
    @Column(name = "order_index", nullable = false)
    private int orderIndex = 0;
    
    /**
     * The difficulty level of this task.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty", nullable = false, length = 32)
    private TaskDifficulty difficulty = TaskDifficulty.MEDIUM;
    
    /**
     * JSON data containing RPlatform requirement configuration.
 *
 * <p>This field stores the requirement type and parameters that will be
     * parsed and validated using the RPlatform requirement system.
     */
    @Column(name = "requirement_data", columnDefinition = "TEXT")
    private String requirementData;
    
    /**
     * JSON data containing RPlatform reward configuration.
 *
 * <p>This field stores the reward types and parameters that will be
     * granted upon task completion using the RPlatform reward system.
     */
    @Column(name = "reward_data", columnDefinition = "TEXT")
    private String rewardData;
    
    /**
     * Whether this task must be completed before subsequent tasks can be started.
     * If true, tasks must be completed in order. If false, tasks can be completed in parallel.
     */
    @Column(name = "sequential", nullable = false)
    private boolean sequential = false;
    
    /**
     * Protected no-argument constructor for JPA.
     */
    protected QuestTask() {
    }
    
    /**
     * Constructs a new quest task.
     *
     * @param quest          the quest this task belongs to
     * @param taskIdentifier the unique identifier for this task
     * @param icon           the icon section with display information
     * @param orderIndex     the display order index
     */
    public QuestTask(
            @NotNull final Quest quest,
            @NotNull final String taskIdentifier,
            @NotNull final IconSection icon,
            final int orderIndex
    ) {
        this.quest = quest;
        this.taskIdentifier = taskIdentifier;
        this.icon = icon;
        this.orderIndex = orderIndex;
    }
    
    /**
     * Executes equals.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof QuestTask questTask)) return false;
        
        if (this.getId() != null && questTask.getId() != null) {
            return this.getId().equals(questTask.getId());
        }
        
        return quest != null && quest.equals(questTask.quest) &&
                taskIdentifier != null && taskIdentifier.equals(questTask.taskIdentifier);
    }
    
    /**
     * Returns whether hCode.
     */
    @Override
    public int hashCode() {
        if (this.getId() != null) {
            return this.getId().hashCode();
        }
        
        return Objects.hash(quest, taskIdentifier);
    }
    
    /**
     * Executes toString.
     */
    @Override
    public String toString() {
        return "QuestTask{" +
                "id=" + getId() +
                ", taskIdentifier='" + taskIdentifier + '\'' +
                ", orderIndex=" + orderIndex +
                ", difficulty=" + difficulty +
                ", sequential=" + sequential +
                '}';
    }
}
