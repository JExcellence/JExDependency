package com.raindropcentral.rds.configs;

import de.jexcellence.configmapper.sections.AConfigSection;
import de.jexcellence.configmapper.sections.CSAlways;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;

@CSAlways
@SuppressWarnings("unused")
public class ConfigSection extends AConfigSection {

    private double initial_cost;

    private double growth_rate;

    public ConfigSection(EvaluationEnvironmentBuilder baseEnvironment) {
        super(baseEnvironment);
    }

    public double getInitialCost() { return this.initial_cost; }

    public double getGrowthRate() { return this.growth_rate; }
}
