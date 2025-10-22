package com.raindropcentral.rdq.config.ranks.system;

import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SystemSettingsSectionTest {

    @Test
    void itReturnsTrueForAllFlagsByDefault() {
        final SystemSettingsSection section = new SystemSettingsSection(new EvaluationEnvironmentBuilder());

        assertTrue(section.getEnableRankSystem(), "rank system should default to enabled");
        assertTrue(section.getEnableFinalRankRules(), "final rank rules should default to enabled");
        assertTrue(section.getEnableCrossTreeProgression(), "cross-tree progression should default to enabled");
        assertTrue(section.getEnableProgressTracking(), "progress tracking should default to enabled");
        assertTrue(section.getEnableRankBroadcasts(), "rank broadcasts should default to enabled");
    }

    @Test
    void itHonorsExplicitDisablingOfFlags() throws Exception {
        final SystemSettingsSection section = new SystemSettingsSection(new EvaluationEnvironmentBuilder());

        setField(section, "enableRankSystem", Boolean.FALSE);
        setField(section, "enableFinalRankRules", Boolean.FALSE);
        setField(section, "enableCrossTreeProgression", Boolean.FALSE);
        setField(section, "enableProgressTracking", Boolean.FALSE);
        setField(section, "enableRankBroadcasts", Boolean.FALSE);

        assertFalse(section.getEnableRankSystem(), "rank system getter should honor explicit false value");
        assertFalse(section.getEnableFinalRankRules(), "final rank rules getter should honor explicit false value");
        assertFalse(section.getEnableCrossTreeProgression(), "cross-tree progression getter should honor explicit false value");
        assertFalse(section.getEnableProgressTracking(), "progress tracking getter should honor explicit false value");
        assertFalse(section.getEnableRankBroadcasts(), "rank broadcasts getter should honor explicit false value");
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
