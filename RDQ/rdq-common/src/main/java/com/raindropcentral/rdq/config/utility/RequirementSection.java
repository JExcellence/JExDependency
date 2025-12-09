package com.raindropcentral.rdq.config.utility;

import de.jexcellence.configmapper.sections.AConfigSection;
import de.jexcellence.configmapper.sections.CSAlways;
import de.jexcellence.evaluable.section.ItemStackSection;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.bukkit.inventory.ItemStack;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a comprehensive configuration section for requirements within the RaindropQuests system.
 * <p>
 * This section supports all requirement types defined in {@link com.raindropcentral.rdq2.requirement.AbstractRequirement.Type}
 * and provides flexible configuration options for complex requirement structures including items, currencies,
 * composite requirements, choice requirements, and plugin-specific requirements.
 * </p>
 * <p>
 * The section follows a hierarchical structure where the base requirement type determines which
 * sub-configuration sections are relevant and used during requirement parsing.
 * Variable names must match exactly with YAML configuration keys for proper config mapping.
 * </p>
 *
 * @author JExcellence
 * @version 1.0.0
 * @since TBD
 */
public class RequirementSection extends AConfigSection {

	/**
	 * The type of requirement. Must match one of the values from AbstractRequirement.Type enum.
	 * YAML key: "type"
	 */
	@CSAlways
	private String type;
	
	@CSAlways
	private IconSection icon;
	
	/**
	 * Optional description key for localization of this requirement's description.
	 * YAML key: "descriptionKey"
	 */
	@CSAlways
	private String descriptionKey;
	
	/**
	 * Optional display order for this requirement when shown in GUIs.
	 * YAML key: "displayOrder"
	 */
	private Integer displayOrder;
	
	/**
	 * Whether this requirement should consume resources when completed.
	 * YAML key: "consumeOnComplete"
	 */
	private Boolean consumeOnComplete;
	
	/**
	 * List of required items for item-based requirements.
	 * Uses ItemBuilder for configuration compatibility.
	 * YAML key: "requiredItems"
	 */
	private Map<String, ItemStackSection> requiredItems;
	
	/**
	 * Alternative: Direct ItemStack list for serialization.
	 * YAML key: "items"
	 */
	private List<ItemStack> items;
	
	/**
	 * Single item requirement (backwards compatibility).
	 * YAML key: "requiredItem"
	 */
	private ItemStackSection requiredItem;
	
	/**
	 * Amount for single item requirements.
	 * YAML key: "requiredAmount"
	 */
	private Integer requiredAmount;
	
	/**
	 * Map of required currencies for currency-based requirements.
	 * Note: In actual implementation, this uses Currency objects as keys,
	 * but for configuration we use String identifiers that get resolved.
	 * YAML key: "requiredCurrencies"
	 */
	private Map<String, Double> requiredCurrencies;
	
	/**
	 * Single currency identifier for simple requirements.
	 * YAML key: "currencyType"
	 */
	private String currencyType;
	
	/**
	 * Single currency amount for simple requirements.
	 * YAML key: "currencyAmount"
	 */
	private Double currencyAmount;
	
	/**
	 * Alternative currency field name.
	 * YAML key: "currency"
	 */
	private String currency;
	
	/**
	 * Alternative amount field name.
	 * YAML key: "amount"
	 */
	private Double amount;
	
	/**
	 * Required experience level for experience-based requirements.
	 * YAML key: "requiredLevel"
	 */
	private Integer requiredLevel;
	
	/**
	 * Alternative level field name.
	 * YAML key: "level"
	 */
	private Integer level;
	
	/**
	 * Type of experience requirement (LEVEL, POINTS).
	 * YAML key: "experienceType"
	 */
	private String experienceType;
	
	/**
	 * Required playtime in seconds for playtime-based requirements.
	 * YAML key: "requiredPlaytimeSeconds"
	 */
	private Long requiredPlaytimeSeconds;
	
	/**
	 * Alternative playtime field name.
	 * YAML key: "playtimeSeconds"
	 */
	private Long playtimeSeconds;
	
	/**
	 * Playtime in minutes (converted to seconds).
	 * YAML key: "requiredPlaytimeMinutes"
	 */
	private Long requiredPlaytimeMinutes;
	
