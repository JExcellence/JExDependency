package com.raindropcentral.rdq2.config.perk.sections.forge;

import com.raindropcentral.rdq2.config.perk.PluginCurrencySection;
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
 * @since 1.0.0
 * @version 1.0.1
 */
public class AtomicEconomizerSection extends AConfigSection {

    /**
     * Map of currency identifiers to the {@link PluginCurrencySection} configuration entries that
     * reduce the forge machine cost.
     */
    private Map<String, PluginCurrencySection> currencySections;

    /**
     * Constructs a new {@code AtomicEconomizerSection} with the specified evaluation environment.
     *
     * @param baseEnvironment the base {@link EvaluationEnvironmentBuilder} to evaluate perk
     *                        expressions against
     */
    public AtomicEconomizerSection(final EvaluationEnvironmentBuilder baseEnvironment) {
        super(baseEnvironment);
    }

    /**
     * Gets the map of currency sections used to reduce the machine cost.
     *
     * @return a map keyed by the currency type with values referencing the corresponding
     *         {@link PluginCurrencySection}
     */
    public Map<String, PluginCurrencySection> getCurrencySections() {
        return currencySections;
    }

}