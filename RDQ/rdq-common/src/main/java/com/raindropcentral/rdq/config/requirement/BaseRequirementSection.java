package com.raindropcentral.rdq.config.requirement;

import com.raindropcentral.rdq.config.utility.IconSection;
import com.raindropcentral.rplatform.logging.CentralLogger;
import de.jexcellence.configmapper.sections.AConfigSection;
import de.jexcellence.configmapper.sections.CSAlways;
import de.jexcellence.configmapper.sections.CSIgnore;
import de.jexcellence.evaluable.section.ItemStackSection;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Base configuration section for all requirement types in the rank progression system.
 *
 * <p>This section serves as a polymorphic container that can hold any type of requirement
 * (item, currency, experience, etc.) along with common properties like display order and icon.
 * Display name and description keys are automatically generated if not provided.
 *
 * @author JExcellence
 * @version 1.0.0
 * @since TBD
 */
@CSAlways
public class BaseRequirementSection extends AConfigSection {
	
	@CSIgnore
	private static final Logger LOGGER = CentralLogger.getLoggerByName("RDQ");
	
	/**
	 * The type of requirement (ITEM, CURRENCY, EXPERIENCE_LEVEL, etc.).
	 * If null, the type will be auto-detected from the configured requirement section.
	 */
	private String type;
	
	/**
	 * The display order of this requirement in UI lists.
	 * If null, defaults to 0.
	 */
	private Integer displayOrder;
	
	/**
	 * The icon configuration for this requirement.
	 * If null, a default IconSection is provided.
	 */
	private IconSection icon;
	
	private ItemRequirementSection            itemRequirement;
	private CurrencyRequirementSection        currencyRequirement;
	private ExperienceLevelRequirementSection experienceRequirement;
	private PlaytimeRequirementSection        playtimeRequirement;
	private PermissionRequirementSection      permissionRequirement;
	private LocationRequirementSection        locationRequirement;
	private CompositeRequirementSection       compositeRequirement;
	private ChoiceRequirementSection          choiceRequirement;
	private AchievementRequirementSection     achievementRequirement;
	private SkillRequirementSection           skillRequirement;
	private JobRequirementSection             jobRequirement;
	private TimeBasedRequirementSection       timeBasedRequirement;
	
	// ==================== Flat-structure fields for inline YAML format ====================
	// These allow YAML like: type: "COMPOSITE", operator: "AND", requirements: [...]
	// instead of requiring: compositeRequirement: { operator: "AND", requirements: [...] }
	
	/** For COMPOSITE/CHOICE: logical operator (AND, OR, XOR, MINIMUM). */
	private String operator;
	
	/** For COMPOSITE/CHOICE: list of sub-requirements. */
	private List<BaseRequirementSection> requirements;
	
	/** For COMPOSITE/CHOICE: list of choices. */
	private List<BaseRequirementSection> choices;
	
	/** For COMPOSITE/CHOICE: minimum required count. */
	private Integer minimumRequired;
	
	/** For COMPOSITE/CHOICE: minimum choices required (alias). */
	private Integer minimumChoicesRequired;
	
	/** For COMPOSITE/CHOICE: maximum required count. */
	private Integer maximumRequired;
	
	/** For COMPOSITE/CHOICE: allow partial progress. */
	private Boolean allowPartialProgress;
	
	/** For CHOICE: mutually exclusive choices. */
	private Boolean mutuallyExclusive;
	
	/** For CHOICE: allow changing choice. */
	private Boolean allowChoiceChange;
	
	/** For EXPERIENCE_LEVEL: required level (flat format). */
	private Integer level;
	
	/** For CURRENCY: flat currency fields (YAML: currency, amount, consumable). */
	private String currency;
	private Double amount;
	private Boolean consumable;
	
	/** For ITEM: single required item (flat format). */
	private de.jexcellence.evaluable.section.ItemStackSection requiredItem;
	
	/**
	 * Context information for auto-generating keys (set by the factory).
	 */
	@CSIgnore
	private String rankTreeName;
	
	@CSIgnore
	private String rankName;
	
	@CSIgnore
	private String requirementKey;
	
	/**
	 * Constructs a new BaseRequirementSection with the given evaluation environment.
	 *
	 * @param evaluationEnvironmentBuilder the evaluation environment builder
	 */
	public BaseRequirementSection(
		final EvaluationEnvironmentBuilder evaluationEnvironmentBuilder
	) {
		
		super(evaluationEnvironmentBuilder);
	}
	