	/**
	 * Playtime in hours (converted to seconds).
	 * YAML key: "requiredPlaytimeHours"
	 */
	private Long requiredPlaytimeHours;
	
	/**
	 * Required achievement identifier for achievement-based requirements.
	 * YAML key: "requiredAchievement"
	 */
	private String requiredAchievement;
	
	/**
	 * Alternative achievement field name.
	 * YAML key: "achievement"
	 */
	private String achievement;
	
	/**
	 * List of required achievements.
	 * YAML key: "requiredAchievements"
	 */
	private List<String> requiredAchievements;
	
	/**
	 * Alternative achievements list field name.
	 * YAML key: "achievements"
	 */
	private List<String> achievements;
	
	/**
	 * Required previous rank identifier.
	 * YAML key: "requiredPreviousRank"
	 */
	private String requiredPreviousRank;
	
	/**
	 * Alternative previous rank field name.
	 * YAML key: "previousRank"
	 */
	private String previousRank;
	
	/**
	 * Required previous rank tree identifier.
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
	 * Required skill identifier for skill-based requirements.
	 * YAML key: "requiredSkill"
	 */
	private String requiredSkill;
	
	/**
	 * Alternative skill field name.
	 * YAML key: "skill"
	 */
	private String skill;
	
	/**
	 * Required skill level for skill-based requirements.
	 * YAML key: "requiredSkillLevel"
	 */
	private Integer requiredSkillLevel;
	
	/**
	 * Alternative skill level field name.
	 * YAML key: "skillLevel"
	 */
	private Integer skillLevel;
	
	/**
	 * Map of multiple skill requirements.
	 * YAML key: "requiredSkills"
	 */
	private Map<String, Integer> requiredSkills;
	
	/**
	 * Alternative skills map field name.
	 * YAML key: "skills"
	 */
	private Map<String, Integer> skills;
	
	/**
	 * Plugin identifier for skill requirements.
	 * YAML key: "skillPlugin"
	 */
	private String skillPlugin;
	
	/**
	 * Required job identifier for job-based requirements.
	 * YAML key: "requiredJob"
	 */
	private String requiredJob;
	
	/**
	 * Alternative job field name.
	 * YAML key: "job"
	 */
	private String job;
	
	/**
	 * Required job level for job-based requirements.
	 * YAML key: "requiredJobLevel"
	 */
	private Integer requiredJobLevel;
	
	/**
	 * Alternative job level field name.
	 * YAML key: "jobLevel"
	 */
	private Integer jobLevel;
	
	/**
	 * Map of multiple job requirements.
	 * YAML key: "requiredJobs"
	 */
	private Map<String, Integer> requiredJobs;
	
	/**
	 * Alternative jobs map field name.
	 * YAML key: "jobs"
	 */
	private Map<String, Integer> jobs;
	
	/**
	 * Plugin identifier for job requirements.
	 * YAML key: "jobPlugin"
	 */
	private String jobPlugin;
	
	/**
	 * Time constraint in seconds for time-based requirements.
	 * YAML key: "timeConstraintSeconds"
	 */
	private Long timeConstraintSeconds;
	
	/**
	 * Alternative time constraint field name.
	 * YAML key: "timeConstraint"
	 */
	private Long timeConstraint;
	
	/**
	 * Cooldown period in seconds for time-based requirements.
	 * YAML key: "cooldownSeconds"
	 */
	private Long cooldownSeconds;
	
	/**
	 * Alternative cooldown field name.
	 * YAML key: "cooldown"
	 */
	private Long cooldown;
	
	/**
	 * Start time for time-based requirements.
	 * YAML key: "startTime"
	 */
	private String startTime;
	
	/**
	 * End time for time-based requirements.
	 * YAML key: "endTime"
	 */
	private String endTime;
	
	/**
	 * Logical operator for composite requirements.
	 * YAML key: "operator"
	 */
	private String operator;
	
	/**
	 * Alternative operator field name.
	 * YAML key: "compositeOperator"
	 */
	private String compositeOperator;
	
	/**
	 * List of sub-requirements for composite requirements.
	 * This should match the actual field name in CompositeRequirement.
	 * YAML key: "requirements"
	 */
	private List<RequirementSection> requirements;
	
	/**
	 * Alternative: Map-based sub-requirements.
	 * YAML key: "subRequirements"
	 */
	private Map<String, RequirementSection> subRequirements;
	
