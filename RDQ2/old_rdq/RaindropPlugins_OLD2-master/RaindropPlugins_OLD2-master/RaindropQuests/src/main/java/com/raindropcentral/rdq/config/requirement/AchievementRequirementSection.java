package com.raindropcentral.rdq.config.requirement;

import de.jexcellence.configmapper.sections.AConfigSection;
import de.jexcellence.configmapper.sections.CSAlways;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration section for achievement-based requirements.
 * <p>
 * This section handles all configuration options specific to AchievementRequirement,
 * including required achievements and achievement checking modes.
 * </p>
 *
 * @author JExcellence
 * @version 1.0.0
 * @since TBD
 */
@CSAlways
public class AchievementRequirementSection extends AConfigSection {
	
	// ~~~ ACHIEVEMENT-SPECIFIC PROPERTIES ~~~
	
	/**
	 * Single required achievement.
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
	 * Whether all achievements must be completed (AND) or just one (OR).
	 * YAML key: "requireAll"
	 */
	private Boolean requireAll;
	
	/**
	 * Achievement plugin identifier (e.g., "advancedachievements", "achievements").
	 * YAML key: "achievementPlugin"
	 */
	private String achievementPlugin;
	
	/**
	 * Constructs a new AchievementRequirementSection.
	 *
	 * @param evaluationEnvironmentBuilder the evaluation environment builder
	 */
	public AchievementRequirementSection(EvaluationEnvironmentBuilder evaluationEnvironmentBuilder) {
		super(evaluationEnvironmentBuilder);
	}
	
	// ~~~ GETTERS ~~~
	
	/**
0	 * Gets the single required achievement, trying multiple field names.
	 *
	 * @return the required achievement
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
	 * Gets the complete list of required achievements from all sources.
	 *
	 * @return combined list of all required achievements
	 */
	public List<String> getRequiredAchievements() {
		List<String> achievementList = new ArrayList<>();
		
		// Add achievements from requiredAchievements list
		if (this.requiredAchievements != null) {
			achievementList.addAll(this.requiredAchievements);
		}
		
		// Add achievements from alternative achievements list
		if (this.achievements != null) {
			achievementList.addAll(this.achievements);
		}
		
		// Add single achievement if specified
		String singleAchievement = getRequiredAchievement();
		if (!singleAchievement.isEmpty() && !achievementList.contains(singleAchievement)) {
			achievementList.add(singleAchievement);
		}
		
		return achievementList;
	}
	
	/**
	 * Gets whether all achievements must be completed.
	 *
	 * @return true if all achievements are required, false if only one is needed
	 */
	public Boolean getRequireAll() {
		return this.requireAll != null ? this.requireAll : true;
	}
	
	/**
	 * Gets the achievement plugin identifier.
	 *
	 * @return the achievement plugin identifier
	 */
	public String getAchievementPlugin() {
		return this.achievementPlugin != null ? this.achievementPlugin : "advancedachievements";
	}
}