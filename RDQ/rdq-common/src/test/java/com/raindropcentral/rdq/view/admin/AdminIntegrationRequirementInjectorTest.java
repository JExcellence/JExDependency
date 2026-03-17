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

package com.raindropcentral.rdq.view.admin;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.Arrays;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies rank-path requirement injection behavior for admin integration views.
 *
 * @author ItsRainingHP
 * @since 6.0.0
 * @version 6.0.0
 */
class AdminIntegrationRequirementInjectorTest {

    @TempDir
    Path tempDirectory;

    @Test
    void injectJobRequirementsSkipsInitialRank() throws IOException {
        final File dataFolder = this.tempDirectory.toFile();
        final File rankPathFile = this.createRankPathFile(dataFolder, "warrior.yml");
        final InjectionStatsView result = this.injectIntoRankPaths(dataFolder, "jobsreborn", "JOBS");

        assertEquals(1, result.entriesAdded());
        assertEquals(1, result.filesUpdated());

        final YamlConfiguration yaml = YamlConfiguration.loadConfiguration(rankPathFile);
        final ConfigurationSection recruitRequirements = yaml.getConfigurationSection("ranks.recruit.requirements");
        final ConfigurationSection soldierRequirements = yaml.getConfigurationSection("ranks.soldier.requirements");

        assertFalse(this.hasInjectedRequirement(recruitRequirements, "jobRequirement", "jobPlugin", "jobsreborn"));
        assertTrue(this.hasInjectedRequirement(soldierRequirements, "jobRequirement", "jobPlugin", "jobsreborn"));
    }

    @Test
    void injectSkillRequirementsSkipsInitialRank() throws IOException {
        final File dataFolder = this.tempDirectory.toFile();
        final File rankPathFile = this.createRankPathFile(dataFolder, "ranger.yml");
        final InjectionStatsView result = this.injectIntoRankPaths(dataFolder, "auraskills", "SKILLS");

        assertEquals(1, result.entriesAdded());
        assertEquals(1, result.filesUpdated());

        final YamlConfiguration yaml = YamlConfiguration.loadConfiguration(rankPathFile);
        final ConfigurationSection recruitRequirements = yaml.getConfigurationSection("ranks.recruit.requirements");
        final ConfigurationSection soldierRequirements = yaml.getConfigurationSection("ranks.soldier.requirements");

        assertFalse(this.hasInjectedRequirement(recruitRequirements, "skillRequirement", "skillPlugin", "auraskills"));
        assertTrue(this.hasInjectedRequirement(soldierRequirements, "skillRequirement", "skillPlugin", "auraskills"));
    }

    private File createRankPathFile(
        final File dataFolder,
        final String fileName
    ) throws IOException {
        final File rankPathsDirectory = new File(new File(dataFolder, "ranks"), "paths");
        if (!rankPathsDirectory.mkdirs() && !rankPathsDirectory.isDirectory()) {
            throw new IOException("Failed to create rank paths directory at: " + rankPathsDirectory.getAbsolutePath());
        }

        final File rankPathFile = new File(rankPathsDirectory, fileName);
        final YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("ranks.recruit.tier", 1);
        yaml.set("ranks.recruit.isInitialRank", true);
        yaml.set("ranks.soldier.tier", 2);
        yaml.save(rankPathFile);
        return rankPathFile;
    }

    private InjectionStatsView injectIntoRankPaths(
        final File dataFolder,
        final String integrationId,
        final String requirementKindName
    ) {
        try {
            final File rankPathsDirectory = new File(new File(dataFolder, "ranks"), "paths");
            final Class<?> injectorClass = AdminIntegrationRequirementInjector.class;
            final Class<?> requirementKindClass = this.resolveNestedClass(injectorClass, "RequirementKind");
            final Class<?> injectionStatsClass = this.resolveNestedClass(injectorClass, "InjectionStats");

            final Method injectIntoRankPathFilesMethod = injectorClass.getDeclaredMethod(
                "injectIntoRankPathFiles",
                File.class,
                String.class,
                requirementKindClass
            );
            injectIntoRankPathFilesMethod.setAccessible(true);

            @SuppressWarnings("unchecked")
            final Object requirementKind = Enum.valueOf((Class<Enum>) requirementKindClass, requirementKindName);
            final Object stats = injectIntoRankPathFilesMethod.invoke(null, rankPathsDirectory, integrationId, requirementKind);

            final Method entriesAddedMethod = injectionStatsClass.getDeclaredMethod("entriesAdded");
            final Method filesUpdatedMethod = injectionStatsClass.getDeclaredMethod("filesUpdated");
            final int entriesAdded = (int) entriesAddedMethod.invoke(stats);
            final int filesUpdated = (int) filesUpdatedMethod.invoke(stats);
            return new InjectionStatsView(entriesAdded, filesUpdated);
        } catch (
            final NoSuchMethodException
            | IllegalAccessException
            | InvocationTargetException exception
        ) {
            throw new IllegalStateException("Failed to invoke rank-path injection", exception);
        }
    }

    private Class<?> resolveNestedClass(
        final Class<?> ownerClass,
        final String nestedClassSimpleName
    ) {
        return Arrays.stream(ownerClass.getDeclaredClasses())
            .filter(candidate -> nestedClassSimpleName.equals(candidate.getSimpleName()))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Missing nested class: " + nestedClassSimpleName));
    }

    private boolean hasInjectedRequirement(
        final ConfigurationSection requirementsSection,
        final String nestedPath,
        final String pluginKey,
        final String expectedIntegrationId
    ) {
        if (requirementsSection == null) {
            return false;
        }

        for (final String requirementKey : requirementsSection.getKeys(false)) {
            final ConfigurationSection requirementSection = requirementsSection.getConfigurationSection(requirementKey);
            if (requirementSection == null) {
                continue;
            }

            final ConfigurationSection nestedSection = requirementSection.getConfigurationSection(nestedPath);
            if (nestedSection == null) {
                continue;
            }

            final String pluginId = nestedSection.getString(pluginKey);
            if (expectedIntegrationId.equals(pluginId)) {
                return true;
            }
        }

        return false;
    }

    private record InjectionStatsView(
        int entriesAdded,
        int filesUpdated
    ) {
    }
}
