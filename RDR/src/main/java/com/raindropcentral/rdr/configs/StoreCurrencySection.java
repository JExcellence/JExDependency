/*
 * StoreCurrencySection.java
 *
 * @author RaindropCentral
 * @version 5.0.0
 */

package com.raindropcentral.rdr.configs;

import de.jexcellence.configmapper.sections.AConfigSection;
import de.jexcellence.configmapper.sections.CSAlways;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

/**
 * Per-currency pricing configuration for storage purchases.
 *
 * <p>Each configured currency entry contributes one required charge when a player purchases an additional
 * storage. The current charge is derived from the initial cost and growth rate using the player's current
 * storage count.</p>
 *
 * @author RaindropCentral
 * @since 5.0.0
 * @version 5.0.0
 */
@CSAlways
@SuppressWarnings("unused")
public class StoreCurrencySection extends AConfigSection {

    private String type;
    private Double initial_cost;
    private Double growth_rate;

    /**
     * Creates a currency pricing section bound to the provided evaluation environment.
     *
     * @param baseEnvironment base environment used by the config mapper
     * @throws NullPointerException if {@code baseEnvironment} is {@code null}
     */
    public StoreCurrencySection(final @NotNull EvaluationEnvironmentBuilder baseEnvironment) {
        super(baseEnvironment);
    }

    /**
     * Returns the normalized currency identifier for this pricing entry.
     *
     * @return normalized currency identifier, defaulting to {@code "vault"}
     */
    public @NotNull String getType() {
        if (this.type == null || this.type.isBlank()) {
            return "vault";
        }

        return this.type.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * Returns the base cost before growth is applied.
     *
     * @return non-negative initial cost, defaulting to {@code 1000.0}
     */
    public double getInitialCost() {
        return this.initial_cost == null ? 1000.0D : Math.max(0D, this.initial_cost);
    }

    /**
     * Returns the multiplicative growth rate applied per owned storage.
     *
     * @return non-negative growth rate, defaulting to {@code 1.125}
     */
    public double getGrowthRate() {
        return this.growth_rate == null ? 1.125D : Math.max(0D, this.growth_rate);
    }

    /**
     * Updates this section with normalized runtime context.
     *
     * @param type normalized currency identifier
     * @param initialCost base cost before growth is applied
     * @param growthRate multiplicative growth rate applied per owned storage
     */
    public void setContext(
        final @NotNull String type,
        final double initialCost,
        final double growthRate
    ) {
        this.type = type;
        this.initial_cost = initialCost;
        this.growth_rate = growthRate;
    }
}
