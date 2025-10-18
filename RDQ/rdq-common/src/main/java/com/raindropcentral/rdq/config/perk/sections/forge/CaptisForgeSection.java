package com.raindropcentral.rdq.config.perk.sections.forge;


import com.raindropcentral.rdq.config.perk.PluginCurrencySection;
import de.jexcellence.configmapper.sections.AConfigSection;
import de.jexcellence.configmapper.sections.CSAlways;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;

import java.util.Map;

/**
 * Configuration section for the Captis Forge perk.
 * <p>
 * This section defines the parameters for earning income while fishing,
 * including cooldowns, required fishing time, costs, and generated income.
 * </p>
 *
 * @author ItsRainingHP
 * @version 1.0.0
 * @since TBD
 */
public class CaptisForgeSection extends AConfigSection {
	
	/**
	 * Time before earning income again.
	 */
	@CSAlways
	private int cooldown;
	
	/**
	 * Amount of time spent fishing to trigger income.
	 */
	@CSAlways
	private double fishing;
	
	/**
	 * Cost to forge perk.
	 */
	@CSAlways
	private Map<String, PluginCurrencySection> cost;
	
	/**
	 * Income generated while fishing.
	 */
	@CSAlways
	private Map<String, PluginCurrencySection> income;
	
	/**
	 * Constructs a new {@code CaptisForgeSection} with the specified evaluation environment.
	 *
	 * @param baseEnvironment the base evaluation environment for this configuration section
	 */
	public CaptisForgeSection(final EvaluationEnvironmentBuilder baseEnvironment) {
		super(baseEnvironment);
	}
	
	public int getCooldown() {
		return this.cooldown;
	}
	
	public double getFishing() {
		return this.fishing;
	}
	
	public Map<String, PluginCurrencySection> getCost() {
		return this.cost;
	}
	
	public Map<String, PluginCurrencySection> getIncome() {
		return this.income;
	}
	
}