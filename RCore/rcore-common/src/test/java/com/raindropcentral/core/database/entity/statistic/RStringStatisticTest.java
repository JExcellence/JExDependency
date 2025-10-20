package com.raindropcentral.core.database.entity.statistic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RStringStatisticTest {

    private static final String IDENTIFIER = "display_name";
    private static final String PLUGIN = "rcore";

    @Test
    void constructorRejectsNullIdentifier() {
        assertThrows(NullPointerException.class, () -> new RStringStatistic(null, PLUGIN, "value"));
    }

    @Test
    void constructorRejectsNullPlugin() {
        assertThrows(NullPointerException.class, () -> new RStringStatistic(IDENTIFIER, null, "value"));
    }

    @Test
    void constructorRejectsNullValue() {
        final NullPointerException exception = assertThrows(
            NullPointerException.class,
            () -> new RStringStatistic(IDENTIFIER, PLUGIN, null)
        );

        assertEquals("value cannot be null", exception.getMessage());
    }

    @Test
    void setValueRejectsNullAssignments() {
        final RStringStatistic statistic = new RStringStatistic(IDENTIFIER, PLUGIN, "initial");

        final NullPointerException exception = assertThrows(
            NullPointerException.class,
            () -> statistic.setValue(null)
        );

        assertEquals("value cannot be null", exception.getMessage());
    }

    @Test
    void getterReturnsStoredStringAndMetadataRemainStable() {
        final RStringStatistic statistic = new RStringStatistic(IDENTIFIER, PLUGIN, "initial");

        assertEquals("initial", statistic.getValue());
        assertEquals(IDENTIFIER, statistic.getIdentifier());
        assertEquals(PLUGIN, statistic.getPlugin());

        statistic.setValue("updated");

        assertEquals("updated", statistic.getValue());
        assertEquals(IDENTIFIER, statistic.getIdentifier());
        assertEquals(PLUGIN, statistic.getPlugin());
    }

    @Test
    void isEmptyAndLengthReflectUnderlyingValue() {
        final RStringStatistic statistic = new RStringStatistic(IDENTIFIER, PLUGIN, "");

        assertTrue(statistic.isEmpty());
        assertEquals(0, statistic.length());

        statistic.setValue("abc");

        assertFalse(statistic.isEmpty());
        assertEquals(3, statistic.length());
    }

    @Test
    void toStringTruncatesValuesLongerThanFiftyCharacters() {
        final String longValue = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do";
        final RStringStatistic statistic = new RStringStatistic(IDENTIFIER, PLUGIN, longValue);

        final String representation = statistic.toString();
        final String expectedTruncated = longValue.substring(0, 47) + "...";

        assertTrue(representation.contains("value=" + expectedTruncated));
        assertFalse(representation.contains(longValue));
    }

    @Test
    void toStringLeavesShortValuesIntact() {
        final String shortValue = "Player Title";
        final RStringStatistic statistic = new RStringStatistic(IDENTIFIER, PLUGIN, shortValue);

        final String representation = statistic.toString();

        assertTrue(representation.contains("identifier=" + IDENTIFIER));
        assertTrue(representation.contains("plugin=" + PLUGIN));
        assertTrue(representation.contains("value=" + shortValue));
    }
}
