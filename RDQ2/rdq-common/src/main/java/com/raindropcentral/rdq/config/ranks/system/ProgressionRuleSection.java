package com.raindropcentral.rdq.config.ranks.system;

import de.jexcellence.configmapper.sections.AConfigSection;
import de.jexcellence.configmapper.sections.CSAlways;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;

import java.util.List;

/**
 * Configuration section for progression rules in the rank system.
 * <p>
 * This section encapsulates progression-related toggles and thresholds that may be omitted in the
 * backing configuration. When a property is left undefined, the getters expose the documented
 * default values while still allowing external configuration tools to observe {@code null} during
 * serialization. Consumers should therefore rely on the accessors rather than the raw fields to
 * honour the defaulting behaviour.
 *
 * @author JExcellence
 * @version 1.0.1
 * @since 1.0.0
 */
@CSAlways
public class ProgressionRuleSection extends AConfigSection {

    /**
     * Whether linear rank progression is required (default: {@code true}).
     */
    private Boolean requireLinearRankProgression;

    /**
     * Whether skipping ranks is allowed (default: {@code false}).
     */
    private Boolean allowSkippingRanks;

    /**
     * Whether multiple active rank trees are enabled (default: {@code true}).
     */
    private Boolean enableMultipleActiveRankTrees;

    /**
     * The maximum number of active rank trees allowed (default: 2).
     */
    private Integer maximumActiveRankTrees;

    /**
     * Whether confirmation is required for rank up (default: {@code true}).
     */
    private Boolean requireConfirmationForRankUp;

    /**
     * Whether switching between different rank trees is enabled (default: {@code true}).
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
     * Constructs a new ProgressionRuleSection bound to the supplied evaluation environment.
     * <p>
     * The environment is propagated to nested expressions so that any cost or condition strings
     * contained in this section can be resolved consistently with other configuration fragments.
     *
     * @param baseEnvironment the base evaluation environment for this section
     */
    public ProgressionRuleSection(final EvaluationEnvironmentBuilder baseEnvironment) {
        super(baseEnvironment);
    }

    /**
     * Returns whether linear rank progression is required.
     * <p>
     * A {@code null} value in the underlying configuration is interpreted as {@code true} to
     * preserve the traditional linear progression requirement.
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
     * <p>
     * The absence of an explicit flag defaults to {@code false}, ensuring rank paths remain
     * sequential unless the behaviour is deliberately enabled.
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
     * <p>
     * When the configuration omits this flag the method answers {@code true}, allowing accounts to
     * participate in multiple trees as per legacy defaults.
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
     * <p>
     * Omitted values fall back to {@code 2}, mirroring the legacy configuration baseline.
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
     * <p>
     * The default behaviour is to require confirmation, so {@code null} entries are treated as
     * {@code true}.
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
     * <p>
     * A missing flag is considered {@code true} to maintain compatibility with previous
     * configurations that allowed cross-tree transitions by default.
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
     * <p>
     * If the configuration omits a tier list, this method provides the legacy sequence
     * {@code [3, 5, 7, 10]} which represents the global milestones where switching becomes
     * available.
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
     * <p>
     * Falling back to {@code 1_728_000L} ensures a two-week cooldown when the configuration omits
     * a value.
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
     * <p>
     * An empty string is returned when no explicit expression is provided, signalling that switching
     * should remain free of charge.
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