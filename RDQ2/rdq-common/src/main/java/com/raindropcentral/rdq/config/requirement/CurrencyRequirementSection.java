package com.raindropcentral.rdq.config.requirement;

import de.jexcellence.configmapper.sections.AConfigSection;
import de.jexcellence.configmapper.sections.CSAlways;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration section for currency-based requirements.
 * <p>
 * This section exposes the configurable knobs that determine how a currency
 * requirement behaves, including whether funds are consumed, the supported
 * field aliases for currency identifiers and amounts, and the plugin that
 * should resolve balances. It also merges legacy single-value fields with the
 * new multi-currency map to provide a unified view to callers.
 * </p>
 *
 * @author JExcellence
 * @version 1.0.1
 * @since 1.0.0
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
     */    /**
     * Default no-arg constructor for Jackson deserialization.
     */
    protected CurrencyRequirementSection() {
        super(new EvaluationEnvironmentBuilder());
    }

    public CurrencyRequirementSection(EvaluationEnvironmentBuilder evaluationEnvironmentBuilder) {
        super(evaluationEnvironmentBuilder);
    }

    /**
     * Indicates whether the configured requirement should remove the specified
     * currency when it is satisfied.
     *
     * @return {@code true} when currency is consumed on completion, otherwise
     *         {@code false}
     */
    public Boolean getConsumeOnComplete() {
        return this.consumeOnComplete != null ? this.consumeOnComplete : true;
    }

    /**
     * Resolves the plugin identifier that should service currency lookups.
     *
     * @return the configured currency plugin or {@code "vault"} when unspecified
     */
    public String getCurrencyPlugin() {
        return this.currencyPlugin != null ? this.currencyPlugin : "vault";
    }

    /**
     * Gets the complete map of required currencies from all declared sources.
     *
     * @return a new mutable map combining multi-currency entries and any legacy
     *         single-value fields
     */
    public Map<String, Double> getRequiredCurrencies() {
        Map<String, Double> currencies = new HashMap<>();

        // Add currencies from requiredCurrencies map
        if (this.requiredCurrencies != null) {
            currencies.putAll(this.requiredCurrencies);
        }

        // Add single currency if specified
        String currencyId = getCurrencyType();
        Double currencyAmount = getCurrencyAmount();

        if (currencyId != null && !currencyId.isEmpty() && currencyAmount != null && currencyAmount > 0) {
            currencies.put(currencyId, currencyAmount);
        }

        return currencies;
    }

    /**
     * Gets the currency type while supporting legacy field aliases.
     *
     * @return the configured currency identifier or {@code "money"} when no
     *         value is provided
     */
    public String getCurrencyType() {
        if (this.currencyType != null) {
            return this.currencyType;
        }
        if (this.currency != null) {
            return this.currency;
        }
        return "money"; // Default currency type
    }

    /**
     * Gets the currency amount while supporting legacy field aliases.
     *
     * @return the configured amount or {@code 0.0} when no positive value is
     *         provided
     */
    public Double getCurrencyAmount() {
        if (this.currencyAmount != null) {
            return this.currencyAmount;
        }
        if (this.amount != null) {
            return this.amount;
        }
        return 0.0;
    }
}
