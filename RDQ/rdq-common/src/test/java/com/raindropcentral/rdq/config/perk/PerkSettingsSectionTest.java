package com.raindropcentral.rdq.config.perk;

import com.raindropcentral.rdq.config.item.IconSection;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PerkSettingsSectionTest {

    @Test
    void itGeneratesDefaultLocalizationKeysAndIconFallbacks() throws Exception {
        final PerkSettingsSection section = new PerkSettingsSection(new EvaluationEnvironmentBuilder());
        final IconSection icon = new IconSection(new EvaluationEnvironmentBuilder());

        icon.setDisplayNameKey("not_defined");
        icon.setDescriptionKey(null);

        setField(section, "perkId", "swift");
        setField(section, "displayNameKey", null);
        setField(section, "descriptionKey", null);
        setField(section, "icon", icon);

        section.afterParsing(Collections.emptyList());

        assertEquals("perk.swift.name", section.getDisplayNameKey(),
            "afterParsing should derive the display name key when none are provided");
        assertEquals("perk.swift.lore", section.getDescriptionKey(),
            "afterParsing should derive the description key when none are provided");
        assertEquals("perk.swift.name", section.getIcon().getDisplayNameKey(),
            "afterParsing should provide the icon display key fallback");
        assertEquals("perk.swift.lore", section.getIcon().getDescriptionKey(),
            "afterParsing should provide the icon description key fallback");
    }

    @Test
    void itDefaultsEnabledStateAndMetadata() {
        final PerkSettingsSection section = new PerkSettingsSection(new EvaluationEnvironmentBuilder());

        assertTrue(section.getEnabled(), "Perks should default to an enabled state when unspecified");

        final Map<String, Object> first = section.getMetadata();
        final Map<String, Object> second = section.getMetadata();

        assertNotNull(first, "Metadata should never be null even when not configured");
        assertTrue(first.isEmpty(), "Metadata should default to an empty map");
        assertNotSame(first, second, "getMetadata should return a new map when the backing field is null");
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
