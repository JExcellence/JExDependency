package com.raindropcentral.rdq.config.perk.sections;

import com.raindropcentral.rdq.config.perk.sections.forge.CaptisForgeSection;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class CaptisSectionTest {

    @Test
    void itReportsConfiguredRateAndForgeSection() throws Exception {
        final CaptisSection section = new CaptisSection(new EvaluationEnvironmentBuilder());
        final CaptisForgeSection forgeSection = new CaptisForgeSection(new EvaluationEnvironmentBuilder());

        setField(section, "rate", 0.75D);
        setField(section, "captisForgeSection", forgeSection);

        assertEquals(0.75D, section.getRate(), 0.0000001,
            "getRate should return the configured rate value");
        assertSame(forgeSection, section.getCaptisForgeSection(),
            "getCaptisForgeSection should expose the configured forge section reference");
    }

    @Test
    void itDefaultsRateToZeroWhenConstructed() {
        final CaptisSection section = new CaptisSection(new EvaluationEnvironmentBuilder());

        assertEquals(0.0D, section.getRate(), 0.0000001,
            "getRate should report zero until the rate is configured");
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
