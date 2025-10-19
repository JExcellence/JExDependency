package com.raindropcentral.rdq.config.perk.sections.forge;

import com.raindropcentral.rdq.config.perk.PluginCurrencySection;
import de.jexcellence.configmapper.sections.AConfigSection;
import de.jexcellence.configmapper.sections.CSAlways;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;

import java.util.Map;

/**
 * Configuration section for the Atomic Investor perk.
 * <p>
 * This section defines the parameters for passive currency generation by the investor module,
 * including the generation interval and the types of currencies generated.
 * </p>
 *
 * @author ItsRainingHP
 * @version 1.0.1
 * @since 1.0.0
 */
public class AtomicInvestorSection extends AConfigSection {

    /**
     * Time interval (in ticks or seconds, depending on system) between each passive currency
     * generation.
     */
    @CSAlways
    private int timer;

    /**
     * Map of currency types to their configuration sections, representing the currencies the
     * machine passively generates with the investor module.
     */
    private Map<String, PluginCurrencySection> currencySections;

    /**
     * Constructs a new {@code AtomicInvestorSection} with the specified evaluation environment.
     *
     * @param baseEnvironment the base evaluation environment for this configuration section
     */
    public AtomicInvestorSection(final EvaluationEnvironmentBuilder baseEnvironment) {
        super(baseEnvironment);
    }

    /**
     * Retrieves the configured interval between passive generation cycles.
     *
     * @return the configured generation interval
     */
    public int getTimer() {
        return timer;
    }

    /**
     * Provides the configured currency sections keyed by the currency identifier.
     *
     * @return a map of currency identifiers to their {@link PluginCurrencySection} definitions
     */
    public Map<String, PluginCurrencySection> getCurrencySections() {
        return currencySections;
    }

}
