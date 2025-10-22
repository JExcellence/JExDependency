package com.raindropcentral.rdq.config.ranks.rank;

import com.raindropcentral.rdq.config.item.IconSection;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RankSectionTest {

    @Test
    void itDerivesLocalizationKeysAndProvidesIconFallbacksAfterParsing() throws Exception {
        final RankSection section = new RankSection(new EvaluationEnvironmentBuilder());

        section.setRankTreeName("ascension");
        section.setRankName("novice");

        section.afterParsing(Collections.emptyList());

        assertEquals("rank.ascension.novice.name", section.getDisplayNameKey(),
            "afterParsing should derive the display name key using the rank tree and rank name");
        assertEquals("rank.ascension.novice.lore", section.getDescriptionKey(),
            "afterParsing should derive the description key using the rank tree and rank name");
        assertEquals("rank.ascension.novice.prefix", section.getPrefixKey(),
            "afterParsing should derive the prefix key using the rank tree and rank name");
        assertEquals("rank.ascension.novice.suffix", section.getSuffixKey(),
            "afterParsing should derive the suffix key using the rank tree and rank name");

        final IconSection firstFallbackIcon = section.getIcon();
        final IconSection secondFallbackIcon = section.getIcon();

        assertNotNull(firstFallbackIcon, "getIcon should return a fallback icon when none is configured");
        assertNotSame(firstFallbackIcon, secondFallbackIcon,
            "getIcon should create a new fallback instance whenever no icon section is configured");
    }

    @Test
    void itHonorsCollectionAndRankStateDefaultsWhileRespectingInjectedValues() throws Exception {
        final RankSection section = new RankSection(new EvaluationEnvironmentBuilder());

        final IconSection defaultIcon = section.getIcon();
        final IconSection nextDefaultIcon = section.getIcon();

        assertNotNull(defaultIcon, "The default icon should not be null");
        assertNotSame(defaultIcon, nextDefaultIcon,
            "Each default icon request should produce a new instance to prevent shared state");

        final List<String> defaultPrevious = section.getPreviousRanks();
        final List<String> anotherDefaultPrevious = section.getPreviousRanks();

        assertTrue(defaultPrevious.isEmpty(), "Previous ranks should default to an empty list");
        assertNotSame(defaultPrevious, anotherDefaultPrevious,
            "getPreviousRanks should return a new list when the backing field is null");

        assertFalse(section.getInitialRank(), "Ranks should not be initial by default");
        assertFalse(section.getFinalRank(), "Ranks should not be final by default");

        final IconSection injectedIcon = new IconSection(new EvaluationEnvironmentBuilder());
        final List<String> injectedPrevious = new ArrayList<>(Arrays.asList("start", "middle"));

        setField(section, "icon", injectedIcon);
        setField(section, "previousRanks", injectedPrevious);
        setField(section, "isInitialRank", Boolean.TRUE);
        setField(section, "isFinalRank", Boolean.FALSE);

        assertSame(injectedIcon, section.getIcon(),
            "getIcon should expose the configured icon section when provided");
        assertSame(injectedPrevious, section.getPreviousRanks(),
            "getPreviousRanks should expose the configured list when provided");
        assertTrue(section.getInitialRank(), "getInitialRank should respect a configured true value when not final");

        setField(section, "isFinalRank", Boolean.TRUE);

        assertTrue(section.getFinalRank(), "getFinalRank should reflect the configured final rank state");
        assertFalse(section.getInitialRank(),
            "getInitialRank should return false when the rank is marked as final even if initially configured true");
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