	/**
	 * Called after parsing the configuration fields. Auto-generates keys and validates requirement configuration.
	 *
	 * @param fields the list of fields parsed
	 *
	 * @throws Exception if an error occurs during post-processing
	 */
	@Override
	public void afterParsing(final List<Field> fields) throws Exception {
		
		super.afterParsing(fields);
		
		// Map flat-structure fields to nested requirement sections based on type
		mapFlatStructureToNestedSections();
		
		if (
			this.type == null ||
			this.type.equals("not_defined")
		) {
			this.type = this.autoDetectRequirementType();
		}
		
		if (
			this.icon != null &&
			this.rankTreeName != null
		) {
			String baseKey = "requirement." + this.rankTreeName + "." + this.type.toLowerCase();
			
			if (
				this.icon.getDisplayNameKey() == null ||
				this.icon.getDisplayNameKey().equals("not_defined")
			) {
				this.icon.setDisplayNameKey(baseKey + ".name");
			}
			
			if (
				this.icon.getDescriptionKey() == null ||
				this.icon.getDescriptionKey().equals("not_defined")
			) {
				this.icon.setDescriptionKey(baseKey + ".lore");
			}
		}
	}
	
	/**
	 * Maps flat-structure YAML fields to nested requirement sections.
	 * This allows YAML configs to use either format:
	 * 
	 * Flat format:
	 *   type: "COMPOSITE"
	 *   operator: "AND"
	 *   requirements: [...]
	 * 
	 * Nested format:
	 *   compositeRequirement:
	 *     operator: "AND"
	 *     requirements: [...]
	 */
	private void mapFlatStructureToNestedSections() {
		if (this.type == null || this.type.equals("not_defined")) {
			return;
		}
		
		String upperType = this.type.toUpperCase();
		LOGGER.fine("Mapping flat structure for type: " + upperType);
		
		switch (upperType) {
			case "COMPOSITE" -> mapToCompositeRequirement();
			case "CHOICE" -> mapToChoiceRequirement();
			case "EXPERIENCE_LEVEL" -> mapToExperienceRequirement();
			case "CURRENCY" -> mapToCurrencyRequirement();
			case "ITEM" -> mapToItemRequirement();
			case "PLAYTIME" -> mapToPlaytimeRequirement();
		}
	}
	
	/**
	 * Maps flat COMPOSITE fields to CompositeRequirementSection.
	 */
	private void mapToCompositeRequirement() {
		if (this.compositeRequirement != null && 
		    this.compositeRequirement.getCompositeRequirements() != null &&
		    !this.compositeRequirement.getCompositeRequirements().isEmpty()) {
			return; // Already has nested section
		}
		
		if (this.requirements == null || this.requirements.isEmpty()) {
			LOGGER.warning("COMPOSITE requirement has no sub-requirements");
			return;
		}
		
		// Create a new CompositeRequirementSection and populate it
		CompositeRequirementSection composite = new CompositeRequirementSection(
			new EvaluationEnvironmentBuilder()
		);
		
		// Use reflection to set private fields since we can't modify the class
		try {
			setFieldValue(composite, "operator", this.operator);
			setFieldValue(composite, "requirements", this.requirements);
			setFieldValue(composite, "minimumRequired", this.minimumRequired);
			setFieldValue(composite, "maximumRequired", this.maximumRequired);
			setFieldValue(composite, "allowPartialProgress", this.allowPartialProgress);
			
			this.compositeRequirement = composite;
			LOGGER.info("Mapped flat COMPOSITE with " + this.requirements.size() + " sub-requirements, operator=" + this.operator);
		} catch (Exception e) {
			LOGGER.warning("Failed to map flat COMPOSITE structure: " + e.getMessage());
		}
	}
	
