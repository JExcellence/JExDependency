package com.raindropcentral.rdq.config.reward;

import de.jexcellence.configmapper.sections.AConfigSection;
import de.jexcellence.configmapper.sections.CSAlways;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;

/**
 * Represents a configuration section for defining a reward in the ranking system.
 * <p>
 * This section specifies the type, target, and amount of a reward, and is intended
 * to be used within the configuration mapping framework.
 * </p>
 *
 * @author JExcellence
 * @version 1.0.0
 * @since TBD
 */
@CSAlways
public class RewardSection extends AConfigSection {
	
	private String type;
	
	private String target;

	private Long amount;
	
	/**
	 * Constructs a new {@code RewardSection} with the specified evaluation environment builder.
	 *
	 * @param evaluationEnvironmentBuilder the builder used to provide evaluation context for this section
	 */
	public RewardSection(
		final EvaluationEnvironmentBuilder evaluationEnvironmentBuilder
	) {
		super(evaluationEnvironmentBuilder);
	}
	
}