	/**
	 * Minimum number of sub-requirements that must be met.
	 * YAML key: "minimumRequired"
	 */
	private Integer minimumRequired;
	
	/**
	 * List of alternative choices for choice requirements.
	 * This should match the actual field name in ChoiceRequirement.
	 * YAML key: "choices"
	 */
	private List<RequirementSection> choices;
	
	/**
	 * Alternative: Map-based choices for named options.
	 * YAML key: "choiceMap"
	 */
	private Map<String, RequirementSection> choiceMap;
	
	/**
	 * Alternative: List field name.
	 * YAML key: "choiceList"
	 */
	private List<RequirementSection> choiceList;
	
	/**
	 * Description for the choice requirement.
	 * YAML key: "description"
	 */
	private String description;
	
	/**
	 * Alternative description field name.
	 * YAML key: "choiceDescription"
	 */
	private String choiceDescription;
	
	// ~~~ ADDITIONAL REQUIREMENT TYPES ~~~
	
	/**
	 * Required permission for permission-based requirements.
	 * YAML key: "requiredPermission"
	 */
	private String requiredPermission;
	
	/**
	 * Alternative permission field name.
	 * YAML key: "permission"
	 */
	private String permission;
	
	/**
	 * List of required permissions.
	 * YAML key: "requiredPermissions"
	 */
	private List<String> requiredPermissions;
	
	/**
	 * Alternative permissions list field name.
	 * YAML key: "permissions"
	 */
	private List<String> permissions;
	
	/**
	 * Required world name for location-based requirements.
	 * YAML key: "requiredWorld"
	 */
	private String requiredWorld;
	
	/**
	 * Alternative world field name.
	 * YAML key: "world"
	 */
	private String world;
	
	/**
	 * Required region name for location-based requirements.
	 * YAML key: "requiredRegion"
	 */
	private String requiredRegion;
	
	/**
	 * Alternative region field name.
	 * YAML key: "region"
	 */
	private String region;
	
	/**
	 * Required coordinates for location-based requirements.
	 * YAML key: "requiredCoordinates"
	 */
	private Map<String, Double> requiredCoordinates;
	
	/**
	 * Alternative coordinates field name.
	 * YAML key: "coordinates"
	 */
	private Map<String, Double> coordinates;
	
	/**
	 * Required distance from coordinates.
	 * YAML key: "requiredDistance"
	 */
	private Double requiredDistance;
	
	/**
	 * Alternative distance field name.
	 * YAML key: "distance"
	 */
	private Double distance;
	
	// ~~~ GENERIC/EXTENSIBLE FIELDS ~~~
	
	/**
	 * Generic data map for custom requirements.
	 * YAML key: "data"
	 */
	private Map<String, Object> data;
	
	/**
	 * Custom requirement data for plugin-specific requirements.
	 * YAML key: "customData"
	 */
	private Map<String, Object> customData;
	
	/**
	 * Custom requirement script or expression.
	 * YAML key: "customScript"
	 */
	private String customScript;
	
	/**
	 * Constructs a new {@code RequirementSection} with the specified evaluation environment builder.
	 *
	 * @param evaluationEnvironmentBuilder the builder used to provide the evaluation environment for this section
	 */
	public RequirementSection(
		final EvaluationEnvironmentBuilder evaluationEnvironmentBuilder
	) {
		
		super(evaluationEnvironmentBuilder);
	}
	
	// ~~~ SMART GETTERS WITH FALLBACKS AND COMPATIBILITY ~~~
	
	/**
	 * Gets the requirement type.
	 *
	 * @return the requirement type, or "ITEM" as default if not specified
	 */
	public String getType() {
		
		return this.type != null ?
		       this.type :
		       "ITEM";
	}
	
	public IconSection getIcon() {
		
		return this.icon != null ?
		       this.icon :
		       new IconSection(new EvaluationEnvironmentBuilder());
	}
	
	/**
	 * Gets the display order for this requirement.
	 *
	 * @return the display order, or 0 if not specified
	 */
	public Integer getDisplayOrder() {
		
		return this.displayOrder != null ?
		       this.displayOrder :
		       0;
	}
	
	/**
	 * Gets the description key for this requirement.
	 *
	 * @return the description key, or null if not specified (will use default)
	 */
	public String getDescriptionKey() {
		
		return this.descriptionKey;
	}
	
