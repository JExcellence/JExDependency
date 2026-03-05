package com.raindropcentral.rds.configs;

import de.jexcellence.configmapper.sections.AConfigSection;
import de.jexcellence.configmapper.sections.CSAlways;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;

import java.util.Locale;

/**
 * Represents the store currency configuration section.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
@CSAlways
@SuppressWarnings("unused")
public class StoreCurrencySection extends AConfigSection {
	
	private String type;
	
	private Double initial_cost;
	
	private Double growth_rate;
	
	/**
	 * Creates a new store currency section.
	 *
	 * @param baseEnvironment evaluation environment used for config expressions
	 */
	public StoreCurrencySection(EvaluationEnvironmentBuilder baseEnvironment) {
		super(baseEnvironment);
	}
	
	/**
	 * Returns the type.
	 *
	 * @return the type
	 */
	public String getType() {
		return type == null || type.isBlank()
				? "vault"
				: type.trim().toLowerCase(Locale.ROOT);
	}
	
	/**
	 * Returns the initial cost.
	 *
	 * @return the initial cost
	 */
	public double getInitialCost() {
		return initial_cost == null ? 1000.0 : initial_cost;
	}
	
	/**
	 * Returns the growth rate.
	 *
	 * @return the growth rate
	 */
	public double getGrowthRate() {
		return growth_rate == null ? 1.125 : growth_rate;
	}
	
	public void setContext(
		String type,
		double initial_cost,
		double growth_rate
	) {
		this.type = type;
		this.initial_cost = initial_cost;
		this.growth_rate = growth_rate;
	}
}
