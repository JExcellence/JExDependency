package com.raindropcentral.rdq.config.requirement;

import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PreviousLevelRequirementSectionTest {

    @Test
    void itMergesRankAndTreeAliasesIntoCombinedLists() throws Exception {
        final PreviousLevelRequirementSection section = new PreviousLevelRequirementSection(new EvaluationEnvironmentBuilder());

        setField(section, "requiredPreviousRanks", new ArrayList<>(List.of("rank-alpha")));
        setField(section, "previousRanks", new ArrayList<>(List.of("rank-beta")));
        setField(section, "previousRank", "rank-legacy");

        setField(section, "requiredPreviousRankTrees", new ArrayList<>(List.of("tree-alpha")));
        setField(section, "previousRankTrees", new ArrayList<>(List.of("tree-beta")));
        setField(section, "previousRankTree", "tree-legacy");

        final List<String> mergedRanks = section.getRequiredPreviousRanks();
        assertEquals(3, mergedRanks.size(), "Merged ranks should include canonical, alias, and single entries");
        assertTrue(mergedRanks.contains("rank-alpha"));
        assertTrue(mergedRanks.contains("rank-beta"));
        assertTrue(mergedRanks.contains("rank-legacy"));

        final List<String> mergedTrees = section.getRequiredPreviousRankTrees();
        assertEquals(3, mergedTrees.size(), "Merged trees should include canonical, alias, and single entries");
        assertTrue(mergedTrees.contains("tree-alpha"));
        assertTrue(mergedTrees.contains("tree-beta"));
        assertTrue(mergedTrees.contains("tree-legacy"));
    }

    @Test
    void itDefaultsBooleansAndMinimumTierWithOverrideSupport() throws Exception {
        final PreviousLevelRequirementSection section = new PreviousLevelRequirementSection(new EvaluationEnvironmentBuilder());

        assertTrue(section.getRequireAll(), "requireAll should default to true");
        assertTrue(section.getCheckDirectOnly(), "checkDirectOnly should default to true");
        assertEquals(0, section.getMinimumTier(), "minimumTier should default to 0");

        setField(section, "requireAll", false);
        setField(section, "checkDirectOnly", false);
        setField(section, "minimumTier", 5);

        assertFalse(section.getRequireAll(), "requireAll should reflect configured value");
        assertFalse(section.getCheckDirectOnly(), "checkDirectOnly should reflect configured value");
        assertEquals(5, section.getMinimumTier(), "minimumTier should reflect configured value");
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
