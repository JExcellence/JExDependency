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

package com.raindropcentral.rdr.view;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Tests default PLUGIN requirement insertion for storage purchase tiers.
 *
 * @author ItsRainingHP
 * @since 5.0.0
 * @version 5.0.0
 */
class StorageRequirementConfigSupportTest {

    @TempDir
    Path tempDir;

    @Test
    void appliesSkillRequirementToAllDefaultStorageTiers() throws IOException {
        final File configFile = this.tempDir.resolve("config.yml").toFile();
        Files.writeString(
            configFile.toPath(),
            """
                requirements:
                  1:
                    vault_purchase:
                      type: "CURRENCY"
                      currency: vault
                      amount: 1000.0
                  2:
                    coins_purchase:
                      type: "CURRENCY"
                      currency: raindrops
                      amount: 50.0
                """
        );

        final int updatedTiers = StorageRequirementConfigSupport.applyDefaultSkillRequirement(
            configFile,
            "auraskills",
            "mining",
            10.0D,
            5.0D
        );

        assertEquals(10, updatedTiers);

        final YamlConfiguration configuration = YamlConfiguration.loadConfiguration(configFile);
        assertEquals("CURRENCY", configuration.getString("requirements.1.vault_purchase.type"));

        assertEquals("PLUGIN", configuration.getString("requirements.1.skills_auraskills_default.type"));
        assertEquals("SKILLS", configuration.getString("requirements.1.skills_auraskills_default.category"));
        assertEquals("auraskills", configuration.getString("requirements.1.skills_auraskills_default.plugin"));
        assertEquals(10.0D, configuration.getDouble("requirements.1.skills_auraskills_default.values.mining"), 0.00001D);
        assertEquals(15.0D, configuration.getDouble("requirements.2.skills_auraskills_default.values.mining"), 0.00001D);
        assertEquals(55.0D, configuration.getDouble("requirements.10.skills_auraskills_default.values.mining"), 0.00001D);
        assertEquals(
            "EXPERIENCE_BOTTLE",
            configuration.getString("requirements.1.skills_auraskills_default.icon.type")
        );
    }

    @Test
    void createsDefaultStorageTiersWhenRequirementsSectionDoesNotExist() throws IOException {
        final File configFile = this.tempDir.resolve("config.yml").toFile();
        Files.writeString(configFile.toPath(), "max_storages: 10\n");

        final int updatedTiers = StorageRequirementConfigSupport.applyDefaultJobRequirement(
            configFile,
            "jobsreborn",
            "miner",
            5.0D,
            2.0D
        );

        assertEquals(10, updatedTiers);

        final YamlConfiguration configuration = YamlConfiguration.loadConfiguration(configFile);
        assertEquals("PLUGIN", configuration.getString("requirements.1.jobs_jobsreborn_default.type"));
        assertEquals("JOBS", configuration.getString("requirements.1.jobs_jobsreborn_default.category"));
        assertEquals("jobsreborn", configuration.getString("requirements.1.jobs_jobsreborn_default.plugin"));
        assertEquals(5.0D, configuration.getDouble("requirements.1.jobs_jobsreborn_default.values.miner"), 0.00001D);
        assertEquals(7.0D, configuration.getDouble("requirements.2.jobs_jobsreborn_default.values.miner"), 0.00001D);
        assertEquals(23.0D, configuration.getDouble("requirements.10.jobs_jobsreborn_default.values.miner"), 0.00001D);
        assertEquals("DIAMOND_PICKAXE", configuration.getString("requirements.1.jobs_jobsreborn_default.icon.type"));
    }

    @Test
    void reappliesUsingSameKeyWithoutDuplicatingTierEntries() throws IOException {
        final File configFile = this.tempDir.resolve("config.yml").toFile();
        Files.writeString(
            configFile.toPath(),
            """
                requirements:
                  1:
                    placeholder:
                      type: "PLAYTIME"
                      requiredPlaytimeSeconds: 60
                """
        );

        StorageRequirementConfigSupport.applyDefaultSkillRequirement(
            configFile,
            "ecoskills",
            "farming",
            12.0D,
            2.0D
        );
        StorageRequirementConfigSupport.applyDefaultSkillRequirement(
            configFile,
            "ecoskills",
            "farming",
            18.0D,
            3.0D
        );

        final YamlConfiguration configuration = YamlConfiguration.loadConfiguration(configFile);
        final ConfigurationSection tierOne = configuration.getConfigurationSection("requirements.1");
        assertNotNull(tierOne);
        assertEquals(2, tierOne.getKeys(false).size());
        assertEquals("PLUGIN", configuration.getString("requirements.1.skills_ecoskills_default.type"));
        assertEquals(18.0D, configuration.getDouble("requirements.1.skills_ecoskills_default.values.farming"), 0.00001D);
        assertEquals(45.0D, configuration.getDouble("requirements.10.skills_ecoskills_default.values.farming"), 0.00001D);
    }
}
