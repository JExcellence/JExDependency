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
import com.raindropcentral.rdq.config.utility.IconSection;
import com.raindropcentral.rdq.config.utility.RewardSection;
import de.jexcellence.configmapper.sections.AConfigSection;
import de.jexcellence.configmapper.sections.CSAlways;
import de.jexcellence.configmapper.sections.CSIgnore;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Configuration section for a quest category.
 * <p>
 * Represents a category of quests, including display properties, requirements,
 * rewards, and nested quests. Categories are used to organize quests into
 * logical groups (e.g., Tutorial, Combat, Mining, Challenge).
 * </p>
 *
 * @author JExcellence
 * @version 1.0.0
 * @since TBD
 */
@CSAlways
public class QuestCategorySection extends AConfigSection {
    
    /** The localization key for the display name of the category. */
    private String displayNameKey;
    
    /** The localization key for the description of the category. */
    private String descriptionKey;
    
    /** The display order of this category in UI or lists. */
    private Integer displayOrder;
    
    /** The icon representing this category. */
    private IconSection icon;
    
    /** Whether this category is enabled. */
    private Boolean isEnabled;
    
    /** Map of requirement keys to their configuration sections. */
    private Map<String, BaseRequirementSection> requirements;
    
    /** Map of reward keys to their configuration sections. */
    private Map<String, RewardSection> rewards;
    
    /** Map of quest keys to their configuration sections. */
    private Map<String, QuestSection> quests;
    
    /** The category ID (set by parent). */
    @CSIgnore
    private String categoryId;
    
    /**
     * Constructs a new QuestCategorySection with the given evaluation environment builder.
     *
     * @param evaluationEnvironmentBuilder the evaluation environment builder
     */
    public QuestCategorySection(
        final EvaluationEnvironmentBuilder evaluationEnvironmentBuilder
    ) {
        super(evaluationEnvironmentBuilder);
    }
    
    /**
     * Called after parsing the configuration fields. Sets default localization keys
     * and processes nested quests.
     *
     * @param fields the list of fields parsed
     * @throws Exception if an error occurs during post-processing
     */
    @Override
    public void afterParsing(final List<Field> fields) throws Exception {
        super.afterParsing(fields);
        
        if (categoryId != null) {
            if (displayNameKey == null) {
                displayNameKey = "quest.category." + categoryId + ".name";
            }
            if (descriptionKey == null) {
                descriptionKey = "quest.category." + categoryId + ".description";
            }
            
            // Process nested quests
            if (quests != null) {
                for (Map.Entry<String, QuestSection> entry : quests.entrySet()) {
                    QuestSection quest = entry.getValue();
                    quest.setCategoryId(categoryId);
                    quest.setQuestId(entry.getKey());
                    quest.afterParsing(new ArrayList<>());
                }
            }
        }
    }
    
    /**
     * Gets the display name key for this category.
     *
     * @return the display name key, or "not_defined" if not set
     */
    public String getDisplayNameKey() {
        return displayNameKey == null ? "not_defined" : displayNameKey;
    }
    
    /**
     * Gets the description key for this category.
     *
     * @return the description key, or "not_defined" if not set
     */
    public String getDescriptionKey() {
        return descriptionKey == null ? "not_defined" : descriptionKey;
    }
    
    /**
     * Gets the display order of this category.
     *
     * @return the display order, or -1 if not set
     */
    public Integer getDisplayOrder() {
        return displayOrder == null ? -1 : displayOrder;
    }
    
    /**
     * Gets the icon section for this category.
     *
     * @return the icon section, or a new default IconSection if not set
     */
    public IconSection getIcon() {
        return icon == null ? new IconSection(new EvaluationEnvironmentBuilder()) : icon;
    }
    
    /**
     * Checks if this category is enabled.
     *
     * @return true if enabled, false otherwise
     */
    public Boolean getEnabled() {
        return isEnabled != null && isEnabled;
    }
    
    /**
     * Gets the requirements for this category.
     *
     * @return the map of requirement keys to sections, or an empty map if not set
     */
    public Map<String, BaseRequirementSection> getRequirements() {
        return requirements == null ? new HashMap<>() : requirements;
    }
    
    /**
     * Gets the rewards for this category.
     *
     * @return the map of reward keys to sections, or an empty map if not set
     */
    public Map<String, RewardSection> getRewards() {
        return rewards == null ? new HashMap<>() : rewards;
    }
    
    /**
     * Gets the quests for this category.
     *
     * @return the map of quest keys to sections, or an empty map if not set
     */
    public Map<String, QuestSection> getQuests() {
        return quests == null ? new HashMap<>() : quests;
    }
    
    /**
     * Gets the category ID.
     *
     * @return the category ID, or a generated one if not set
     */
    public String getCategoryId() {
        return categoryId == null ? "not_defined_" + UUID.randomUUID() : categoryId;
    }
    
    /**
     * Sets the category ID.
     *
     * @param categoryId the category ID
     */
    public void setCategoryId(final String categoryId) {
        this.categoryId = categoryId;
    }
}