	/**
	 * Maps flat CHOICE fields to ChoiceRequirementSection.
	 */
	private void mapToChoiceRequirement() {
		if (this.choiceRequirement != null && 
		    this.choiceRequirement.getChoices() != null &&
		    !this.choiceRequirement.getChoices().isEmpty()) {
			return; // Already has nested section
		}
		
		List<BaseRequirementSection> choiceList = this.choices != null ? this.choices : this.requirements;
		if (choiceList == null || choiceList.isEmpty()) {
			LOGGER.warning("CHOICE requirement has no choices");
			return;
		}
		
		ChoiceRequirementSection choice = new ChoiceRequirementSection(
			new EvaluationEnvironmentBuilder()
		);
		
		try {
			setFieldValue(choice, "choices", choiceList);
			Integer minReq = this.minimumChoicesRequired != null ? this.minimumChoicesRequired : this.minimumRequired;
			setFieldValue(choice, "minimumRequired", minReq);
			setFieldValue(choice, "maximumRequired", this.maximumRequired);
			setFieldValue(choice, "allowPartialProgress", this.allowPartialProgress);
			setFieldValue(choice, "mutuallyExclusive", this.mutuallyExclusive);
			setFieldValue(choice, "allowChoiceChange", this.allowChoiceChange);
			
			this.choiceRequirement = choice;
			LOGGER.info("Mapped flat CHOICE with " + choiceList.size() + " choices, minRequired=" + minReq);
		} catch (Exception e) {
			LOGGER.warning("Failed to map flat CHOICE structure: " + e.getMessage());
		}
	}
	
	/**
	 * Maps flat EXPERIENCE_LEVEL fields to ExperienceLevelRequirementSection.
	 */
	private void mapToExperienceRequirement() {
		if (this.experienceRequirement != null && this.experienceRequirement.getRequiredLevel() > 0) {
			return; // Already has nested section
		}
		
		if (this.level == null || this.level <= 0) {
			LOGGER.warning("EXPERIENCE_LEVEL requirement has no level specified");
			return;
		}
		
		ExperienceLevelRequirementSection exp = new ExperienceLevelRequirementSection(
			new EvaluationEnvironmentBuilder()
		);
		
		try {
			// The field in ExperienceLevelRequirementSection is 'requiredLevel'
			setFieldValue(exp, "requiredLevel", this.level);
			setFieldValue(exp, "requiredType", "LEVEL");
			
			this.experienceRequirement = exp;
			LOGGER.info("Mapped flat EXPERIENCE_LEVEL with level=" + this.level);
		} catch (Exception e) {
			LOGGER.warning("Failed to map flat EXPERIENCE_LEVEL structure: " + e.getMessage());
		}
	}
	
	/**
	 * Maps flat CURRENCY fields to CurrencyRequirementSection.
	 */
	private void mapToCurrencyRequirement() {
		// Only map if we have flat fields and no nested section
		if (this.currency == null || this.amount == null || this.amount <= 0) {
			LOGGER.warning("CURRENCY requirement missing currency or amount fields");
			return;
		}
		
		CurrencyRequirementSection curr = new CurrencyRequirementSection(new EvaluationEnvironmentBuilder());
		
		try {
			setFieldValue(curr, "currency", this.currency);
			setFieldValue(curr, "amount", this.amount);
			if (this.consumable != null) {
				setFieldValue(curr, "consumeOnComplete", this.consumable);
			}
			
			this.currencyRequirement = curr;
			LOGGER.info("Mapped flat CURRENCY: " + this.currency + " = " + this.amount);
		} catch (Exception e) {
			LOGGER.warning("Failed to map flat CURRENCY: " + e.getMessage());
		}
	}
	
	/**
	 * Maps flat ITEM fields to ItemRequirementSection.
	 */
	private void mapToItemRequirement() {
		if (this.itemRequirement != null && 
		    this.itemRequirement.getRequiredItemsList() != null &&
		    !this.itemRequirement.getRequiredItemsList().isEmpty()) {
			return; // Already has nested section
		}
		
		if (this.requiredItem == null) {
			LOGGER.warning("ITEM requirement has no requiredItem specified");
			return;
		}
		
		ItemRequirementSection item = new ItemRequirementSection(
			new EvaluationEnvironmentBuilder()
		);
		
		try {
			// Convert ItemStackSection to the expected map format
			Map<String, ItemStackSection> itemsMap = new HashMap<>();
			itemsMap.put("item1", this.requiredItem);
			setFieldValue(item, "requiredItems", itemsMap);
			
			this.itemRequirement = item;
			LOGGER.info("Mapped flat ITEM with requiredItem");
		} catch (Exception e) {
			LOGGER.warning("Failed to map flat ITEM structure: " + e.getMessage());
		}
	}
	
