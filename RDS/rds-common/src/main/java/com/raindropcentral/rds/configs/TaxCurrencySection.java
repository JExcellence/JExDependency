package com.raindropcentral.rds.configs;

import de.jexcellence.configmapper.sections.AConfigSection;
import de.jexcellence.configmapper.sections.CSAlways;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

/**
 * Represents the tax currency configuration section.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
@CSAlways
@SuppressWarnings("unused")
public class TaxCurrencySection extends AConfigSection {

    private String type;
    private Double initial_cost;
    private Double growth_rate;
    private Double maximum_tax;

    /**
     * Creates a new tax currency section.
     *
     * @param baseEnvironment evaluation environment used for config expressions
     */
    public TaxCurrencySection(
            final EvaluationEnvironmentBuilder baseEnvironment
    ) {
        super(baseEnvironment);
    }

    /**
     * Returns the type.
     *
     * @return the type
     */
    public @NotNull String getType() {
        return this.type == null || this.type.isBlank()
                ? "vault"
                : this.type.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * Returns the initial cost.
     *
     * @return the initial cost
     */
    public double getInitialCost() {
        return this.initial_cost == null ? 100.0D : Math.max(0D, this.initial_cost);
    }

    /**
     * Returns the growth rate.
     *
     * @return the growth rate
     */
    public double getGrowthRate() {
        return this.growth_rate == null ? 1.125D : Math.max(0D, this.growth_rate);
    }

    /**
     * Returns the maximum tax.
     *
     * @return the maximum tax
     */
    public double getMaximumTax() {
        return this.maximum_tax == null ? -1D : this.maximum_tax;
    }

    /**
     * Indicates whether tax cap is available.
     *
     * @return {@code true} if tax cap; otherwise {@code false}
     */
    public boolean hasTaxCap() {
        return this.getMaximumTax() >= 0D;
    }

    /**
     * Sets context.
     */
    public void setContext(
            final @NotNull String type,
            final double initialCost,
            final double growthRate,
            final double maximumTax
    ) {
        this.type = type;
        this.initial_cost = initialCost;
        this.growth_rate = growthRate;
        this.maximum_tax = maximumTax;
    }
}
