package com.raindropcentral.rdq.config.perks.sections.forge;

import de.jexcellence.configmapper.sections.AConfigSection;
import de.jexcellence.configmapper.sections.CSAlways;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;

/**
 * Configuration section for the Atomic Forge perk.
 * <p>
 * This section aggregates the configuration for the Atomic Accelerator, Economizer,
 * and Investor modules, each of which provides specific enhancements or cost reductions
 * for the atomic forge system.
 * </p>
 *
 * @author ItsRainingHP
 * @version 1.0.0
 * @since TBD
 */
public class AtomicForgeSection extends AConfigSection {
	
	/**
	 * Configuration section for the Accelerator module, which improves the atomic reactor timer.
	 */
	@CSAlways
	private AtomicAcceleratorSection atomicAcceleratorSection;
	
	/**
	 * Configuration section for the Economizer module, which defines currencies to reduce machine cost.
	 */
	@CSAlways
	private AtomicEconomizerSection atomicEconomizerSection;
	
	/**
	 * Configuration section for the Investor module, which manages passive currency generation.
	 */
	@CSAlways
	private AtomicInvestorSection atomicInvestorSection;
	
	/**
	 * Constructs a new {@code AtomicForgeSection} with the specified evaluation environment.
	 *
	 * @param baseEnvironment the base evaluation environment for this configuration section
	 */
	public AtomicForgeSection(final EvaluationEnvironmentBuilder baseEnvironment) {
		super(baseEnvironment);
	}
	
	public AtomicAcceleratorSection getAtomicAcceleratorSection() {
		return atomicAcceleratorSection;
	}
	
	public AtomicEconomizerSection getAtomicEconomizerSection() {
		return atomicEconomizerSection;
	}
	
	public AtomicInvestorSection getAtomicInvestorSection() {
		return atomicInvestorSection;
	}
	
}