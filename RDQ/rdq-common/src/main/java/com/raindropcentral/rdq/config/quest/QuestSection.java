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
import com.raindropcentral.rplatform.config.icon.IconSection;
import de.jexcellence.configmapper.sections.AConfigSection;
import de.jexcellence.configmapper.sections.CSAlways;
import de.jexcellence.configmapper.sections.CSIgnore;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Represents a configuration section for a quest.
 * Contains all properties, requirements, rewards, and tasks for a specific quest.
 *
 * @author RaindropCentral
 * @version 1.0.0
 */
@CSAlways
public class QuestSection extends AConfigSection {
    
    /** The unique identifier for this quest. */
    private String identifier;
    
    /** The category this quest belongs to. */
    private String category;
    
    /** The difficulty level of this quest. */
    private String difficulty;
    
    /** Whether this quest is enabled. */
    private Boolean enabled;
    
    /** The display order of this quest within its category. */
    private Integer displayOrder;
    
    /** Whether this quest is repeatable. */
    private Boolean repeatable;
    
    /** Maximum number of completions allowed (-1 for unlimited). */
    private Integer maxCompletions;
    
    /** Cooldown in seconds between completions. */
    private Integer cooldownSeconds;
    
    /** Time limit in seconds (0 for no limit). */
    private Integer timeLimitSeconds;
    
    /** The key for the display name of the quest (for localization). */
    private String displayNameKey;
    
    /** The key for the description of the quest (for localization). */
    private String descriptionKey;
    
    /** The icon representing this quest. */
    private IconSection icon;
    
    /** Map of requirement keys to their configuration sections. */
    private Map<String, BaseRequirementSection> requirements;
    
    /** Map of task keys to their configuration sections. */
    private Map<String, QuestTaskSection> tasks;
    
    /** Map of reward keys to their configuration sections. */
    private Map<String, RewardSection> rewards;
    
    /** The quest identifier (set during parsing). */
    @CSIgnore
    private String questId;
    
    /**
     * Constructs a new QuestSection with the given evaluation environment builder.
     *
     * @param evaluationEnvironmentBuilder the evaluation environment builder
     */
    public QuestSection(final EvaluationEnvironmentBuilder evaluationEnvironmentBuilder) {
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
        
        if (this.questId != null) {
            if (this.displayNameKey == null) {
                this.displayNameKey = "quest." + this.questId + ".name";
            }
            if (this.descriptionKey == null) {
                this.descriptionKey = "quest." + this.questId + ".description";
            }
        }
        
        // Set quest ID for all tasks
        if (this.tasks != null) {
            for (Map.Entry<String, QuestTaskSection> entry : this.tasks.entrySet()) {
                entry.getValue().setQuestId(this.questId);
                entry.getValue().setTaskId(entry.getKey());
            }
        }
    }
    
    /**
     * Gets the unique identifier for this quest.
     *
     * @return the quest identifier, or a generated one if not set
     */
    public String getIdentifier() {
        return this.identifier == null ? "not_defined_" + UUID.randomUUID() : this.identifier;
    }
    
    /**
     * Gets the category this quest belongs to.
     *
     * @return the category, or "general" if not set
     */
    public String getCategory() {
        return this.category == null ? "general" : this.category;
    }
    
    /**
     * Gets the difficulty level of this quest.
     *
     * @return the difficulty, or "EASY" if not set
     */
    public String getDifficulty() {
        return this.difficulty == null ? "EASY" : this.difficulty;
    }
    
    /**
     * Checks if this quest is enabled.
     *
     * @return true if enabled, false otherwise
     */
    public Boolean getEnabled() {
        return this.enabled != null && this.enabled;
    }
    
    /**
     * Gets the display order of this quest.
     *
     * @return the display order, or 0 if not set
     */
    public Integer getDisplayOrder() {
        return this.displayOrder == null ? 0 : this.displayOrder;
    }
    
    /**
     * Checks if this quest is repeatable.
     *
     * @return true if repeatable, false otherwise
     */
    public Boolean getRepeatable() {
        return this.repeatable != null && this.repeatable;
    }
    
    /**
     * Gets the maximum number of completions allowed.
     *
     * @return the max completions, or 1 if not set
     */
    public Integer getMaxCompletions() {
        return this.maxCompletions == null ? 1 : this.maxCompletions;
    }
    
    /**
     * Gets the cooldown in seconds between completions.
     *
     * @return the cooldown seconds, or 0 if not set
     */
    public Integer getCooldownSeconds() {
        return this.cooldownSeconds == null ? 0 : this.cooldownSeconds;
    }
    
    /**
     * Gets the time limit in seconds.
     *
     * @return the time limit seconds, or 0 if not set
     */
    public Integer getTimeLimitSeconds() {
        return this.timeLimitSeconds == null ? 0 : this.timeLimitSeconds;
    }
    
    /**
     * Gets the display name key for this quest.
     *
     * @return the display name key, or "not_defined" if not set
     */
    public String getDisplayNameKey() {
        return this.displayNameKey == null ? "not_defined" : this.displayNameKey;
    }
    
    /**
     * Gets the description key for this quest.
     *
     * @return the description key, or "not_defined" if not set
     */
    public String getDescriptionKey() {
        return this.descriptionKey == null ? "not_defined" : this.descriptionKey;
    }
    
    /**
     * Gets the icon section for this quest.
     *
     * @return the icon section, or a new default IconSection if not set
     */
    public IconSection getIcon() {
        return this.icon == null ? new IconSection(new EvaluationEnvironmentBuilder()) : this.icon;
    }
    
    /**
     * Gets the requirements for this quest.
     *
     * @return the map of requirement keys to sections, or an empty map if not set
     */
    public Map<String, BaseRequirementSection> getRequirements() {
        return this.requirements == null ? new HashMap<>() : this.requirements;
    }
    
    /**
     * Gets the tasks for this quest.
     *
     * @return the map of task keys to sections, or an empty map if not set
     */
    public Map<String, QuestTaskSection> getTasks() {
        return this.tasks == null ? new HashMap<>() : this.tasks;
    }
    
    /**
     * Gets the rewards for this quest.
     *
     * @return the map of reward keys to sections, or an empty map if not set
     */
    public Map<String, RewardSection> getRewards() {
        return this.rewards == null ? new HashMap<>() : this.rewards;
    }
    
    /**
     * Gets the quest ID.
     *
     * @return the quest ID, or a generated one if not set
     */
    public String getQuestId() {
        return this.questId == null ? "not_defined_" + UUID.randomUUID() : this.questId;
    }
    
    /**
     * Sets the quest ID.
     *
     * @param questId the quest ID
     */
    public void setQuestId(final String questId) {
        this.questId = questId;
    }
}
