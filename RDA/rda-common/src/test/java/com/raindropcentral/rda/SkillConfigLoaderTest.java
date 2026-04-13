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

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests {@link SkillConfigLoader}.
 */
class SkillConfigLoaderTest {

    @Test
    void preservesConfiguredRateOrder() throws Exception {
        final YamlConfiguration configuration = new YamlConfiguration();
        configuration.loadFromString("""
            enabled: true
            display:
              icon: BOW
            rates:
              first:
                trigger: ENTITY_DAMAGE
                xp: 1
                icon: ARROW
                projectile-kind: ARROW
              second:
                trigger: ENTITY_DAMAGE
                xp: 2
                icon: TIPPED_ARROW
                projectile-kind: TIPPED_ARROW
              third:
                trigger: ENTITY_DAMAGE
                xp: 3
                icon: SPECTRAL_ARROW
                projectile-kind: SPECTRAL_ARROW
            """);

        final SkillConfig skillConfig =
            SkillConfigLoader.parseConfiguration(SkillType.ARCHERY, configuration, "test-archery.yml");

        assertEquals(List.of("first", "second", "third"), skillConfig.getRates().stream().map(SkillConfig.RateDefinition::key).toList());
    }

    @Test
    void supportsLegacyBlockXpMigration() throws Exception {
        final YamlConfiguration configuration = new YamlConfiguration();
        configuration.loadFromString("""
            enabled: true
            display:
              icon: DIAMOND_PICKAXE
            block-xp:
              STONE: 1
              DIAMOND_ORE: 32
            """);

        final SkillConfig skillConfig =
            SkillConfigLoader.parseConfiguration(SkillType.MINING, configuration, "test-mining.yml");

        assertEquals(
            List.of(Material.STONE, Material.DIAMOND_ORE),
            skillConfig.getRates().stream().map(rate -> rate.materials().iterator().next()).toList()
        );
    }

    @Test
    void rejectsInvalidMaterialNames() throws Exception {
        final YamlConfiguration configuration = new YamlConfiguration();
        configuration.loadFromString("""
            enabled: true
            display:
              icon: DIAMOND_AXE
            rates:
              broken:
                trigger: BLOCK_BREAK
                xp: 5
                icon: BARRIER
                material: NOT_A_BLOCK
            """);

        assertThrows(
            IllegalStateException.class,
            () -> SkillConfigLoader.parseConfiguration(SkillType.WOODCUTTING, configuration, "test-woodcutting.yml")
        );
    }

    @Test
    void rejectsGroupedMaterialRates() throws Exception {
        final YamlConfiguration configuration = new YamlConfiguration();
        configuration.loadFromString("""
            enabled: true
            display:
              icon: WHEAT
            rates:
              grouped:
                trigger: BLOCK_BREAK
                xp: 8
                icon: WHEAT
                materials:
                  - WHEAT
                  - CARROTS
            """);

        assertThrows(
            IllegalStateException.class,
            () -> SkillConfigLoader.parseConfiguration(SkillType.FARMING, configuration, "test-farming.yml")
        );
    }

    @Test
    void rejectsOverlappingTrackedMaterialsAcrossEnabledSkills() throws Exception {
        final YamlConfiguration miningConfiguration = new YamlConfiguration();
        miningConfiguration.loadFromString("""
            enabled: true
            display:
              icon: DIAMOND_PICKAXE
            rates:
              stone:
                trigger: BLOCK_BREAK
                xp: 1
                icon: STONE
                material: STONE
              oak_log:
                trigger: BLOCK_BREAK
                xp: 1
                icon: OAK_LOG
                material: OAK_LOG
            """);

        final YamlConfiguration woodcuttingConfiguration = new YamlConfiguration();
        woodcuttingConfiguration.loadFromString("""
            enabled: true
            display:
              icon: DIAMOND_AXE
            rates:
              logs:
                trigger: BLOCK_BREAK
                xp: 1
                icon: OAK_LOG
                material: OAK_LOG
            """);

        final EnumMap<SkillType, SkillConfig> configurations = new EnumMap<>(SkillType.class);
        configurations.put(
            SkillType.MINING,
            SkillConfigLoader.parseConfiguration(SkillType.MINING, miningConfiguration, "test-mining.yml")
        );
        configurations.put(
            SkillType.WOODCUTTING,
            SkillConfigLoader.parseConfiguration(SkillType.WOODCUTTING, woodcuttingConfiguration, "test-woodcutting.yml")
        );

        assertThrows(
            IllegalStateException.class,
            () -> SkillConfigLoader.validateTrackedMaterialUniqueness(configurations)
        );
    }

