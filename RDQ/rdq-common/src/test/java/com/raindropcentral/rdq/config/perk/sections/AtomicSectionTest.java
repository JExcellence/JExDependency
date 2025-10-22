package com.raindropcentral.rdq.config.perk.sections;

import com.raindropcentral.rdq.config.perk.sections.forge.AtomicForgeSection;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AtomicSectionTest {

    @Test
    void itReturnsConfiguredValuesFromGetters() throws Exception {
        final AtomicSection section = new AtomicSection(new EvaluationEnvironmentBuilder());
        final AtomicForgeSection forgeSection = new AtomicForgeSection(new EvaluationEnvironmentBuilder());

        setField(section, "xp", 128);
        setField(section, "sound", true);
        setField(section, "particles", false);
        setField(section, "block", "minecraft:crying_obsidian");
        setField(section, "notify", 9);
        setField(section, "atomicForgeSection", forgeSection);

        assertEquals(128, section.getXp(), "getXp should return the configured xp requirement");
        assertTrue(section.isSound(), "isSound should report the configured sound state");
        assertFalse(section.isParticles(), "isParticles should report the configured particle state");
        assertEquals("minecraft:crying_obsidian", section.getBlock(),
            "getBlock should expose the configured supporting block");
        assertEquals(9, section.getNotify(), "getNotify should expose the configured notification timer");
        assertSame(forgeSection, section.getAtomicForgeSection(),
            "getAtomicForgeSection should return the configured forge section reference");
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
