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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests parsing and default fallback behavior for {@link SecurityConfigSection}.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
class SecurityConfigSectionTest {

    @Test
    void createDefaultExposesFiveSecurityLevelsWithMixedRequirements() {
        final SecurityConfigSection section = SecurityConfigSection.createDefault();

        assertEquals(5, section.getLevels().size());
        assertEquals(5, section.getHighestConfiguredLevel());
        assertEquals(2, section.getNextLevel(1));
        assertNull(section.getNextLevel(5));

        final LevelDefinition levelTwo = section.getLevelDefinition(2);
        assertNotNull(levelTwo);
        assertTrue(levelTwo.getRequirements().containsKey("vault_upgrade"));
        assertTrue(levelTwo.getRequirements().containsKey("reinforcement_materials"));
        assertTrue(levelTwo.getRewards().containsKey("town_broadcast"));

        assertTrue(section.getFuel().isEnabled());
        assertEquals(60, section.getFuel().getCalculationIntervalSeconds());
        assertEquals(3, section.getFuel().getTankPlacementRadiusBlocks());
        assertEquals(25.0D, section.getFuelDefinition(Material.REDSTONE).units());
        assertEquals(0.45D, section.getFuelChunkTypeDefinition(com.raindropcentral.rdt.utils.ChunkType.SECURITY).minWeight());
    }

    @Test
    void fromInputStreamParsesSparseSecurityLevelsAndMixedDefinitions() {
        final SecurityConfigSection section = SecurityConfigSection.fromInputStream(new ByteArrayInputStream("""
            fuel:
              enabled: false
              offline_protection: true
              calculation_interval_seconds: 90
              tank_placement_radius_blocks: 5
              fuels:
                coal:
                  material: COAL
                  units: 40
              base_rate: 1.25
              chunk_exponent: 1.1
              town_level_rate: 0.05
              minimum_effective_chunk_ratio: 0.8
              chunk_types:
                security:
                  weight: 0.9
                  level_scale: -0.1
                  min_weight: 0.6
            levels:
              "4":
                requirements:
                  " Treasury ":
                    type: CURRENCY
                    currencyId: vault
                    amount: 800
                  " Blocks ":
                    type: ITEM
                    exactMatch: false
                    requiredItems:
                      - type: IRON_INGOT
                        amount: 16
                rewards:
                  " Broadcast ":
                    type: COMMAND
                    command: "rt broadcast {town_uuid} hi"
              "9":
                requirements:
                  " Gems ":
                    type: ITEM
                    requiredItems:
                      - type: DIAMOND
                        amount: 3
                rewards:
                  " Bonus ":
                    type: CURRENCY
                    currencyId: tokens
                    amount: 22
            """.getBytes(StandardCharsets.UTF_8)));

        assertEquals(2, section.getLevels().size());
        assertEquals(9, section.getHighestConfiguredLevel());
        assertEquals(4, section.getNextLevel(1));
        assertEquals(9, section.getNextLevel(4));
        assertNull(section.getNextLevel(9));

        final LevelDefinition levelFour = section.getLevelDefinition(4);
        assertNotNull(levelFour);
        assertTrue(levelFour.getRequirements().containsKey("treasury"));
        assertTrue(levelFour.getRequirements().containsKey("blocks"));
        assertTrue(levelFour.getRewards().containsKey("broadcast"));
        assertEquals("vault", levelFour.getRequirements().get("treasury").get("currencyId"));

        final LevelDefinition levelNine = section.getLevelDefinition(9);
        assertNotNull(levelNine);
        assertTrue(levelNine.getRequirements().containsKey("gems"));
        assertTrue(levelNine.getRewards().containsKey("bonus"));

        assertFalse(section.getFuel().isEnabled());
        assertTrue(section.getFuel().isOfflineProtection());
        assertEquals(90, section.getFuel().getCalculationIntervalSeconds());
        assertEquals(5, section.getFuel().getTankPlacementRadiusBlocks());
        assertEquals(40.0D, section.getFuelDefinition(Material.COAL).units());
        assertEquals(1.25D, section.getFuel().getBaseRate());
        assertEquals(0.6D, section.getFuelChunkTypeDefinition(com.raindropcentral.rdt.utils.ChunkType.SECURITY).minWeight());
    }

    @Test
    void invalidFuelIntervalAndRadiusFallBackToDefaults() {
        final SecurityConfigSection section = SecurityConfigSection.fromInputStream(new ByteArrayInputStream("""
            fuel:
              calculation_interval_seconds: 0
              tank_placement_radius_blocks: -2
            """.getBytes(StandardCharsets.UTF_8)));

        assertEquals(60, section.getFuel().getCalculationIntervalSeconds());
        assertEquals(3, section.getFuel().getTankPlacementRadiusBlocks());
    }

    @Test
    void chunkTypesWithoutConfiguredMinWeightPreserveNullClamp() {
        final SecurityConfigSection section = SecurityConfigSection.fromInputStream(new ByteArrayInputStream("""
            fuel:
              chunk_types:
                base:
                  weight: 1.10
                  level_scale: 0.02
                nexus:
                  weight: 1.45
                  level_scale: 0.03
            """.getBytes(StandardCharsets.UTF_8)));

        final SecurityConfigSection.FuelChunkTypeDefinition baseDefinition =
            section.getFuelChunkTypeDefinition(com.raindropcentral.rdt.utils.ChunkType.DEFAULT);
        final SecurityConfigSection.FuelChunkTypeDefinition nexusDefinition =
            section.getFuelChunkTypeDefinition(com.raindropcentral.rdt.utils.ChunkType.NEXUS);

        assertEquals(1.10D, baseDefinition.weight());
        assertEquals(0.02D, baseDefinition.levelScale());
        assertNull(baseDefinition.minWeight());
        assertEquals(1.45D, nexusDefinition.weight());
        assertEquals(0.03D, nexusDefinition.levelScale());
        assertNull(nexusDefinition.minWeight());
    }
}
