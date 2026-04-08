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
 * Tests parsing and default fallback behavior for {@link BankConfigSection}.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
class BankConfigSectionTest {

    @Test
    void createDefaultExposesExpectedBankLevels() {
        final BankConfigSection section = BankConfigSection.createDefault();

        assertEquals(5, section.getHighestConfiguredLevel());
        assertEquals(2, section.getNextLevel(1));
        assertNull(section.getNextLevel(5));

        final LevelDefinition levelTwo = section.getLevelDefinition(2);
        assertNotNull(levelTwo);
        assertTrue(levelTwo.getRequirements().containsKey("vault_upgrade"));
        assertTrue(levelTwo.getRequirements().containsKey("reserve_materials"));
        assertTrue(levelTwo.getRewards().containsKey("town_broadcast"));
    }

    @Test
    void fromInputStreamParsesSparseConfiguredLevels() {
        final BankConfigSection section = BankConfigSection.fromInputStream(new ByteArrayInputStream("""
            levels:
              "3":
                requirements:
                  " Vault Req ":
                    type: CURRENCY
                    currencyId: vault
                    amount: 700
                rewards:
                  " Bonus ":
                    type: COMMAND
                    command: "rt broadcast {town_uuid} bank"
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
    }
}
