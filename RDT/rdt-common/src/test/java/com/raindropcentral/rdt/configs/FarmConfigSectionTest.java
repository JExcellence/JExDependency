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

import com.raindropcentral.rdt.utils.FarmReplantPriority;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests parsing and default fallback behavior for {@link FarmConfigSection}.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
class FarmConfigSectionTest {

    @Test
    void createDefaultExposesExpectedFarmLevels() {
        final FarmConfigSection section = FarmConfigSection.createDefault();

        assertEquals(5, section.getHighestConfiguredLevel());
        assertEquals(2, section.getNextLevel(1));
        assertNull(section.getNextLevel(5));

        final LevelDefinition levelTwo = section.getLevelDefinition(2);
        assertNotNull(levelTwo);
        assertTrue(levelTwo.getRequirements().containsKey("vault_upgrade"));
        assertTrue(levelTwo.getRequirements().containsKey("harvest_materials"));
        assertTrue(levelTwo.getRewards().containsKey("town_broadcast"));
        assertEquals(Material.HAY_BLOCK, section.getBlockMaterial());
        assertTrue(section.getGrowth().enabledByDefault());
        assertEquals(2, section.getGrowth().tierOneUnlockLevel());
        assertEquals(2.0D, section.getGrowth().tierOneGrowthSpeedMultiplier());
        assertEquals(4, section.getGrowth().tierTwoUnlockLevel());
        assertEquals(3.0D, section.getGrowth().tierTwoGrowthSpeedMultiplier());
        assertTrue(section.getCropFailure().enabled());
        assertEquals(0.5D, section.getCropFailure().failureRate());
        assertEquals(3, section.getSeedBox().unlockLevel());
        assertEquals(3, section.getSeedBox().placementRadiusBlocks());
        assertTrue(section.isAllowedSeedMaterial(Material.WHEAT_SEEDS));
        assertTrue(section.isAllowedSeedMaterial(Material.CARROT));
        assertEquals(3, section.getReplant().unlockLevel());
        assertTrue(section.getReplant().enabledByDefault());
        assertEquals(FarmReplantPriority.INVENTORY_FIRST, section.getReplant().defaultSourcePriority());
        assertEquals(5, section.getDoubleHarvest().unlockLevel());
        assertEquals(2, section.getDoubleHarvest().multiplier());
    }

    @Test
    void fromInputStreamParsesSparseConfiguredLevels() {
        final FarmConfigSection section = FarmConfigSection.fromInputStream(new ByteArrayInputStream("""
            block_material: DRIED_KELP_BLOCK
            growth:
              enabled_by_default: false
              tier_1_unlock_level: 4
              tier_1_growth_speed_multiplier: 1.75
              tier_2_unlock_level: 7
              tier_2_growth_speed_multiplier: 2.5
            crop_failure:
              enabled: false
              failure_rate: 0.25
            seed_box:
              unlock_level: 6
              placement_radius_blocks: 5
              allowed_materials:
                - WHEAT_SEEDS
                - BEETROOT_SEEDS
                - MELON_SEEDS
            replant:
              unlock_level: 6
              enabled_by_default: false
              default_source_priority: SEED_BOX_FIRST
            double_harvest:
              unlock_level: 9
              multiplier: 3
            levels:
              "4":
                requirements:
                  " Seeds ":
                    type: ITEM
                    requiredItems:
                      - type: WHEAT_SEEDS
                        amount: 12
                rewards:
                  " Bonus ":
                    type: CURRENCY
                    currencyId: vault
                    amount: 35
              "9":
                requirements:
                  " Treasury ":
                    type: CURRENCY
                    currencyId: vault
                    amount: 400
                rewards:
                  " Broadcast ":
                    type: COMMAND
                    command: "rt broadcast {town_uuid} farm"
            """.getBytes(StandardCharsets.UTF_8)));

        assertEquals(2, section.getLevels().size());
        assertEquals(9, section.getHighestConfiguredLevel());
        assertEquals(4, section.getNextLevel(1));
        assertEquals(9, section.getNextLevel(4));
        assertNull(section.getNextLevel(9));
        assertEquals(Material.DRIED_KELP_BLOCK, section.getBlockMaterial());
        assertFalse(section.getGrowth().enabledByDefault());
        assertEquals(4, section.getGrowth().tierOneUnlockLevel());
        assertEquals(1.75D, section.getGrowth().tierOneGrowthSpeedMultiplier());
        assertEquals(7, section.getGrowth().tierTwoUnlockLevel());
        assertEquals(2.5D, section.getGrowth().tierTwoGrowthSpeedMultiplier());
        assertFalse(section.getCropFailure().enabled());
        assertEquals(0.25D, section.getCropFailure().failureRate());
        assertEquals(6, section.getSeedBox().unlockLevel());
        assertEquals(5, section.getSeedBox().placementRadiusBlocks());
        assertTrue(section.isAllowedSeedMaterial(Material.WHEAT_SEEDS));
        assertTrue(section.isAllowedSeedMaterial(Material.MELON_SEEDS));
        assertFalse(section.isAllowedSeedMaterial(Material.POTATO));
        assertEquals(6, section.getReplant().unlockLevel());
        assertFalse(section.getReplant().enabledByDefault());
        assertEquals(FarmReplantPriority.SEED_BOX_FIRST, section.getReplant().defaultSourcePriority());
        assertEquals(9, section.getDoubleHarvest().unlockLevel());
        assertEquals(3, section.getDoubleHarvest().multiplier());
    }

