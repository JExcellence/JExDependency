package com.raindropcentral.rdq.config.requirement;

import de.jexcellence.configmapper.sections.AConfigSection;
import de.jexcellence.configmapper.sections.CSAlways;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration section for previous level or rank requirements.
 * <p>
 * This section captures the configuration data required by previous level
 * requirements, including individual and list-based aliases for both rank and
 * rank-tree references as well as optional tuning flags that govern how the
 * completion checks should behave.
 * </p>
 *
 * @author JExcellence
 * @version 1.0.1
 * @since 1.0.0
 */
@CSAlways
public class PreviousLevelRequirementSection extends AConfigSection {

    // ~~~ PREVIOUS LEVEL-SPECIFIC PROPERTIES ~~~

    /**
     * Single required previous rank.
     * YAML key: "requiredPreviousRank"
     */
    private String requiredPreviousRank;

    /**
     * Alternative previous rank field name.
     * YAML key: "previousRank"
     */
    private String previousRank;

    /**
     * Required previous rank tree.
     * YAML key: "requiredPreviousRankTree"
     */
    private String requiredPreviousRankTree;

    /**
     * Alternative previous rank tree field name.
     * YAML key: "previousRankTree"
     */
    private String previousRankTree;

    /**
     * List of required previous ranks.
     * YAML key: "requiredPreviousRanks"
     */
    private List<String> requiredPreviousRanks;

    /**
     * Alternative previous ranks list field name.
     * YAML key: "previousRanks"
     */
    private List<String> previousRanks;

    /**
     * List of required previous rank trees.
     * YAML key: "requiredPreviousRankTrees"
     */
    private List<String> requiredPreviousRankTrees;

    /**
     * Alternative previous rank trees list field name.
     * YAML key: "previousRankTrees"
     */
    private List<String> previousRankTrees;

    /**
     * Whether all previous ranks/trees must be completed (AND) or just one (OR).
     * YAML key: "requireAll"
     */
    private Boolean requireAll;

    /**
     * Whether to check only direct previous ranks or any rank in the tree.
     * YAML key: "checkDirectOnly"
     */
    private Boolean checkDirectOnly;

    /**
     * Minimum tier/level that must be reached in previous ranks.
     * YAML key: "minimumTier"
     */
    private Integer minimumTier;

    /**
     * Constructs a new PreviousLevelRequirementSection bound to the supplied
     * evaluation environment builder.
     *
     * @param evaluationEnvironmentBuilder the evaluation environment builder used for expression resolution
     */    /**
     * Default no-arg constructor for Jackson deserialization.
     */
    protected PreviousLevelRequirementSection() {
        super(new EvaluationEnvironmentBuilder());
    }

    public PreviousLevelRequirementSection(
            final EvaluationEnvironmentBuilder evaluationEnvironmentBuilder
    ) {
        super(evaluationEnvironmentBuilder);
    }

    // ~~~ GETTERS ~~~

    /**
     * Gets the single required previous rank, attempting both canonical and
     * legacy aliases for backwards compatibility.
     *
     * @return the required previous rank, or an empty string when unspecified
     */
    public String getRequiredPreviousRank() {
        if (this.requiredPreviousRank != null) {
            return this.requiredPreviousRank;
        }
        if (this.previousRank != null) {
            return this.previousRank;
        }
        return "";
    }

    /**
     * Gets the required previous rank tree identifier, resolving alternate
     * configuration keys when necessary.
     *
     * @return the required previous rank tree, or an empty string when unspecified
     */
    public String getRequiredPreviousRankTree() {
        if (this.requiredPreviousRankTree != null) {
            return this.requiredPreviousRankTree;
        }
        if (this.previousRankTree != null) {
            return this.previousRankTree;
        }
        return "";
    }

    /**
     * Aggregates the complete list of required previous ranks from every
     * configured source, including the single-rank alias.
     *
     * @return combined list of all required previous ranks without duplicates
     */
    public List<String> getRequiredPreviousRanks() {
        List<String> rankList = new ArrayList<>();

        // Add ranks from requiredPreviousRanks list
        if (this.requiredPreviousRanks != null) {
            rankList.addAll(this.requiredPreviousRanks);
        }

        // Add ranks from alternative previousRanks list
        if (this.previousRanks != null) {
            rankList.addAll(this.previousRanks);
        }

        // Add single rank if specified
        String singleRank = this.getRequiredPreviousRank();
        if (!singleRank.isEmpty() && !rankList.contains(singleRank)) {
            rankList.add(singleRank);
        }

        return rankList;
    }

    /**
     * Aggregates the complete list of required previous rank trees from every
     * configured source, including the single-tree alias.
     *
     * @return combined list of all required previous rank trees without duplicates
     */
    public List<String> getRequiredPreviousRankTrees() {
        List<String> treeList = new ArrayList<>();

        // Add trees from requiredPreviousRankTrees list
        if (this.requiredPreviousRankTrees != null) {
            treeList.addAll(this.requiredPreviousRankTrees);
        }

        // Add trees from alternative previousRankTrees list
        if (this.previousRankTrees != null) {
            treeList.addAll(this.previousRankTrees);
        }

        // Add single tree if specified
        String singleTree = this.getRequiredPreviousRankTree();
        if (!singleTree.isEmpty() && !treeList.contains(singleTree)) {
            treeList.add(singleTree);
        }

        return treeList;
    }

    /**
     * Indicates whether all configured previous ranks or rank trees must be
     * completed.
     *
     * @return {@code true} when all entries are required, or {@code true} by default when unspecified
     */
    public Boolean getRequireAll() {
        return this.requireAll != null ? this.requireAll : true;
    }

    /**
     * Indicates whether the completion check should only consider direct
     * predecessors.
     *
     * @return {@code true} to limit checks to direct predecessors, or {@code true} by default when unspecified
     */
    public Boolean getCheckDirectOnly() {
        return this.checkDirectOnly != null ? this.checkDirectOnly : true;
    }

    /**
     * Provides the minimum tier that must be reached within previous ranks.
     *
     * @return the minimum tier, defaulting to {@code 0} when not configured
     */
    public Integer getMinimumTier() {
        return this.minimumTier != null ? this.minimumTier : 0;
    }
}
