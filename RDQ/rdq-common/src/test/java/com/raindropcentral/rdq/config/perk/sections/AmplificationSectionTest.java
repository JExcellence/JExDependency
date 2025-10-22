package com.raindropcentral.rdq.config.perk.sections;

import com.raindropcentral.rdq.config.perk.sections.forge.AmplificationForgeSection;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AmplificationSectionTest {

    @Test
    void itReturnsMutablePotionListAndPreservesMutations() {
        final AmplificationSection section = new AmplificationSection(new EvaluationEnvironmentBuilder());

        final List<String> potions = section.getPotions();

        assertNotNull(potions, "getPotions should never return null");
        assertTrue(potions.isEmpty(), "getPotions should supply an empty list when none configured");

        potions.add("minecraft:speed");

        assertSame(potions, section.getPotions(), "getPotions should return the same list instance");
        assertEquals(List.of("minecraft:speed"), section.getPotions(),
            "getPotions should preserve values appended to the returned list");
    }

    @Test
    void itExposesForgeSectionBasedOnBackingField() throws Exception {
        final AmplificationSection section = new AmplificationSection(new EvaluationEnvironmentBuilder());

        final AmplificationForgeSection first = section.getForgeAmplificationSection();
        final AmplificationForgeSection second = section.getForgeAmplificationSection();

        assertNotNull(first, "getForgeAmplificationSection should provide a non-null forge section");
        assertNotSame(first, second,
            "getForgeAmplificationSection should return a new instance when no backing field is set");

        final AmplificationForgeSection forgeSection = new AmplificationForgeSection(new EvaluationEnvironmentBuilder());
        setField(section, "amplificationForgeSection", forgeSection);

        assertSame(forgeSection, section.getForgeAmplificationSection(),
            "getForgeAmplificationSection should return the configured forge section reference");
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
