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

package com.raindropcentral.core.config;

import com.raindropcentral.core.service.central.cookie.DropletCookieDefinitions;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RCentralSectionTest {

    @Test
    void defaultsDropletStoreSettingsToEnabledWhenMissing() {
        final RCentralSection section = RCentralSection.fromConfiguration(load("""
                backendUrl: "https://api.raindropcentral.com"
                """));
        final RCentralConfig.DropletStoreCompatibilitySnapshot snapshot =
                RCentralConfig.resolveDropletStoreCompatibilitySnapshot(section);

        assertTrue(section.isDropletsStoreEnabled());
        for (final String itemCode : DropletCookieDefinitions.allItemCodes()) {
            assertTrue(section.isDropletStoreRewardEnabled(itemCode));
        }
        assertTrue(snapshot.dropletStoreEnabled());
        assertIterableEquals(DropletCookieDefinitions.allItemCodes(), snapshot.enabledItemCodes());
    }

    @Test
    void readsExplicitDropletStoreSettingsFromYaml() {
        final RCentralSection section = RCentralSection.fromConfiguration(load("""
                droplets_store:
                  enabled: false
                  rewards:
                    skill-level-cookie: false
                    job-level-cookie: false
                """));
        final RCentralConfig.DropletStoreCompatibilitySnapshot snapshot =
                RCentralConfig.resolveDropletStoreCompatibilitySnapshot(section);

        assertFalse(section.isDropletsStoreEnabled());
        assertFalse(section.isDropletStoreRewardEnabled("skill-level-cookie"));
        assertFalse(section.isDropletStoreRewardEnabled("job-level-cookie"));
        assertFalse(snapshot.dropletStoreEnabled());
        assertTrue(snapshot.enabledItemCodes().isEmpty());
    }

    @Test
    void omitsDisabledSupportedRewardFromCompatibilitySnapshot() {
        final RCentralSection section = RCentralSection.fromConfiguration(load("""
                droplets_store:
                  enabled: true
                  rewards:
                    skill-level-cookie: false
                    job-level-cookie: true
                """));
        final RCentralConfig.DropletStoreCompatibilitySnapshot snapshot =
                RCentralConfig.resolveDropletStoreCompatibilitySnapshot(section);

        assertTrue(section.isDropletsStoreEnabled());
        assertFalse(section.isDropletStoreRewardEnabled("skill-level-cookie"));
        assertTrue(section.isDropletStoreRewardEnabled("job-level-cookie"));
        assertTrue(snapshot.dropletStoreEnabled());
        assertFalse(snapshot.enabledItemCodes().contains("skill-level-cookie"));
        assertTrue(snapshot.enabledItemCodes().contains("job-level-cookie"));
    }

    private static YamlConfiguration load(final String yaml) {
        final YamlConfiguration configuration = new YamlConfiguration();
        assertDoesNotThrow(() -> configuration.loadFromString(yaml));
        return configuration;
    }
}
