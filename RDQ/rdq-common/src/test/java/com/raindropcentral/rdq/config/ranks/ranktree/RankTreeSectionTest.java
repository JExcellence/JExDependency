package com.raindropcentral.rdq.config.ranks.ranktree;

import com.raindropcentral.rdq.config.item.IconSection;
import com.raindropcentral.rdq.config.ranks.rank.RankSection;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RankTreeSectionTest {

    @Test
    void itGeneratesTreeLocalizationKeysAndPropagatesIconMetadata() throws Exception {
        final RankTreeSection section = new RankTreeSection(new EvaluationEnvironmentBuilder());

        section.setTreeId("ascension");

        final IconSection icon = new IconSection(new EvaluationEnvironmentBuilder());
        setField(section, "icon", icon);

        section.afterParsing(Collections.emptyList());

        assertEquals("tree.ascension.name", section.getDisplayNameKey(),
            "afterParsing should derive the display name key using the tree identifier");
        assertEquals("tree.ascension.lore", section.getDescriptionKey(),
            "afterParsing should derive the description key using the tree identifier");

        assertSame(icon, section.getIcon(),
            "getIcon should expose the configured icon when one is provided");
        assertEquals("tree.ascension.name", icon.getDisplayNameKey(),
            "afterParsing should propagate the generated display key to the icon");
        assertEquals("tree.ascension.lore", icon.getDescriptionKey(),
            "afterParsing should propagate the generated description key to the icon");
    }

    @Test
    void itProcessesChildRankSectionsAndSuppliesDefaultCollections() throws Exception {
        final RankTreeSection section = new RankTreeSection(new EvaluationEnvironmentBuilder());

        final List<String> prerequisites = section.getPrerequisiteRankTrees();
        final List<String> unlocked = section.getUnlockedRankTrees();
        final List<String> connected = section.getConnectedRankTrees();
        final List<Integer> switchable = section.getSwitchableRankTiers();
        final Map<String, RankSection> defaultRanks = section.getRanks();

        assertTrue(prerequisites.isEmpty(), "Prerequisite trees should default to an empty list");
        assertTrue(unlocked.isEmpty(), "Unlocked trees should default to an empty list");
        assertTrue(connected.isEmpty(), "Connected trees should default to an empty list");
        assertTrue(switchable.isEmpty(), "Switchable tiers should default to an empty list");
        assertTrue(defaultRanks.isEmpty(), "Rank mappings should default to an empty map");

        assertNotSame(prerequisites, section.getPrerequisiteRankTrees(),
            "getPrerequisiteRankTrees should return a defensive copy when the backing field is null");
        assertNotSame(unlocked, section.getUnlockedRankTrees(),
            "getUnlockedRankTrees should return a defensive copy when the backing field is null");
        assertNotSame(connected, section.getConnectedRankTrees(),
            "getConnectedRankTrees should return a defensive copy when the backing field is null");
        assertNotSame(switchable, section.getSwitchableRankTiers(),
            "getSwitchableRankTiers should return a defensive copy when the backing field is null");
        assertNotSame(defaultRanks, section.getRanks(),
            "getRanks should return a defensive copy when the backing field is null");

        section.setTreeId("ascension");

        final RankSection rank = Mockito.mock(RankSection.class);
        final Map<String, RankSection> ranks = new LinkedHashMap<>();
        ranks.put("novice", rank);
        setField(section, "ranks", ranks);

        section.afterParsing(Collections.emptyList());

        Mockito.verify(rank).setRankTreeName("ascension");
        Mockito.verify(rank).setRankName("novice");
        Mockito.verify(rank).afterParsing(Mockito.anyList());

        assertSame(ranks, section.getRanks(),
            "getRanks should expose the configured rank map when one is present");
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