	/**
	 * Maps flat PLAYTIME fields to PlaytimeRequirementSection.
	 */
	private void mapToPlaytimeRequirement() {
		if (this.playtimeRequirement != null && this.playtimeRequirement.getRequiredPlaytimeSeconds() > 0) {
			// Already has nested section - just ensure it's assigned
			return;
		}
	}
	
	/**
	 * Helper to set private field values via reflection.
	 */
	private void setFieldValue(Object target, String fieldName, Object value) throws Exception {
		if (value == null) return;
		
		Class<?> clazz = target.getClass();
		while (clazz != null) {
			try {
				Field field = clazz.getDeclaredField(fieldName);
				field.setAccessible(true);
				field.set(target, value);
				return;
			} catch (NoSuchFieldException e) {
				clazz = clazz.getSuperclass();
			}
		}
		throw new NoSuchFieldException("Field " + fieldName + " not found in " + target.getClass().getName());
	}
	
	/**
	 * Auto-detects the requirement type based on which requirement section is configured.
	 *
	 * @return the detected requirement type, or "UNKNOWN" if none found
	 */
	private String autoDetectRequirementType() {
		
		if (
			this.itemRequirement != null &&
			! this.itemRequirement.getRequiredItemsList().isEmpty()
		) {
			return "ITEM";
		}
		if (
			this.currencyRequirement != null &&
			this.currencyRequirement.getRequiredCurrencies() != null &&
			! this.currencyRequirement.getRequiredCurrencies().isEmpty()
		) {
			return "CURRENCY";
		}
		if (
			this.experienceRequirement != null &&
			this.experienceRequirement.getRequiredLevel() > 0
		) {
			return "EXPERIENCE_LEVEL";
		}
		if (
			this.playtimeRequirement != null &&
			this.playtimeRequirement.getRequiredPlaytimeSeconds() > 0
		) {
			return "PLAYTIME";
		}
		if (
			this.permissionRequirement != null &&
			this.permissionRequirement.getRequiredPermissions() != null &&
			! this.permissionRequirement.getRequiredPermissions().isEmpty()
		) {
			return "PERMISSION";
		}
		if (
			this.locationRequirement != null
		) {
			return "LOCATION";
		}
		
		if (
			this.compositeRequirement != null &&
			this.compositeRequirement.getCompositeRequirements() != null &&
			! this.compositeRequirement.getCompositeRequirements().isEmpty()
		) {
			return "COMPOSITE";
		}
		if (
			this.choiceRequirement != null &&
			this.choiceRequirement.getChoices() != null &&
			! this.choiceRequirement.getChoices().isEmpty()
		) {
			return "CHOICE";
		}
		if (
			this.achievementRequirement != null &&
			this.achievementRequirement.getRequiredAchievements() != null &&
			! this.achievementRequirement.getRequiredAchievements().isEmpty()
		) {
			return "ACHIEVEMENT";
		}
		if (
			this.skillRequirement != null &&
			this.skillRequirement.getRequiredSkills() != null &&
			! this.skillRequirement.getRequiredSkills().isEmpty()
		) {
			return "SKILLS";
		}
		if (
			this.jobRequirement != null &&
			this.jobRequirement.getRequiredJobs() != null &&
			! this.jobRequirement.getRequiredJobs().isEmpty()
		) {
			return "JOBS";
		}
		if (
			this.timeBasedRequirement != null &&
			this.timeBasedRequirement.getTimeConstraintSeconds() > 0
		) {
			return "TIME_BASED";
		}
		
		return "UNKNOWN";
	}
	
	/**
	 * Sets the context information for this requirement.
	 *
	 * @param rankTreeName   the name of the rank tree
	 * @param rankName       the name of the rank
	 * @param requirementKey the key of this requirement
	 */
	public void setContext(
		final String rankTreeName,
		final String rankName,
		final String requirementKey
	) {
		
		this.rankTreeName = rankTreeName;
		this.rankName = rankName;
		this.requirementKey = requirementKey;
	}
	
	/**
	 * Gets the requirement type.
	 *
	 * @return the requirement type, or "not_defined" if not set
	 */
	public String getType() {
		
		return this.type == null ?
		       "not_defined" :
		       this.type;
	}
	
