package com.raindropcentral.rdq2.config.perk;

import de.jexcellence.configmapper.sections.AConfigSection;
import de.jexcellence.configmapper.sections.CSAlways;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;

import java.util.HashMap;
import java.util.Map;

@CSAlways
public class PerkSystemSection extends AConfigSection {

    private Map<String, Integer> maxAllowedPerksByPermission = new HashMap<>();

    private Map<String, Integer> maxAllowedPerksByRank = new HashMap<>();

    public PerkSystemSection(EvaluationEnvironmentBuilder baseEnvironment) {
        super(baseEnvironment);
    }

    public Map<String, Integer> getMaxAllowedPerksByPermission() {
        return maxAllowedPerksByPermission != null ? maxAllowedPerksByPermission : new HashMap<>();
    }

    public Map<String, Integer> getMaxAllowedPerksByRank() {
        return maxAllowedPerksByRank != null ? maxAllowedPerksByRank : new HashMap<>();
    }
}
