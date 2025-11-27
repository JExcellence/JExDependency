package com.raindropcentral.rdq.config.perk;

import de.jexcellence.configmapper.sections.AConfigSection;
import de.jexcellence.configmapper.sections.CSAlways;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;

/**
 * Configuration section that maps a perk reward to a specific currency managed by an external plugin.
 *
 * <p>The section provides safe defaults for optional entries to avoid null propagation in downstream
 * evaluators.</p>
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
@CSAlways
public class PluginCurrencySection extends AConfigSection {

    private String targetPluginId;

    private String currencyTypeId;

    private Double amount;

    /**
     * Creates the currency section with the supplied evaluation environment.
     *
     * @param baseEnvironment the base evaluation environment for this configuration section
     */
    public PluginCurrencySection(
            final EvaluationEnvironmentBuilder baseEnvironment
    ) {
        super(baseEnvironment);
    }

    /**
     * Retrieves the identifier of the external plugin that supplies the currency.
     *
     * @return the plugin identifier or an empty string when none is configured
     */
    public String getTargetPluginId() {
        return this.targetPluginId == null ? "" : this.targetPluginId;
    }

    /**
     * Provides the unique currency type identifier recognized by the external plugin.
     *
     * @return the currency type identifier or an empty string when undefined
     */
    public String getCurrencyTypeId() {
        return this.currencyTypeId == null ? "" : this.currencyTypeId;
    }

    /**
     * Determines the currency amount to be applied when the perk is redeemed.
     *
     * @return the configured amount or {@code 0.0} if the value is not provided
     */
    public Double getAmount() {
        return this.amount == null ? 0.0 : this.amount;
    }

}
