package com.raindropcentral.rdq.config.requirement;

import de.jexcellence.configmapper.sections.AConfigSection;
import de.jexcellence.configmapper.sections.CSAlways;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration section for skill-based requirements.
 * <p>
 * The section consolidates multiple configuration aliases into a single
 * canonical view that downstream requirement processors can consume without
 * caring which YAML keys were supplied. It also exposes default values so that
 * missing configuration never results in {@code null} handling.
 * </p>
 *
 * @author JExcellence
 * @version 1.0.1
 * @since 1.0.0
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
         * @param evaluationEnvironmentBuilder the evaluation environment builder used by
         *                                      the parent {@link AConfigSection} to
         *                                      resolve expressions embedded within the
         *                                      configuration
         */    /**
     * Default no-arg constructor for Jackson deserialization.
     */
    protected SkillRequirementSection() {
        super(new EvaluationEnvironmentBuilder());
    }

	public SkillRequirementSection(EvaluationEnvironmentBuilder evaluationEnvironmentBuilder) {
		super(evaluationEnvironmentBuilder);
	}
	
	// ~~~ GETTERS ~~~
	
        /**
         * Gets the single required skill, trying multiple field names.
         * <p>
         * The primary YAML key is {@code requiredSkill}, but the legacy alias
         * {@code skill} is also accepted. When neither key is present an empty
         * string is returned to simplify downstream comparisons.
         * </p>
         *
         * @return the required skill name, or an empty string when not configured
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
         * <p>
         * This method reads {@code requiredSkillLevel} first and then falls back to
         * {@code skillLevel}. A default value of {@code 1} is returned when neither
         * field is configured to guarantee a non-null level for downstream logic.
         * </p>
         *
         * @return the required skill level, defaulting to 1 when unspecified
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
         * <p>
         * The resulting map merges {@code requiredSkills}, {@code skills}, and the
         * singular skill entry (if present). Later values overwrite earlier ones,
         * ensuring the most recently configured alias wins when duplicates exist.
         * </p>
         *
         * @return combined map of all required skills with alias precedence applied
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
         * <p>
         * Defaults to {@code mcmmo} so that existing installations without an
         * explicit plugin mapping continue to function.
         * </p>
         *
         * @return the skill plugin identifier, defaulting to {@code mcmmo}
         */
	public String getSkillPlugin() {
		return this.skillPlugin != null ? this.skillPlugin : "mcmmo";
	}
	
        /**
         * Gets whether skill levels should be consumed on completion.
         * <p>
         * When unset the requirement does not consume any levels, enabling a
         * passive check-only mode without additional configuration.
         * </p>
         *
         * @return {@code true} if skill levels should be consumed, otherwise {@code false}
         */
	public Boolean getConsumeOnComplete() {
		return this.consumeOnComplete != null ? this.consumeOnComplete : false;
	}
}