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

package com.raindropcentral.rdq.config.quest;

import com.raindropcentral.rdq.config.requirement.BaseRequirementSection;
import com.raindropcentral.rdq.config.utility.RewardSection;
import de.jexcellence.configmapper.sections.AConfigSection;
import de.jexcellence.configmapper.sections.CSAlways;
import de.jexcellence.configmapper.sections.CSIgnore;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;

import java.lang.reflect.Field;
import java.util.List;
import java.util.UUID;

/**
 * Represents a configuration section for a quest task.
 * Contains all properties, requirements, and rewards for a specific task within a quest.
 *
 * @author RaindropCentral
 * @version 1.0.0
 */
@CSAlways
public class QuestTaskSection extends AConfigSection {
    
    /** The unique identifier for this task. */
    private String identifier;
    
    /** The key for the display name of the task (for localization). */
    private String nameKey;
    
    /** The key for the description of the task (for localization). */
    private String descriptionKey;
    
    /** The order index of this task within the quest. */
    private Integer orderIndex;
    
    /** The difficulty level of this task. */
    private String difficulty;
    
    /** Whether this task must be completed before the next task can start. */
    private Boolean sequential;
    
    /** The requirement that must be met to complete this task. */
    private BaseRequirementSection requirement;
    
    /** The reward given when this task is completed. */
    private RewardSection reward;
    
    /** The quest ID this task belongs to (set during parsing). */
    @CSIgnore
    private String questId;
    
    /** The task ID (set during parsing). */
    @CSIgnore
    private String taskId;
    
    /**
     * Constructs a new QuestTaskSection with the given evaluation environment builder.
     *
     * @param evaluationEnvironmentBuilder the evaluation environment builder
     */
    public QuestTaskSection(final EvaluationEnvironmentBuilder evaluationEnvironmentBuilder) {
        super(evaluationEnvironmentBuilder);
    }
    
    /**
     * Called after parsing the configuration fields. Sets default localization keys if not provided.
     *
     * @param fields the list of fields parsed
     * @throws Exception if an error occurs during post-processing
     */
    @Override
    public void afterParsing(final List<Field> fields) throws Exception {
        super.afterParsing(fields);
        
        if (this.questId != null && this.taskId != null) {
            if (this.nameKey == null) {
                this.nameKey = "quest." + this.questId + ".task." + this.taskId + ".name";
            }
            if (this.descriptionKey == null) {
                this.descriptionKey = "quest." + this.questId + ".task." + this.taskId + ".description";
            }
        }
    }
    
    /**
     * Gets the unique identifier for this task.
     *
     * @return the task identifier, or a generated one if not set
     */
    public String getIdentifier() {
        return this.identifier == null ? "not_defined_" + UUID.randomUUID() : this.identifier;
    }
    
    /**
     * Gets the name key for this task.
     *
     * @return the name key, or "not_defined" if not set
     */
    public String getNameKey() {
        return this.nameKey == null ? "not_defined" : this.nameKey;
    }
    
    /**
     * Gets the description key for this task.
     *
     * @return the description key, or "not_defined" if not set
     */
    public String getDescriptionKey() {
        return this.descriptionKey == null ? "not_defined" : this.descriptionKey;
    }
    
    /**
     * Gets the order index of this task.
     *
     * @return the order index, or 0 if not set
     */
    public Integer getOrderIndex() {
        return this.orderIndex == null ? 0 : this.orderIndex;
    }
    
    /**
     * Gets the difficulty level of this task.
     *
     * @return the difficulty, or "EASY" if not set
     */
    public String getDifficulty() {
        return this.difficulty == null ? "EASY" : this.difficulty;
    }
    
    /**
     * Checks if this task is sequential.
     *
     * @return true if sequential, false otherwise
     */
    public Boolean getSequential() {
        return this.sequential != null && this.sequential;
    }
    
    /**
     * Gets the requirement for this task.
     *
     * @return the requirement section, or null if not set
     */
    public BaseRequirementSection getRequirement() {
        return this.requirement;
    }
    
    /**
     * Gets the reward for this task.
     *
     * @return the reward section, or null if not set
     */
    public RewardSection getReward() {
        return this.reward;
    }
    
    /**
     * Gets the quest ID this task belongs to.
     *
     * @return the quest ID, or a generated one if not set
     */
    public String getQuestId() {
        return this.questId == null ? "not_defined_" + UUID.randomUUID() : this.questId;
    }
    
    /**
     * Sets the quest ID this task belongs to.
     *
     * @param questId the quest ID
     */
    public void setQuestId(final String questId) {
        this.questId = questId;
    }
    
    /**
     * Gets the task ID.
     *
     * @return the task ID, or a generated one if not set
     */
    public String getTaskId() {
        return this.taskId == null ? "not_defined_" + UUID.randomUUID() : this.taskId;
    }
    
    /**
     * Sets the task ID.
     *
     * @param taskId the task ID
     */
    public void setTaskId(final String taskId) {
        this.taskId = taskId;
    }
}
