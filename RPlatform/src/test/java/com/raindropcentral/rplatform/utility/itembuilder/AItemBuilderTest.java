package com.raindropcentral.rplatform.utility.itembuilder;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AItemBuilderTest {

    private ServerMock server;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void setNameUsesLegacyFallbackWhenPaperUnavailable() {
        ModernItemBuilder builder = new ModernItemBuilder(Material.DIAMOND_SWORD);
        Component name = Component.text("Legendary Blade");

        ModernItemBuilder chained = builder.setName(name);
        assertSame(builder, chained, "setName should support fluent chaining");

        ItemStack built = builder.build();
        ItemMeta meta = built.getItemMeta();
        assertEquals(LegacyComponentSerializer.legacySection().serialize(name), meta.getDisplayName());
    }

    @Test
    void loreManipulationSupportsChainingAndOrdering() {
        ModernItemBuilder builder = new ModernItemBuilder(Material.PAPER);
        Component first = Component.text("First");
        Component second = Component.text("Second");
        Component third = Component.text("Third");

        builder.setLore(List.of(first));
        builder.addLoreLine(second);
        builder.addLoreLines(List.of(third));

        ItemStack built = builder.build();
        List<String> lore = built.getItemMeta().getLore();
        assertNotNull(lore);
        assertEquals(3, lore.size());
        assertEquals(LegacyComponentSerializer.legacySection().serialize(first), lore.get(0));
        assertEquals(LegacyComponentSerializer.legacySection().serialize(second), lore.get(1));
        assertEquals(LegacyComponentSerializer.legacySection().serialize(third), lore.get(2));
    }

    @Test
    void addLoreLinesVarArgsAppendsInOrder() {
        ModernItemBuilder builder = new ModernItemBuilder(Material.BOOK);
        Component first = Component.text("Alpha");
        Component second = Component.text("Beta");
        Component third = Component.text("Gamma");

        builder.addLoreLines(first, second);
        builder.addLoreLines(List.of(third));

        List<String> lore = builder.build().getItemMeta().getLore();
        assertNotNull(lore);
        assertEquals(3, lore.size());
        assertEquals(LegacyComponentSerializer.legacySection().serialize(first), lore.get(0));
        assertEquals(LegacyComponentSerializer.legacySection().serialize(second), lore.get(1));
        assertEquals(LegacyComponentSerializer.legacySection().serialize(third), lore.get(2));
    }

    @Test
    void settersMutateUnderlyingItemMeta() {
        ModernItemBuilder builder = new ModernItemBuilder(Material.STICK);

        builder.setAmount(8)
                .setCustomModelData(42)
                .addEnchantment(Enchantment.DAMAGE_ALL, 3)
                .addItemFlags(ItemFlag.HIDE_ENCHANTS);

        ItemStack built = builder.build();

        assertEquals(8, built.getAmount());
        ItemMeta meta = built.getItemMeta();
        assertEquals(42, meta.getCustomModelData());
        assertEquals(3, meta.getEnchantLevel(Enchantment.DAMAGE_ALL));
        assertTrue(meta.hasItemFlag(ItemFlag.HIDE_ENCHANTS));
    }

    @Test
    void glowingToggleAddsAndRemovesEnchantments() {
        ModernItemBuilder builder = new ModernItemBuilder(Material.BLAZE_ROD);

        builder.setGlowing(true);
        ItemMeta glowingMeta = builder.build().getItemMeta();
        assertTrue(glowingMeta.hasEnchant(Enchantment.LURE));
        assertTrue(glowingMeta.hasItemFlag(ItemFlag.HIDE_ENCHANTS));

        builder.setGlowing(false);
        ItemMeta nonGlowingMeta = builder.build().getItemMeta();
        assertFalse(nonGlowingMeta.hasEnchant(Enchantment.LURE));
        assertFalse(nonGlowingMeta.hasItemFlag(ItemFlag.HIDE_ENCHANTS));
    }

    @Test
    void buildReturnsSameInstanceWithCommittedMeta() {
        ModernItemBuilder builder = new ModernItemBuilder(Material.APPLE);
        builder.setName(Component.text("Snack"));

        ItemStack built = builder.build();
        assertSame(built, builder.build());
        assertEquals("Snack", built.getItemMeta().getDisplayName());
    }
}
