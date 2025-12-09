package com.raindropcentral.rdq.config.requirement;

import com.raindropcentral.rdq.config.utility.IconSection;
import de.jexcellence.configmapper.sections.AConfigSection;
import de.jexcellence.configmapper.sections.CSAlways;
import de.jexcellence.configmapper.sections.CSIgnore;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;

import java.lang.reflect.Field;
import java.util.List;

/**
 * Base configuration section for all requirement types in the rank progression system.
 * <p>
 * This section serves as a polymorphic container that can hold any type of requirement
 * (item, currency, experience, etc.) along with common properties like display order and icon.
 * Display name and description keys are automatically generated if not provided.
 * </p>
 *
 * @author JExcellence
 * @version 1.0.0
 * @since TBD
 */
@CSAlways
public class BaseRequirementSection extends AConfigSection {
	
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
	
	public ItemRequirementSection getItemRequirement() {
		
		return this.itemRequirement == null ?
		       new ItemRequirementSection(new EvaluationEnvironmentBuilder()) :
		       this.itemRequirement;
	}
	
	public CurrencyRequirementSection getCurrencyRequirement() {
		
		return this.currencyRequirement == null ?
		       new CurrencyRequirementSection(new EvaluationEnvironmentBuilder()) :
		       this.currencyRequirement;
	}
	
	public ExperienceLevelRequirementSection getExperienceRequirement() {
		
		return this.experienceRequirement == null ?
		       new ExperienceLevelRequirementSection(new EvaluationEnvironmentBuilder()) :
		       this.experienceRequirement;
	}
	
	public PlaytimeRequirementSection getPlaytimeRequirement() {
		
		return this.playtimeRequirement == null ?
		       new PlaytimeRequirementSection(new EvaluationEnvironmentBuilder()) :
		       this.playtimeRequirement;
	}
	
	public PermissionRequirementSection getPermissionRequirement() {
		
		return this.permissionRequirement == null ?
		       new PermissionRequirementSection(new EvaluationEnvironmentBuilder()) :
		       this.permissionRequirement;
	}
	
	public LocationRequirementSection getLocationRequirement() {
		
		return this.locationRequirement == null ?
		       new LocationRequirementSection(new EvaluationEnvironmentBuilder()) :
		       this.locationRequirement;
	}
	
	public CompositeRequirementSection getCompositeRequirement() {
		
		return this.compositeRequirement == null ?
		       new CompositeRequirementSection(new EvaluationEnvironmentBuilder()) :
		       this.compositeRequirement;
	}
	
	public ChoiceRequirementSection getChoiceRequirement() {
		
		return this.choiceRequirement == null ?
		       new ChoiceRequirementSection(new EvaluationEnvironmentBuilder()) :
		       this.choiceRequirement;
	}
	
	public AchievementRequirementSection getAchievementRequirement() {
		
		return this.achievementRequirement == null ?
		       new AchievementRequirementSection(new EvaluationEnvironmentBuilder()) :
		       this.achievementRequirement;
	}
	
	public SkillRequirementSection getSkillRequirement() {
		
		return this.skillRequirement == null ?
		       new SkillRequirementSection(new EvaluationEnvironmentBuilder()) :
		       this.skillRequirement;
	}
	
	public JobRequirementSection getJobRequirement() {
		
		return this.jobRequirement == null ?
		       new JobRequirementSection(new EvaluationEnvironmentBuilder()) :
		       this.jobRequirement;
	}
	
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
	
	public String getRankName() {
		
		return this.rankName;
	}
	
	public String getRequirementKey() {
		
		return this.requirementKey;
	}
	
}