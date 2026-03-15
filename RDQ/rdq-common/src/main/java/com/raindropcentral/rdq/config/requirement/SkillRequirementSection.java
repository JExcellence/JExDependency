package com.raindropcentral.rdq.config.requirement;

import de.jexcellence.configmapper.sections.AConfigSection;
import de.jexcellence.configmapper.sections.CSAlways;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration section for skill-based requirements.
 *
 * <p>This section handles all configuration options specific to SkillRequirement,
 * including required skills, skill levels, and skill plugin integration.
 *
 * @author JExcellence
 * @version 1.0.0
 * @since TBD
 */
@CSAlways
public class SkillRequirementSection extends AConfigSection {
	
	// ~~~ SKILL-SPECIFIC PROPERTIES ~~~
	
	/**
	 * Single required skill name.
	 * YAML key: "requiredSkill"
	 */
	private String requiredSkill;
	
	/**
	 * Alternative skill field name.
	 * YAML key: "skill"
	 */
	private String skill;
	
	/**
	 * Required skill level for single skill.
	 * YAML key: "requiredSkillLevel"
	 */
	private Integer requiredSkillLevel;
	
	/**
	 * Alternative skill level field name.
	 * YAML key: "skillLevel"
	 */
	private Integer skillLevel;
	
	/**
	 * Map of required skills with their levels.
	 * YAML key: "requiredSkills"
	 */
	private Map<String, Integer> requiredSkills;
	
	/**
	 * Alternative skills map field name.
	 * YAML key: "skills"
	 */
	private Map<String, Integer> skills;
	
	/**
	 * Skill plugin identifier (e.g., "mcmmo", "skillapi", "aureliumskills").
	 * YAML key: "skillPlugin"
	 */
	private String skillPlugin;
	
	/**
	 * Whether this requirement should consume skill levels when completed.
	 * YAML key: "consumeOnComplete"
	 */
	private Boolean consumeOnComplete;
	
	/**
	 * Constructs a new SkillRequirementSection.
	 *
	 * @param evaluationEnvironmentBuilder the evaluation environment builder
	 */
	public SkillRequirementSection(EvaluationEnvironmentBuilder evaluationEnvironmentBuilder) {
		super(evaluationEnvironmentBuilder);
	}
	
	// ~~~ GETTERS ~~~
	
	/**
	 * Gets the single required skill, trying multiple field names.
	 *
	 * @return the required skill name
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
	 * Gets the required skill level, trying multiple field names.
	 *
	 * @return the required skill level, defaulting to 1
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
	 * Gets the complete map of required skills from all sources.
	 *
	 * @return combined map of all required skills
	 */
	public Map<String, Integer> getRequiredSkills() {
		Map<String, Integer> skillMap = new HashMap<>();
		
		// Add skills from requiredSkills map
		if (this.requiredSkills != null) {
			skillMap.putAll(this.requiredSkills);
		}
		
		// Add skills from alternative skills map
		if (this.skills != null) {
			skillMap.putAll(this.skills);
		}
		
		// Add single skill if specified
		String singleSkill = getRequiredSkill();
		if (!singleSkill.isEmpty()) {
			skillMap.put(singleSkill, getRequiredSkillLevel());
		}
		
		return skillMap;
	}
	
	/**
	 * Gets the skill plugin identifier.
	 *
	 * @return the skill plugin identifier
	 */
	public String getSkillPlugin() {
		return this.skillPlugin != null ? this.skillPlugin : "mcmmo";
	}
	
	/**
	 * Gets whether skill levels should be consumed on completion.
	 *
	 * @return true if skill levels should be consumed, false otherwise
	 */
	public Boolean getConsumeOnComplete() {
		return this.consumeOnComplete != null ? this.consumeOnComplete : false;
	}
}
