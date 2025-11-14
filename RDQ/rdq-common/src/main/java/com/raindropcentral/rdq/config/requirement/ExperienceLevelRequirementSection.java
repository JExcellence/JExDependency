package com.raindropcentral.rdq.config.requirement;

import de.jexcellence.configmapper.sections.AConfigSection;
import de.jexcellence.configmapper.sections.CSAlways;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;

/**
 * Configuration section for experience level-based requirements.
 * <p>
 * This section handles all configuration options specific to experience level requirements,
 * including required levels, experience types, and consumption settings.
 * </p>
 *
 * @author JExcellence
 * @version 1.0.1
 * @since 1.0.0
 */
@CSAlways
public class ExperienceLevelRequirementSection extends AConfigSection {

    /**
     * Whether this requirement should consume experience when completed.
     * YAML key: "consumeOnComplete"
     */
    private Boolean consumeOnComplete;

    /**
     * Required experience level.
     * YAML key: "requiredLevel"
     */
    private Integer requiredExperience;

    /**
     * Type of experience requirement (LEVEL, POINTS).
     * YAML key: "experienceType"
     */
    private String requiredType;

    /**
     * Custom description for this specific requirement.
     * YAML key: "description"
     */
    private String description;

    /**
     * Constructs a new ExperienceLevelRequirementSection.
     *
     * @param evaluationEnvironmentBuilder the evaluation environment builder
     */    /**
     * Default no-arg constructor for Jackson deserialization.
     */
    protected ExperienceLevelRequirementSection() {
        super(new EvaluationEnvironmentBuilder());
    }

    public ExperienceLevelRequirementSection(
            final EvaluationEnvironmentBuilder evaluationEnvironmentBuilder
    ) {
        super(evaluationEnvironmentBuilder);
    }

    /**
     * Indicates whether the requirement should consume experience once completed.
     *
     * @return {@code true} if experience should be consumed, {@code false} otherwise
     */
    public Boolean getConsumeOnComplete() {
        return this.consumeOnComplete != null ? this.consumeOnComplete : true;
    }

    /**
     * Provides the custom description configured for this requirement.
     *
     * @return the configured description, or {@code null} if none was specified
     */
    public String getDescription() {
        return this.description;
    }

    /**
     * Gets the required experience level.
     *
     * @return the required level, defaulting to {@code 0} when not configured
     */
    public Integer getRequiredLevel() {
        if (
                this.requiredExperience != null
        ) {
            return this.requiredExperience;
        }
        return 0;
    }

    /**
     * Gets the experience type.
     *
     * @return the experience type, defaulting to {@code "LEVEL"}
     */
    public String getExperienceType() {
        return this.requiredType != null ? this.requiredType : "LEVEL";
    }
}