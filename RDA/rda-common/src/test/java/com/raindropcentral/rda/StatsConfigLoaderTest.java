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

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests {@link StatsConfigLoader}.
 */
class StatsConfigLoaderTest {

    @Test
    void parsesStatsConfigurationAndOverrides() {
        final YamlConfiguration configuration = new YamlConfiguration();
        configuration.set("ability-points.default-interval", 5);
        configuration.set("ability-points.skill-overrides.MINING", 7);
        configuration.set("activation.allowed-modes", List.of("COMMAND", "RIGHT_CLICK"));
        configuration.set("respec.cooldown-seconds", 60);
        configuration.set("respec.point-tax-percent", 10);
        configuration.set("mana.base", 120.0D);
        configuration.set("mana.per-spi-point", 6.0D);
        configuration.set("mana.base-regen-per-second", 2.0D);
        configuration.set("mana.regen-per-spi-point", 0.5D);
        configuration.set("mana.default-display-mode", "BOSS_BAR");
        configuration.set("mana.low-threshold-percent", 15.0D);

        for (final CoreStatType coreStatType : CoreStatType.values()) {
            final String statPath = "stats." + coreStatType.getId();
            configuration.set(statPath + ".icon", coreStatType.getFallbackIcon().name());
            configuration.set(statPath + ".description", coreStatType.name());
            configuration.set(statPath + ".passive.label", coreStatType.name());
            configuration.set(statPath + ".passive.unit", "x");
            configuration.set(statPath + ".passive.base", 0.0D);
            configuration.set(statPath + ".passive.full-rate", 1.0D);
            configuration.set(statPath + ".passive.half-rate", 0.5D);
            configuration.set(statPath + ".passive.quarter-rate", 0.25D);
            configuration.set(statPath + ".passive.first-soft-cap", 10);
            configuration.set(statPath + ".passive.second-soft-cap", 20);
            configuration.set(statPath + ".passive.max", 30.0D);
        }
        configuration.set("stats.vit.description-key", "ra_config_descriptions.stats.vit");

        final StatsConfig statsConfig = StatsConfigLoader.parseConfiguration(configuration, "test-stats.yml");

        assertEquals(5, statsConfig.getDefaultAbilityPointInterval());
        assertEquals(7, statsConfig.getAbilityPointInterval(SkillType.MINING));
        assertEquals("ra_config_descriptions.stats.vit", statsConfig.getStatDefinition(CoreStatType.VIT).loreDescriptionTranslationKey());
        assertEquals(12.5D, statsConfig.getStatDefinition(CoreStatType.VIT).resolvePassiveValue(15), 0.0001D);
        assertEquals(EnumSet.of(ActivationMode.COMMAND, ActivationMode.RIGHT_CLICK), statsConfig.getAllowedActivationModes());
        assertEquals(60L, statsConfig.getRespecSettings().cooldownSeconds());
        assertEquals(10, statsConfig.getRespecSettings().pointTaxPercent());
        assertEquals(ManaDisplayMode.BOSS_BAR, statsConfig.getManaSettings().defaultDisplayMode());
        assertEquals(150.0D, statsConfig.getManaSettings().resolveMaxMana(5), 0.0001D);
    }

    @Test
    void parsesBundledStatsResourceWithoutLegacyPrestigeSections() throws Exception {
        try (InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("stats.yml")) {
            assertNotNull(inputStream);
            final String content = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            assertFalse(content.contains("skill-links:"));
            assertFalse(content.contains("feeders:"));
            assertFalse(content.contains("refund:"));
            assertTrue(content.contains("description-key:"));

            final YamlConfiguration configuration = new YamlConfiguration();
            configuration.loadFromString(content);

            final StatsConfig statsConfig = StatsConfigLoader.parseConfiguration(configuration, "stats.yml");

            assertEquals(5, statsConfig.getDefaultAbilityPointInterval());
            assertEquals("ra_config_descriptions.stats.cha", statsConfig.getStatDefinition(CoreStatType.CHA).loreDescriptionTranslationKey());
            assertEquals(ManaDisplayMode.ACTION_BAR, statsConfig.getManaSettings().defaultDisplayMode());
        }
    }
}
