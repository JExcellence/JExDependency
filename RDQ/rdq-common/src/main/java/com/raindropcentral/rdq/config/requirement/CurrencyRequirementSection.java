package com.raindropcentral.rdq.config.requirement;

import de.jexcellence.configmapper.sections.AConfigSection;
import de.jexcellence.configmapper.sections.CSAlways;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration section for currency-based requirements.
 *
 * <p>This section handles all configuration options specific to CurrencyRequirement,
 * including required currencies, amounts, and consumption settings.
 *
 * @author JExcellence
 * @version 1.0.0
 * @since TBD
 */
@CSAlways
public class CurrencyRequirementSection extends AConfigSection {
	
	// ~~~ CURRENCY-SPECIFIC PROPERTIES ~~~
	
	/**
	 * Whether this requirement should consume currency when completed.
	 * YAML key: "consumeOnComplete"
	 */
	private Boolean consumeOnComplete;
	
	/**
	 * Map of required currencies with their amounts.
	 * YAML key: "requiredCurrencies"
	 */
	private Map<String, Double> requiredCurrencies;
	
	/**
	 * Single currency identifier for simple requirements.
	 * YAML key: "currencyType"
	 */
	private String currencyType;
	
	/**
	 * Single currency amount for simple requirements.
	 * YAML key: "currencyAmount"
	 */
	private Double currencyAmount;
	
	/**
	 * Alternative currency field name.
	 * YAML key: "currency"
	 */
	private String currency;
	
	/**
	 * Alternative amount field name.
	 * YAML key: "amount"
	 */
	private Double amount;
	
	/**
	 * Currency plugin identifier (e.g., "vault", "playerpoints").
	 * YAML key: "currencyPlugin"
	 */
	private String currencyPlugin;
	
	/**
	 * Constructs a new CurrencyRequirementSection.
	 *
	 * @param evaluationEnvironmentBuilder the evaluation environment builder
	 */
	public CurrencyRequirementSection(EvaluationEnvironmentBuilder evaluationEnvironmentBuilder) {
		super(evaluationEnvironmentBuilder);
	}
	
	/**
	 * Gets consumeOnComplete.
	 */
	public Boolean getConsumeOnComplete() {
		return this.consumeOnComplete != null ? this.consumeOnComplete : true;
	}
	
	/**
	 * Gets currencyPlugin.
	 */
	public String getCurrencyPlugin() {
		return this.currencyPlugin != null ? this.currencyPlugin : "vault";
	}
	
	/**
	 * Gets the complete map of required currencies from all sources.
	 * For the new API, this will typically contain a single currency.
	 *
	 * @return combined map of all required currencies
	 */
	public Map<String, Double> getRequiredCurrencies() {
		Map<String, Double> currencies = new HashMap<>();
		
		// Add currencies from requiredCurrencies map (old format)
		if (this.requiredCurrencies != null) {
			currencies.putAll(this.requiredCurrencies);
		}
		
		// Add single currency if specified (new format)
		String currencyId = getCurrencyType();
		Double currencyAmount = getCurrencyAmount();
		
		if (currencyId != null && !currencyId.isEmpty() && currencyAmount != null && currencyAmount > 0) {
			currencies.put(currencyId, currencyAmount);
		}
		
		return currencies;
	}
	
	/**
	 * Gets the currency type, trying multiple field names.
	 * Supports both 'currency' and 'currencyType' field names.
	 *
	 * @return the currency type, or null if not specified
	 */
	public String getCurrencyType() {
		if (this.currency != null) {
			return this.currency;
		}
		if (this.currencyType != null) {
			return this.currencyType;
		}
		return null;
	}
	
	/**
	 * Gets the currency amount, trying multiple field names.
	 * Supports both 'amount' and 'currencyAmount' field names.
	 *
	 * @return the currency amount, or null if not specified
	 */
	public Double getCurrencyAmount() {
		if (this.amount != null) {
			return this.amount;
		}
		if (this.currencyAmount != null) {
			return this.currencyAmount;
		}
		return null;
	}
}
