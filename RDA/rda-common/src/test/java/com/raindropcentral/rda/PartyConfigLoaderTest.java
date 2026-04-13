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

package com.raindropcentral.rda;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests {@link PartyConfigLoader}.
 */
class PartyConfigLoaderTest {

    @Test
    void parsesDefaultConfigurationValues() {
        final PartyConfig config = PartyConfigLoader.parseConfiguration(new YamlConfiguration(), "party.yml");

        assertEquals(4, config.maxMembers());
        assertEquals(300L, config.inviteTimeoutSeconds());
        assertEquals(0.75D, config.xpShareSettings().selfShare());
        assertEquals(0.25D, config.xpShareSettings().othersTotalShare());
        assertEquals(32.0D, config.xpShareSettings().rangeBlocks());
    }

    @Test
    void supportsUnlimitedCapAndBonusShareValues() throws Exception {
        final YamlConfiguration configuration = new YamlConfiguration();
        configuration.loadFromString("""
            max-members: -1
            invite-timeout-seconds: 900
            xp-share:
              self: 1.0
              others-total: 0.25
              range-blocks: 48.0
            """);

        final PartyConfig config = PartyConfigLoader.parseConfiguration(configuration, "party.yml");

        assertTrue(config.hasUnlimitedMemberCap());
        assertEquals(900L, config.inviteTimeoutSeconds());
        assertEquals(1.0D, config.xpShareSettings().selfShare());
        assertEquals(0.25D, config.xpShareSettings().othersTotalShare());
        assertEquals(48.0D, config.xpShareSettings().rangeBlocks());
    }

    @Test
    void treatsZeroCapAsUnlimited() throws Exception {
        final YamlConfiguration configuration = new YamlConfiguration();
        configuration.loadFromString("""
            max-members: 0
            """);

        final PartyConfig config = PartyConfigLoader.parseConfiguration(configuration, "party.yml");

        assertEquals(0, config.maxMembers());
        assertTrue(config.hasUnlimitedMemberCap());
        assertTrue(config.canAcceptAnotherMember(999));
    }

    @Test
    void rejectsNegativeShareValues() throws Exception {
        final YamlConfiguration configuration = new YamlConfiguration();
        configuration.loadFromString("""
            xp-share:
              self: -0.1
            """);

        assertThrows(
            IllegalStateException.class,
            () -> PartyConfigLoader.parseConfiguration(configuration, "party.yml")
        );
    }

    @Test
    void rejectsNegativeRangeValues() throws Exception {
        final YamlConfiguration configuration = new YamlConfiguration();
        configuration.loadFromString("""
            xp-share:
              range-blocks: -4.0
            """);

        assertThrows(
            IllegalStateException.class,
            () -> PartyConfigLoader.parseConfiguration(configuration, "party.yml")
        );
    }
}