	/**
	 * Gets whether this requirement should consume resources when completed.
	 *
	 * @return true if resources should be consumed, false otherwise. Defaults to true.
	 */
	public Boolean getConsumeOnComplete() {
		
		return this.consumeOnComplete != null ?
		       this.consumeOnComplete :
		       true;
	}
	
	// ~~~ ITEM REQUIREMENT GETTERS ~~~
	
	/**
	 * Gets the list of required items as ItemStack objects.
	 * Combines all item sources and converts to ItemStack.
	 *
	 * @return the list of required items, or an empty list if not specified
	 */
	public List<ItemStack> getRequiredItemsList() {
		
		final List<ItemStack> itemList = new ArrayList<>();
		
		// Add items from requiredItems (ItemBuilder list)
		if (this.requiredItems != null) {
			itemList.addAll(this.requiredItems.values().stream().map(itemSection -> itemSection.asItem().build()).toList());
		}
		
		// Add items from direct ItemStack list
		if (this.items != null) {
			itemList.addAll(this.items);
		}
		
		// Add single requiredItem if specified
		if (this.requiredItem != null) {
			final ItemStack singleItem = this.requiredItem.asItem().build();
			if (this.requiredAmount != null && this.requiredAmount > 1) {
				singleItem.setAmount(this.requiredAmount);
			}
			itemList.add(singleItem);
		}
		
		return itemList;
	}
	
	// ~~~ CURRENCY REQUIREMENT GETTERS ~~~
	
	/**
	 * Gets the map of required currencies.
	 * Combines all currency sources.
	 *
	 * @return the map of required currencies, or an empty map if not specified
	 */
	public Map<String, Double> getRequiredCurrencies() {
		
		final Map<String, Double> currencies = new HashMap<>();
		
		
		
		// Add currencies from requiredCurrencies map
		if (this.requiredCurrencies != null) {
			currencies.putAll(this.requiredCurrencies);
		}
		
		// Add single currency if specified (try multiple field combinations)
		String currencyId = null;
		Double currencyAmount = null;
		
		if (this.currencyType != null && this.currencyAmount != null) {
			currencyId = this.currencyType;
			currencyAmount = this.currencyAmount;
		} else if (this.currency != null && this.amount != null) {
			currencyId = this.currency;
			currencyAmount = this.amount;
		}
		
		if (currencyId != null) {
			currencies.put(
				currencyId,
				currencyAmount
			);
		}
		
		return currencies;
	}
	
	// ~~~ EXPERIENCE REQUIREMENT GETTERS ~~~
	
	/**
	 * Gets the required experience level.
	 * Tries multiple field names for compatibility.
	 *
	 * @return the required level, or 1 if not specified
	 */
	public Integer getRequiredLevel() {
		
		if (this.requiredLevel != null) {
			return this.requiredLevel;
		}
		if (this.level != null) {
			return this.level;
		}
		return 1;
	}
	
	/**
	 * Gets the experience type.
	 *
	 * @return the experience type, or "LEVEL" if not specified
	 */
	public String getExperienceType() {
		
		return this.experienceType != null ?
		       this.experienceType :
		       "LEVEL";
	}
	
	// ~~~ PLAYTIME REQUIREMENT GETTERS ~~~
	
	/**
	 * Gets the required playtime in seconds.
	 * Converts from minutes or hours if specified, tries multiple field names.
	 *
	 * @return the required playtime in seconds, or 0 if not specified
	 */
	public Long getRequiredPlaytimeSeconds() {
		
		if (this.requiredPlaytimeSeconds != null) {
			return this.requiredPlaytimeSeconds;
		}
		if (this.playtimeSeconds != null) {
			return this.playtimeSeconds;
		}
		if (this.requiredPlaytimeMinutes != null) {
			return this.requiredPlaytimeMinutes * 60;
		}
		if (this.requiredPlaytimeHours != null) {
			return this.requiredPlaytimeHours * 3600;
		}
		return 0L;
	}
	
	// ~~~ ACHIEVEMENT REQUIREMENT GETTERS ~~~
	
