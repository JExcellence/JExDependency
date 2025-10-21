package com.raindropcentral.rplatform.database.converter;

import be.seeseemelk.mockbukkit.MockBukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ItemStackConverterTest {

    private final ItemStackConverter converter = new ItemStackConverter();

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void convertToDatabaseColumnReturnsNullForNullItem() {
        assertNull(converter.convertToDatabaseColumn(null));
    }

    @Test
    void convertToDatabaseColumnReturnsEmptyStringForEmptyItem() {
        ItemStack emptyStack = new ItemStack(Material.AIR);
        assertTrue(emptyStack.isEmpty());
        assertEquals("", converter.convertToDatabaseColumn(emptyStack));
    }

    @Test
    void convertToEntityAttributeReturnsNullForNullColumn() {
        assertNull(converter.convertToEntityAttribute(null));
    }

    @Test
    void convertToEntityAttributeReturnsAirStackForEmptyColumn() {
        ItemStack stack = converter.convertToEntityAttribute("");
        assertNotNull(stack);
        assertEquals(Material.AIR, stack.getType());
        assertTrue(stack.isEmpty());
    }

    @Test
    void convertToEntityAttributeThrowsForInvalidBase64() {
        assertThrows(IllegalArgumentException.class, () -> converter.convertToEntityAttribute("@@not-base64@@"));
    }

    @Test
    void roundTripPreservesItemMetaLoreAndEnchantments() {
        ItemStack original = createDecoratedStack();
        String columnValue = converter.convertToDatabaseColumn(original);
        assertNotNull(columnValue);
        ItemStack reconstructed = converter.convertToEntityAttribute(columnValue);
        assertNotNull(reconstructed);
        assertItemStacksEquivalent(original, reconstructed);
    }

    @Test
    void convertToEntityAttributeMaintainsDecoratedStackFromStoredPayload() {
        ItemStack decorated = createDecoratedStack();
        String payload = converter.convertToDatabaseColumn(decorated);
        ItemStack converted = converter.convertToEntityAttribute(payload);
        assertItemStacksEquivalent(decorated, converted);
    }

    private ItemStack createDecoratedStack() {
        ItemStack stack = new ItemStack(Material.DIAMOND_SWORD, 1);
        ItemMeta meta = stack.getItemMeta();
        assertNotNull(meta);
        meta.setDisplayName("Radiant Edge");
        meta.setLore(List.of("Forged in starlight", "Unmatched sharpness"));
        meta.addEnchant(Enchantment.DAMAGE_ALL, 5, true);
        meta.addEnchant(Enchantment.DURABILITY, 3, true);
        stack.setItemMeta(meta);
        return stack;
    }

    private void assertItemStacksEquivalent(ItemStack expected, ItemStack actual) {
        assertEquals(expected.getType(), actual.getType());
        assertEquals(expected.getAmount(), actual.getAmount());

        ItemMeta expectedMeta = expected.getItemMeta();
        ItemMeta actualMeta = actual.getItemMeta();
        assertNotNull(expectedMeta);
        assertNotNull(actualMeta);
        assertEquals(expectedMeta.getDisplayName(), actualMeta.getDisplayName());
        assertEquals(expectedMeta.getLore(), actualMeta.getLore());

        Map<Enchantment, Integer> expectedEnchants = expected.getEnchantments();
        Map<Enchantment, Integer> actualEnchants = actual.getEnchantments();
        assertEquals(expectedEnchants, actualEnchants);
    }
}
