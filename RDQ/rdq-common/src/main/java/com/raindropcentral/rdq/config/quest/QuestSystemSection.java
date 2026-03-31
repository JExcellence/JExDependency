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

/**
 * Represents the main configuration section for the quest system.
 * Contains global quest system settings.
 *
 * @author RaindropCentral
 * @version 1.0.0
 */
@CSAlways
public class QuestSystemSection extends AConfigSection {
    
    /** Whether the quest system is enabled. */
    private Boolean enabled;
    
    /** Maximum number of active quests per player. */
    private Integer maxActiveQuests;
    
    /** Whether to auto-save quest progress. */
    private Boolean autoSave;
    
    /** Auto-save interval in seconds. */
    private Integer autoSaveInterval;
    
    /** Whether to broadcast quest completions. */
    private Boolean broadcastCompletions;
    
    /** Whether to show quest notifications. */
    private Boolean showNotifications;
    
    /** Whether to track quest statistics. */
    private Boolean trackStatistics;
    
    /**
     * Constructs a new QuestSystemSection with the given evaluation environment builder.
     *
     * @param evaluationEnvironmentBuilder the evaluation environment builder
     */
    public QuestSystemSection(final EvaluationEnvironmentBuilder evaluationEnvironmentBuilder) {
        super(evaluationEnvironmentBuilder);
    }
    
    /**
     * Checks if the quest system is enabled.
     *
     * @return true if enabled, false otherwise
     */
    public Boolean getEnabled() {
        return this.enabled != null && this.enabled;
    }
    
    /**
     * Gets the maximum number of active quests per player.
     *
     * @return the max active quests, or 5 if not set
     */
    public Integer getMaxActiveQuests() {
        return this.maxActiveQuests == null ? 5 : this.maxActiveQuests;
    }
    
    /**
     * Checks if auto-save is enabled.
     *
     * @return true if auto-save is enabled, false otherwise
     */
    public Boolean getAutoSave() {
        return this.autoSave != null && this.autoSave;
    }
    
    /**
     * Gets the auto-save interval in seconds.
     *
     * @return the auto-save interval, or 300 (5 minutes) if not set
     */
    public Integer getAutoSaveInterval() {
        return this.autoSaveInterval == null ? 300 : this.autoSaveInterval;
    }
    
    /**
     * Checks if quest completions should be broadcast.
     *
     * @return true if broadcast is enabled, false otherwise
     */
    public Boolean getBroadcastCompletions() {
        return this.broadcastCompletions != null && this.broadcastCompletions;
    }
    
    /**
     * Checks if quest notifications should be shown.
     *
     * @return true if notifications are enabled, false otherwise
     */
    public Boolean getShowNotifications() {
        return this.showNotifications == null || this.showNotifications;
    }
    
    /**
     * Checks if quest statistics should be tracked.
     *
     * @return true if statistics tracking is enabled, false otherwise
     */
    public Boolean getTrackStatistics() {
        return this.trackStatistics == null || this.trackStatistics;
    }
}