	/**
	 * Gets the required achievement identifier.
	 * Tries multiple field names for compatibility.
	 *
	 * @return the achievement identifier, or empty string if not specified
	 */
	public String getRequiredAchievement() {
		
		if (this.requiredAchievement != null) {
			return this.requiredAchievement;
		}
		if (this.achievement != null) {
			return this.achievement;
		}
		return "";
	}
	
	/**
	 * Gets the list of required achievements.
	 * Combines single achievement and achievement lists.
	 *
	 * @return the list of required achievements
	 */
	public List<String> getRequiredAchievements() {
		
		final List<String> achievementList = new ArrayList<>();
		
		if (this.requiredAchievements != null) {
			achievementList.addAll(this.requiredAchievements);
		}
		
		if (this.achievements != null) {
			achievementList.addAll(this.achievements);
		}
		
		// Add single achievement if specified
		final String singleAchievement = this.getRequiredAchievement();
		if (! singleAchievement.isEmpty() && ! achievementList.contains(singleAchievement)) {
			achievementList.add(singleAchievement);
		}
		
		return achievementList;
	}
	
	// ~~~ PREVIOUS LEVEL REQUIREMENT GETTERS ~~~
	
	/**
	 * Gets the required previous rank identifier.
	 * Tries multiple field names for compatibility.
	 *
	 * @return the previous rank identifier, or empty string if not specified
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
	 * Gets the required previous rank tree identifier.
	 * Tries multiple field names for compatibility.
	 *
	 * @return the previous rank tree identifier, or empty string if not specified
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
	 * Gets the list of required previous ranks.
	 *
	 * @return the list of required previous ranks
	 */
	public List<String> getRequiredPreviousRanks() {
		
		final List<String> ranks = new ArrayList<>();
		
		if (this.requiredPreviousRanks != null) {
			ranks.addAll(this.requiredPreviousRanks);
		}
		
		// Add single previous rank if specified
		final String singleRank = this.getRequiredPreviousRank();
		if (! singleRank.isEmpty() && ! ranks.contains(singleRank)) {
			ranks.add(singleRank);
		}
		
		return ranks;
	}
	
	// ~~~ SKILL REQUIREMENT GETTERS ~~~
	
	/**
	 * Gets the required skill identifier.
	 * Tries multiple field names for compatibility.
	 *
	 * @return the skill identifier, or empty string if not specified
	 */
	public String getRequiredSkill() {
		
		if (this.requiredSkill != null) {
			return this.requiredSkill;
		}
		if (this.skill != null) {
			return this.skill;
		}
		return "";
	}
	
	/**
	 * Gets the required skill level.
	 * Tries multiple field names for compatibility.
	 *
	 * @return the skill level, or 1 if not specified
	 */
	public Integer getRequiredSkillLevel() {
		
		if (this.requiredSkillLevel != null) {
			return this.requiredSkillLevel;
		}
		if (this.skillLevel != null) {
			return this.skillLevel;
		}
		return 1;
	}
	
	/**
	 * Gets the map of required skills.
	 * Combines all skill sources.
	 *
	 * @return the map of required skills
	 */
	public Map<String, Integer> getRequiredSkills() {
		
		final Map<String, Integer> skillMap = new HashMap<>();
		
		if (this.requiredSkills != null) {
			skillMap.putAll(this.requiredSkills);
		}
		
		if (this.skills != null) {
			skillMap.putAll(this.skills);
		}
		
		// Add single skill if specified
		final String singleSkill = this.getRequiredSkill();
		if (! singleSkill.isEmpty()) {
			skillMap.put(
				singleSkill,
				this.getRequiredSkillLevel()
			);
		}
		
		return skillMap;
	}
	
	/**
	 * Gets the skill plugin identifier.
	 *
	 * @return the skill plugin identifier, or empty string if not specified
	 */
	public String getSkillPlugin() {
		
		return this.skillPlugin != null ?
		       this.skillPlugin :
		       "";
	}
	
	// ~~~ JOB REQUIREMENT GETTERS ~~~
	
	/**
	 * Gets the required job identifier.
	 * Tries multiple field names for compatibility.
	 *
	 * @return the job identifier, or empty string if not specified
	 */
	public String getRequiredJob() {
		
		if (this.requiredJob != null) {
			return this.requiredJob;
		}
		if (this.job != null) {
			return this.job;
		}
		return "";
	}
	
