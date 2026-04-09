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

import com.raindropcentral.rdt.utils.ChunkType;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests parsing and default fallback behavior for {@link ConfigSection}.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
class ConfigSectionTest {

    @Test
    void createDefaultExposesExpectedFallbackValues() {
        final ConfigSection section = ConfigSection.createDefault();

        assertEquals(64, section.getGlobalMaxChunkLimit());
        assertEquals(-10, section.getChunkBlockMinY());
        assertEquals(10, section.getChunkBlockMaxY());
        assertEquals(3, section.getTownSpawnTeleportDelaySeconds());
        assertEquals(86_400L, section.getTownArchetypeChangeCooldownSeconds());
        assertEquals(21_600L, section.getTownRelationshipChangeCooldownSeconds());
        assertEquals(5, section.getTownRelationshipUnlockLevel());
        assertTrue(section.isCornerClaimAdjacencyExcluded());
        assertFalse(section.isProxyEnabled());
        assertFalse(section.isProxyTownSpawnEnabled());
        assertTrue(section.isChunkTypeResetOnChange());
        assertEquals("", section.getProxyServerRouteId());
        assertEquals(Material.REINFORCED_DEEPSLATE, section.getChunkTypeIconMaterial(ChunkType.NEXUS));
        assertEquals(Material.OAK_PLANKS, section.getChunkTypeIconMaterial(ChunkType.DEFAULT));
        assertEquals(Material.OAK_PLANKS, section.getDefaultChunkBlockMaterial());
    }

    @Test
    void fromFileParsesConfiguredValuesAndFallsBackForInvalidOnes(
        final @TempDir Path tempDirectory
    ) throws IOException {
        final Path configFile = tempDirectory.resolve("config.yml");
        Files.writeString(configFile, """
            global_max_chunk_limit: 5
            chunk_block_min_y: 7
            chunk_block_max_y: 4
            town_spawn_teleport_delay_seconds: -1
            exclude_corner_claim_adjacency: false
            chunk_type:
              reset_state_on_change: false
            proxy:
              enabled: true
              server_route_id: "alpha"
              town_spawn_enabled: true
            chunk_type_icon_nexus: DIAMOND_BLOCK
            chunk_type_icon_default: not_a_material
            town:
              archetype_change_cooldown_seconds: 1800
              relationship_change_cooldown_seconds: -5
              relationship_unlock_level: 0
            """);

        final ConfigSection section = ConfigSection.fromFile(configFile.toFile());

        assertEquals(5, section.getGlobalMaxChunkLimit());
        assertEquals(-10, section.getChunkBlockMinY());
        assertEquals(10, section.getChunkBlockMaxY());
        assertEquals(3, section.getTownSpawnTeleportDelaySeconds());
        assertEquals(1_800L, section.getTownArchetypeChangeCooldownSeconds());
        assertEquals(21_600L, section.getTownRelationshipChangeCooldownSeconds());
        assertEquals(5, section.getTownRelationshipUnlockLevel());
        assertFalse(section.isCornerClaimAdjacencyExcluded());
        assertTrue(section.isProxyEnabled());
        assertTrue(section.isProxyTownSpawnEnabled());
        assertFalse(section.isChunkTypeResetOnChange());
        assertEquals("alpha", section.getProxyServerRouteId());
        assertEquals(Material.DIAMOND_BLOCK, section.getChunkTypeIconMaterial(ChunkType.NEXUS));
        assertEquals(Material.OAK_PLANKS, section.getChunkTypeIconMaterial(ChunkType.DEFAULT));
        assertEquals(Material.OAK_PLANKS, section.getDefaultChunkBlockMaterial());
    }
}
