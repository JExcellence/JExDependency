package com.raindropcentral.core.database.entity.statistic;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RBooleanStatisticTest {

    @Nested
    @DisplayName("constructor guards")
    class ConstructorGuards {

        @Test
        @DisplayName("rejects null identifier")
        void rejectsNullIdentifier() {
            assertThrows(NullPointerException.class, () -> new RBooleanStatistic(null, "plugin", true));
        }

        @Test
        @DisplayName("rejects null plugin")
        void rejectsNullPlugin() {
            assertThrows(NullPointerException.class, () -> new RBooleanStatistic("identifier", null, true));
        }

        @Test
        @DisplayName("rejects null value")
        void rejectsNullValue() {
            assertThrows(NullPointerException.class, () -> new RBooleanStatistic("identifier", "plugin", null));
        }
    }

    @Test
    @DisplayName("toggle flips the stored state and returns to original after even toggles")
    void toggleFlipsStatePredictably() {
        RBooleanStatistic statistic = new RBooleanStatistic("identifier", "plugin", true);

        assertTrue(statistic.getValue(), "initial value should be true");

        statistic.toggle();
        assertFalse(statistic.getValue(), "first toggle should flip value to false");

        statistic.toggle();
        assertTrue(statistic.getValue(), "second toggle should return value to true");
    }

    @Test
    @DisplayName("setValue updates payload and rejects null assignments")
    void setValueUpdatesAndRejectsNull() {
        RBooleanStatistic statistic = new RBooleanStatistic("identifier", "plugin", false);

        statistic.setValue(true);
        assertTrue(statistic.getValue(), "setValue should update persisted state");

        assertThrows(NullPointerException.class, () -> statistic.setValue(null));
    }

    @Test
    @DisplayName("toString includes identifier, plugin, and boolean state")
    void toStringIncludesMetadata() {
        RBooleanStatistic statistic = new RBooleanStatistic("identifier", "plugin", false);

        String representation = statistic.toString();

        assertTrue(representation.startsWith("RBooleanStatistic["));
        assertTrue(representation.contains("identifier=identifier"));
        assertTrue(representation.contains("plugin=plugin"));
        assertTrue(representation.contains("value=false"));
    }
}
