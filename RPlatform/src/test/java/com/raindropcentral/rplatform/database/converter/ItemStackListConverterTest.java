package com.raindropcentral.rplatform.database.converter;

import org.mockbukkit.mockbukkit.MockBukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ItemStackListConverterTest {

    private final ItemStackListConverter converter = new ItemStackListConverter();

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void convertToDatabaseColumnReturnsNullForNullList() {
        assertNull(converter.convertToDatabaseColumn(null));
    }

    @Test
    void convertToDatabaseColumnReturnsEmptyStringForEmptyList() {
        assertEquals("", converter.convertToDatabaseColumn(new ArrayList<>()));
    }

    @Test
    void convertToDatabaseColumnSkipsNullAndEmptyEntries() {
        ItemStack decorated = createSword();
        List<ItemStack> mixed = Arrays.asList(null, new ItemStack(Material.AIR), decorated, null);
        String column = converter.convertToDatabaseColumn(mixed);
        assertEquals(converter.convertToDatabaseColumn(List.of(decorated)), column);
    }

    @Test
    void convertToEntityAttributeReturnsNullForNullColumn() {
        assertNull(converter.convertToEntityAttribute(null));
    }

    @Test
    void convertToEntityAttributeReturnsEmptyListForBlankColumn() {
        List<ItemStack> result = converter.convertToEntityAttribute("");
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void convertToEntityAttributeTreatsEmptyTokensAsAirStacks() {
        List<ItemStack> result = converter.convertToEntityAttribute(";");
        assertEquals(2, result.size());
        assertTrue(result.get(0).isEmpty());
        assertTrue(result.get(1).isEmpty());
    }

    @Test
    void roundTripPreservesOrderAndMetadata() {
        List<ItemStack> original = List.of(
                createSword(),
                createGoldenApple(),
                createBow()
        );

        String column = converter.convertToDatabaseColumn(original);
        assertNotNull(column);

        List<ItemStack> reconstructed = converter.convertToEntityAttribute(column);
        assertNotNull(reconstructed);
        assertEquals(original.size(), reconstructed.size());

        for (int i = 0; i < original.size(); i++) {
            assertItemStacksEquivalent(original.get(i), reconstructed.get(i));
        }
    }

    private ItemStack createSword() {
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

    private ItemStack createGoldenApple() {
        ItemStack stack = new ItemStack(Material.GOLDEN_APPLE, 2);
        ItemMeta meta = stack.getItemMeta();
        assertNotNull(meta);
        meta.setDisplayName("Blessed Harvest");
        meta.setLore(List.of("Sweet radiance"));
        stack.setItemMeta(meta);
        return stack;
    }

    private ItemStack createBow() {
        ItemStack stack = new ItemStack(Material.BOW, 1);
        ItemMeta meta = stack.getItemMeta();
        assertNotNull(meta);
        meta.setDisplayName("Skylark");
        meta.addEnchant(Enchantment.ARROW_INFINITE, 1, true);
        meta.addEnchant(Enchantment.ARROW_DAMAGE, 4, true);
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