    @Test
    void parsesAbilityTreeDefinitions() throws Exception {
        final YamlConfiguration configuration = new YamlConfiguration();
        configuration.loadFromString("""
            enabled: true
            display:
              icon: DIAMOND_PICKAXE
            rates:
              stone:
                trigger: BLOCK_BREAK
                xp: 1
                icon: STONE
                material: STONE
            ability-point-interval-override: 7
            tree:
              passives:
                quarrying:
                  key: quarrying
                  name: Quarrying
                  description: Improve mining throughput.
                  icon: IRON_PICKAXE
                  primary-stat: STR
                  tiers:
                    t1:
                      required-skill-level: 10
                      required-stat-points: 1
                      base-value: 0.1
                      primary-coefficient: 0.02
                      secondary-coefficient: 0.0
                      hard-cap: 0.3
              active:
                key: super_breaker
                name: Super Breaker
                description: Break matching veins quickly.
                icon: DIAMOND_PICKAXE
                primary-stat: STR
                secondary-stat: SPI
                tiers:
                  t1:
                    required-skill-level: 25
                    required-stat-points: 5
                    base-value: 1.0
                    primary-coefficient: 0.2
                    secondary-coefficient: 0.1
                    hard-cap: 4.0
                active:
                  cooldown-seconds: 60
                  duration-seconds: 10
                  mana-cost: 30
                  activator-item: DIAMOND_PICKAXE
                  allowed-triggers:
                    - COMMAND
                    - RIGHT_CLICK
            """);

        final SkillConfig skillConfig =
            SkillConfigLoader.parseConfiguration(SkillType.MINING, configuration, "test-mining-tree.yml");

        assertEquals(7, skillConfig.getAbilityPointIntervalOverride());
        assertEquals(1, skillConfig.getPassiveAbilities().size());
        assertEquals("quarrying", skillConfig.getPassiveAbilities().getFirst().key());
        assertEquals(CoreStatType.STR, skillConfig.getPassiveAbilities().getFirst().primaryStat());
        assertNotNull(skillConfig.getActiveAbility());
        assertEquals("super_breaker", skillConfig.getActiveAbility().key());
        assertEquals(CoreStatType.SPI, skillConfig.getActiveAbility().secondaryStat());
        assertNotNull(skillConfig.getActiveAbility().activeConfig());
        assertEquals(
            Set.of(ActivationMode.COMMAND, ActivationMode.RIGHT_CLICK),
            skillConfig.getActiveAbility().activeConfig().allowedActivationModes()
        );
    }

    @Test
    void loadsLegacySkillConfigPathWhenCurrentFileIsMissing(final @TempDir Path tempDirectory) throws Exception {
        final Path legacyConfigPath = tempDirectory.resolve(SkillType.MINING.getLegacyResourcePath());
        Files.createDirectories(legacyConfigPath.getParent());
        Files.writeString(legacyConfigPath, """
            enabled: true
            display:
              icon: DIAMOND_PICKAXE
            rates:
              stone:
                trigger: BLOCK_BREAK
                xp: 1
                icon: STONE
                material: STONE
            """);

        final JavaPlugin plugin = mock(JavaPlugin.class);
        when(plugin.getDataFolder()).thenReturn(tempDirectory.toFile());

        final SkillConfig skillConfig = new SkillConfigLoader(plugin, SkillType.MINING).load();

        assertEquals(Material.DIAMOND_PICKAXE, skillConfig.getDisplayIcon());
        assertEquals(1, skillConfig.getRates().size());
        assertEquals(Material.STONE, skillConfig.getRates().getFirst().materials().iterator().next());
    }

    @Test
    void parsesDescriptionTranslationKeysForRatesAndAbilities() throws Exception {
        final YamlConfiguration configuration = new YamlConfiguration();
        configuration.loadFromString("""
            enabled: true
            display:
              icon: DIAMOND_PICKAXE
            rates:
              stone:
                trigger: BLOCK_BREAK
                xp: 1
                icon: STONE
                material: STONE
                description-key: ra_config_descriptions.skills.mining.rates.stone
            tree:
              passives:
                quarrying:
                  key: quarrying
                  name: Quarrying
                  description-key: ra_config_descriptions.skills.mining.abilities.quarrying
                  icon: IRON_PICKAXE
                  primary-stat: STR
                  tiers:
                    t1:
                      required-skill-level: 10
                      required-stat-points: 1
                      base-value: 0.1
                      primary-coefficient: 0.02
                      secondary-coefficient: 0.0
                      hard-cap: 0.3
            """);

        final SkillConfig skillConfig =
            SkillConfigLoader.parseConfiguration(SkillType.MINING, configuration, "translation-keys.yml");

        assertEquals(
            "ra_config_descriptions.skills.mining.rates.stone",
            skillConfig.getRates().getFirst().descriptionTranslationKey()
        );
        assertEquals(
            "ra_config_descriptions.skills.mining.abilities.quarrying",
            skillConfig.getPassiveAbilities().getFirst().descriptionTranslationKey()
        );
    }

    @Test
    void parsesBundledSkillResourcesWithAbilityTrees() throws Exception {
        for (final SkillType skillType : SkillType.values()) {
            try (InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(skillType.getResourcePath())) {
                assertNotNull(inputStream, skillType.getResourcePath());
                final YamlConfiguration configuration = new YamlConfiguration();
                configuration.loadFromString(new String(inputStream.readAllBytes(), StandardCharsets.UTF_8));

                final SkillConfig skillConfig =
                    SkillConfigLoader.parseConfiguration(skillType, configuration, skillType.getResourcePath());

                assertEquals(5, skillConfig.getPassiveAbilities().size(), skillType.name());
                assertNotNull(skillConfig.getActiveAbility(), skillType.name());
            }
        }
    }
}
