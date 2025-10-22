package com.raindropcentral.rdq.config.ranks.system;

import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotificationTypeSectionTest {

    @Test
    void itDefaultsBooleanFlagsToTrueWhenUnset() {
        final NotificationTypeSection section = new NotificationTypeSection(new EvaluationEnvironmentBuilder());

        assertTrue(section.getEnabled(), "getEnabled should default to true when not configured");
        assertTrue(section.getBroadcast(), "getBroadcast should default to true when not configured");
        assertTrue(section.getSendToPlayer(), "getSendToPlayer should default to true when not configured");
        assertTrue(section.getShowTitle(), "getShowTitle should default to true when not configured");
        assertTrue(section.getShowActionBar(), "getShowActionBar should default to true when not configured");
    }

    @Test
    void itReturnsFallbackKeysAndSoundWhenUnset() {
        final NotificationTypeSection section = new NotificationTypeSection(new EvaluationEnvironmentBuilder());

        assertEquals("rank.notification.fallback.title", section.getTitleTextKey(),
            "getTitleTextKey should return the fallback key when not configured");
        assertEquals("rank.notification.fallback.subtitle", section.getSubtitleTextKey(),
            "getSubtitleTextKey should return the fallback key when not configured");
        assertEquals("rank.notification.fallback.action_bar", section.getActionBarTextKey(),
            "getActionBarTextKey should return the fallback key when not configured");
        assertEquals("ENTITY_PLAYER_LEVELUP", section.getSoundType(),
            "getSoundType should return the fallback sound when not configured");
    }

    @Test
    void itHonorsCustomKeysAndSoundWhenConfigured() throws Exception {
        final NotificationTypeSection section = new NotificationTypeSection(new EvaluationEnvironmentBuilder());

        setField(section, "titleTextKey", "custom.title");
        setField(section, "subtitleTextKey", "custom.subtitle");
        setField(section, "actionBarTextKey", "custom.actionbar");
        setField(section, "soundType", "CUSTOM_SOUND");

        assertEquals("custom.title", section.getTitleTextKey(),
            "getTitleTextKey should return the configured value");
        assertEquals("custom.subtitle", section.getSubtitleTextKey(),
            "getSubtitleTextKey should return the configured value");
        assertEquals("custom.actionbar", section.getActionBarTextKey(),
            "getActionBarTextKey should return the configured value");
        assertEquals("CUSTOM_SOUND", section.getSoundType(),
            "getSoundType should return the configured value");
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
