package com.raindropcentral.rdq.config.perk.sections.forge;

import com.raindropcentral.rdq.config.perk.PluginCurrencySection;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class CaptisForgeSectionTest {

    @Test
    void itProvidesDefaultValuesWhenFieldsAreUnset() {
        final CaptisForgeSection forgeSection = new CaptisForgeSection(new EvaluationEnvironmentBuilder());

        assertEquals(0, forgeSection.getCooldown(),
            "getCooldown should return zero when the value has not been configured");
        assertEquals(0.0, forgeSection.getFishing(), 0.0,
            "getFishing should return zero when the value has not been configured");
        assertNull(forgeSection.getCost(),
            "getCost should return null when no currency costs have been configured");
        assertNull(forgeSection.getIncome(),
            "getIncome should return null when no currency incomes have been configured");
    }

    @Test
    void itReturnsConfiguredValuesFromGetters() throws Exception {
        final CaptisForgeSection forgeSection = new CaptisForgeSection(new EvaluationEnvironmentBuilder());
        final Map<String, PluginCurrencySection> cost = new HashMap<>();
        final Map<String, PluginCurrencySection> income = new HashMap<>();

        cost.put("cost-currency", new PluginCurrencySection(new EvaluationEnvironmentBuilder()));
        income.put("income-currency", new PluginCurrencySection(new EvaluationEnvironmentBuilder()));

        setField(forgeSection, "cooldown", 120);
        setField(forgeSection, "fishing", 45.5D);
        setField(forgeSection, "cost", cost);
        setField(forgeSection, "income", income);

        assertEquals(120, forgeSection.getCooldown(),
            "getCooldown should expose the configured cooldown");
        assertEquals(45.5D, forgeSection.getFishing(), 0.0,
            "getFishing should expose the configured fishing time");
        assertSame(cost, forgeSection.getCost(),
            "getCost should expose the configured cost map");
        assertSame(income, forgeSection.getIncome(),
            "getIncome should expose the configured income map");
    }

    private static void setField(final Object target, final String name, final Object value) throws Exception {
        final Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
