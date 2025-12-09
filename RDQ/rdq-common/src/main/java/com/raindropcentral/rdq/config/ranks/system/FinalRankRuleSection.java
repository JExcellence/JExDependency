package com.raindropcentral.rdq.config.ranks.system;

import de.jexcellence.configmapper.sections.AConfigSection;
import de.jexcellence.configmapper.sections.CSAlways;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;

import java.util.List;

/**
 * Configuration section for defining the rules and requirements for achieving the final rank in the system.
 * <p>
 * This section allows customization of required rank trees, minimum tiers, and special rank options.
 *
 * @author JExcellence
 * @version 1.0.0
 * @since TBD
 */
@CSAlways
public class FinalRankRuleSection extends AConfigSection {
	
	/**
	 * Whether all rank trees are required to achieve the final rank.
	 * If null or true, all rank trees are required.
	 */
	private Boolean requireAllRankTrees;
	
	/**
	 * The minimum number of rank trees required to achieve the final rank.
	 * If null, defaults to 4.
	 */
	private Integer minimumRequiredRankTrees;
	
	/**
	 * The minimum rank tier required in each rank tree to qualify for the final rank.
	 * If null, defaults to 5.
	 */
	private Integer minimumRankTierPerRankTree;
	
	/**
	 * The list of specific rank trees that are required to achieve the final rank.
	 * If null, defaults to ["warrior", "cleric", "mage", "rogue"].
	 */
	private List<String> specificRequiredRankTrees;
	
	/**
	 * Whether alternate final ranks are allowed.
	 * If null or true, alternate final ranks are permitted.
	 */
	private Boolean allowAlternateFinalRank;
	
	/**
	 * Whether ultimate ranks are enabled in the system.
	 * If null or true, ultimate ranks are enabled.
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
	 * If the value is null, defaults to true.
	 *
	 * @return true if all rank trees are required, false otherwise
	 */
	public Boolean getRequireAllRankTrees() {
		
		return
			this.requireAllRankTrees == null ||
			this.requireAllRankTrees;
	}
	
	/**
	 * Returns the minimum number of rank trees required to achieve the final rank.
	 * If the value is null, defaults to 4.
	 *
	 * @return the minimum required rank trees
	 */
	public Integer getMinimumRequiredRankTrees() {
		
		return
			this.minimumRequiredRankTrees == null ?
			4 :
			this.minimumRequiredRankTrees;
	}
	
	/**
	 * Returns the minimum rank tier required in each rank tree to qualify for the final rank.
	 * If the value is null, defaults to 5.
	 *
	 * @return the minimum rank tier per rank tree
	 */
	public Integer getMinimumRankTierPerRankTree() {
		
		return
			this.minimumRankTierPerRankTree == null ?
			5 :
			this.minimumRankTierPerRankTree;
	}
	
	/**
	 * Returns the list of specific rank trees required to achieve the final rank.
	 * If the value is null, defaults to ["warrior", "cleric", "mage", "rogue"].
	 *
	 * @return the list of specific required rank trees
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
	 * If the value is null, defaults to true.
	 *
	 * @return true if alternate final ranks are allowed, false otherwise
	 */
	public Boolean getAllowAlternateFinalRank() {
		
		return
			this.allowAlternateFinalRank == null ||
			this.allowAlternateFinalRank;
	}
	
	/**
	 * Returns whether ultimate ranks are enabled in the system.
	 * If the value is null, defaults to true.
	 *
	 * @return true if ultimate ranks are enabled, false otherwise
	 */
	public Boolean getEnableUltimateRanks() {
		
		return
			this.enableUltimateRanks == null ||
			this.enableUltimateRanks;
	}
	
}