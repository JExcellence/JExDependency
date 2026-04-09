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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests parsing and default fallback behavior for {@link ArmoryConfigSection}.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
class ArmoryConfigSectionTest {

    @Test
    void createDefaultExposesExpectedArmoryLevels() {
        final ArmoryConfigSection section = ArmoryConfigSection.createDefault();

        assertEquals(5, section.getHighestConfiguredLevel());
        assertEquals(2, section.getNextLevel(1));
        assertNull(section.getNextLevel(5));
        assertEquals(Material.IRON_BLOCK, section.getBlockMaterial());
        assertTrue(section.getFreeRepair().enabled());
        assertEquals(2, section.getFreeRepair().unlockLevel());
        assertEquals(86_400, section.getFreeRepair().cooldownSeconds());
        assertTrue(section.getSalvageBlock().enabled());
        assertEquals(3, section.getSalvageBlock().unlockLevel());
        assertEquals(Material.GOLD_BLOCK, section.getSalvageBlock().blockMaterial());
        assertEquals(3, section.getSalvageBlock().placementRadiusBlocks());
        assertTrue(section.getRepairBlock().enabled());
        assertEquals(4, section.getRepairBlock().unlockLevel());
        assertEquals(Material.IRON_BLOCK, section.getRepairBlock().blockMaterial());
        assertEquals(1, section.getRepairBlock().iron().materialCost());
        assertEquals(25.0D, section.getRepairBlock().iron().repairPercent());
        assertTrue(section.getDoubleSmelt().enabled());
        assertEquals(5, section.getDoubleSmelt().unlockLevel());
        assertTrue(!section.getDoubleSmelt().enabledByDefault());
        assertEquals(1.0D, section.getDoubleSmelt().burnFasterMultiplier());
        assertEquals(0, section.getDoubleSmelt().extraFuelPerSmeltUnits());

        final LevelDefinition levelTwo = section.getLevelDefinition(2);
        assertNotNull(levelTwo);
        assertTrue(levelTwo.getRequirements().containsKey("vault_upgrade"));
        assertTrue(levelTwo.getRequirements().containsKey("forging_materials"));
        assertTrue(levelTwo.getRewards().containsKey("munition_cache"));
        assertTrue(levelTwo.getRewards().containsKey("town_broadcast"));
    }

    @Test
    void fromInputStreamParsesSparseConfiguredLevels() {
        final ArmoryConfigSection section = ArmoryConfigSection.fromInputStream(new ByteArrayInputStream("""
            block_material: COPPER_BLOCK
            free_repair:
              enabled: false
              unlock_level: 6
              cooldown_seconds: -5
            salvage_block:
              unlock_level: 7
              block_material: ARROW
              placement_radius_blocks: 0
            repair_block:
              unlock_level: 0
              block_material: STICK
              iron:
                material_cost: 2
                repair_percent: 40
              gold:
                material_cost: 3
                repair_percent: 150
              netherite:
                material_cost: -9
                repair_percent: -1
            double_smelt:
              enabled_by_default: true
              unlock_level: 0
              burn_faster_multiplier: 2.5
              extra_fuel_per_smelt_units: -3
            levels:
              "4":
                requirements:
                  " Forge ":
                    type: CURRENCY
                    currencyId: vault
                    amount: 800
                rewards:
                  " Cache ":
                    type: ITEM
                    item:
                      material: ARROW
                      amount: 12
              "9":
                requirements:
                  " Hardware ":
                    type: ITEM
                    requiredItems:
                      - type: CROSSBOW
                        amount: 1
                rewards:
                  " Vault Bonus ":
                    type: CURRENCY
                    currencyId: vault
                    amount: 22
            """.getBytes(StandardCharsets.UTF_8)));

        assertEquals(2, section.getLevels().size());
        assertEquals(9, section.getHighestConfiguredLevel());
        assertEquals(4, section.getNextLevel(1));
        assertEquals(9, section.getNextLevel(4));
        assertNull(section.getNextLevel(9));
        assertEquals(Material.COPPER_BLOCK, section.getBlockMaterial());
        assertTrue(!section.getFreeRepair().enabled());
        assertEquals(6, section.getFreeRepair().unlockLevel());
        assertEquals(86_400, section.getFreeRepair().cooldownSeconds());
        assertEquals(7, section.getSalvageBlock().unlockLevel());
        assertEquals(Material.GOLD_BLOCK, section.getSalvageBlock().blockMaterial());
        assertEquals(3, section.getSalvageBlock().placementRadiusBlocks());
        assertEquals(4, section.getRepairBlock().unlockLevel());
        assertEquals(Material.IRON_BLOCK, section.getRepairBlock().blockMaterial());
        assertEquals(2, section.getRepairBlock().iron().materialCost());
        assertEquals(40.0D, section.getRepairBlock().iron().repairPercent());
        assertEquals(3, section.getRepairBlock().gold().materialCost());
        assertEquals(100.0D, section.getRepairBlock().gold().repairPercent());
        assertEquals(1, section.getRepairBlock().netherite().materialCost());
        assertEquals(25.0D, section.getRepairBlock().netherite().repairPercent());
        assertTrue(section.getDoubleSmelt().enabledByDefault());
        assertEquals(5, section.getDoubleSmelt().unlockLevel());
        assertEquals(2.5D, section.getDoubleSmelt().burnFasterMultiplier());
        assertEquals(0, section.getDoubleSmelt().extraFuelPerSmeltUnits());
    }
}
