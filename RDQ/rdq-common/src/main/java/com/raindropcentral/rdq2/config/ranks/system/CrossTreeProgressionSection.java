package com.raindropcentral.rdq2.config.ranks.system;

import de.jexcellence.configmapper.sections.AConfigSection;
import de.jexcellence.configmapper.sections.CSAlways;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration section for cross-tree rank progression settings.
 * <p>
 * This section controls whether players can switch between rank trees, the conditions and costs for switching,
 * and any cooldowns or restrictions that apply. Used in the context of rank progression systems where multiple
 * rank trees exist and players may want to move between them.
 *
 * @author JExcellence
 * @version 1.0.1
 * @since 1.0.0
 */
@CSAlways
public class CrossTreeProgressionSection extends AConfigSection {

    /**
     * Whether switching between rank paths is allowed.
     * If {@code true}, players can move from one rank tree to another.
     */
    private Boolean allowSwitching;

    /**
     * List of rank tiers eligible for switching between trees.
     * Only players in these tiers can switch paths.
     */
    private List<Integer> switchRankTiers;

    /**
     * Whether switching is restricted to the same rank tier.
     * If {@code true}, players can only switch to the same tier in another path.
     */
    private Boolean requireSameRankTier;

    /**
     * Whether downgrading to a lower rank is allowed when switching paths.
     * If {@code true}, players can move to a lower rank in another path.
     */
    private Boolean allowDowngrade;

    /**
     * The cost expression or value for switching between rank paths.
     * Can be a string representing a formula or a fixed path.
     */
    private String switchingCosts;

    /**
     * The cooldown period (in seconds) required between switches.
     * Defaults to 172800 seconds (2 days) if not set.
     */
    private Long switchingCooldown;

    /**
     * Constructs a new CrossTreeProgressionSection with the given evaluation environment.
     *
     * @param baseEnvironment the base evaluation environment for config mapping and expression evaluation
     */
    public CrossTreeProgressionSection(final EvaluationEnvironmentBuilder baseEnvironment) {

        super(baseEnvironment);
    }

    /**
     * Checks if switching between rank trees is allowed.
     *
     * @return {@code true} if switching is allowed; otherwise {@code false}
     */
    public Boolean getAllowSwitching() {

        return this.allowSwitching != null && this.allowSwitching;
    }

    /**
     * Gets the list of rank tiers that are eligible for switching between trees.
     *
     * @return a mutable list of rank tier integers; empty if not set
     */
    public List<Integer> getSwitchRankTiers() {

        return this.switchRankTiers == null ? new ArrayList<>() : this.switchRankTiers;
    }

    /**
     * Checks if switching is restricted to the same rank tier.
     *
     * @return {@code true} if switching requires the same rank tier; otherwise {@code false}
     */
    public Boolean getRequireSameRankTier() {

        return this.requireSameRankTier == null || this.requireSameRankTier;
    }

    /**
     * Checks if downgrading to a lower rank is allowed when switching trees.
     *
     * @return {@code true} if downgrading is allowed; otherwise {@code false}
     */
    public Boolean getAllowDowngrade() {

        return this.allowDowngrade == null || this.allowDowngrade;
    }

    /**
     * Gets the cost expression or value for switching between rank trees.
     *
     * @return the switching cost as a string, or an empty string if not set
     */
    public String getSwitchingCosts() {

        return this.switchingCosts == null ? "" : this.switchingCosts;
    }

    /**
     * Gets the cooldown period (in seconds) required between switches.
     *
     * @return the cooldown in seconds; defaults to 172800 (2 days) if not set
     */
    public Long getSwitchingCooldown() {

        return this.switchingCooldown == null ? 172800L : this.switchingCooldown;
    }

}
