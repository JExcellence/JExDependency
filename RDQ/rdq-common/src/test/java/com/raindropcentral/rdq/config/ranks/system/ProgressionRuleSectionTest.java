package com.raindropcentral.rdq.config.ranks.system;

import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProgressionRuleSectionTest {

    private ProgressionRuleSection section;

    @BeforeEach
    void setUp() {
        this.section = new ProgressionRuleSection(new EvaluationEnvironmentBuilder());
    }

    @Test
    @DisplayName("Defaults are returned when configuration values are null")
    void testDefaultValuesWhenNull() {
        assertTrue(section.getRequireLinearRankProgression(), "linear progression should default to true");
        assertFalse(section.getAllowSkippingRanks(), "skipping ranks should default to false");
        assertTrue(section.getEnableMultipleActiveRankTrees(), "multiple trees should default to true");
        assertEquals(2, section.getMaximumActiveRankTrees(), "maximum trees should default to 2");
        assertTrue(section.getRequireConfirmationForRankUp(), "confirmation should default to true");
        assertTrue(section.getEnableCrossRankTreeSwitching(), "cross tree switching should default to true");
        assertEquals(List.of(3, 5, 7, 10), section.getGlobalSwitchingTiers(), "switching tiers should default to [3,5,7,10]");
        assertEquals(1_728_000L, section.getSwitchingCooldown(), "switching cooldown should default to 1728000L");
        assertEquals("", section.getSwitchingCost(), "switching cost should default to empty string");
    }

    @Nested
    class CustomValues {

        @Test
        @DisplayName("Reflection-populated fields are surfaced by the getters")
        void testCustomValuesViaReflection() throws Exception {
            setField("requireLinearRankProgression", false);
            setField("allowSkippingRanks", true);
            setField("enableMultipleActiveRankTrees", false);
            setField("maximumActiveRankTrees", 5);
            setField("requireConfirmationForRankUp", false);
            setField("enableCrossRankTreeSwitching", false);

            List<Integer> tiers = List.of(1, 2, 3);
            setField("globalSwitchingTiers", tiers);
            setField("switchingCooldown", 3600L);
            setField("switchingCost", "gold * 5");

            assertFalse(section.getRequireLinearRankProgression(), "linear progression should reflect custom value");
            assertTrue(section.getAllowSkippingRanks(), "skipping ranks should reflect custom value");
            assertFalse(section.getEnableMultipleActiveRankTrees(), "multiple trees should reflect custom value");
            assertEquals(5, section.getMaximumActiveRankTrees(), "maximum trees should reflect custom value");
            assertFalse(section.getRequireConfirmationForRankUp(), "confirmation should reflect custom value");
            assertFalse(section.getEnableCrossRankTreeSwitching(), "cross tree switching should reflect custom value");
            assertSame(tiers, section.getGlobalSwitchingTiers(), "switching tiers should return same instance");
            assertEquals(3_600L, section.getSwitchingCooldown(), "switching cooldown should reflect custom value");
            assertEquals("gold * 5", section.getSwitchingCost(), "switching cost should reflect custom value");
        }

        private void setField(String name, Object value) throws Exception {
            Field field = ProgressionRuleSection.class.getDeclaredField(name);
            field.setAccessible(true);
            field.set(section, value);
        }
    }
}
