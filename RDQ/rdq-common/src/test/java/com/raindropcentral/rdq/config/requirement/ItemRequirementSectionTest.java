package com.raindropcentral.rdq.config.requirement;

import de.jexcellence.evaluable.section.ItemStackSection;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ItemRequirementSectionTest {

    @Test
    void itProvidesDefaultCompletionAndProgressFlags() {
        final ItemRequirementSection section = new ItemRequirementSection(new EvaluationEnvironmentBuilder());

        assertTrue(section.getConsumeOnComplete(), "getConsumeOnComplete should default to true when unset");
        assertFalse(section.getAllowPartialProgress(), "getAllowPartialProgress should default to false when unset");
    }

    @Test
    void itBuildsRequiredItemsFromConfiguredSections() throws Exception {
        final ItemRequirementSection section = new ItemRequirementSection(new EvaluationEnvironmentBuilder());

        final ItemStack firstStack = new ItemStack(Material.DIAMOND, 3);
        final ItemStackSection firstSection = mock(ItemStackSection.class, Answers.RETURNS_DEEP_STUBS);
        when(firstSection.asItem().build()).thenReturn(firstStack);

        final ItemStack secondStack = new ItemStack(Material.EMERALD, 2);
        final ItemStackSection secondSection = mock(ItemStackSection.class, Answers.RETURNS_DEEP_STUBS);
        when(secondSection.asItem().build()).thenReturn(secondStack);

        final Map<String, ItemStackSection> requiredItems = new HashMap<>();
        requiredItems.put("first", firstSection);
        requiredItems.put("second", secondSection);

        setField(section, "requiredItems", requiredItems);

        final List<ItemStack> builtItems = section.getRequiredItemsList();
        assertEquals(2, builtItems.size(), "getRequiredItemsList should build each configured item stack");
        assertTrue(builtItems.contains(firstStack), "getRequiredItemsList should include the first configured item stack");
        assertTrue(builtItems.contains(secondStack), "getRequiredItemsList should include the second configured item stack");

        final Map<String, ItemStackSection> exposedItems = section.getRequiredItems();
        assertSame(requiredItems, exposedItems, "getRequiredItems should expose the underlying requiredItems map");
    }

    private static void setField(final Object target, final String name, final Object value) throws Exception {
        final Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
