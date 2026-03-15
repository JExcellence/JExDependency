package com.raindropcentral.rdq.config.ranks.system;

import de.jexcellence.configmapper.sections.AConfigSection;
import de.jexcellence.configmapper.sections.CSAlways;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;

import java.util.List;

/**
 * Configuration section for progression rules in the rank system.
 *
 * <p>This class defines various rules and options that control how users progress through ranks,
 * including linear progression, skipping, multiple active trees, switching, and related costs.
 *
 * @author JExcellence
 * @version 1.0.0
 * @since TBD
 */
@CSAlways
public class ProgressionRuleSection extends AConfigSection {

    /**
     * Whether linear rank progression is required (default: true).
     */
    private Boolean requireLinearRankProgression;

    /**
     * Whether skipping ranks is allowed (default: false).
     */
    private Boolean allowSkippingRanks;

    /**
     * Whether multiple active rank trees are enabled (default: true).
     */
    private Boolean enableMultipleActiveRankTrees;

    /**
     * The maximum number of active rank trees allowed (default: 2).
     */
    private Integer maximumActiveRankTrees;

    /**
     * Whether confirmation is required for rank up (default: true).
     */
    private Boolean requireConfirmationForRankUp;

    /**
     * Whether switching between different rank trees is enabled (default: true).
     */
    private Boolean enableCrossRankTreeSwitching;

    /**
     * The global tiers at which switching between rank trees is allowed (default: [3, 5, 7, 10]).
     */
    private List<Integer> globalSwitchingTiers;

    /**
     * The cooldown period (in seconds) for switching between rank trees (default: 1728000L).
     */
    private Long switchingCooldown;

    /**
     * The cost expression for switching between rank trees (default: empty string).
     */
    private String switchingCost;

    /**
     * Constructs a new ProgressionRuleSection with the given evaluation environment.
     *
     * @param baseEnvironment the base evaluation environment for this section
     */
    public ProgressionRuleSection(final EvaluationEnvironmentBuilder baseEnvironment) {
        super(baseEnvironment);
    }

    /**
     * Returns whether linear rank progression is required.
     *
     * @return true if linear progression is required or not set, false otherwise
     */
    public Boolean getRequireLinearRankProgression() {
        return
            this.requireLinearRankProgression == null ||
            this.requireLinearRankProgression;
    }

    /**
     * Returns whether skipping ranks is allowed.
     *
     * @return true if skipping ranks is allowed, false otherwise
     */
    public Boolean getAllowSkippingRanks() {
        return
            this.allowSkippingRanks != null &&
            this.allowSkippingRanks;
    }

    /**
     * Returns whether multiple active rank trees are enabled.
     *
     * @return true if multiple active rank trees are enabled or not set, false otherwise
     */
    public Boolean getEnableMultipleActiveRankTrees() {
        return
            this.enableMultipleActiveRankTrees == null ||
            this.enableMultipleActiveRankTrees;
    }

    /**
     * Returns the maximum number of active rank trees allowed.
     *
     * @return the maximum number of active rank trees, or 2 if not set
     */
    public Integer getMaximumActiveRankTrees() {
        return
            this.maximumActiveRankTrees == null ?
            2 :
            this.maximumActiveRankTrees;
    }

    /**
     * Returns whether confirmation is required for rank up.
     *
     * @return true if confirmation is required or not set, false otherwise
     */
    public Boolean getRequireConfirmationForRankUp() {
        return
            this.requireConfirmationForRankUp == null ||
            this.requireConfirmationForRankUp;
    }

    /**
     * Returns whether switching between different rank trees is enabled.
     *
     * @return true if cross rank tree switching is enabled or not set, false otherwise
     */
    public Boolean getEnableCrossRankTreeSwitching() {
        return
            this.enableCrossRankTreeSwitching == null ||
            this.enableCrossRankTreeSwitching;
    }

    /**
     * Returns the global tiers at which switching between rank trees is allowed.
     *
     * @return a list of switching tiers, or [3, 5, 7, 10] if not set
     */
    public List<Integer> getGlobalSwitchingTiers() {
        return
            this.globalSwitchingTiers == null ?
            List.of(
                3,
                5,
                7,
                10
            ) :
            this.globalSwitchingTiers;
    }

    /**
     * Returns the cooldown period (in seconds) for switching between rank trees.
     *
     * @return the switching cooldown, or 1728000L if not set
     */
    public Long getSwitchingCooldown() {
        return
            this.switchingCooldown == null ?
            1728000L :
            this.switchingCooldown;
    }

    /**
     * Returns the cost expression for switching between rank trees.
     *
     * @return the switching cost expression, or an empty string if not set
     */
    public String getSwitchingCost() {
        return
            this.switchingCost == null ?
            "" :
            this.switchingCost;
    }

}
