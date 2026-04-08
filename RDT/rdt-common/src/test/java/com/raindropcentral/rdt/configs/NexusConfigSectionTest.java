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
 * Tests parsing and default fallback behavior for {@link NexusConfigSection}.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
class NexusConfigSectionTest {

    @Test
    void createDefaultExposesExpectedFallbackLevels() {
        final NexusConfigSection section = NexusConfigSection.createDefault();

        assertEquals(10, section.getHighestConfiguredLevel());
        assertEquals(2, section.getNextLevel(1));
        assertNull(section.getNextLevel(10));

        final LevelDefinition levelOne = section.getLevelDefinition(1);
        assertNotNull(levelOne);
        assertTrue(levelOne.getRequirements().containsKey("town_charter"));
        assertTrue(levelOne.getRewards().containsKey("town_broadcast"));
        assertEquals("vault", levelOne.getRequirements().get("town_charter").get("currency"));

        final LevelDefinition levelTwo = section.getLevelDefinition(2);
        assertNotNull(levelTwo);
        assertTrue(levelTwo.getRequirements().containsKey("vault_upgrade"));
        assertTrue(levelTwo.getRewards().containsKey("vault_bonus"));
        assertTrue(levelTwo.getRewards().containsKey("town_broadcast"));
        assertEquals("vault", levelTwo.getRequirements().get("vault_upgrade").get("currency"));
    }

    @Test
    void fromInputStreamParsesSparseConfiguredLevels() {
        final NexusConfigSection section = NexusConfigSection.fromInputStream(new ByteArrayInputStream("""
            levels:
              "3":
                requirements:
                  " Vault Req ":
                    type: CURRENCY
                    currencyId: tokens
                    amount: 123.45
                    description: "Token funding"
                rewards:
                  " Bonus ":
                    type: COMMAND
                    command: "rt broadcast {town_uuid} test"
              "7":
                requirements:
                  " Rare Item ":
                    type: ITEM
                    exactMatch: true
                    requiredItems:
                      - type: DIAMOND
                        amount: 4
                rewards:
                  " Vault Bonus ":
                    type: CURRENCY
                    currencyId: vault
                    amount: 50
              "abc":
                requirements: {}
            """.getBytes(StandardCharsets.UTF_8)));

        assertEquals(2, section.getLevels().size());
        assertEquals(7, section.getHighestConfiguredLevel());
        assertEquals(3, section.getNextLevel(1));
        assertEquals(7, section.getNextLevel(3));
        assertNull(section.getNextLevel(7));

        final LevelDefinition levelThree = section.getLevelDefinition(3);
        assertNotNull(levelThree);
        assertTrue(levelThree.getRequirements().containsKey("vault req"));
        assertTrue(levelThree.getRewards().containsKey("bonus"));
        assertEquals("tokens", levelThree.getRequirements().get("vault req").get("currencyId"));
        assertEquals("rt broadcast {town_uuid} test", levelThree.getRewards().get("bonus").get("command"));

        final LevelDefinition levelSeven = section.getLevelDefinition(7);
        assertNotNull(levelSeven);
        assertTrue(levelSeven.getRequirements().containsKey("rare item"));
        assertTrue(levelSeven.getRewards().containsKey("vault bonus"));
    }
}
