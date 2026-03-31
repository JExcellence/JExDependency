package com.raindropcentral.rdq.config.quest;

import de.jexcellence.configmapper.sections.AConfigSection;
import de.jexcellence.configmapper.sections.CSAlways;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;

/**
 * Configuration section for reward distribution settings.
 * Controls how rewards are distributed to players.
 *
 * @author RaindropCentral
 * @version 1.0.0
 */
@CSAlways
public class RewardsSection extends AConfigSection {
    
    /** Whether to retry failed currency rewards. */
    private Boolean retryCurrency;
    
    /** Whether to drop items on ground if inventory is full. */
    private Boolean dropOnFull;
    
    /** Whether to continue distributing rewards if one fails. */
    private Boolean continueOnError;
    
    /**
     * Constructs a new RewardsSection with the given evaluation environment builder.
     *
     * @param evaluationEnvironmentBuilder the evaluation environment builder
     */
    public RewardsSection(final EvaluationEnvironmentBuilder evaluationEnvironmentBuilder) {
        super(evaluationEnvironmentBuilder);
    }
    
    /**
     * Checks if currency rewards should be retried on failure.
     *
     * @return true if retry is enabled, false otherwise
     */
    public Boolean getRetryCurrency() {
        return retryCurrency == null || retryCurrency;
    }
    
    /**
     * Checks if items should be dropped on ground when inventory is full.
     *
     * @return true if drop on full is enabled, false otherwise
     */
    public Boolean getDropOnFull() {
        return dropOnFull == null || dropOnFull;
    }
    
    /**
     * Checks if reward distribution should continue when one reward fails.
     *
     * @return true if continue on error is enabled, false otherwise
     */
    public Boolean getContinueOnError() {
        return continueOnError == null || continueOnError;
    }
    
    /**
     * Sets whether to retry failed currency rewards.
     *
     * @param retryCurrency true to enable retry, false to disable
     */
    public void setRetryCurrency(Boolean retryCurrency) {
        this.retryCurrency = retryCurrency;
    }
    
    /**
     * Sets whether to drop items on ground if inventory is full.
     *
     * @param dropOnFull true to enable drop on full, false to disable
     */
    public void setDropOnFull(Boolean dropOnFull) {
        this.dropOnFull = dropOnFull;
    }
    
    /**
     * Sets whether to continue distributing rewards if one fails.
     *
     * @param continueOnError true to enable continue on error, false to disable
     */
    public void setContinueOnError(Boolean continueOnError) {
        this.continueOnError = continueOnError;
    }
}