	/**
	 * Gets the display order of this requirement.
	 *
	 * @return the display order, or 0 if not set
	 */
	public Integer getDisplayOrder() {
		
		return this.displayOrder == null ?
		       0 :
		       this.displayOrder;
	}
	
	/**
	 * Gets the icon configuration for this requirement.
	 *
	 * @return the icon section, or a default IconSection if not set
	 */
	public IconSection getIcon() {
		
		return this.icon == null ?
		       new IconSection(new EvaluationEnvironmentBuilder()) :
		       this.icon;
	}
	
	/**
	 * Gets itemRequirement.
	 */
	public ItemRequirementSection getItemRequirement() {
		
		return this.itemRequirement == null ?
		       new ItemRequirementSection(new EvaluationEnvironmentBuilder()) :
		       this.itemRequirement;
	}
	
	/**
	 * Gets currencyRequirement.
	 */
	public CurrencyRequirementSection getCurrencyRequirement() {
		return this.currencyRequirement;
	}
	
	/**
	 * Gets experienceRequirement.
	 */
	public ExperienceLevelRequirementSection getExperienceRequirement() {
		
		return this.experienceRequirement == null ?
		       new ExperienceLevelRequirementSection(new EvaluationEnvironmentBuilder()) :
		       this.experienceRequirement;
	}
	
	/**
	 * Gets playtimeRequirement.
	 */
	public PlaytimeRequirementSection getPlaytimeRequirement() {
		
		return this.playtimeRequirement == null ?
		       new PlaytimeRequirementSection(new EvaluationEnvironmentBuilder()) :
		       this.playtimeRequirement;
	}
	
	/**
	 * Gets permissionRequirement.
	 */
	public PermissionRequirementSection getPermissionRequirement() {
		
		return this.permissionRequirement == null ?
		       new PermissionRequirementSection(new EvaluationEnvironmentBuilder()) :
		       this.permissionRequirement;
	}
	
	/**
	 * Gets locationRequirement.
	 */
	public LocationRequirementSection getLocationRequirement() {
		
		return this.locationRequirement == null ?
		       new LocationRequirementSection(new EvaluationEnvironmentBuilder()) :
		       this.locationRequirement;
	}
	
	/**
	 * Gets compositeRequirement.
	 */
	public CompositeRequirementSection getCompositeRequirement() {
		
		return this.compositeRequirement == null ?
		       new CompositeRequirementSection(new EvaluationEnvironmentBuilder()) :
		       this.compositeRequirement;
	}
	
	/**
	 * Gets choiceRequirement.
	 */
	public ChoiceRequirementSection getChoiceRequirement() {
		
		return this.choiceRequirement == null ?
		       new ChoiceRequirementSection(new EvaluationEnvironmentBuilder()) :
		       this.choiceRequirement;
	}
	
	/**
	 * Gets achievementRequirement.
	 */
	public AchievementRequirementSection getAchievementRequirement() {
		
		return this.achievementRequirement == null ?
		       new AchievementRequirementSection(new EvaluationEnvironmentBuilder()) :
		       this.achievementRequirement;
	}
	
	/**
	 * Gets skillRequirement.
	 */
	public SkillRequirementSection getSkillRequirement() {
		
		return this.skillRequirement == null ?
		       new SkillRequirementSection(new EvaluationEnvironmentBuilder()) :
		       this.skillRequirement;
	}
	
	/**
	 * Gets jobRequirement.
	 */
	public JobRequirementSection getJobRequirement() {
		
		return this.jobRequirement == null ?
		       new JobRequirementSection(new EvaluationEnvironmentBuilder()) :
		       this.jobRequirement;
	}
	
	/**
	 * Gets timeBasedRequirement.
	 */
	public TimeBasedRequirementSection getTimeBasedRequirement() {
		
		return this.timeBasedRequirement == null ?
		       new TimeBasedRequirementSection(new EvaluationEnvironmentBuilder()) :
		       this.timeBasedRequirement;
	}
	
	/**
	 * Gets the context information.
	 */
	public String getRankTreeName() {
		
		return this.rankTreeName;
	}
	
	/**
	 * Gets rankName.
	 */
	public String getRankName() {
		
		return this.rankName;
	}
	
	/**
	 * Gets requirementKey.
	 */
	public String getRequirementKey() {
		
		return this.requirementKey;
	}
	
}
