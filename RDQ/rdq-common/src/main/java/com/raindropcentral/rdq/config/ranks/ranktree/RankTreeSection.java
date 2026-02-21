package com.raindropcentral.rdq.config.ranks.ranktree;

import com.raindropcentral.rdq.config.ranks.rank.RankSection;
import com.raindropcentral.rdq.config.ranks.system.CrossTreeProgressionSection;
import com.raindropcentral.rdq.config.utility.IconSection;
import com.raindropcentral.rplatform.logging.CentralLogger;
import de.jexcellence.configmapper.sections.AConfigSection;
import de.jexcellence.configmapper.sections.CSAlways;
import de.jexcellence.configmapper.sections.CSIgnore;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents a configuration section for a rank tree in the rank progression system.
 * <p>
 * A rank tree is a collection of ranks that players can progress through, possibly with prerequisites,
 * connections to other trees, and cross-tree progression rules. This class encapsulates all configuration
 * options for a single rank tree, including display properties, requirements, icon, and the ranks it contains.
 * </p>
 *
 * <p>
 * Each field is mapped from configuration and provides sensible defaults if not set.
 * Display name and description keys are automatically generated if not provided.
 * </p>
 *
 * @author JExcellence
 * @version 1.0.0
 * @since TBD
 */
@CSAlways
public class RankTreeSection extends AConfigSection {
	
	private static final Logger LOGGER = CentralLogger.getLoggerByName("RDQ");
	
	/**
	 * The localization key for the display name of the rank tree.
	 * If null, defaults to "tree.{treeId}.name".
	 */
	private String displayNameKey;
	
	/**
	 * The localization key for the description/lore of the rank tree.
	 * If null, defaults to "tree.{treeId}.lore".
	 */
	private String descriptionKey;
	
	/**
	 * The display order of this rank tree in UI or lists.
	 * If null, defaults to -1.
	 */
	private Integer displayOrder;
	
	/**
	 * Whether this rank tree is enabled and available for progression.
	 * If null, treated as false.
	 */
	private Boolean isEnabled;
	
	/**
	 * Whether this rank tree is the final tree in the progression system.
	 * If null, treated as false.
	 */
	private Boolean isFinalRankTree;
	
	/**
	 * Whether all rank trees must be completed to access this tree.
	 * If null, treated as true.
	 */
	private Boolean requiresAllRankTreesToBeDone;
	
	/**
	 * The minimum number of rank trees that must be completed before this tree can be accessed.
	 * If null, defaults to 1.
	 */
	private Integer minimumRankTreesToBeDone;
	
	/**
	 * The icon configuration for this rank tree.
	 * If null, a default IconSection is provided.
	 */
	private IconSection icon;
	
	/**
	 * List of prerequisite rank tree names that must be completed before this tree is accessible.
	 * If null, defaults to an empty list.
	 */
	private List<String> prerequisiteRankTrees;
	
	private List<String> unlockedRankTrees;
	
	/**
	 * List of other rank trees that are connected to this one (for navigation or progression).
	 * If null, defaults to an empty list.
	 */
	private List<String> connectedRankTrees;
	
	/**
	 * List of rank tiers within this tree that are eligible for switching from other trees.
	 * If null, defaults to an empty list.
	 */
	private List<Integer> switchableRankTiers;
	
	/**
	 * Configuration for cross-tree progression (switching between trees, costs, cooldowns, etc).
	 * If null, a default CrossTreeProgressionSection is provided.
	 */
	private CrossTreeProgressionSection crossTreeProgression;
	
	/**
	 * Map of rank names to their configuration sections within this tree.
	 * If null, defaults to an empty map.
	 */
	private Map<String, RankSection> ranks;
	
	/**
	 * The name of this rank tree (set by the factory).
	 */
	@CSIgnore
	private String treeId;
	
	/**
	 * Constructs a new RankTreeSection with the given evaluation environment.
	 *
	 * @param baseEnvironment the evaluation environment builder for config mapping and expression evaluation
	 */
	public RankTreeSection(final EvaluationEnvironmentBuilder baseEnvironment) {
		super(baseEnvironment);
	}
	
	/**
	 * Called after parsing the configuration fields. Sets default localization keys if not provided.
	 *
	 * @param fields the list of fields parsed
	 * @throws Exception if an error occurs during post-processing
	 */
	@Override
	public void afterParsing(final List<Field> fields) throws Exception {
		super.afterParsing(fields);
		
		if (
			this.treeId != null
		) {
			if (
				this.displayNameKey == null
			) {
				this.displayNameKey = "tree." + this.treeId + ".name";
			}
			
			if (
				this.descriptionKey == null
			) {
				this.descriptionKey = "tree." + this.treeId + ".lore";
			}
			
			if (
				this.icon != null
			) {
				if (
					this.icon.getDisplayNameKey() == null ||
					this.icon.getDisplayNameKey().equals("not_defined")
				) {
					this.icon.setDisplayNameKey("tree." + this.treeId + ".name");
				}
				if (
					this.icon.getDescriptionKey() == null ||
					this.icon.getDescriptionKey().equals("not_defined")
				) {
					this.icon.setDescriptionKey("tree." + this.treeId + ".lore");
				}
			}
			
			if (
				this.ranks != null
			) {
				for (
					Map.Entry<String, RankSection> entry : this.ranks.entrySet()
				) {
					String rankId = entry.getKey();
					RankSection rankSection = entry.getValue();
					
					rankSection.setRankTreeName(this.treeId);
					rankSection.setRankName(rankId);
					
					try {
						rankSection.afterParsing(new ArrayList<>());
					} catch (
						final Exception exception
					) {
						LOGGER.log(
							Level.WARNING, "Failed to process rank " + rankId + " in tree " + this.treeId + ": ", exception
						);
					}
				}
			}
		}
	}
	
