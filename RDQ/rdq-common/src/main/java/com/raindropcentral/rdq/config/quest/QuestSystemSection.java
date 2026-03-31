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

import de.jexcellence.configmapper.sections.AConfigSection;
import de.jexcellence.configmapper.sections.CSAlways;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration section for the quest system.
 * <p>
 * Represents the top-level configuration for the entire quest system,
 * including system-wide settings and all quest categories.
 * </p>
 *
 * @author JExcellence
 * @version 1.0.0
 * @since TBD
 */
@CSAlways
public class QuestSystemSection extends AConfigSection {
    
    /** Maximum number of active quests a player can have simultaneously. */
    private Integer maxActiveQuests;
    
    /** Whether the quest log feature is enabled. */
    private Boolean enableQuestLog;
    
    /** Whether quest tracking is enabled. */
    private Boolean enableQuestTracking;
    
    /** Whether quest notifications are enabled. */
    private Boolean enableQuestNotifications;
    
    /** Map of category keys to their configuration sections. */
    private Map<String, QuestCategorySection> categories;
    
    /**
     * Constructs a new QuestSystemSection with the given evaluation environment builder.
     *
     * @param evaluationEnvironmentBuilder the evaluation environment builder
     */
    public QuestSystemSection(
        final EvaluationEnvironmentBuilder evaluationEnvironmentBuilder
    ) {
        super(evaluationEnvironmentBuilder);
    }
    
    /**
     * Gets the maximum number of active quests a player can have.
     *
     * @return the max active quests, or 5 if not set
     */
    public Integer getMaxActiveQuests() {
        return maxActiveQuests == null ? 5 : maxActiveQuests;
    }
    
    /**
     * Checks if the quest log feature is enabled.
     *
     * @return true if enabled, false otherwise
     */
    public Boolean getEnableQuestLog() {
        return enableQuestLog == null || enableQuestLog;
    }
    
    /**
     * Checks if quest tracking is enabled.
     *
     * @return true if enabled, false otherwise
     */
    public Boolean getEnableQuestTracking() {
        return enableQuestTracking == null || enableQuestTracking;
    }
    
    /**
     * Checks if quest notifications are enabled.
     *
     * @return true if enabled, false otherwise
     */
    public Boolean getEnableQuestNotifications() {
        return enableQuestNotifications == null || enableQuestNotifications;
    }
    
    /**
     * Gets the quest categories.
     *
     * @return the map of category keys to sections, or an empty map if not set
     */
    public Map<String, QuestCategorySection> getCategories() {
        return categories == null ? new HashMap<>() : categories;
    }
}
