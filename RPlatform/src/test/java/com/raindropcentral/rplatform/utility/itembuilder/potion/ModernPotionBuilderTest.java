package com.raindropcentral.rplatform.utility.itembuilder.potion;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import com.raindropcentral.rplatform.utility.itembuilder.ModernItemBuilder;
import com.raindropcentral.rplatform.version.ServerEnvironment;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ModernPotionBuilderTest {

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
    void refreshEnvironment() {
        resetServerEnvironment();
    }

    @Test
    void buildsPotionWithModernMetadataAndFluentBaseBuilderOperations() {
        ModernPotionBuilder builder = new ModernPotionBuilder();

        Component displayName = Component.text("Arcane Elixir");
        Component loreOne = Component.text("Distilled under moonlight");
        Component loreTwo = Component.text("Best sipped at dawn");

        ModernPotionBuilder chainedBuilder = builder.setName(displayName);
        assertSame(builder, chainedBuilder, "setName should return the builder instance");

        chainedBuilder = chainedBuilder.setLore(List.of(loreOne));
        assertSame(builder, chainedBuilder, "setLore should return the builder instance");

        chainedBuilder = chainedBuilder.addLoreLine(loreTwo);
        assertSame(builder, chainedBuilder, "addLoreLine should return the builder instance");

        PotionEffect speedEffect = new PotionEffect(PotionEffectType.SPEED, 600, 1);
        ItemStack potion = chainedBuilder
                .setBasePotionType(PotionType.STRENGTH)
                .addCustomEffect(speedEffect, true)
                .setCustomModelData(451)
                .setAmount(3)
                .build();

        assertEquals(Material.POTION, potion.getType());
        assertEquals(3, potion.getAmount());

        PotionMeta meta = (PotionMeta) potion.getItemMeta();
        assertNotNull(meta, "Potion meta should be present");

        assertTrue(meta.hasCustomModelData());
        assertEquals(451, meta.getCustomModelData());

        PotionData baseData = meta.getBasePotionData();
        assertNotNull(baseData, "Base potion data should not be null");
        assertEquals(PotionType.STRENGTH, baseData.getType());

        assertTrue(meta.hasCustomEffect(PotionEffectType.SPEED));
        PotionEffect appliedEffect = meta.getCustomEffects().stream()
                .filter(effect -> effect.getType() == PotionEffectType.SPEED)
                .findFirst()
                .orElse(null);
        assertNotNull(appliedEffect);
        assertEquals(speedEffect.getDuration(), appliedEffect.getDuration());
        assertEquals(speedEffect.getAmplifier(), appliedEffect.getAmplifier());

        String displayNameString = meta.getDisplayName();
        assertNotNull(displayNameString, "Display name should be stored on the item");
        Component actualName = LegacyComponentSerializer.legacySection().deserialize(displayNameString);
        assertEquals(displayName, actualName);

        List<String> loreStrings = meta.getLore();
        assertNotNull(loreStrings, "Lore should not be null after setting components");
        assertEquals(2, loreStrings.size());
        assertEquals(loreOne, LegacyComponentSerializer.legacySection().deserialize(loreStrings.get(0)));
        assertEquals(loreTwo, LegacyComponentSerializer.legacySection().deserialize(loreStrings.get(1)));
    }

    @Test
    void addLoreLineHandlesNullLoreGracefully() {
        ModernPotionBuilder builder = new ModernPotionBuilder();

        ItemStack potion = builder
                .addLoreLine(Component.text("First"))
                .addLoreLine(Component.text("Second"))
                .build();

        PotionMeta meta = (PotionMeta) potion.getItemMeta();
        assertNotNull(meta);

        List<String> loreStrings = meta.getLore();
        assertNotNull(loreStrings);
        assertEquals(2, loreStrings.size());
        assertEquals(Component.text("First"), LegacyComponentSerializer.legacySection().deserialize(loreStrings.get(0)));
        assertEquals(Component.text("Second"), LegacyComponentSerializer.legacySection().deserialize(loreStrings.get(1)));
    }

    @Test
    void wrappingPotionInGenericBuilderPreservesPotionMetadata() {
        ModernPotionBuilder potionBuilder = new ModernPotionBuilder();
        ItemStack potion = potionBuilder
                .setBasePotionType(PotionType.REGEN)
                .addCustomEffect(new PotionEffect(PotionEffectType.HEAL, 200, 2), false)
                .build();

        ModernItemBuilder itemBuilder = new ModernItemBuilder(potion);
        ItemStack rebuilt = itemBuilder
                .setName(Component.text("Restorative Brew"))
                .setCustomModelData(77)
                .build();

        assertEquals(Material.POTION, rebuilt.getType());

        PotionMeta meta = (PotionMeta) rebuilt.getItemMeta();
        assertNotNull(meta);
        assertEquals(PotionType.REGEN, meta.getBasePotionData().getType());
        assertTrue(meta.hasCustomEffect(PotionEffectType.HEAL));
        assertEquals(77, meta.getCustomModelData());

        String displayName = meta.getDisplayName();
        assertNotNull(displayName);
        Component actualName = LegacyComponentSerializer.legacySection().deserialize(displayName);
        assertEquals(Component.text("Restorative Brew"), actualName);
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
