package com.raindropcentral.rdq2.config.ranks.system;

import de.jexcellence.configmapper.sections.AConfigSection;
import de.jexcellence.configmapper.sections.CSAlways;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;

import java.util.List;

/**
 * Configuration section for defining the rules and requirements for achieving the final rank in the system.
 * <p>
 * This section exposes options for requiring all trees, customizing the minimum number of active trees,
 * setting tier requirements, and toggling alternate or ultimate rank availability.
 *
 * @author JExcellence
 * @version 1.0.1
 * @since 1.0.0
 */
@CSAlways
public class FinalRankRuleSection extends AConfigSection {
	
        /**
         * Whether all rank trees are required to achieve the final rank.
         * If {@code null} or {@code true}, all rank trees are required.
         */
	private Boolean requireAllRankTrees;
	
        /**
         * The minimum number of rank trees required to achieve the final rank.
         * If {@code null}, defaults to {@code 4}.
         */
	private Integer minimumRequiredRankTrees;
	
        /**
         * The minimum rank tier required in each rank tree to qualify for the final rank.
         * If {@code null}, defaults to {@code 5}.
         */
	private Integer minimumRankTierPerRankTree;
	
        /**
         * The list of specific rank trees that are required to achieve the final rank.
         * If {@code null}, defaults to ["warrior", "cleric", "mage", "rogue", "merchant"].
         */
	private List<String> specificRequiredRankTrees;
	
        /**
         * Whether alternate final ranks are allowed.
         * If {@code null} or {@code true}, alternate final ranks are permitted.
         */
	private Boolean allowAlternateFinalRank;
	
        /**
         * Whether ultimate ranks are enabled in the system.
         * If {@code null} or {@code true}, ultimate ranks are enabled.
         */
	private Boolean enableUltimateRanks;
	
	/**
	 * Constructs a new FinalRankRuleSection with the given evaluation environment.
	 *
	 * @param baseEnvironment the base evaluation environment for this config section
	 */
	public FinalRankRuleSection(final EvaluationEnvironmentBuilder baseEnvironment) {
		
		super(baseEnvironment);
	}
	
        /**
         * Returns whether all rank trees are required to achieve the final rank.
         * Defaults to {@code true} when not configured.
         *
         * @return {@code true} if all rank trees are required, {@code false} otherwise
         */
	public Boolean getRequireAllRankTrees() {
		
		return
			this.requireAllRankTrees == null ||
			this.requireAllRankTrees;
	}
	
        /**
         * Returns the minimum number of rank trees required to achieve the final rank.
         * Defaults to {@code 4} when not configured.
         *
         * @return the minimum required rank trees, or {@code 4} if unset
         */
	public Integer getMinimumRequiredRankTrees() {
		
		return
			this.minimumRequiredRankTrees == null ?
			4 :
			this.minimumRequiredRankTrees;
	}
	
        /**
         * Returns the minimum rank tier required in each rank tree to qualify for the final rank.
         * Defaults to {@code 5} when not configured.
         *
         * @return the minimum rank tier per rank tree, or {@code 5} if unset
         */
	public Integer getMinimumRankTierPerRankTree() {
		
		return
			this.minimumRankTierPerRankTree == null ?
			5 :
			this.minimumRankTierPerRankTree;
	}
	
        /**
         * Returns the list of specific rank trees required to achieve the final rank.
         * Defaults to ["warrior", "cleric", "mage", "rogue", "merchant"] when not configured.
         *
         * @return the list of specific required rank trees, or the default list if unset
         */
	public List<String> getSpecificRequiredRankTrees() {
		
		return
			this.specificRequiredRankTrees == null ?
			List.of(
				"warrior",
				"cleric",
				"mage",
				"rogue",
				"merchant"
			) :
			this.specificRequiredRankTrees;
	}
	
        /**
         * Returns whether alternate final ranks are allowed.
         * Defaults to {@code true} when not configured.
         *
         * @return {@code true} if alternate final ranks are allowed, {@code false} otherwise
         */
	public Boolean getAllowAlternateFinalRank() {
		
		return
			this.allowAlternateFinalRank == null ||
			this.allowAlternateFinalRank;
	}
	
        /**
         * Returns whether ultimate ranks are enabled in the system.
         * Defaults to {@code true} when not configured.
         *
         * @return {@code true} if ultimate ranks are enabled, {@code false} otherwise
         */
	public Boolean getEnableUltimateRanks() {
		
		return
			this.enableUltimateRanks == null ||
			this.enableUltimateRanks;
	}
	
}