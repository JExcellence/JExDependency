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

package com.raindropcentral.rdq.config.requirement;//package com.raindropcentral.rdq.config.requirement;
//
//import de.jexcellence.configmapper.sections.CSAlways;
//import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
//
//import java.util.ArrayList;
//import java.util.List;
//
///**
// * Configuration section for previous level/rank requirements.
// * <p>
// * This section handles all configuration options specific to PreviousLevelRequirement,
// * including required previous ranks, rank trees, and completion checking modes.
// * </p>
// *
// * @author JExcellence
// * @version 1.0.0
// * @since TBD
// */
//@CSAlways
//public class PreviousLevelRequirementSection extends BaseRequirementSection {
//
//	// ~~~ PREVIOUS LEVEL-SPECIFIC PROPERTIES ~~~
//
//	/**
//	 * Single required previous rank.
//	 * YAML key: "requiredPreviousRank"
//	 */
//	private String requiredPreviousRank;
//
//	/**
//	 * Alternative previous rank field name.
//	 * YAML key: "previousRank"
//	 */
//	private String previousRank;
//
//	/**
//	 * Required previous rank tree.
//	 * YAML key: "requiredPreviousRankTree"
//	 */
//	private String requiredPreviousRankTree;
//
//	/**
//	 * Alternative previous rank tree field name.
//	 * YAML key: "previousRankTree"
//	 */
//	private String previousRankTree;
//
//	/**
//	 * List of required previous ranks.
//	 * YAML key: "requiredPreviousRanks"
//	 */
//	private List<String> requiredPreviousRanks;
//
//	/**
//	 * Alternative previous ranks list field name.
//	 * YAML key: "previousRanks"
//	 */
//	private List<String> previousRanks;
//
//	/**
//	 * List of required previous rank trees.
//	 * YAML key: "requiredPreviousRankTrees"
//	 */
//	private List<String> requiredPreviousRankTrees;
//
//	/**
//	 * Alternative previous rank trees list field name.
//	 * YAML key: "previousRankTrees"
//	 */
//	private List<String> previousRankTrees;
//
//	/**
//	 * Whether all previous ranks/trees must be completed (AND) or just one (OR).
//	 * YAML key: "requireAll"
//	 */
//	private Boolean requireAll;
//
//	/**
//	 * Whether to check only direct previous ranks or any rank in the tree.
//	 * YAML key: "checkDirectOnly"
//	 */
//	private Boolean checkDirectOnly;
//
//	/**
//	 * Minimum tier/level that must be reached in previous ranks.
//	 * YAML key: "minimumTier"
//	 */
//	private Integer minimumTier;
//
//	/**
//	 * Constructs a new PreviousLevelRequirementSection.
//	 *
//	 * @param evaluationEnvironmentBuilder the evaluation environment builder
//	 */
//	public PreviousLevelRequirementSection(EvaluationEnvironmentBuilder evaluationEnvironmentBuilder) {
//		super(evaluationEnvironmentBuilder);
//	}
//
//	// ~~~ GETTERS ~~~
//
//	@Override
//	protected String getDefaultDescriptionKey() {
//		return "requirement.previous_level.lore";
//	}
//
//	/**
//	 * Gets the single required previous rank, trying multiple field names.
//	 *
//	 * @return the required previous rank
//	 */
//	public String getRequiredPreviousRank() {
//		if (this.requiredPreviousRank != null) {
//			return this.requiredPreviousRank;
//		}
//		if (this.previousRank != null) {
//			return this.previousRank;
//		}
//		return "";
//	}
//
//	/**
//	 * Gets the required previous rank tree, trying multiple field names.
//	 *
//	 * @return the required previous rank tree
//	 */
//	public String getRequiredPreviousRankTree() {
//		if (this.requiredPreviousRankTree != null) {
//			return this.requiredPreviousRankTree;
//		}
//		if (this.previousRankTree != null) {
//			return this.previousRankTree;
//		}
//		return "";
//	}
//
//	/**
//	 * Gets the complete list of required previous ranks from all sources.
//	 *
//	 * @return combined list of all required previous ranks
//	 */
//	public List<String> getRequiredPreviousRanks() {
//		List<String> rankList = new ArrayList<>();
//
//		// Add ranks from requiredPreviousRanks list
//		if (this.requiredPreviousRanks != null) {
//			rankList.addAll(this.requiredPreviousRanks);
//		}
//
//		// Add ranks from alternative previousRanks list
//		if (this.previousRanks != null) {
//			rankList.addAll(this.previousRanks);
//		}
//
//		// Add single rank if specified
//		String singleRank = getRequiredPreviousRank();
//		if (!singleRank.isEmpty() && !rankList.contains(singleRank)) {
//			rankList.add(singleRank);
//		}
//
//		return rankList;
//	}
//
//	/**
//	 * Gets the complete list of required previous rank trees from all sources.
//	 *
//	 * @return combined list of all required previous rank trees
//	 */
//	public List<String> getRequiredPreviousRankTrees() {
//		List<String> treeList = new ArrayList<>();
//
//		// Add trees from requiredPreviousRankTrees list
//		if (this.requiredPreviousRankTrees != null) {
//			treeList.addAll(this.requiredPreviousRankTrees);
//		}
//
//		// Add trees from alternative previousRankTrees list
//		if (this.previousRankTrees != null) {
//			treeList.addAll(this.previousRankTrees);
//		}
//
//		// Add single tree if specified
//		String singleTree = getRequiredPreviousRankTree();
//		if (!singleTree.isEmpty() && !treeList.contains(singleTree)) {
//			treeList.add(singleTree);
//		}
//
//		return treeList;
//	}
//
//	/**
//	 * Gets whether all previous ranks/trees must be completed.
//	 *
//	 * @return true if all are required, false if only one is needed
//	 */
//	public Boolean getRequireAll() {
//		return this.requireAll != null ? this.requireAll : true;
//	}
//
//	/**
//	 * Gets whether to check only direct previous ranks.
//	 *
//	 * @return true to check only direct previous ranks, false to check any rank in tree
//	 */
//	public Boolean getCheckDirectOnly() {
//		return this.checkDirectOnly != null ? this.checkDirectOnly : true;
//	}
//
//	/**
//	 * Gets the minimum tier that must be reached.
//	 *
//	 * @return the minimum tier, defaulting to 0
//	 */
//	public Integer getMinimumTier() {
//		return this.minimumTier != null ? this.minimumTier : 0;
//	}
//}
