package com.raindropcentral.rds.configs;

import de.jexcellence.configmapper.sections.AConfigSection;
import de.jexcellence.configmapper.sections.CSAlways;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;

import java.util.Locale;

@CSAlways
@SuppressWarnings("unused")
public class StoreCurrencySection extends AConfigSection {
	
	private String type;
	
	private Double initial_cost;
	
	private Double growth_rate;
	
	public StoreCurrencySection(EvaluationEnvironmentBuilder baseEnvironment) {
		super(baseEnvironment);
	}
	
	public String getType() {
		return type == null || type.isBlank()
				? "vault"
				: type.trim().toLowerCase(Locale.ROOT);
	}
	
	public double getInitialCost() {
		return initial_cost == null ? 1000.0 : initial_cost;
	}
	
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
