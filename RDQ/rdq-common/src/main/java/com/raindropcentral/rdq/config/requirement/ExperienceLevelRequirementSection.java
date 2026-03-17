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

package com.raindropcentral.rdq.config.requirement;

import de.jexcellence.configmapper.sections.AConfigSection;
import de.jexcellence.configmapper.sections.CSAlways;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;

/**
 * Configuration section for experience level-based requirements.
 *
 * <p>This section handles all configuration options specific to ExperienceLevelRequirement,
 * including required levels, experience types, and consumption settings.
 *
 * @author JExcellence
 * @version 1.0.0
 * @since TBD
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
	private Integer requiredLevel;
	
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
	 */
	public ExperienceLevelRequirementSection(
		final EvaluationEnvironmentBuilder evaluationEnvironmentBuilder
	) {
		super(evaluationEnvironmentBuilder);
	}
	
	/**
	 * Gets consumeOnComplete.
	 */
	public Boolean getConsumeOnComplete() {
		return this.consumeOnComplete != null ? this.consumeOnComplete : true;
	}
	
	/**
	 * Gets description.
	 */
	public String getDescription() {
		return this.description;
	}
	
	/**
	 * Gets the required experience level.
	 *
	 * @return the required level, defaulting to 0
	 */
	public Integer getRequiredLevel() {
		return this.requiredLevel != null ? this.requiredLevel : 0;
	}
	
	/**
	 * Gets the experience type.
	 *
	 * @return the experience type, defaulting to "LEVEL"
	 */
	public String getExperienceType() {
		return this.requiredType != null ? this.requiredType : "LEVEL";
	}
}
