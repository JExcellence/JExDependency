package com.raindropcentral.rdq.config.ranks.system;

import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FinalRankRuleSectionTest {

    @Test
    void itProvidesDefaultsForMinimumsAndSpecificTreesWhenUnset() {
        final FinalRankRuleSection section = new FinalRankRuleSection(new EvaluationEnvironmentBuilder());

        assertEquals(4, section.getMinimumRequiredRankTrees(),
            "minimumRequiredRankTrees should default to 4 when the field is null");
        assertEquals(5, section.getMinimumRankTierPerRankTree(),
            "minimumRankTierPerRankTree should default to 5 when the field is null");
        assertIterableEquals(Arrays.asList("warrior", "cleric", "mage", "rogue", "merchant"),
            section.getSpecificRequiredRankTrees(),
            "specificRequiredRankTrees should expose the predefined defaults when the field is null");
        assertTrue(section.getRequireAllRankTrees(),
            "requireAllRankTrees should default to true when the field is null");
        assertTrue(section.getAllowAlternateFinalRank(),
            "allowAlternateFinalRank should default to true when the field is null");
    }

    @Test
    void itHonorsExplicitOverridesForRankTreeRules() throws Exception {
        final FinalRankRuleSection section = new FinalRankRuleSection(new EvaluationEnvironmentBuilder());

        final List<String> injectedTrees = new ArrayList<>(List.of("invention", "alchemy"));

        setField(section, "requireAllRankTrees", Boolean.FALSE);
        setField(section, "minimumRequiredRankTrees", 6);
        setField(section, "minimumRankTierPerRankTree", 7);
        setField(section, "specificRequiredRankTrees", injectedTrees);
        setField(section, "allowAlternateFinalRank", Boolean.FALSE);

        assertFalse(section.getRequireAllRankTrees(),
            "getRequireAllRankTrees should reflect a configured false value");
        assertEquals(6, section.getMinimumRequiredRankTrees(),
            "getMinimumRequiredRankTrees should return the configured override");
        assertEquals(7, section.getMinimumRankTierPerRankTree(),
            "getMinimumRankTierPerRankTree should return the configured override");
        assertSame(injectedTrees, section.getSpecificRequiredRankTrees(),
            "getSpecificRequiredRankTrees should expose the configured list instance");
        assertFalse(section.getAllowAlternateFinalRank(),
            "getAllowAlternateFinalRank should reflect a configured false value");
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