	/**
	 * Gets the required job level.
	 * Tries multiple field names for compatibility.
	 *
	 * @return the job level, or 1 if not specified
	 */
	public Integer getRequiredJobLevel() {
		
		if (this.requiredJobLevel != null) {
			return this.requiredJobLevel;
		}
		if (this.jobLevel != null) {
			return this.jobLevel;
		}
		return 1;
	}
	
	/**
	 * Gets the map of required jobs.
	 * Combines all job sources.
	 *
	 * @return the map of required jobs
	 */
	public Map<String, Integer> getRequiredJobs() {
		
		final Map<String, Integer> jobMap = new HashMap<>();
		
		if (this.requiredJobs != null) {
			jobMap.putAll(this.requiredJobs);
		}
		
		if (this.jobs != null) {
			jobMap.putAll(this.jobs);
		}
		
		// Add single job if specified
		final String singleJob = this.getRequiredJob();
		if (! singleJob.isEmpty()) {
			jobMap.put(
				singleJob,
				this.getRequiredJobLevel()
			);
		}
		
		return jobMap;
	}
	
	/**
	 * Gets the job plugin identifier.
	 *
	 * @return the job plugin identifier, or empty string if not specified
	 */
	public String getJobPlugin() {
		
		return this.jobPlugin != null ?
		       this.jobPlugin :
		       "";
	}
	
	// ~~~ TIME-BASED REQUIREMENT GETTERS ~~~
	
	/**
	 * Gets the time constraint in seconds.
	 * Tries multiple field names for compatibility.
	 *
	 * @return the time constraint in seconds, or 0 if not specified
	 */
	public Long getTimeConstraintSeconds() {
		
		if (this.timeConstraintSeconds != null) {
			return this.timeConstraintSeconds;
		}
		if (this.timeConstraint != null) {
			return this.timeConstraint;
		}
		return 0L;
	}
	
	/**
	 * Gets the cooldown period in seconds.
	 * Tries multiple field names for compatibility.
	 *
	 * @return the cooldown in seconds, or 0 if not specified
	 */
	public Long getCooldownSeconds() {
		
		if (this.cooldownSeconds != null) {
			return this.cooldownSeconds;
		}
		if (this.cooldown != null) {
			return this.cooldown;
		}
		return 0L;
	}
	
	/**
	 * Gets the start time.
	 *
	 * @return the start time, or null if not specified
	 */
	public String getStartTime() {
		
		return this.startTime == null ? LocalDateTime.now().toString() : this.startTime;
	}
	
	/**
	 * Gets the end time.
	 *
	 * @return the end time, or null if not specified
	 */
	public String getEndTime() {
		
		return this.endTime;
	}
	
	// ~~~ COMPOSITE REQUIREMENT GETTERS ~~~
	
	/**
	 * Gets the composite operator for composite requirements.
	 * Tries multiple field names for compatibility.
	 *
	 * @return the composite operator, or "AND" if not specified
	 */
	public String getOperator() {
		
		if (this.operator != null) {
			return this.operator;
		}
		if (this.compositeOperator != null) {
			return this.compositeOperator;
		}
		return "AND";
	}
	
	/**
	 * Gets the list of requirements for composite requirements.
	 *
	 * @return the list of requirements, or an empty list if not specified
	 */
	public List<RequirementSection> getRequirements() {
		
		return this.requirements != null ?
		       this.requirements :
		       new ArrayList<>();
	}
	
	/**
	 * Gets the sub-requirements map for composite requirements.
	 *
	 * @return the map of sub-requirements, or an empty map if not specified
	 */
	public Map<String, RequirementSection> getSubRequirements() {
		
		return this.subRequirements != null ?
		       this.subRequirements :
		       new HashMap<>();
	}
	
	/**
	 * Gets the minimum number of requirements that must be met.
	 *
	 * @return the minimum required, or 1 if not specified
	 */
	public Integer getMinimumRequired() {
		
		return this.minimumRequired != null ?
		       this.minimumRequired :
		       1;
	}
	
	// ~~~ CHOICE REQUIREMENT GETTERS ~~~
	
	/**
	 * Gets the choices for choice requirements.
	 * Combines list and choiceList sources.
	 *
	 * @return the list of choices, or an empty list if not specified
	 */
	public List<RequirementSection> getChoices() {
		
		final List<RequirementSection> choicesList = new ArrayList<>();
		
		if (this.choices != null) {
			choicesList.addAll(this.choices);
		}
		
		if (this.choiceList != null) {
			choicesList.addAll(this.choiceList);
		}
		
		return choicesList;
	}
	