	/**
	 * Sets the tree ID for this rank tree section.
	 *
	 * @param treeId the tree identifier
	 */
	public void setTreeId(final String treeId) {
		this.treeId = treeId;
	}
	
	/**
	 * Gets the tree ID for this rank tree section.
	 *
	 * @return the tree identifier
	 */
	public String getTreeId() {
		return this.treeId;
	}
	
	/**
	 * Gets the localization key for the display name of the rank tree.
	 *
	 * @return the display name key, or "not_defined" if not set
	 */
	public String getDisplayNameKey() {
		return this.displayNameKey == null ? "not_defined" : this.displayNameKey;
	}
	
	/**
	 * Gets the localization key for the description/lore of the rank tree.
	 *
	 * @return the description key, or "not_defined" if not set
	 */
	public String getDescriptionKey() {
		return this.descriptionKey == null ? "not_defined" : this.descriptionKey;
	}
	
	/**
	 * Gets the display order of this rank tree.
	 *
	 * @return the display order, or -1 if not set
	 */
	public Integer getDisplayOrder() {
		return this.displayOrder == null ? -1 : this.displayOrder;
	}
	
	/**
	 * Checks if this rank tree is enabled.
	 *
	 * @return true if enabled, false otherwise
	 */
	public Boolean getEnabled() {
		return this.isEnabled != null && this.isEnabled;
	}
	
	/**
	 * Checks if this rank tree is the final tree in the progression system.
	 *
	 * @return true if this is the final rank tree, false otherwise
	 */
	public Boolean getFinalRankTree() {
		return this.isFinalRankTree != null && this.isFinalRankTree;
	}
	
	/**
	 * Checks if all rank trees must be completed to access this tree.
	 *
	 * @return true if all rank trees are required, false otherwise
	 */
	public Boolean getRequiresAllRankTreesToBeDone() {
		return this.requiresAllRankTreesToBeDone == null || this.requiresAllRankTreesToBeDone;
	}
	
	/**
	 * Gets the minimum number of rank trees that must be completed before this tree can be accessed.
	 *
	 * @return the minimum number, or 1 if not set
	 */
	public Integer getMinimumRankTreesToBeDone() {
		return this.minimumRankTreesToBeDone == null ? 0 : this.minimumRankTreesToBeDone;
	}
	
	/**
	 * Gets the icon configuration for this rank tree.
	 *
	 * @return the icon section, or a default IconSection if not set
	 */
	public IconSection getIcon() {
		return this.icon == null ? new IconSection(new EvaluationEnvironmentBuilder()) : this.icon;
	}
	
	/**
	 * Gets the list of prerequisite rank tree names.
	 *
	 * @return the list of prerequisite rank trees, or an empty list if not set
	 */
	public List<String> getPrerequisiteRankTrees() {
		return this.prerequisiteRankTrees == null ? new ArrayList<>() : this.prerequisiteRankTrees;
	}
	
	public void setMinimumRankTreesToBeDone(final Integer minimumRankTreesToBeDone) {
		this.minimumRankTreesToBeDone = minimumRankTreesToBeDone;
	}
	
	public List<String> getUnlockedRankTrees() {
		return this.unlockedRankTrees == null ? new ArrayList<>() : this.unlockedRankTrees;
	}
	
	/**
	 * Gets the list of connected rank tree names.
	 *
	 * @return the list of connected rank trees, or an empty list if not set
	 */
	public List<String> getConnectedRankTrees() {
		return this.connectedRankTrees == null ? new ArrayList<>() : this.connectedRankTrees;
	}
	
	/**
	 * Gets the list of rank tiers eligible for switching from other trees.
	 *
	 * @return the list of switchable rank tiers, or an empty list if not set
	 */
	public List<Integer> getSwitchableRankTiers() {
		return this.switchableRankTiers == null ? new ArrayList<>() : this.switchableRankTiers;
	}
	
	/**
	 * Gets the cross-tree progression configuration for this rank tree.
	 *
	 * @return the cross-tree progression section, or a default CrossTreeProgressionSection if not set
	 */
	public CrossTreeProgressionSection getCrossTreeProgression() {
		return this.crossTreeProgression == null ? new CrossTreeProgressionSection(new EvaluationEnvironmentBuilder()) : this.crossTreeProgression;
	}
	
	/**
	 * Gets the map of rank names to their configuration sections within this tree.
	 *
	 * @return the map of ranks, or an empty map if not set
	 */
	public Map<String, RankSection> getRanks() {
		return this.ranks == null ? new HashMap<>() : this.ranks;
	}
}