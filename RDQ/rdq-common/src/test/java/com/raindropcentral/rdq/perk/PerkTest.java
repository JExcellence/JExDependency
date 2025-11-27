package com.raindropcentral.rdq.perk;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PerkTest {

    @Test
    void createValidPerk() {
        var perk = new Perk(
            "speed",
            "perk.speed.name",
            "perk.speed.description",
            new PerkType.Toggleable(),
            "combat",
            300,
            60,
            true,
            new PerkEffect.PotionEffect("SPEED", 1),
            "SUGAR",
            List.of()
        );

        assertEquals("speed", perk.id());
        assertEquals("perk.speed.name", perk.displayNameKey());
        assertTrue(perk.enabled());
        assertTrue(perk.hasCooldown());
        assertTrue(perk.hasDuration());
        assertTrue(perk.isToggleable());
        assertFalse(perk.isEventBased());
        assertFalse(perk.isPassive());
    }

    @Test
    void rejectNullId() {
        assertThrows(NullPointerException.class, () ->
            new Perk(null, "name", "desc", new PerkType.Toggleable(), null, 0, 0, true,
                new PerkEffect.PotionEffect("SPEED", 0), "SUGAR", List.of())
        );
    }

    @Test
    void rejectNegativeCooldown() {
        assertThrows(IllegalArgumentException.class, () ->
            new Perk("test", "name", "desc", new PerkType.Toggleable(), null, -1, 0, true,
                new PerkEffect.PotionEffect("SPEED", 0), "SUGAR", List.of())
        );
    }

    @Test
    void rejectNegativeDuration() {
        assertThrows(IllegalArgumentException.class, () ->
            new Perk("test", "name", "desc", new PerkType.Toggleable(), null, 0, -1, true,
                new PerkEffect.PotionEffect("SPEED", 0), "SUGAR", List.of())
        );
    }

    @Test
    void eventBasedPerkType() {
        var perk = new Perk(
            "xp_boost",
            "perk.xp.name",
            "perk.xp.description",
            new PerkType.EventBased("EXPERIENCE_GAIN"),
            "utility",
            0,
            0,
            true,
            new PerkEffect.ExperienceMultiplier(2.0),
            "EXPERIENCE_BOTTLE",
            List.of()
        );

        assertTrue(perk.isEventBased());
        assertFalse(perk.isToggleable());
        assertFalse(perk.isPassive());
        assertFalse(perk.hasCooldown());
        assertFalse(perk.hasDuration());
    }

    @Test
    void passivePerkType() {
        var perk = new Perk(
            "luck",
            "perk.luck.name",
            "perk.luck.description",
            new PerkType.Passive(),
            "utility",
            0,
            0,
            true,
            new PerkEffect.AttributeModifier("GENERIC_LUCK", 1.0, "ADD"),
            "RABBIT_FOOT",
            List.of()
        );

        assertTrue(perk.isPassive());
        assertFalse(perk.isToggleable());
        assertFalse(perk.isEventBased());
    }
}
