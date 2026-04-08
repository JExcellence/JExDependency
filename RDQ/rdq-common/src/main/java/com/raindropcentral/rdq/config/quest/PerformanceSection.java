package com.raindropcentral.rdq.config.quest;

import de.jexcellence.configmapper.sections.AConfigSection;
import de.jexcellence.configmapper.sections.CSAlways;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;

/**
 * Configuration section for performance optimization settings.
 * Controls how the quest system optimizes performance.
 *
 * @author RaindropCentral
 * @version 1.0.0
 */
@CSAlways
public class PerformanceSection extends AConfigSection {
    
    /** Whether to skip processing for players with no active quests. */
    private Boolean skipInactivePlayers;
    
    /** Whether to use cached quest data instead of database queries. */
    private Boolean useCache;
    
    /** Maximum time in milliseconds to spend processing a single event. */
    private Integer maxProcessingTime;
    
    /**
     * Constructs a new PerformanceSection with the given evaluation environment builder.
     *
     * @param evaluationEnvironmentBuilder the evaluation environment builder
     */
    public PerformanceSection(final EvaluationEnvironmentBuilder evaluationEnvironmentBuilder) {
        super(evaluationEnvironmentBuilder);
    }
    
    /**
     * Checks if processing should be skipped for players with no active quests.
     *
     * @return true if skip is enabled, false otherwise
     */
    public Boolean getSkipInactivePlayers() {
        return skipInactivePlayers == null || skipInactivePlayers;
    }
    
    /**
     * Checks if cached quest data should be used instead of database queries.
     *
     * @return true if cache usage is enabled, false otherwise
     */
    public Boolean getUseCache() {
        return useCache == null || useCache;
    }
    
    /**
     * Gets the maximum time in milliseconds to spend processing a single event.
     *
     * @return the max processing time, or 5ms if not set
     */
    public Integer getMaxProcessingTime() {
        return maxProcessingTime == null ? 5 : maxProcessingTime;
    }
    
    /**
     * Sets whether to skip processing for players with no active quests.
     *
     * @param skipInactivePlayers true to enable skip, false to disable
     */
    public void setSkipInactivePlayers(Boolean skipInactivePlayers) {
        this.skipInactivePlayers = skipInactivePlayers;
    }
    
    /**
     * Sets whether to use cached quest data instead of database queries.
     *
     * @param useCache true to enable cache usage, false to disable
     */
    public void setUseCache(Boolean useCache) {
        this.useCache = useCache;
    }
    
    /**
     * Sets the maximum time in milliseconds to spend processing a single event.
     *
     * @param maxProcessingTime the max processing time in milliseconds
     */
    public void setMaxProcessingTime(Integer maxProcessingTime) {
        this.maxProcessingTime = maxProcessingTime;
    }
}
