package com.raindropcentral.rdq.config.quest;

import de.jexcellence.configmapper.sections.AConfigSection;
import de.jexcellence.configmapper.sections.CSAlways;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration section for task handler settings.
 * Controls which task handlers are enabled and their behavior.
 *
 * @author RaindropCentral
 * @version 1.0.0
 */
@CSAlways
public class TaskHandlersSection extends AConfigSection {
    
    /** Map of task handler types to their enabled status. */
    private Map<String, TaskHandlerConfig> handlers;
    
    /**
     * Constructs a new TaskHandlersSection with the given evaluation environment builder.
     *
     * @param evaluationEnvironmentBuilder the evaluation environment builder
     */
    public TaskHandlersSection(final EvaluationEnvironmentBuilder evaluationEnvironmentBuilder) {
        super(evaluationEnvironmentBuilder);
        this.handlers = new HashMap<>();
    }
    
    /**
     * Checks if a specific task handler is enabled.
     *
     * @param taskType the task type (e.g., "KILL_MOBS")
     * @return true if the handler is enabled, false otherwise
     */
    public boolean isHandlerEnabled(String taskType) {
        TaskHandlerConfig config = handlers.get(taskType);
        return config != null && config.isEnabled();
    }
    
    /**
     * Gets the configuration for a specific task handler.
     *
     * @param taskType the task type
     * @return the handler configuration, or null if not found
     */
    public TaskHandlerConfig getHandlerConfig(String taskType) {
        return handlers.get(taskType);
    }
    
    /**
     * Gets all task handler configurations.
     *
     * @return map of task types to their configurations
     */
    public Map<String, TaskHandlerConfig> getHandlers() {
        return handlers;
    }
    
    /**
     * Configuration for a single task handler.
     */
    public static class TaskHandlerConfig {
        /** Whether this handler is enabled. */
        private Boolean enabled;
        
        /**
         * Checks if this handler is enabled.
         *
         * @return true if enabled, false otherwise
         */
        public boolean isEnabled() {
            return enabled != null && enabled;
        }
        
        /**
         * Sets whether this handler is enabled.
         *
         * @param enabled true to enable, false to disable
         */
        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }
    }
}
