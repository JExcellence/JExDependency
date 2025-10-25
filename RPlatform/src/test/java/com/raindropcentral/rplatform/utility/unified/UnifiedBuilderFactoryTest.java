package com.raindropcentral.rplatform.utility.unified;

import org.mockbukkit.mockbukkit.MockBukkit;
import com.raindropcentral.rplatform.utility.itembuilder.LegacyItemBuilder;
import com.raindropcentral.rplatform.utility.itembuilder.ModernItemBuilder;
import com.raindropcentral.rplatform.utility.itembuilder.potion.LegacyPotionBuilder;
import com.raindropcentral.rplatform.utility.itembuilder.potion.ModernPotionBuilder;
import com.raindropcentral.rplatform.utility.itembuilder.skull.LegacyHeadBuilder;
import com.raindropcentral.rplatform.utility.itembuilder.skull.ModernHeadBuilder;
import com.raindropcentral.rplatform.version.ServerEnvironment;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

class UnifiedBuilderFactoryTest {

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
        resetServerEnvironment();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
        resetServerEnvironment();
    }

    @Test
    void headFactoryReturnsModernBuilderWhenPaperDetected() {
        installServerEnvironment(true);

        ModernHeadBuilder builder = assertInstanceOf(ModernHeadBuilder.class, UnifiedBuilderFactory.head());
        ModernHeadBuilder expectedBuilder = new ModernHeadBuilder();

        Component label = Component.text("Modern Head");
        ItemStack expected = expectedBuilder
                .setName(label)
                .setAmount(2)
                .build();
        ItemStack actual = builder
                .setName(label)
                .setAmount(2)
                .build();

        assertEquals(expected, actual);
    }

    @Test
    void headFactoryReturnsLegacyBuilderWhenPaperUnavailable() {
        installServerEnvironment(false);

        LegacyHeadBuilder builder = assertInstanceOf(LegacyHeadBuilder.class, UnifiedBuilderFactory.head());
        LegacyHeadBuilder expectedBuilder = new LegacyHeadBuilder();

        Component label = Component.text("Legacy Head");
        ItemStack expected = expectedBuilder
                .setName(label)
                .setAmount(3)
                .build();
        ItemStack actual = builder
                .setName(label)
                .setAmount(3)
                .build();

        assertEquals(expected, actual);
    }

    @Test
    void potionFactoryReturnsModernBuilderWhenPaperDetected() {
        installServerEnvironment(true);

        ModernPotionBuilder builder = assertInstanceOf(ModernPotionBuilder.class, UnifiedBuilderFactory.potion());
        ModernPotionBuilder expectedBuilder = new ModernPotionBuilder();

        PotionEffect heal = new PotionEffect(PotionEffectType.HEAL, 200, 1);
        ItemStack expected = expectedBuilder
                .setBasePotionType(PotionType.INSTANT_HEAL)
                .addCustomEffect(heal, true)
                .setAmount(2)
                .build();
        ItemStack actual = builder
                .setBasePotionType(PotionType.INSTANT_HEAL)
                .addCustomEffect(heal, true)
                .setAmount(2)
                .build();

        assertEquals(expected, actual);
    }

    @Test
    void potionFactoryReturnsLegacyBuilderWhenPaperUnavailable() {
        installServerEnvironment(false);

        LegacyPotionBuilder builder = assertInstanceOf(LegacyPotionBuilder.class, UnifiedBuilderFactory.potion());
        LegacyPotionBuilder expectedBuilder = new LegacyPotionBuilder();

        PotionEffect regeneration = new PotionEffect(PotionEffectType.REGENERATION, 300, 2);
        ItemStack expected = expectedBuilder
                .setBasePotionType(PotionType.REGEN)
                .addCustomEffect(regeneration, false)
                .setAmount(1)
                .build();
        ItemStack actual = builder
                .setBasePotionType(PotionType.REGEN)
                .addCustomEffect(regeneration, false)
                .setAmount(1)
                .build();

        assertEquals(expected, actual);
    }

    @Test
    void itemFactoryReturnsModernBuilderWhenPaperDetected() {
        installServerEnvironment(true);

        ModernItemBuilder builder = assertInstanceOf(ModernItemBuilder.class, UnifiedBuilderFactory.item(Material.DIAMOND_SWORD));
        ModernItemBuilder expectedBuilder = new ModernItemBuilder(Material.DIAMOND_SWORD);

        Component displayName = Component.text("Modern Blade");
        ItemStack expected = expectedBuilder
                .setName(displayName)
                .setAmount(4)
                .build();
        ItemStack actual = builder
                .setName(displayName)
                .setAmount(4)
                .build();

        assertEquals(expected, actual);

        ItemStack base = new ItemStack(Material.GOLDEN_APPLE);
        ModernItemBuilder fromStack = assertInstanceOf(ModernItemBuilder.class, UnifiedBuilderFactory.item(base));
        ModernItemBuilder expectedFromStack = new ModernItemBuilder(base.clone());

        ItemStack expectedStack = expectedFromStack
                .setCustomModelData(42)
                .setAmount(5)
                .build();
        ItemStack actualStack = fromStack
                .setCustomModelData(42)
                .setAmount(5)
                .build();

        assertEquals(expectedStack, actualStack);
    }

    @Test
    void itemFactoryReturnsLegacyBuilderWhenPaperUnavailable() {
        installServerEnvironment(false);

        LegacyItemBuilder builder = assertInstanceOf(LegacyItemBuilder.class, UnifiedBuilderFactory.item(Material.IRON_SWORD));
        LegacyItemBuilder expectedBuilder = new LegacyItemBuilder(Material.IRON_SWORD);

        Component displayName = Component.text("Legacy Blade");
        ItemStack expected = expectedBuilder
                .setName(displayName)
                .setAmount(6)
                .build();
        ItemStack actual = builder
                .setName(displayName)
                .setAmount(6)
                .build();

        assertEquals(expected, actual);

        ItemStack base = new ItemStack(Material.APPLE);
        LegacyItemBuilder fromStack = assertInstanceOf(LegacyItemBuilder.class, UnifiedBuilderFactory.item(base));
        LegacyItemBuilder expectedFromStack = new LegacyItemBuilder(base.clone());

        ItemStack expectedStack = expectedFromStack
                .setCustomModelData(7)
                .setAmount(2)
                .build();
        ItemStack actualStack = fromStack
                .setCustomModelData(7)
                .setAmount(2)
                .build();

        assertEquals(expectedStack, actualStack);
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
