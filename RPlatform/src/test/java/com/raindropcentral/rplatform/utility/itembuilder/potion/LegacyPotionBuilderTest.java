package com.raindropcentral.rplatform.utility.itembuilder.potion;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LegacyPotionBuilderTest {

    private ServerMock server;

    @BeforeEach
    void setUp() {
        this.server = MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
        this.server = null;
    }

    @Test
    void legacyPotionBuilderAppliesBaseTypeAndCustomEffects() {
        LegacyPotionBuilder builder = new LegacyPotionBuilder();

        PotionEffect speedEffect = new PotionEffect(PotionEffectType.SPEED, 600, 1, true, true, true);
        ItemStack result = builder.setBasePotionType(PotionType.SPEED)
                .addCustomEffect(speedEffect, true)
                .build();

        assertEquals(Material.POTION, result.getType());

        PotionMeta meta = (PotionMeta) result.getItemMeta();
        assertNotNull(meta);
        assertTrue(meta.hasCustomEffect(PotionEffectType.SPEED));

        List<PotionEffect> customEffects = meta.getCustomEffects();
        assertEquals(1, customEffects.size());

        PotionEffect storedEffect = customEffects.get(0);
        assertEquals(speedEffect.getType(), storedEffect.getType());
        assertEquals(speedEffect.getDuration(), storedEffect.getDuration());
        assertEquals(speedEffect.getAmplifier(), storedEffect.getAmplifier());
        assertEquals(speedEffect.isAmbient(), storedEffect.isAmbient());
        assertEquals(speedEffect.hasParticles(), storedEffect.hasParticles());
        assertEquals(speedEffect.hasIcon(), storedEffect.hasIcon());

        Color expectedColor = PotionEffectType.SPEED.getColor();
        assertEquals(expectedColor, meta.getColor());
    }

    @Test
    void legacyPotionBuilderProducesSimilarMetadataAsModernBuilder() {
        PotionEffect regeneration = new PotionEffect(PotionEffectType.REGENERATION, 400, 2, true, false, true);

        ItemStack legacyPotion = new LegacyPotionBuilder()
                .setBasePotionType(PotionType.REGEN)
                .addCustomEffect(regeneration, true)
                .build();

        ItemStack modernPotion = new ModernPotionBuilder()
                .setBasePotionType(PotionType.REGEN)
                .addCustomEffect(regeneration, true)
                .build();

        assertEquals(Material.POTION, legacyPotion.getType());
        assertEquals(modernPotion.getType(), legacyPotion.getType());

        PotionMeta legacyMeta = (PotionMeta) legacyPotion.getItemMeta();
        PotionMeta modernMeta = (PotionMeta) modernPotion.getItemMeta();
        assertNotNull(legacyMeta);
        assertNotNull(modernMeta);

        assertTrue(legacyMeta.hasCustomEffect(PotionEffectType.REGENERATION));
        assertTrue(modernMeta.hasCustomEffect(PotionEffectType.REGENERATION));
        assertEquals(legacyMeta.getCustomEffects(), modernMeta.getCustomEffects());
        assertEquals(PotionType.REGEN, modernMeta.getBasePotionData().getType());

        Color expectedColor = PotionEffectType.REGENERATION.getColor();
        assertEquals(expectedColor, legacyMeta.getColor());
        assertEquals(expectedColor, modernMeta.getColor());
    }

    @Test
    void legacyPotionBuilderGracefullyHandlesTypesWithoutEffects() {
        ItemStack potion = new LegacyPotionBuilder()
                .setBasePotionType(PotionType.WATER)
                .build();

        PotionMeta meta = (PotionMeta) potion.getItemMeta();
        assertNotNull(meta);
        assertFalse(meta.hasCustomEffects());
        assertTrue(meta.getCustomEffects().isEmpty());
        assertNull(meta.getColor());
    }
}
