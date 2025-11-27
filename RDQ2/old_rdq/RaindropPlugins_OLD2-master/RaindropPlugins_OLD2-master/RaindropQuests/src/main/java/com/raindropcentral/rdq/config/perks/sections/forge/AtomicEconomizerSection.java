package com.raindropcentral.rdq.config.perks.sections.forge;

import com.raindropcentral.rdq.config.perks.PluginCurrencySection;
import de.jexcellence.configmapper.sections.AConfigSection;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;

import java.util.Map;

/**
 * Configuration section for the Atomic Economizer perk.
 * <p>
 * This section defines the currencies that can be used to reduce the cost of the machine.
 * </p>
 *
 * @author ItsRainingHP
 * @version 1.0.0
 * @since TBD
 */
public class AtomicEconomizerSection extends AConfigSection {
	
	/**
	 * List of currencies to reduce cost of the machine.
	 * The key is the currency type, and the value is the corresponding currency section.
	 */
	private Map<String, PluginCurrencySection> currencySections;
	
	/**
	 * Constructs a new {@code AtomicEconomizerSection} with the specified evaluation environment.
	 *
	 * @param baseEnvironment the base evaluation environment for this configuration section
	 */
	public AtomicEconomizerSection(final EvaluationEnvironmentBuilder baseEnvironment) {
		super(baseEnvironment);
	}

	/**
	 * Gets the map of currency sections used to reduce the machine cost.
	 *
	 * @return a map where the key is the currency type and the value is the corresponding {@link com.raindropcentral.rdq.config.perks.PluginCurrencySection}
	 */
	public Map<String, PluginCurrencySection> getCurrencySections() {
		return currencySections;
	}
	
}