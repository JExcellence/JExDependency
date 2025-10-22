package com.raindropcentral.rdq.config.item;

import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IconSectionTest {

    @Test
    void itExposesDocumentedDefaults() {
        final IconSection section = new IconSection(new EvaluationEnvironmentBuilder());

        assertEquals("PAPER", section.getMaterial(), "material should default to PAPER");
        assertEquals("not_defined", section.getDisplayNameKey(), "display name should default to not_defined");
        assertEquals("not_defined", section.getDescriptionKey(), "description should default to not_defined");
        assertEquals(1, section.getAmount(), "amount should default to 1");
        assertEquals(0, section.getCustomModelData(), "custom model data should default to 0");
        assertFalse(section.getEnchanted(), "enchanted flag should default to false");

        final List<String> hideFlags = section.getHideFlags();
        assertNotNull(hideFlags, "hide flags should never be null");
        assertTrue(hideFlags.isEmpty(), "hide flags should default to an empty list");
    }

    @Test
    void itAllowsConfigOverridesThroughSetters() {
        final IconSection section = new IconSection(new EvaluationEnvironmentBuilder());

        section.setMaterial("DIAMOND_SWORD");
        section.setDisplayNameKey("display.key");
        section.setDescriptionKey("description.key");
        section.setAmount(64);
        section.setCustomModelData(1234);
        section.setEnchanted(true);

        final List<String> configuredFlags = new ArrayList<>();
        configuredFlags.add("HIDE_ATTRIBUTES");
        configuredFlags.add("HIDE_ENCHANTS");
        section.setHideFlags(configuredFlags);

        assertEquals("DIAMOND_SWORD", section.getMaterial(), "material should reflect the configured value");
        assertEquals("display.key", section.getDisplayNameKey(), "display name should reflect the configured value");
        assertEquals("description.key", section.getDescriptionKey(), "description should reflect the configured value");
        assertEquals(64, section.getAmount(), "amount should reflect the configured value");
        assertEquals(1234, section.getCustomModelData(), "custom model data should reflect the configured value");
        assertTrue(section.getEnchanted(), "enchanted flag should reflect the configured value");
        assertSame(configuredFlags, section.getHideFlags(), "hide flags should expose the configured list instance");
    }
}
