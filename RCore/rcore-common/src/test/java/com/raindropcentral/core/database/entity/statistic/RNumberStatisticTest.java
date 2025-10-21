package com.raindropcentral.core.database.entity.statistic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RNumberStatisticTest {

    private static final String IDENTIFIER = "kills";
    private static final String PLUGIN = "rcore";

    @Test
    void constructorRejectsNullIdentifier() {
        assertThrows(NullPointerException.class, () -> new RNumberStatistic(null, PLUGIN, 1.0));
    }

    @Test
    void constructorRejectsNullPlugin() {
        assertThrows(NullPointerException.class, () -> new RNumberStatistic(IDENTIFIER, null, 1.0));
    }

    @Test
    void constructorRejectsNullValue() {
        assertThrows(NullPointerException.class, () -> new RNumberStatistic(IDENTIFIER, PLUGIN, null));
    }

    @Test
    void setValueReplacesStoredDoubleAndAllowsNull() {
        final RNumberStatistic statistic = new RNumberStatistic(IDENTIFIER, PLUGIN, 3.0);

        statistic.setValue(12.5);
        assertEquals(12.5, statistic.getValue());
        assertEquals(IDENTIFIER, statistic.getIdentifier());
        assertEquals(PLUGIN, statistic.getPlugin());

        statistic.setValue(null);
        assertNull(statistic.getValue());
    }

    @Test
    void incrementDecrementAndMultiplyAdjustValue() {
        final RNumberStatistic statistic = new RNumberStatistic(IDENTIFIER, PLUGIN, 10.0);

        statistic.increment(2.5);
        assertEquals(12.5, statistic.getValue());

        statistic.increment(-5.0);
        assertEquals(7.5, statistic.getValue());

        statistic.decrement(2.0);
        assertEquals(5.5, statistic.getValue());

        statistic.decrement(-1.5);
        assertEquals(7.0, statistic.getValue());

        statistic.multiply(1.5);
        assertEquals(10.5, statistic.getValue());
    }

    @Test
    void decrementClampsValueAtZero() {
        final RNumberStatistic statistic = new RNumberStatistic(IDENTIFIER, PLUGIN, 3.25);

        statistic.decrement(10.0);

        assertEquals(0.0, statistic.getValue());
    }

    @Test
    void positivityAndZeroDetectionRespectThresholds() {
        final RNumberStatistic statistic = new RNumberStatistic(IDENTIFIER, PLUGIN, 0.0);

        assertFalse(statistic.isPositive());
        assertTrue(statistic.isZero());

        statistic.setValue(0.00005);
        assertTrue(statistic.isZero());
        assertTrue(statistic.isPositive());

        statistic.setValue(0.00011);
        assertFalse(statistic.isZero());
        assertTrue(statistic.isPositive());

        statistic.setValue(-0.00005);
        assertTrue(statistic.isZero());
        assertFalse(statistic.isPositive());
    }

    @Test
    void toStringIncludesRoundedValueAndMetadata() {
        final RNumberStatistic statistic = new RNumberStatistic(IDENTIFIER, PLUGIN, 12.3456);

        final String representation = statistic.toString();

        assertTrue(representation.contains("id="));
        assertTrue(representation.contains("identifier=" + IDENTIFIER));
        assertTrue(representation.contains("plugin=" + PLUGIN));
        assertTrue(representation.contains("value=12.35"));
    }

    @Test
    void mutationsDoNotAlterIdentifierOrPlugin() {
        final RNumberStatistic statistic = new RNumberStatistic(IDENTIFIER, PLUGIN, 4.0);

        statistic.increment(1.0);
        statistic.decrement(0.5);
        statistic.multiply(2.0);
        statistic.setValue(8.0);

        assertEquals(IDENTIFIER, statistic.getIdentifier());
        assertEquals(PLUGIN, statistic.getPlugin());
        assertEquals(8.0, statistic.getValue());
    }

}
