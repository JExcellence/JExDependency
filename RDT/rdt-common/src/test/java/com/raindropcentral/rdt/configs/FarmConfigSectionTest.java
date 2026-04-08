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

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    }

    @Test
    void fromInputStreamParsesSparseConfiguredLevels() {
        final FarmConfigSection section = FarmConfigSection.fromInputStream(new ByteArrayInputStream("""
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
    }
}