	/**
	 * Gets the choice map for choice requirements.
	 *
	 * @return the map of choices, or an empty map if not specified
	 */
	public Map<String, RequirementSection> getChoiceMap() {
		
		return this.choiceMap != null ?
		       this.choiceMap :
		       new HashMap<>();
	}
	
	/**
	 * Gets the description for requirements.
	 * Tries multiple field names for compatibility.
	 *
	 * @return the description, or empty string if not specified
	 */
	public String getDescription() {
		
		if (this.description != null) {
			return this.description;
		}
		if (this.choiceDescription != null) {
			return this.choiceDescription;
		}
		return "";
	}
	
	// ~~~ ADDITIONAL REQUIREMENT GETTERS ~~~
	
	/**
	 * Gets the required permission.
	 * Tries multiple field names for compatibility.
	 *
	 * @return the required permission, or empty string if not specified
	 */
	public String getRequiredPermission() {
		
		if (this.requiredPermission != null) {
			return this.requiredPermission;
		}
		if (this.permission != null) {
			return this.permission;
		}
		return "";
	}
	
	/**
	 * Gets the list of required permissions.
	 * Combines all permission sources.
	 *
	 * @return the list of required permissions
	 */
	public List<String> getRequiredPermissions() {
		
		final List<String> permissionList = new ArrayList<>();
		
		if (this.requiredPermissions != null) {
			permissionList.addAll(this.requiredPermissions);
		}
		
		if (this.permissions != null) {
			permissionList.addAll(this.permissions);
		}
		
		// Add single permission if specified
		final String singlePermission = this.getRequiredPermission();
		if (! singlePermission.isEmpty() && ! permissionList.contains(singlePermission)) {
			permissionList.add(singlePermission);
		}
		
		return permissionList;
	}
	
	/**
	 * Gets the required world name.
	 * Tries multiple field names for compatibility.
	 *
	 * @return the required world name, or empty string if not specified
	 */
	public String getRequiredWorld() {
		
		if (this.requiredWorld != null) {
			return this.requiredWorld;
		}
		if (this.world != null) {
			return this.world;
		}
		return "";
	}
	
	/**
	 * Gets the required region name.
	 * Tries multiple field names for compatibility.
	 *
	 * @return the required region name, or empty string if not specified
	 */
	public String getRequiredRegion() {
		
		if (this.requiredRegion != null) {
			return this.requiredRegion;
		}
		if (this.region != null) {
			return this.region;
		}
		return "";
	}
	
	/**
	 * Gets the required coordinates.
	 * Tries multiple field names for compatibility.
	 *
	 * @return the map of required coordinates, or an empty map if not specified
	 */
	public Map<String, Double> getRequiredCoordinates() {
		
		if (this.requiredCoordinates != null) {
			return this.requiredCoordinates;
		}
		if (this.coordinates != null) {
			return this.coordinates;
		}
		return new HashMap<>();
	}
	
	/**
	 * Gets the required distance.
	 * Tries multiple field names for compatibility.
	 *
	 * @return the required distance, or 0.0 if not specified
	 */
	public Double getRequiredDistance() {
		
		if (this.requiredDistance != null) {
			return this.requiredDistance;
		}
		if (this.distance != null) {
			return this.distance;
		}
		return 0.0;
	}
	
	// ~~~ GENERIC/EXTENSIBLE GETTERS ~~~
	
	/**
	 * Gets the generic data map.
	 *
	 * @return the data map, or an empty map if not specified
	 */
	public Map<String, Object> getData() {
		
		return this.data != null ?
		       this.data :
		       new HashMap<>();
	}
	
	/**
	 * Gets the custom data map.
	 *
	 * @return the custom data map, or an empty map if not specified
	 */
	public Map<String, Object> getCustomData() {
		
		return this.customData != null ?
		       this.customData :
		       new HashMap<>();
	}
	
	/**
	 * Gets the custom script.
	 *
	 * @return the custom script, or empty string if not specified
	 */
	public String getCustomScript() {
		
		return this.customScript != null ?
		       this.customScript :
		       "";
	}
	
}