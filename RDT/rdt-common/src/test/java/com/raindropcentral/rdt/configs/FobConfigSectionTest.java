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
 * Tests parsing and default fallback behavior for {@link FobConfigSection}.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
class FobConfigSectionTest {

    @Test
    void createDefaultExposesFiveFobLevelsWithTargetMarker() {
        final FobConfigSection section = FobConfigSection.createDefault();

        assertEquals(5, section.getLevels().size());
        assertEquals(5, section.getHighestConfiguredLevel());
        assertEquals(2, section.getNextLevel(1));
        assertNull(section.getNextLevel(5));
        assertEquals(Material.TARGET, section.getBlockMaterial());

        final LevelDefinition levelThree = section.getLevelDefinition(3);
        assertNotNull(levelThree);
        assertTrue(levelThree.getRequirements().containsKey("targeting_arrays"));
        assertTrue(levelThree.getRewards().containsKey("vault_bonus"));
        assertTrue(levelThree.getRewards().containsKey("town_broadcast"));
    }

    @Test
    void fromInputStreamParsesSparseFobLevelsAndFallsBackForInvalidBlockMaterial() {
        final FobConfigSection section = FobConfigSection.fromInputStream(new ByteArrayInputStream("""
            block_material: HOPPER
            levels:
              "4":
                requirements:
                  "Funding":
                    type: CURRENCY
                    currencyId: vault
                    amount: 5000
                  "Relay Parts":
                    type: ITEM
                    requiredItems:
                      - type: ENDER_EYE
                        amount: 5
                rewards:
                  "Broadcast":
                    type: COMMAND
                    command: "rt broadcast {town_uuid} hi"
            """.getBytes(StandardCharsets.UTF_8)));

        assertEquals(1, section.getLevels().size());
        assertEquals(4, section.getHighestConfiguredLevel());
        assertEquals(4, section.getNextLevel(1));
        assertEquals(Material.TARGET, section.getBlockMaterial());

        final LevelDefinition levelFour = section.getLevelDefinition(4);
        assertNotNull(levelFour);
        assertTrue(levelFour.getRequirements().containsKey("funding"));
        assertTrue(levelFour.getRequirements().containsKey("relay parts"));
        assertTrue(levelFour.getRewards().containsKey("broadcast"));
    }
}
