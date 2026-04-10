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
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests parsing and default fallback behavior for {@link NationConfigSection}.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
class NationConfigSectionTest {

    @Test
    void createDefaultExposesFormationAndProgressionFallbacks() {
        final NationConfigSection section = NationConfigSection.createDefault();

        assertEquals(1, section.getFormationLevels().size());
        assertEquals(10, section.getProgressionLevels().size());
        assertTrue(section.getFormationLevelDefinition(1).getRequirements().containsKey("nation_charter_funding"));
        assertTrue(section.getProgressionLevelDefinition(1).getRequirements().isEmpty());
        assertNotNull(section.getProgressionLevelDefinition(10));
        assertEquals(10, section.getHighestConfiguredProgressionLevel());
        assertEquals(2, section.getNextProgressionLevel(1));
    }

    @Test
    void legacyTopLevelLevelsStillPopulateFormationConfig() {
        final NationConfigSection section = NationConfigSection.fromInputStream(new ByteArrayInputStream("""
            levels:
              "1":
                requirements:
                  charter:
                    type: CURRENCY
                    currency: vault
                    amount: 1500
                    consumable: true
                rewards:
                  formed:
                    type: COMMAND
                    command: "say formed"
            """.getBytes(StandardCharsets.UTF_8)));

        assertEquals(1, section.getFormationLevels().size());
        assertTrue(section.getFormationLevelDefinition(1).getRequirements().containsKey("charter"));
        assertEquals(10, section.getProgressionLevels().size());
        assertEquals(2, section.getNextProgressionLevel(1));
    }

    @Test
    void formationAndProgressionSectionsParseIndependently() {
        final NationConfigSection section = NationConfigSection.fromInputStream(new ByteArrayInputStream("""
            formation:
              levels:
                "1":
                  requirements:
                    charter:
                      type: CURRENCY
                      currency: vault
                      amount: 2000
                      consumable: true
            progression:
              levels:
                "1":
                  requirements: {}
                  rewards: {}
                "2":
                  requirements:
                    treasury:
                      type: CURRENCY
                      currency: vault
                      amount: 500
                      consumable: true
                  rewards:
                    rebate:
                      type: CURRENCY
                      currencyId: vault
                      amount: 50
            """.getBytes(StandardCharsets.UTF_8)));

        assertEquals(1, section.getFormationLevels().size());
        assertTrue(section.getFormationLevelDefinition(1).getRequirements().containsKey("charter"));
        assertEquals(2, section.getProgressionLevels().size());
        assertTrue(section.getProgressionLevelDefinition(2).getRequirements().containsKey("treasury"));
        assertEquals(1, section.getHighestConfiguredFormationLevel());
        assertEquals(2, section.getHighestConfiguredProgressionLevel());
        assertEquals(1, section.getNextFormationLevel(0));
        assertEquals(2, section.getNextProgressionLevel(1));
    }
}
