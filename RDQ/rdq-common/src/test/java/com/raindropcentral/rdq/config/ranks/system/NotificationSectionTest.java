package com.raindropcentral.rdq.config.ranks.system;

import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

class NotificationSectionTest {

    @Test
    void itReturnsDefaultSectionsWhenFieldsAreNull() {
        final NotificationSection section = new NotificationSection(new EvaluationEnvironmentBuilder());

        assertNotNull(section.getRankUnlock(), "getRankUnlock should supply a default section when unset");
        assertNotNull(section.getRankTreeComplete(), "getRankTreeComplete should supply a default section when unset");
        assertNotNull(section.getFinalRankUnlock(), "getFinalRankUnlock should supply a default section when unset");
        assertNotNull(section.getCrossRankTreeSwitch(), "getCrossRankTreeSwitch should supply a default section when unset");
    }

    @Test
    void itReturnsInjectedSectionWhileLeavingOthersDefault() throws Exception {
        final NotificationSection section = new NotificationSection(new EvaluationEnvironmentBuilder());
        final NotificationTypeSection injectedSection = new NotificationTypeSection(new EvaluationEnvironmentBuilder());

        setField(section, "rankTreeComplete", injectedSection);

        assertSame(injectedSection, section.getRankTreeComplete(),
            "getRankTreeComplete should return the injected section");
        assertNotNull(section.getRankUnlock(), "getRankUnlock should still provide a default section");
        assertNotNull(section.getFinalRankUnlock(), "getFinalRankUnlock should still provide a default section");
        assertNotNull(section.getCrossRankTreeSwitch(), "getCrossRankTreeSwitch should still provide a default section");
        assertNotSame(injectedSection, section.getRankUnlock(),
            "getRankUnlock should not return the injected section");
        assertNotSame(injectedSection, section.getFinalRankUnlock(),
            "getFinalRankUnlock should not return the injected section");
        assertNotSame(injectedSection, section.getCrossRankTreeSwitch(),
            "getCrossRankTreeSwitch should not return the injected section");
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
