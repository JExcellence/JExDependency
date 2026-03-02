package com.raindropcentral.rds.configs;

import de.jexcellence.configmapper.sections.AConfigSection;
import de.jexcellence.configmapper.sections.CSAlways;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

@CSAlways
@SuppressWarnings("unused")
public class TaxCurrencySection extends AConfigSection {

    private String type;
    private Double initial_cost;
    private Double growth_rate;
    private Double maximum_tax;

    public TaxCurrencySection(
            final EvaluationEnvironmentBuilder baseEnvironment
    ) {
        super(baseEnvironment);
    }

    public @NotNull String getType() {
        return this.type == null || this.type.isBlank()
                ? "vault"
                : this.type.trim().toLowerCase(Locale.ROOT);
    }

    public double getInitialCost() {
        return this.initial_cost == null ? 100.0D : Math.max(0D, this.initial_cost);
    }

    public double getGrowthRate() {
        return this.growth_rate == null ? 1.125D : Math.max(0D, this.growth_rate);
    }

    public double getMaximumTax() {
        return this.maximum_tax == null ? -1D : this.maximum_tax;
    }

    public boolean hasTaxCap() {
        return this.getMaximumTax() >= 0D;
    }

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
