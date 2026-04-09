/*
 * Copyright (c) 2021-2026 Antimatter Zone LLC. All rights reserved.
 *
 * This source code is proprietary and confidential to Antimatter Zone LLC.
 * Unauthorized copying, modification, distribution, display, performance,
 * publication, sublicensing, or creation of derivative works is prohibited
 * without prior written permission from Antimatter Zone LLC, except to the
 * extent permitted by applicable United States law.
 *
 * This notice is intended to preserve all rights and remedies available under
 * the laws of the State of Washington and the United States of America.
 */

package com.raindropcentral.rdt.configs;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests parsing and default fallback behavior for {@link MedicConfigSection}.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
class MedicConfigSectionTest {

    @Test
    void createDefaultExposesExpectedMedicLevels() {
        final MedicConfigSection section = MedicConfigSection.createDefault();

        assertEquals(5, section.getHighestConfiguredLevel());
        assertEquals(2, section.getNextLevel(1));
        assertNull(section.getNextLevel(5));
        assertEquals(Material.SEA_LANTERN, section.getBlockMaterial());

        final LevelDefinition levelTwo = section.getLevelDefinition(2);
        assertNotNull(levelTwo);
        assertTrue(levelTwo.getRequirements().containsKey("vault_upgrade"));
        assertTrue(levelTwo.getRequirements().containsKey("clinic_supplies"));
        assertTrue(levelTwo.getRewards().containsKey("field_supplies"));
        assertTrue(levelTwo.getRewards().containsKey("town_broadcast"));
        assertEquals(1, section.getFoodRegen().unlockLevel());
        assertEquals(40, section.getFoodRegen().intervalTicks());
        assertEquals(2, section.getHealthRegen().unlockLevel());
        assertEquals(3, section.getCleanse().unlockLevel());
        assertEquals(4, section.getFortifiedRecovery().unlockLevel());
        assertEquals(300, section.getFortifiedRecovery().durationSeconds());
        assertEquals(5, section.getEmergencyRefill().unlockLevel());
        assertEquals(MedicConfigSection.TargetHealthMode.CURRENT_MAX, section.getEmergencyRefill().targetHealthMode());
    }

    @Test
    void fromInputStreamParsesSparseConfiguredLevels() {
        final MedicConfigSection section = MedicConfigSection.fromInputStream(new ByteArrayInputStream("""
            block_material: GLOWSTONE
            levels:
              "3":
                requirements:
                  " Triage ":
                    type: CURRENCY
                    currencyId: vault
                    amount: 700
                rewards:
                  " Supplies ":
                    type: ITEM
                    item:
                      material: HONEY_BOTTLE
                      amount: 2
              "7":
                requirements:
                  " Gems ":
                    type: ITEM
                    requiredItems:
                      - type: EMERALD
                        amount: 4
                rewards:
                  " Vault Bonus ":
                    type: CURRENCY
                    currencyId: vault
                    amount: 50
            """.getBytes(StandardCharsets.UTF_8)));

        assertEquals(2, section.getLevels().size());
        assertEquals(7, section.getHighestConfiguredLevel());
        assertEquals(3, section.getNextLevel(1));
        assertEquals(7, section.getNextLevel(3));
        assertNull(section.getNextLevel(7));
        assertEquals(Material.GLOWSTONE, section.getBlockMaterial());
    }

    @Test
    void fromInputStreamParsesMedicPerkSectionsAndSanitizesInvalidValues() {
        final MedicConfigSection section = MedicConfigSection.fromInputStream(new ByteArrayInputStream("""
            food_regen:
              unlock_level: 2
              interval_ticks: -5
              food_points_per_pulse: 3
              saturation_per_pulse: 1.25
            health_regen:
              unlock_level: 4
              health_points_per_pulse: 2.5
            cleanse:
              unlock_level: 5
              interval_ticks: 60
              harmful_effects:
                - POISON
                - invalid_effect
                - WITHER
            fortified_recovery:
              unlock_level: 6
              duration_seconds: 120
              target_max_health: 50.0
              upkeep_interval_ticks: 10
              target_food_level: 18
              target_saturation: 0.0
            emergency_refill:
              unlock_level: 7
              cooldown_seconds: 0
              target_health_mode: vanilla_max
              target_food_level: 0
              target_saturation: 0.0
            """.getBytes(StandardCharsets.UTF_8)));

        assertEquals(2, section.getFoodRegen().unlockLevel());
        assertEquals(40, section.getFoodRegen().intervalTicks());
        assertEquals(3, section.getFoodRegen().foodPointsPerPulse());
        assertEquals(1.25D, section.getFoodRegen().saturationPerPulse());
        assertEquals(4, section.getHealthRegen().unlockLevel());
        assertEquals(2.5D, section.getHealthRegen().healthPointsPerPulse());
        assertEquals(5, section.getCleanse().unlockLevel());
        assertEquals(60, section.getCleanse().intervalTicks());
        assertEquals(Set.of(
            "poison",
            "wither"
        ), section.getCleanse().harmfulEffects());
        assertEquals(6, section.getFortifiedRecovery().unlockLevel());
        assertEquals(120, section.getFortifiedRecovery().durationSeconds());
        assertEquals(50.0D, section.getFortifiedRecovery().targetMaxHealth());
        assertEquals(10, section.getFortifiedRecovery().upkeepIntervalTicks());
        assertEquals(18, section.getFortifiedRecovery().targetFoodLevel());
        assertEquals(0.0D, section.getFortifiedRecovery().targetSaturation());
        assertEquals(7, section.getEmergencyRefill().unlockLevel());
        assertEquals(0, section.getEmergencyRefill().cooldownSeconds());
        assertEquals(MedicConfigSection.TargetHealthMode.VANILLA_MAX, section.getEmergencyRefill().targetHealthMode());
        assertEquals(0, section.getEmergencyRefill().targetFoodLevel());
        assertEquals(0.0D, section.getEmergencyRefill().targetSaturation());
    }

    @Test
    void cleanseFallsBackToDefaultHarmfulEffectsWhenSectionIsEmpty() {
        final MedicConfigSection section = MedicConfigSection.fromInputStream(new ByteArrayInputStream("""
            cleanse:
              harmful_effects: []
            """.getBytes(StandardCharsets.UTF_8)));

        assertTrue(section.getCleanse().harmfulEffects().contains("poison"));
        assertTrue(section.getCleanse().harmfulEffects().contains("wither"));
    }
}
