package com.raindropcentral.rplatform.serializer;

import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ItemStackSerializerTest {

    private static ServerMock server;
    private static Plugin plugin;
    private static NamespacedKey stringKey;
    private static NamespacedKey intKey;

    private ItemStackSerializer serializer;

    @BeforeAll
    static void startServer() {
        server = MockBukkit.mock();
        plugin = MockBukkit.createMockPlugin();
        stringKey = new NamespacedKey(plugin, "custom-string");
        intKey = new NamespacedKey(plugin, "custom-int");
    }

    @AfterAll
    static void stopServer() {
        MockBukkit.unmock();
    }

    @BeforeEach
    void setUp() {
        serializer = new ItemStackSerializer();
    }

    @Test
    void jsonRoundTripPreservesMetadata() {
        ItemStack original = createDecoratedStack();

        byte[] binary = serializer.serialize(original);
        String json = toJson(binary);
        byte[] restoredBinary = fromJson(json);
        ItemStack restored = serializer.deserialize(restoredBinary);

        assertItemStacksEquivalent(original, restored);
    }

    @Test
    void base64RoundTripPreservesMetadata() {
        ItemStack original = createDecoratedStack();

        String base64 = serializer.toBase64(original);
        ItemStack restored = serializer.fromBase64(base64);

        assertItemStacksEquivalent(original, restored);
    }

    @Test
    void jsonArrayRoundTripHandlesNullEntries() {
        ItemStack decorated = createDecoratedStack();
        ItemStack goldenApple = createGoldenApple();
        ItemStack[] original = new ItemStack[] { decorated, null, goldenApple };

        byte[] binary = serializer.serializeArray(original);
        String json = toJson(binary);
        ItemStack[] restored = serializer.deserializeArray(fromJson(json));

        assertEquals(3, restored.length);
        assertItemStacksEquivalent(decorated, restored[0]);
        assertEquals(Material.AIR, restored[1].getType());
        assertItemStacksEquivalent(goldenApple, restored[2]);
    }

    @Test
    void base64ArrayRoundTripPreservesMetadata() {
        ItemStack decorated = createDecoratedStack();
        ItemStack bow = createBow();
        ItemStack[] original = new ItemStack[] { decorated, bow };

        String base64 = serializer.arrayToBase64(original);
        ItemStack[] restored = serializer.arrayFromBase64(base64);

        assertEquals(2, restored.length);
        assertItemStacksEquivalent(decorated, restored[0]);
        assertItemStacksEquivalent(bow, restored[1]);
    }

    @Test
    void serializeNullProducesAirStack() {
        ItemStack restored = serializer.deserialize(serializer.serialize(null));
        assertEquals(Material.AIR, restored.getType());
        assertTrue(restored.isEmpty());
    }

    @Test
    void arraySerializationHandlesNullAndEmptyInputs() {
        ItemStack[] restoredFromNull = serializer.deserializeArray(serializer.serializeArray(null));
        assertEquals(0, restoredFromNull.length);

        ItemStack[] restoredFromEmpty = serializer.deserializeArray(serializer.serializeArray(new ItemStack[0]));
        assertEquals(0, restoredFromEmpty.length);

        ItemStack[] restoredFromBase64Null = serializer.arrayFromBase64(serializer.arrayToBase64(null));
        assertEquals(0, restoredFromBase64Null.length);
    }

    @Test
    void toBase64NormalizesNullToAir() {
        ItemStack restored = serializer.fromBase64(serializer.toBase64(null));
        assertEquals(Material.AIR, restored.getType());
        assertTrue(restored.isEmpty());
    }

    private ItemStack createDecoratedStack() {
        ItemStack stack = new ItemStack(Material.DIAMOND_SWORD, 1);
        ItemMeta meta = stack.getItemMeta();
        assertNotNull(meta);
        meta.setDisplayName("Radiant Edge");
        meta.setLore(List.of("Forged in starlight", "Unmatched sharpness"));
        meta.addEnchant(Enchantment.DAMAGE_ALL, 5, true);
        meta.addEnchant(Enchantment.DURABILITY, 3, true);
        meta.setCustomModelData(77);
        meta.getPersistentDataContainer().set(stringKey, PersistentDataType.STRING, "legendary");
        meta.getPersistentDataContainer().set(intKey, PersistentDataType.INTEGER, 1337);
        stack.setItemMeta(meta);
        return stack;
    }

    private ItemStack createGoldenApple() {
        ItemStack stack = new ItemStack(Material.GOLDEN_APPLE, 2);
        ItemMeta meta = stack.getItemMeta();
        assertNotNull(meta);
        meta.setDisplayName("Blessed Harvest");
        meta.setLore(List.of("Sweet radiance"));
        meta.getPersistentDataContainer().set(stringKey, PersistentDataType.STRING, "nourishment");
        stack.setItemMeta(meta);
        return stack;
    }

    private ItemStack createBow() {
        ItemStack stack = new ItemStack(Material.BOW, 1);
        ItemMeta meta = stack.getItemMeta();
        assertNotNull(meta);
        meta.setDisplayName("Skylark");
        meta.addEnchant(Enchantment.ARROW_DAMAGE, 4, true);
        meta.addEnchant(Enchantment.ARROW_INFINITE, 1, true);
        meta.getPersistentDataContainer().set(intKey, PersistentDataType.INTEGER, 21);
        stack.setItemMeta(meta);
        return stack;
    }

    private void assertItemStacksEquivalent(ItemStack expected, ItemStack actual) {
        assertNotSame(expected, actual);
        assertEquals(expected.getType(), actual.getType());
        assertEquals(expected.getAmount(), actual.getAmount());

        ItemMeta expectedMeta = expected.getItemMeta();
        ItemMeta actualMeta = actual.getItemMeta();
        assertNotNull(expectedMeta);
        assertNotNull(actualMeta);
        assertEquals(expectedMeta.getDisplayName(), actualMeta.getDisplayName());
        assertEquals(expectedMeta.getLore(), actualMeta.getLore());
        assertEquals(expectedMeta.getEnchants(), actualMeta.getEnchants());

        if (expectedMeta.hasCustomModelData()) {
            assertTrue(actualMeta.hasCustomModelData());
            assertEquals(expectedMeta.getCustomModelData(), actualMeta.getCustomModelData());
        } else {
            assertFalse(actualMeta.hasCustomModelData());
        }

        assertEquals(
            expectedMeta.getPersistentDataContainer().get(stringKey, PersistentDataType.STRING),
            actualMeta.getPersistentDataContainer().get(stringKey, PersistentDataType.STRING)
        );
        assertEquals(
            expectedMeta.getPersistentDataContainer().get(intKey, PersistentDataType.INTEGER),
            actualMeta.getPersistentDataContainer().get(intKey, PersistentDataType.INTEGER)
        );
    }

    private String toJson(byte[] data) {
        StringBuilder builder = new StringBuilder();
        builder.append('[');
        for (int i = 0; i < data.length; i++) {
            builder.append(Byte.toUnsignedInt(data[i]));
            if (i < data.length - 1) {
                builder.append(',');
            }
        }
        builder.append(']');
        return builder.toString();
    }

    private byte[] fromJson(String json) {
        String trimmed = json.trim();
        if (trimmed.length() <= 2) {
            return new byte[0];
        }

        String content = trimmed.substring(1, trimmed.length() - 1).trim();
        if (content.isEmpty()) {
            return new byte[0];
        }

        String[] tokens = content.split("\\s*,\\s*");
        byte[] data = new byte[tokens.length];
        for (int i = 0; i < tokens.length; i++) {
            int value = Integer.parseInt(tokens[i]);
            data[i] = (byte) value;
        }
        return data;
    }
}
