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
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Writes default PLUGIN requirement entries into RDR storage purchase tiers.
 *
 * <p>This support targets the bundled default purchase range of tiers {@code 1..10} under
 * {@code requirements.<purchase>.<key>} inside {@code config/config.yml}.</p>
 *
 * @author ItsRainingHP
 * @since 5.0.0
 * @version 5.0.0
 */
public final class StorageRequirementConfigSupport {

    private static final String REQUIREMENTS_PATH = "requirements";
    private static final String REQUIREMENT_TYPE_PLUGIN = "PLUGIN";
    private static final String CATEGORY_SKILLS = "SKILLS";
    private static final String CATEGORY_JOBS = "JOBS";
    private static final String SKILL_KEY_PREFIX = "skills";
    private static final String JOB_KEY_PREFIX = "jobs";
    private static final String SKILL_ICON = "EXPERIENCE_BOTTLE";
    private static final String JOB_ICON = "DIAMOND_PICKAXE";
    private static final int DEFAULT_TIER_COUNT = 10;

    private StorageRequirementConfigSupport() {
    }

    /**
     * Applies an incrementing skill-plugin requirement to tiers {@code 1..10}.
     *
     * @param configFile config file to mutate
     * @param integrationId skill integration identifier (for example {@code "auraskills"})
     * @param skillId default skill identifier
     * @param requiredLevel base required level for tier {@code 1}
     * @param levelIncrement per-tier level increment
     * @return number of tiers updated
     * @throws NullPointerException if {@code configFile}, {@code integrationId}, or {@code skillId} is {@code null}
     * @throws IllegalArgumentException if identifiers are blank, or either level value is negative
     * @throws IllegalStateException if the file cannot be saved
     */
    public static int applyDefaultSkillRequirement(
            final @NotNull File configFile,
            final @NotNull String integrationId,
            final @NotNull String skillId,
            final double requiredLevel,
            final double levelIncrement
    ) {
        return applyDefaultPluginRequirement(
                configFile,
                CATEGORY_SKILLS,
                integrationId,
                skillId,
                requiredLevel,
                levelIncrement,
                SKILL_KEY_PREFIX,
                SKILL_ICON
        );
    }

    /**
     * Applies an incrementing job-plugin requirement to tiers {@code 1..10}.
     *
     * @param configFile config file to mutate
     * @param integrationId job integration identifier (for example {@code "jobsreborn"})
     * @param jobId default job identifier
     * @param requiredLevel base required level for tier {@code 1}
     * @param levelIncrement per-tier level increment
     * @return number of tiers updated
     * @throws NullPointerException if {@code configFile}, {@code integrationId}, or {@code jobId} is {@code null}
     * @throws IllegalArgumentException if identifiers are blank, or either level value is negative
     * @throws IllegalStateException if the file cannot be saved
     */
    public static int applyDefaultJobRequirement(
            final @NotNull File configFile,
            final @NotNull String integrationId,
            final @NotNull String jobId,
            final double requiredLevel,
            final double levelIncrement
    ) {
        return applyDefaultPluginRequirement(
                configFile,
                CATEGORY_JOBS,
                integrationId,
                jobId,
                requiredLevel,
                levelIncrement,
                JOB_KEY_PREFIX,
                JOB_ICON
        );
    }

    private static int applyDefaultPluginRequirement(
            final @NotNull File configFile,
            final @NotNull String category,
            final @NotNull String integrationId,
            final @NotNull String valueKey,
            final double requiredLevel,
            final double levelIncrement,
            final @NotNull String requirementKeyPrefix,
            final @NotNull String iconType
    ) {
        final String normalizedIntegrationId = requireIdentifier(integrationId, "integrationId")
                .toLowerCase(Locale.ROOT);
        final String normalizedValueKey = requireIdentifier(valueKey, "valueKey")
                .toLowerCase(Locale.ROOT);
        if (requiredLevel < 0.0D) {
            throw new IllegalArgumentException("requiredLevel must be non-negative");
        }
        if (levelIncrement < 0.0D) {
            throw new IllegalArgumentException("levelIncrement must be non-negative");
        }

        final YamlConfiguration configuration = YamlConfiguration.loadConfiguration(configFile);
        final ConfigurationSection requirementsSection = resolveRequirementsSection(configuration);
        final String requirementKey = buildRequirementKey(requirementKeyPrefix, normalizedIntegrationId);

        int updatedTiers = 0;
        for (int tier = 1; tier <= DEFAULT_TIER_COUNT; tier++) {
            final ConfigurationSection tierSection = resolveTierSection(requirementsSection, tier);
            final ConfigurationSection requirementSection = resolveRequirementSection(tierSection, requirementKey);
            final Map<String, Object> values = new LinkedHashMap<>();
            values.put(normalizedValueKey, requiredLevel + (Math.max(0, tier - 1) * levelIncrement));

            requirementSection.set("type", REQUIREMENT_TYPE_PLUGIN);
            requirementSection.set("category", category);
            requirementSection.set("plugin", normalizedIntegrationId);
            requirementSection.set("consumable", false);
            requirementSection.set("values", values);
            requirementSection.set("icon", Map.of("type", iconType));
            updatedTiers++;
        }

        persistConfiguration(configuration, configFile);
        return updatedTiers;
    }

    private static @NotNull ConfigurationSection resolveRequirementsSection(
            final @NotNull YamlConfiguration configuration
    ) {
        final ConfigurationSection requirementsSection = configuration.getConfigurationSection(REQUIREMENTS_PATH);
        return requirementsSection != null
                ? requirementsSection
                : configuration.createSection(REQUIREMENTS_PATH);
    }

    private static @NotNull ConfigurationSection resolveTierSection(
            final @NotNull ConfigurationSection requirementsSection,
            final int tier
    ) {
        final String tierKey = String.valueOf(tier);
        final ConfigurationSection tierSection = requirementsSection.getConfigurationSection(tierKey);
        return tierSection != null
                ? tierSection
                : requirementsSection.createSection(tierKey);
    }

    private static @NotNull ConfigurationSection resolveRequirementSection(
            final @NotNull ConfigurationSection tierSection,
            final @NotNull String requirementKey
    ) {
        final ConfigurationSection requirementSection = tierSection.getConfigurationSection(requirementKey);
        return requirementSection != null
                ? requirementSection
                : tierSection.createSection(requirementKey);
    }

    private static @NotNull String buildRequirementKey(
            final @NotNull String prefix,
            final @NotNull String integrationId
    ) {
        String normalized = integrationId
                .trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "_");
        normalized = normalized.replaceAll("^_+", "").replaceAll("_+$", "");
        if (normalized.isBlank()) {
            normalized = "plugin";
        }
        return prefix + "_" + normalized + "_default";
    }

    private static @NotNull String requireIdentifier(
            final @NotNull String value,
            final @NotNull String fieldName
    ) {
        final String trimmed = value.trim();
        if (trimmed.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be blank");
        }
        return trimmed;
    }

    private static void persistConfiguration(
            final @NotNull YamlConfiguration configuration,
            final @NotNull File configFile
    ) {
        final File parentFolder = configFile.getParentFile();
        if (parentFolder != null && !parentFolder.exists() && !parentFolder.mkdirs()) {
            throw new IllegalStateException("Failed to create config directory: " + parentFolder.getAbsolutePath());
        }

        try {
            configuration.save(configFile);
        } catch (final IOException exception) {
            throw new IllegalStateException("Failed to save config: " + configFile.getAbsolutePath(), exception);
        }
    }
}
