package com.raindropcentral.rplatform.utility.itembuilder;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import com.raindropcentral.rplatform.version.ServerEnvironment;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LegacyItemBuilderTest {

    private ServerMock server;

    @BeforeEach
    void setUp() {
        this.server = MockBukkit.mock();
        resetServerEnvironment();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
        resetServerEnvironment();
    }

    @Test
    void legacyBuilderAppliesLegacyMetadataAcrossFluentApi() {
        installServerEnvironment(false);

        ItemStack damagedSword = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta baseMeta = damagedSword.getItemMeta();
        assertNotNull(baseMeta);
        Damageable damageable = (Damageable) baseMeta;
        damageable.setDamage(7);
        damagedSword.setItemMeta(baseMeta);

        Component name = Component.text("Legacy Blade", NamedTextColor.GOLD);
        List<Component> lore = List.of(
                Component.text("One of a kind", NamedTextColor.AQUA),
                Component.text("Forged before the rift", NamedTextColor.GRAY)
        );

        LegacyItemBuilder builder = new LegacyItemBuilder(damagedSword);
        builder.setName(name)
                .setLore(lore)
                .addLoreLine(Component.text("Still sharp", NamedTextColor.GREEN))
                .addLoreLines(List.of(Component.text("Remember the past", NamedTextColor.YELLOW)))
                .addLoreLines(
                        Component.text("Guard it", NamedTextColor.RED),
                        Component.text("Honor it", NamedTextColor.BLUE)
                )
                .setAmount(2)
                .setCustomModelData(87)
                .addEnchantment(Enchantment.DAMAGE_ALL, 5)
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .setGlowing(true);

        ItemStack result = builder.build();

        assertEquals(Material.DIAMOND_SWORD, result.getType());
        assertEquals(2, result.getAmount());

        ItemMeta meta = result.getItemMeta();
        assertNotNull(meta);
        assertEquals(
                LegacyComponentSerializer.legacySection().serialize(name),
                meta.getDisplayName()
        );

        List<String> expectedLore = new ArrayList<>();
        expectedLore.add(LegacyComponentSerializer.legacySection().serialize(lore.get(0)));
        expectedLore.add(LegacyComponentSerializer.legacySection().serialize(lore.get(1)));
        expectedLore.add(LegacyComponentSerializer.legacySection().serialize(Component.text("Still sharp", NamedTextColor.GREEN)));
        expectedLore.add(LegacyComponentSerializer.legacySection().serialize(Component.text("Remember the past", NamedTextColor.YELLOW)));
        expectedLore.add(LegacyComponentSerializer.legacySection().serialize(Component.text("Guard it", NamedTextColor.RED)));
        expectedLore.add(LegacyComponentSerializer.legacySection().serialize(Component.text("Honor it", NamedTextColor.BLUE)));
        assertEquals(expectedLore, meta.getLore());

        assertEquals(87, meta.getCustomModelData());
        assertEquals(5, meta.getEnchantLevel(Enchantment.DAMAGE_ALL));
        assertTrue(meta.hasEnchant(Enchantment.LURE));
        assertTrue(meta.hasItemFlag(ItemFlag.HIDE_ATTRIBUTES));
        assertTrue(meta.hasItemFlag(ItemFlag.HIDE_ENCHANTS));

        Damageable resultDamageable = (Damageable) meta;
        assertEquals(7, resultDamageable.getDamage());
    }

    @Test
    void legacyBuilderProducesSameResultAsModernBuilder() {
        installServerEnvironment(false);

        Component name = Component.text("Twin Relics", NamedTextColor.DARK_PURPLE);
        List<Component> lore = List.of(
                Component.text("Recovered from ruins", NamedTextColor.GRAY),
                Component.text("Bound to the bearer", NamedTextColor.GOLD)
        );

        ItemStack legacyItem = new LegacyItemBuilder(Material.BLAZE_ROD)
                .setName(name)
                .setLore(lore)
                .addLoreLine(Component.text("Legacy path", NamedTextColor.YELLOW))
                .addLoreLines(List.of(Component.text("Ancient text", NamedTextColor.DARK_AQUA)))
                .addLoreLines(Component.text("Power", NamedTextColor.RED))
                .setAmount(3)
                .setCustomModelData(42)
                .addEnchantment(Enchantment.KNOCKBACK, 2)
                .addItemFlags(ItemFlag.HIDE_ENCHANTS)
                .setGlowing(true)
                .build();

        installServerEnvironment(true);

        ItemStack modernItem = new ModernItemBuilder(Material.BLAZE_ROD)
                .setName(name)
                .setLore(lore)
                .addLoreLine(Component.text("Legacy path", NamedTextColor.YELLOW))
                .addLoreLines(List.of(Component.text("Ancient text", NamedTextColor.DARK_AQUA)))
                .addLoreLines(Component.text("Power", NamedTextColor.RED))
                .setAmount(3)
                .setCustomModelData(42)
                .addEnchantment(Enchantment.KNOCKBACK, 2)
                .addItemFlags(ItemFlag.HIDE_ENCHANTS)
                .setGlowing(true)
                .build();

        assertEquals(modernItem, legacyItem);
        assertEquals(modernItem.getItemMeta(), legacyItem.getItemMeta());
    }

    @Test
    void legacyBuilderRejectsMaterialsWithoutItemMeta() {
        installServerEnvironment(false);

        assertThrows(RuntimeException.class, () -> {
            LegacyItemBuilder builder = new LegacyItemBuilder(Material.AIR);
            builder.setName(Component.text("Unsupported", NamedTextColor.GRAY));
        });
    }

    private void installServerEnvironment(boolean paper) {
        ServerEnvironment environment = Mockito.mock(ServerEnvironment.class);
        Mockito.when(environment.isPaper()).thenReturn(paper);
        try {
            Field instance = ServerEnvironment.class.getDeclaredField("instance");
            instance.setAccessible(true);
            instance.set(null, environment);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to install test server environment", exception);
        }
    }

    private void resetServerEnvironment() {
        try {
            Field instance = ServerEnvironment.class.getDeclaredField("instance");
            instance.setAccessible(true);
            instance.set(null, null);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to reset server environment", exception);
        }
    }
}
