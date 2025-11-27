package com.raindropcentral.rdq.config.perks;

import de.jexcellence.configmapper.sections.AConfigSection;
import de.jexcellence.configmapper.sections.CSAlways;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;

@CSAlways
public class PluginCurrencySection extends AConfigSection {
	
	private String targetPluginId;
	
	private String currencyTypeId;
	
	private Double amount;
	
	/**
	 * Constructs a new {@code PluginCurrencyRewardSection} with the specified evaluation environment.
	 *
	 * @param baseEnvironment the base evaluation environment for this configuration section
	 */
	public PluginCurrencySection(
		final EvaluationEnvironmentBuilder baseEnvironment
	) {
		super(baseEnvironment);
	}
	
	public String getTargetPluginId() {
		
		return
			this.targetPluginId == null ?
			"" :
			this.targetPluginId;
	}
	
	public String getCurrencyTypeId() {
		
		return
			this.currencyTypeId == null ?
			"" :
			this.currencyTypeId;
	}
	
	public Double getAmount() {
		
		return
			this.amount == null ?
			0.00 :
			this.amount;
	}
	
}