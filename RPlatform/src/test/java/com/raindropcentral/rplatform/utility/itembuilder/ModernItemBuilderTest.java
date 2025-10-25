package com.raindropcentral.rplatform.utility.itembuilder;

import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import com.raindropcentral.rplatform.version.ServerEnvironment;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class ModernItemBuilderTest {

    private static ServerMock server;

    @BeforeAll
    static void startServer() {
        server = MockBukkit.mock();
        resetServerEnvironment();
    }

    @AfterAll
    static void stopServer() {
        MockBukkit.unmock();
        resetServerEnvironment();
    }

    @BeforeEach
    void ensureFreshEnvironment() {
        resetServerEnvironment();
    }

    @Test
    void buildsItemWithAdventureMetadataAndModernSettings() {
        ModernItemBuilder builder = new ModernItemBuilder(Material.DIAMOND_SWORD);

        Component displayName = Component.text("Celestial Edge");
        Component loreOne = Component.text("Forged in starlight");
        Component loreTwo = Component.text("Sings with power");
        Component loreThree = Component.text("Gleams at dawn");
        Component loreFour = Component.text("Awaits its hero");

        ModernItemBuilder chainedBuilder = builder.setName(displayName);
        assertSame(builder, chainedBuilder);

        chainedBuilder = chainedBuilder.setLore(List.of(loreOne));
        assertSame(builder, chainedBuilder);

        chainedBuilder = chainedBuilder.addLoreLine(loreTwo);
        assertSame(builder, chainedBuilder);

        ItemStack item = chainedBuilder
                .addLoreLines(loreThree, loreFour)
                .setAmount(2)
                .setCustomModelData(123)
                .addEnchantment(Enchantment.DAMAGE_ALL, 5)
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .setGlowing(true)
                .build();

        assertEquals(Material.DIAMOND_SWORD, item.getType());
        assertEquals(2, item.getAmount());

        ItemMeta meta = item.getItemMeta();
        assertNotNull(meta);

        assertNotNull(meta.getDisplayName());
        Component actualName = LegacyComponentSerializer.legacySection().deserialize(meta.getDisplayName());
        assertEquals(displayName, actualName);

        List<Component> expectedLore = List.of(loreOne, loreTwo, loreThree, loreFour);
        List<String> loreStrings = meta.getLore();
        assertNotNull(loreStrings);
        List<Component> actualLore = loreStrings.stream()
                .map(line -> LegacyComponentSerializer.legacySection().deserialize(line))
                .collect(Collectors.toList());
        assertEquals(expectedLore, actualLore);

        assertTrue(meta.hasCustomModelData());
        assertEquals(123, meta.getCustomModelData());
        assertEquals(5, meta.getEnchantLevel(Enchantment.DAMAGE_ALL));
        assertTrue(meta.hasEnchant(Enchantment.LURE));
        assertEquals(1, meta.getEnchantLevel(Enchantment.LURE));
        assertTrue(meta.hasItemFlag(ItemFlag.HIDE_ENCHANTS));
        assertTrue(meta.hasItemFlag(ItemFlag.HIDE_ATTRIBUTES));
    }

    @Test
    void addLoreLineCreatesLoreWhenAbsent() {
        ModernItemBuilder builder = new ModernItemBuilder(Material.EMERALD);

        ItemStack item = builder.addLoreLine(Component.text("Singular line")).build();
        ItemMeta meta = item.getItemMeta();
        assertNotNull(meta);

        List<String> loreStrings = meta.getLore();
        assertNotNull(loreStrings);
        assertEquals(1, loreStrings.size());

        Component actual = LegacyComponentSerializer.legacySection().deserialize(loreStrings.get(0));
        assertEquals(Component.text("Singular line"), actual);
    }

    private static void resetServerEnvironment() {
        try {
            Field instanceField = ServerEnvironment.class.getDeclaredField("instance");
            instanceField.setAccessible(true);
            instanceField.set(null, null);
        } catch (ReflectiveOperationException exception) {
            fail("Failed to reset ServerEnvironment", exception);
        }
    }
}
