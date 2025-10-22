package com.raindropcentral.rdq.config.ranks.system;

import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CrossTreeProgressionSectionTest {

    @Test
    void itAppliesDocumentedDefaultsWhenUnset() {
        final CrossTreeProgressionSection section = new CrossTreeProgressionSection(new EvaluationEnvironmentBuilder());

        assertFalse(section.getAllowSwitching(), "allowSwitching should default to false");
        assertTrue(section.getRequireSameRankTier(), "requireSameRankTier should default to true");
        assertEquals(172800L, section.getSwitchingCooldown(), "switching cooldown should default to two days");
    }

    @Test
    void itReflectsConfiguredValuesFromConfigMapping() throws Exception {
        final CrossTreeProgressionSection section = new CrossTreeProgressionSection(new EvaluationEnvironmentBuilder());
        final List<Integer> configuredTiers = new ArrayList<>();
        configuredTiers.add(3);
        configuredTiers.add(7);

        setField(section, "allowSwitching", Boolean.TRUE);
        setField(section, "switchRankTiers", configuredTiers);
        setField(section, "requireSameRankTier", Boolean.FALSE);
        setField(section, "allowDowngrade", Boolean.FALSE);
        setField(section, "switchingCosts", "points * 2");
        setField(section, "switchingCooldown", 3600L);

        assertTrue(section.getAllowSwitching(), "allowSwitching should respect configured true value");
        final List<Integer> tiers = section.getSwitchRankTiers();
        assertSame(configuredTiers, tiers, "switchRankTiers should expose the configured mutable list instance");
        tiers.add(11);
        assertEquals(3, configuredTiers.size(), "switchRankTiers should remain mutable after retrieval");
        assertFalse(section.getRequireSameRankTier(), "requireSameRankTier should respect configured false value");
        assertFalse(section.getAllowDowngrade(), "allowDowngrade should respect configured false value");
        assertEquals("points * 2", section.getSwitchingCosts(), "switchingCosts should respect configured value");
        assertEquals(3600L, section.getSwitchingCooldown(), "switchingCooldown should respect configured value");
    }

    private static void setField(final Object target, final String name, final Object value) throws Exception {
        final Field field = locateField(target.getClass(), name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static Field locateField(final Class<?> type, final String name) throws NoSuchFieldException {
        Class<?> current = type;
        while (current != null) {
            try {
                return current.getDeclaredField(name);
            } catch (final NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
    }
}
