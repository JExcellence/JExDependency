package com.raindropcentral.rdq.config.perks.sections;

import com.raindropcentral.rdq.config.perks.PerkSection;
import com.raindropcentral.rdq.config.perks.sections.forge.CaptisForgeSection;
import de.jexcellence.configmapper.sections.CSAlways;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;

/**
 * Configuration section for the Captis perk.
 * <p>
 * This section defines the parameters for improving fishing catch chance,
 * including the improvement rate and an optional forge section for advanced configuration.
 * </p>
 *
 * @author ItsRainingHP
 * @version 1.0.0
 * @since TBD
 */
public class CaptisSection extends PerkSection {
	
	/**
	 * Rate to improve fishing catch chance. 0.5 = 50% improvement.
	 */
	@CSAlways
	private double rate;
	
	/**
	 * Forge section of the perk, providing advanced configuration for Captis.
	 */
	@CSAlways
	private CaptisForgeSection captisForgeSection;
	
	/**
	 * Constructs a new {@code CaptisSection} with the specified evaluation environment.
	 *
	 * @param evaluationEnvironmentBuilder the base evaluation environment for this configuration section
	 */
	public CaptisSection(final EvaluationEnvironmentBuilder evaluationEnvironmentBuilder) {
		super(evaluationEnvironmentBuilder);
	}
	
	/**
	 * Gets the rate to improve fishing catch chance.
	 *
	 * @return the improvement rate as a double (e.g., 0.5 for 50% improvement)
	 */
	public double getRate() {
		return this.rate;
	}
	
	/**
	 * Gets the forge section for advanced Captis configuration.
	 *
	 * @return the {@link CaptisForgeSection} instance, or {@code null} if not set
	 */
	public CaptisForgeSection getCaptisForgeSection() {
		return this.captisForgeSection;
	}
	
}