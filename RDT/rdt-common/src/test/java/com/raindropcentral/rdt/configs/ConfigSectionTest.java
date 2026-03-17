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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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
        assertFalse(section.isProxyEnabled());
        assertFalse(section.isProxyTownSpawnEnabled());
        assertEquals("", section.getProxyServerRouteId());
        assertEquals(Material.REINFORCED_DEEPSLATE, section.getChunkTypeIconMaterial(ChunkType.NEXUS));
        assertEquals(Material.OAK_PLANKS, section.getChunkTypeIconMaterial(ChunkType.DEFAULT));
        assertEquals(10, section.getHighestConfiguredTownLevel());
        assertEquals(2, section.getNextTownLevel(1));
        assertNull(section.getNextTownLevel(10));

        final ConfigSection.TownLevelSection levelTwo = section.getTownLevelSection(2);
        assertNotNull(levelTwo);
        assertTrue(levelTwo.getRequirements().containsKey("vault_upgrade"));
        assertTrue(levelTwo.getRewards().containsKey("vault_bonus"));
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
            proxy:
              enabled: true
              server_route_id: "alpha"
              town_spawn_enabled: true
            chunk_type_icon_nexus: DIAMOND_BLOCK
            chunk_type_icon_default: not_a_material
            town:
              levels:
                "3":
                  requirements:
                    " Vault_Req ":
                      type: CURRENCY
                      amount: 123.45
                  rewards:
                    " Bonus ":
                      type: CURRENCY
                      amount: 22
                "abc":
                  requirements: {}
            """);

        final ConfigSection section = ConfigSection.fromFile(configFile.toFile());

        assertEquals(5, section.getGlobalMaxChunkLimit());
        assertEquals(-10, section.getChunkBlockMinY());
        assertEquals(10, section.getChunkBlockMaxY());
        assertEquals(3, section.getTownSpawnTeleportDelaySeconds());
        assertTrue(section.isProxyEnabled());
        assertTrue(section.isProxyTownSpawnEnabled());
        assertEquals("alpha", section.getProxyServerRouteId());
        assertEquals(Material.DIAMOND_BLOCK, section.getChunkTypeIconMaterial(ChunkType.NEXUS));
        assertEquals(Material.OAK_PLANKS, section.getChunkTypeIconMaterial(ChunkType.DEFAULT));

        assertEquals(1, section.getTownLevels().size());
        assertEquals(3, section.getHighestConfiguredTownLevel());
        assertEquals(3, section.getNextTownLevel(1));
        assertNull(section.getNextTownLevel(3));

        final ConfigSection.TownLevelSection levelThree = section.getTownLevelSection(3);
        assertNotNull(levelThree);
        assertTrue(levelThree.getRequirements().containsKey("vault_req"));
        assertTrue(levelThree.getRewards().containsKey("bonus"));
    }

    @Test
    void townLevelSectionPerformsDeepCopiesAndClampsLevel() {
        final Map<String, Object> nestedRequirement = new LinkedHashMap<>();
        nestedRequirement.put("type", "CURRENCY");
        nestedRequirement.put("list", new ArrayList<>(List.of("A")));

        final Map<String, Map<String, Object>> requirements = new LinkedHashMap<>();
        requirements.put("req", nestedRequirement);

        final ConfigSection.TownLevelSection section = new ConfigSection.TownLevelSection(
            0,
            requirements,
            Map.of()
        );

        assertEquals(1, section.level());

        @SuppressWarnings("unchecked")
        final List<Object> returnedList =
            (List<Object>) section.getRequirements().get("req").get("list");
        returnedList.add("B");

        @SuppressWarnings("unchecked")
        final List<Object> secondReadList =
            (List<Object>) section.getRequirements().get("req").get("list");
        assertEquals(List.of("A"), secondReadList);
    }
}
