package com.raindropcentral.rdq.perk;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PerkEffectTest {

    @Test
    void createPotionEffect() {
        var effect = new PerkEffect.PotionEffect("SPEED", 2);

        assertEquals("SPEED", effect.potionType());
        assertEquals(2, effect.amplifier());
    }

    @Test
    void rejectNullPotionType() {
        assertThrows(NullPointerException.class, () ->
            new PerkEffect.PotionEffect(null, 0)
        );
    }

    @Test
    void rejectNegativeAmplifier() {
        assertThrows(IllegalArgumentException.class, () ->
            new PerkEffect.PotionEffect("SPEED", -1)
        );
    }

    @Test
    void createAttributeModifier() {
        var effect = new PerkEffect.AttributeModifier("GENERIC_MOVEMENT_SPEED", 0.2, "MULTIPLY");

        assertEquals("GENERIC_MOVEMENT_SPEED", effect.attribute());
        assertEquals(0.2, effect.value());
        assertEquals("MULTIPLY", effect.operation());
    }

    @Test
    void createFlightEffect() {
        var effectWithCombat = new PerkEffect.Flight(true);
        var effectWithoutCombat = new PerkEffect.Flight(false);

        assertTrue(effectWithCombat.allowInCombat());
        assertFalse(effectWithoutCombat.allowInCombat());
    }

    @Test
    void createExperienceMultiplier() {
        var effect = new PerkEffect.ExperienceMultiplier(2.5);

        assertEquals(2.5, effect.multiplier());
    }

    @Test
    void rejectNonPositiveMultiplier() {
        assertThrows(IllegalArgumentException.class, () ->
            new PerkEffect.ExperienceMultiplier(0)
        );
        assertThrows(IllegalArgumentException.class, () ->
            new PerkEffect.ExperienceMultiplier(-1.0)
        );
    }

    @Test
    void createDeathPrevention() {
        var effect = new PerkEffect.DeathPrevention(4);

        assertEquals(4, effect.healthOnSave());
    }

    @Test
    void rejectZeroHealthOnSave() {
        assertThrows(IllegalArgumentException.class, () ->
            new PerkEffect.DeathPrevention(0)
        );
    }

    @Test
    void createCustomEffect() {
        var config = Map.<String, Object>of("key", "value", "number", 42);
        var effect = new PerkEffect.Custom("my_handler", config);

        assertEquals("my_handler", effect.handler());
        assertEquals(config, effect.config());
    }

    @Test
    void customEffectWithNullConfigUsesEmptyMap() {
        var effect = new PerkEffect.Custom("handler", null);

        assertNotNull(effect.config());
        assertTrue(effect.config().isEmpty());
    }

    @Test
    void patternMatchingOnEffects() {
        PerkEffect effect = new PerkEffect.PotionEffect("SPEED", 1);

        var result = switch (effect) {
            case PerkEffect.PotionEffect(var type, var amp) -> "potion:" + type + ":" + amp;
            case PerkEffect.AttributeModifier(var attr, var val, var op) -> "attr:" + attr;
            case PerkEffect.Flight(var combat) -> "flight:" + combat;
            case PerkEffect.ExperienceMultiplier(var mult) -> "xp:" + mult;
            case PerkEffect.DeathPrevention(var health) -> "death:" + health;
            case PerkEffect.Custom(var handler, var config) -> "custom:" + handler;
        };

        assertEquals("potion:SPEED:1", result);
    }
}
