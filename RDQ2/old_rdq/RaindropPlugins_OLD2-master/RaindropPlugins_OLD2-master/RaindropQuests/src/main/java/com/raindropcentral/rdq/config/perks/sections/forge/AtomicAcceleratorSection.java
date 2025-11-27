package com.raindropcentral.rdq.config.perks.sections.forge;

import de.jexcellence.configmapper.sections.AConfigSection;
import de.jexcellence.configmapper.sections.CSAlways;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;

/**
 * Configuration section for the Atomic Accelerator perk.
 * <p>
 * This section defines the parameters for the accelerator module, specifically
 * the rate at which it improves the atomic reactor timer.
 * </p>
 *
 * @author ItsRainingHP
 * @version 1.0.0
 * @since TBD
 */
public class AtomicAcceleratorSection extends AConfigSection {
	
	/**
	 * Rate the accelerator module improves the atomic reactor timer.
	 */
	@CSAlways
	private double rate;

	/**
	 * Constructs a new {@code AtomicAcceleratorSection} with the specified evaluation environment.
	 *
	 * @param baseEnvironment the base evaluation environment for this configuration section
	 */
	public AtomicAcceleratorSection(final EvaluationEnvironmentBuilder baseEnvironment) {
		super(baseEnvironment);
	}

	/**
	 * Gets the rate at which the accelerator module improves the atomic reactor timer.
	 *
	 * @return the improvement rate as a double
	 */
	public double getRate() {
		return this.rate;
	}
	
}