    @Test
    void invalidEnhancementValuesFallBackToFarmDefaults() {
        final FarmConfigSection section = FarmConfigSection.fromInputStream(new ByteArrayInputStream("""
            block_material: CHEST
            growth:
              tier_1_unlock_level: 0
              tier_1_growth_speed_multiplier: 0.5
              tier_2_unlock_level: 1
              tier_2_growth_speed_multiplier: 0.0
            crop_failure:
              failure_rate: 4.5
            seed_box:
              unlock_level: -2
              placement_radius_blocks: 0
              allowed_materials:
                - NOT_A_MATERIAL
            replant:
              unlock_level: 0
              default_source_priority: invalid
            double_harvest:
              unlock_level: 0
              multiplier: -4
            """.getBytes(StandardCharsets.UTF_8)));

        assertEquals(Material.HAY_BLOCK, section.getBlockMaterial());
        assertEquals(2, section.getGrowth().tierOneUnlockLevel());
        assertEquals(2.0D, section.getGrowth().tierOneGrowthSpeedMultiplier());
        assertEquals(2, section.getGrowth().tierTwoUnlockLevel());
        assertEquals(3.0D, section.getGrowth().tierTwoGrowthSpeedMultiplier());
        assertTrue(section.getCropFailure().enabled());
        assertEquals(1.0D, section.getCropFailure().failureRate());
        assertEquals(3, section.getSeedBox().unlockLevel());
        assertEquals(3, section.getSeedBox().placementRadiusBlocks());
        assertTrue(section.isAllowedSeedMaterial(Material.WHEAT_SEEDS));
        assertEquals(3, section.getReplant().unlockLevel());
        assertEquals(FarmReplantPriority.INVENTORY_FIRST, section.getReplant().defaultSourcePriority());
        assertEquals(5, section.getDoubleHarvest().unlockLevel());
        assertEquals(2, section.getDoubleHarvest().multiplier());
    }

    @Test
    void legacyExtraGrowthStepKeysMapToSpeedMultipliers() {
        final FarmConfigSection section = FarmConfigSection.fromInputStream(new ByteArrayInputStream("""
            growth:
              tier_1_extra_growth_steps: 2
              tier_2_extra_growth_steps: 4
            """.getBytes(StandardCharsets.UTF_8)));

        assertEquals(3.0D, section.getGrowth().tierOneGrowthSpeedMultiplier());
        assertEquals(5.0D, section.getGrowth().tierTwoGrowthSpeedMultiplier());
    }

    @Test
    void cropFailureProbabilityClampsAtZero() {
        final FarmConfigSection section = FarmConfigSection.fromInputStream(new ByteArrayInputStream("""
            crop_failure:
              failure_rate: -0.25
            """.getBytes(StandardCharsets.UTF_8)));

        assertTrue(section.getCropFailure().enabled());
        assertEquals(0.0D, section.getCropFailure().failureRate());
    }
}
