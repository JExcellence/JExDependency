package com.raindropcentral.rplatform.database.converter;

import be.seeseemelk.mockbukkit.MockBukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ItemStackMapConverterTest {

    private ItemStackMapConverter converter;

    @BeforeAll
    static void startServer() {
        MockBukkit.mock();
    }

    @AfterAll
    static void stopServer() {
        MockBukkit.unmock();
    }

    @BeforeEach
    void setUp() {
        converter = new ItemStackMapConverter();
    }

    @Test
    void roundTripPreservesEntriesAndMetadata() {
        final Map<String, ItemStack> original = new LinkedHashMap<>();

        final ItemStack sword = new ItemStack(Material.DIAMOND_SWORD);
        sword.addUnsafeEnchantment(Enchantment.DAMAGE_ALL, 5);
        final ItemMeta swordMeta = sword.getItemMeta();
        swordMeta.setCustomModelData(42);
        sword.setItemMeta(swordMeta);
        original.put("primary-slot", sword);

        final ItemStack leather = new ItemStack(Material.LEATHER_CHESTPLATE, 2);
        final LeatherArmorMeta leatherMeta = (LeatherArmorMeta) leather.getItemMeta();
        leatherMeta.setColor(Color.AQUA);
        leather.setItemMeta(leatherMeta);
        original.put("备用", leather);

        original.put("empty-slot", null);

        final String columnValue = converter.convertToDatabaseColumn(original);
        final Map<String, ItemStack> restored = converter.convertToEntityAttribute(columnValue);

        assertNotNull(restored);
        assertEquals(3, restored.size());
        assertTrue(restored.containsKey("primary-slot"));
        assertTrue(restored.containsKey("备用"));
        assertTrue(restored.containsKey("empty-slot"));

        final ItemStack restoredSword = restored.get("primary-slot");
        assertNotNull(restoredSword);
        assertNotSame(sword, restoredSword);
        assertEquals(sword, restoredSword);
        assertEquals(sword.getEnchantments(), restoredSword.getEnchantments());
        assertEquals(sword.getItemMeta(), restoredSword.getItemMeta());

        final ItemStack restoredLeather = restored.get("备用");
        assertNotNull(restoredLeather);
        assertNotSame(leather, restoredLeather);
        assertEquals(leather, restoredLeather);
        assertEquals(((LeatherArmorMeta) leather.getItemMeta()).getColor(),
            ((LeatherArmorMeta) restoredLeather.getItemMeta()).getColor());

        final ItemStack airStack = restored.get("empty-slot");
        assertNotNull(airStack);
        assertEquals(Material.AIR, airStack.getType());
    }

    @Test
    void handlesNullAndEmptyMapsSafely() {
        assertNull(converter.convertToDatabaseColumn(null));

        final Map<String, ItemStack> empty = new LinkedHashMap<>();
        assertEquals("", converter.convertToDatabaseColumn(empty));

        assertNull(converter.convertToEntityAttribute(null));
        final Map<String, ItemStack> emptyFromBlank = converter.convertToEntityAttribute("");
        assertNotNull(emptyFromBlank);
        assertTrue(emptyFromBlank.isEmpty());

        final Map<String, ItemStack> emptyFromWhitespace = converter.convertToEntityAttribute("   ");
        assertNotNull(emptyFromWhitespace);
        assertTrue(emptyFromWhitespace.isEmpty());
    }
}
