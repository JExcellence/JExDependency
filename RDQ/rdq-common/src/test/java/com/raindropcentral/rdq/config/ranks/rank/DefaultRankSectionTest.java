package com.raindropcentral.rdq.config.ranks.rank;

import com.raindropcentral.rdq.config.item.IconSection;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultRankSectionTest {

    @Test
    void itReturnsDocumentedDefaultsWhenUnset() {
        final DefaultRankSection section = new DefaultRankSection(new EvaluationEnvironmentBuilder());

        assertEquals("default_unselected", section.getDefaultRankTreeIdentifier(),
            "default rank tree identifier should fall back to default_unselected");
        assertEquals("unselected_rank", section.getDefaultRankIdentifier(),
            "default rank identifier should fall back to unselected_rank");
        assertEquals("rank.default.unselected.name", section.getDisplayNameKey(),
            "display name key should use documented default");
        assertEquals("rank.default.unselected.lore", section.getDescriptionKey(),
            "description key should use documented default");
        assertEquals("", section.getPrefixKey(),
            "prefix key should default to empty string");
        assertEquals("", section.getSuffixKey(),
            "suffix key should default to empty string");
        assertTrue(section.getVisible(), "visibility should default to true");
        assertFalse(section.getSelectable(), "selectable should default to false");
        assertFalse(section.getStartingRank(), "starting rank should default to false");
        assertTrue(section.getEnabled(), "enabled flag should default to true");
        assertEquals("default", section.getLuckPermsGroup(),
            "LuckPerms group should default to 'default'");
        assertEquals(1, section.getTier(), "tier should default to 1");
        assertEquals(0, section.getWeight(), "weight should default to 0");

        final IconSection icon = section.getIcon();
        assertNotNull(icon, "icon should default to a new IconSection instance");
    }

    @Test
    void itHonorsConfiguredValuesWhenPresent() throws Exception {
        final DefaultRankSection section = new DefaultRankSection(new EvaluationEnvironmentBuilder());
        final IconSection iconSection = new IconSection(new EvaluationEnvironmentBuilder());

        setField(section, "defaultRankTreeIdentifier", "custom_tree");
        setField(section, "defaultRankIdentifier", "custom_rank");
        setField(section, "displayNameKey", "rank.custom.name");
        setField(section, "descriptionKey", "rank.custom.description");
        setField(section, "prefixKey", "rank.custom.prefix");
        setField(section, "suffixKey", "rank.custom.suffix");
        setField(section, "isVisible", Boolean.FALSE);
        setField(section, "isSelectable", Boolean.TRUE);
        setField(section, "isInitialRank", Boolean.TRUE);
        setField(section, "enabled", Boolean.FALSE);
        setField(section, "luckPermsGroup", "custom_group");
        setField(section, "tier", 5);
        setField(section, "weight", 42);
        setField(section, "icon", iconSection);

        assertEquals("custom_tree", section.getDefaultRankTreeIdentifier(),
            "default rank tree identifier should reflect configured value");
        assertEquals("custom_rank", section.getDefaultRankIdentifier(),
            "default rank identifier should reflect configured value");
        assertEquals("rank.custom.name", section.getDisplayNameKey(),
            "display name key should reflect configured value");
        assertEquals("rank.custom.description", section.getDescriptionKey(),
            "description key should reflect configured value");
        assertEquals("rank.custom.prefix", section.getPrefixKey(),
            "prefix key should reflect configured value");
        assertEquals("rank.custom.suffix", section.getSuffixKey(),
            "suffix key should reflect configured value");
        assertFalse(section.getVisible(), "visibility should respect configured false value");
        assertTrue(section.getSelectable(), "selectable should respect configured true value");
        assertTrue(section.getStartingRank(), "starting rank should respect configured true value");
        assertFalse(section.getEnabled(), "enabled flag should respect configured false value");
        assertEquals("custom_group", section.getLuckPermsGroup(),
            "LuckPerms group should reflect configured value");
        assertEquals(5, section.getTier(), "tier should reflect configured value");
        assertEquals(42, section.getWeight(), "weight should reflect configured value");
        assertSame(iconSection, section.getIcon(), "icon should reflect configured icon section");
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
