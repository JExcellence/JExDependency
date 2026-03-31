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

import com.raindropcentral.rdq.RDQ;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;

/**
 * Injects plugin-integration requirements into live RDQ YAML files.
 *
 * <p>This utility updates files in the plugin data folder only (never bundled defaults) and adds
 * themed SKILLS/JOBS requirements to perk and rank-path configurations when triggered by admin
 * integration views.</p>
 */
final class AdminIntegrationRequirementInjector {

    private static final String PERKS_DIRECTORY = "perks";
    private static final String RANKS_DIRECTORY = "ranks";
    private static final String RANK_PATHS_DIRECTORY = "paths";
    private static final String PERK_SYSTEM_FILE = "perk-system.yml";
    private static final String FILE_EXTENSION = ".yml";

    private AdminIntegrationRequirementInjector() {
    }

    static @NotNull InjectionResult injectSkillRequirements(
        final @NotNull RDQ rdq,
        final @NotNull String integrationId
    ) throws IOException {
        return injectRequirements(rdq, integrationId, RequirementKind.SKILLS);
    }

    static @NotNull InjectionResult injectJobRequirements(
        final @NotNull RDQ rdq,
        final @NotNull String integrationId
    ) throws IOException {
        return injectRequirements(rdq, integrationId, RequirementKind.JOBS);
    }

    private static @NotNull InjectionResult injectRequirements(
        final @NotNull RDQ rdq,
        final @NotNull String integrationId,
        final @NotNull RequirementKind requirementKind
    ) throws IOException {
        final String normalizedIntegrationId = normalizeIntegrationId(integrationId);
        if (normalizedIntegrationId.isBlank()) {
            return InjectionResult.EMPTY;
        }

        final File dataFolder = rdq.getPlugin().getDataFolder();
        final File perksDirectory = new File(dataFolder, PERKS_DIRECTORY);
        final File rankPathsDirectory = new File(new File(dataFolder, RANKS_DIRECTORY), RANK_PATHS_DIRECTORY);

        final InjectionStats perkStats = injectIntoPerkFiles(perksDirectory, normalizedIntegrationId, requirementKind);
        final InjectionStats rankStats = injectIntoRankPathFiles(rankPathsDirectory, normalizedIntegrationId, requirementKind);

        if (perkStats.entriesAdded() > 0 && rdq.getPerkSystemFactory() != null) {
            rdq.getPerkSystemFactory().reload();
        }
        if (rankStats.entriesAdded() > 0 && rdq.getRankSystemFactory() != null) {
            rdq.getRankSystemFactory().initialize();
        }

        return new InjectionResult(
            perkStats.entriesAdded(),
            rankStats.entriesAdded(),
            perkStats.filesUpdated(),
            rankStats.filesUpdated()
        );
    }

    private static @NotNull InjectionStats injectIntoPerkFiles(
        final @NotNull File perksDirectory,
        final @NotNull String integrationId,
        final @NotNull RequirementKind requirementKind
    ) throws IOException {
        final File[] perkFiles = listYamlFiles(perksDirectory);
        int entriesAdded = 0;
        int filesUpdated = 0;

        for (final File perkFile : perkFiles) {
            if (PERK_SYSTEM_FILE.equalsIgnoreCase(perkFile.getName())) {
                continue;
            }

            final YamlConfiguration yaml = YamlConfiguration.loadConfiguration(perkFile);
            ConfigurationSection requirementsSection = yaml.getConfigurationSection("requirements");
            if (requirementsSection == null) {
                requirementsSection = yaml.createSection("requirements");
            }

            if (hasIntegrationRequirement(requirementsSection, integrationId, requirementKind)) {
                continue;
            }

            final String category = yaml.getString("category", "UTILITY");
            final int displayOrder = yaml.getInt("displayOrder", 0);
            addRequirement(
                requirementsSection,
                integrationId,
                requirementKind,
                resolvePerkThemeName(requirementKind, integrationId, category),
                resolvePerkLevel(displayOrder),
                resolvePerkIcon(category, requirementKind)
            );

            yaml.save(perkFile);
            entriesAdded++;
            filesUpdated++;
        }

        return new InjectionStats(entriesAdded, filesUpdated);
    }

    private static @NotNull InjectionStats injectIntoRankPathFiles(
        final @NotNull File rankPathDirectory,
        final @NotNull String integrationId,
        final @NotNull RequirementKind requirementKind
    ) throws IOException {
        final File[] rankPathFiles = listYamlFiles(rankPathDirectory);
        int entriesAdded = 0;
        int filesUpdated = 0;

        for (final File rankPathFile : rankPathFiles) {
            final YamlConfiguration yaml = YamlConfiguration.loadConfiguration(rankPathFile);
            final ConfigurationSection ranksSection = yaml.getConfigurationSection("ranks");
            if (ranksSection == null) {
                continue;
            }

            final String treeId = toIdentifier(rankPathFile.getName());
            final String firstRankKey = resolveFirstRankKey(ranksSection);
            boolean fileChanged = false;

            for (final String rankKey : ranksSection.getKeys(false)) {
                if (rankKey.equals(firstRankKey)) {
                    continue;
                }

                final ConfigurationSection rankSection = ranksSection.getConfigurationSection(rankKey);
                if (rankSection == null) {
                    continue;
                }

                ConfigurationSection requirementsSection = rankSection.getConfigurationSection("requirements");
                if (requirementsSection == null) {
                    requirementsSection = rankSection.createSection("requirements");
                }

                if (hasIntegrationRequirement(requirementsSection, integrationId, requirementKind)) {
                    continue;
                }

                final int tier = rankSection.getInt("tier", 1);
                final String rankIcon = rankSection.getString("icon.type");
                addRequirement(
                    requirementsSection,
                    integrationId,
                    requirementKind,
                    resolveRankThemeName(requirementKind, integrationId, treeId, rankKey),
                    resolveRankLevel(tier),
                    resolveRankIcon(treeId, requirementKind, rankIcon)
                );

                entriesAdded++;
                fileChanged = true;
            }

            if (fileChanged) {
                yaml.save(rankPathFile);
                filesUpdated++;
            }
        }

        return new InjectionStats(entriesAdded, filesUpdated);
    }

    private static @Nullable String resolveFirstRankKey(final @NotNull ConfigurationSection ranksSection) {
        String fallbackRankKey = null;
        int lowestTier = Integer.MAX_VALUE;

        for (final String rankKey : ranksSection.getKeys(false)) {
            final ConfigurationSection rankSection = ranksSection.getConfigurationSection(rankKey);
            if (rankSection == null) {
                continue;
            }

            if (rankSection.getBoolean("isInitialRank", false)) {
                return rankKey;
            }

            final int tier = rankSection.getInt("tier", Integer.MAX_VALUE);
            if (tier < lowestTier) {
                lowestTier = tier;
                fallbackRankKey = rankKey;
            }
        }

        return fallbackRankKey;
    }

    private static void addRequirement(
        final @NotNull ConfigurationSection requirementsSection,
        final @NotNull String integrationId,
        final @NotNull RequirementKind requirementKind,
        final @NotNull String themedKeyName,
        final int requiredValue,
        final @NotNull String iconMaterial
    ) {
        final int nextDisplayOrder = resolveNextDisplayOrder(requirementsSection);
        final String requirementKey = resolveRequirementKey(requirementsSection, requirementKind, integrationId);

        final ConfigurationSection requirementSection = requirementsSection.createSection(requirementKey);
        requirementSection.set("type", requirementKind.typeId);
        requirementSection.set("displayOrder", nextDisplayOrder);
        requirementSection.set("icon.type", iconMaterial);

        final String nestedBase = requirementKind.nestedPath;
        requirementSection.set(nestedBase + "." + requirementKind.pluginKey, integrationId);
        requirementSection.set(nestedBase + "." + requirementKind.valuesKey + "." + themedKeyName, requiredValue);
        requirementSection.set(nestedBase + ".consumeOnComplete", false);
    }

    private static int resolveNextDisplayOrder(final @NotNull ConfigurationSection requirementsSection) {
        int maxDisplayOrder = 0;
        for (final String requirementKey : requirementsSection.getKeys(false)) {
            final ConfigurationSection requirementSection = requirementsSection.getConfigurationSection(requirementKey);
            if (requirementSection == null) {
                continue;
            }
            maxDisplayOrder = Math.max(maxDisplayOrder, requirementSection.getInt("displayOrder", 0));
        }
        return maxDisplayOrder + 1;
    }

    private static @NotNull String resolveRequirementKey(
        final @NotNull ConfigurationSection requirementsSection,
        final @NotNull RequirementKind requirementKind,
        final @NotNull String integrationId
    ) {
        final String keyBase = requirementKind.keyPrefix + "_" + integrationId;
        if (!requirementsSection.contains(keyBase)) {
            return keyBase;
        }

        int suffix = 2;
        while (requirementsSection.contains(keyBase + "_" + suffix)) {
            suffix++;
        }
        return keyBase + "_" + suffix;
    }

    private static boolean hasIntegrationRequirement(
        final @NotNull ConfigurationSection requirementsSection,
        final @NotNull String integrationId,
        final @NotNull RequirementKind requirementKind
    ) {
        for (final String requirementKey : requirementsSection.getKeys(false)) {
            final ConfigurationSection requirementSection = requirementsSection.getConfigurationSection(requirementKey);
            if (requirementSection == null) {
                continue;
            }

            final boolean hasNestedSection = requirementSection.isConfigurationSection(requirementKind.nestedPath);
            final String type = requirementSection.getString("type");
            if (!isMatchingType(type, requirementKind, hasNestedSection)) {
                continue;
            }

            final String pluginId = resolvePluginId(requirementSection, requirementKind);
            if (integrationId.equals(normalizeIntegrationId(pluginId))) {
                return true;
            }
        }
        return false;
    }

    private static boolean isMatchingType(
        final @Nullable String type,
        final @NotNull RequirementKind requirementKind,
        final boolean hasNestedSection
    ) {
        if (type == null || type.isBlank()) {
            return hasNestedSection;
        }

        final String normalizedType = type.trim().toUpperCase(Locale.ROOT);
        return normalizedType.equals(requirementKind.typeId) || "PLUGIN".equals(normalizedType);
    }

    private static @Nullable String resolvePluginId(
        final @NotNull ConfigurationSection requirementSection,
        final @NotNull RequirementKind requirementKind
    ) {
        final ConfigurationSection nestedSection = requirementSection.getConfigurationSection(requirementKind.nestedPath);
        if (nestedSection != null) {
            final String nestedPluginId = nestedSection.getString(requirementKind.pluginKey);
            if (nestedPluginId != null && !nestedPluginId.isBlank()) {
                return nestedPluginId;
            }
        }

        final String directPluginId = requirementSection.getString(requirementKind.pluginKey);
        if (directPluginId != null && !directPluginId.isBlank()) {
            return directPluginId;
        }

        final String legacyPluginId = requirementSection.getString("plugin");
        if (legacyPluginId != null && !legacyPluginId.isBlank()) {
            return legacyPluginId;
        }

        return requirementSection.getString("pluginId");
    }

    private static @NotNull String resolvePerkThemeName(
        final @NotNull RequirementKind requirementKind,
        final @NotNull String integrationId,
        final @Nullable String category
    ) {
        final String normalizedCategory = category == null
            ? "UTILITY"
            : category.trim().toUpperCase(Locale.ROOT);
        return requirementKind == RequirementKind.SKILLS
            ? resolveSkillNameByCategory(integrationId, normalizedCategory)
            : resolveJobNameByCategory(normalizedCategory);
    }

    private static @NotNull String resolveRankThemeName(
        final @NotNull RequirementKind requirementKind,
        final @NotNull String integrationId,
        final @NotNull String treeId,
        final @NotNull String rankKey
    ) {
        final String normalizedTreeId = treeId.trim().toLowerCase(Locale.ROOT);
        final String normalizedRankKey = rankKey.trim().toLowerCase(Locale.ROOT);
        return requirementKind == RequirementKind.SKILLS
            ? resolveSkillNameByTree(integrationId, normalizedTreeId, normalizedRankKey)
            : resolveJobNameByTree(normalizedTreeId, normalizedRankKey);
    }

    private static int resolvePerkLevel(final int displayOrder) {
        return Math.max(10, Math.min(70, 12 + displayOrder));
    }

    private static int resolveRankLevel(final int tier) {
        return Math.max(10, Math.min(80, tier * 12));
    }

    private static @NotNull String resolvePerkIcon(
        final @Nullable String category,
        final @NotNull RequirementKind requirementKind
    ) {
        if (requirementKind == RequirementKind.JOBS) {
            return "DIAMOND_PICKAXE";
        }

        final String normalizedCategory = category == null
            ? "UTILITY"
            : category.trim().toUpperCase(Locale.ROOT);
        return switch (normalizedCategory) {
            case "COMBAT" -> "DIAMOND_SWORD";
            case "MOVEMENT" -> "FEATHER";
            case "SURVIVAL" -> "SHIELD";
            case "ECONOMY" -> "EMERALD";
            case "SOCIAL", "COSMETIC" -> "AMETHYST_SHARD";
            case "SPECIAL" -> "NETHER_STAR";
            default -> "EXPERIENCE_BOTTLE";
        };
    }

    private static @NotNull String resolveRankIcon(
        final @NotNull String treeId,
        final @NotNull RequirementKind requirementKind,
        final @Nullable String existingRankIcon
    ) {
        if (existingRankIcon != null && !existingRankIcon.isBlank()) {
            return existingRankIcon;
        }

        if (requirementKind == RequirementKind.JOBS) {
            return "GOLDEN_PICKAXE";
        }

        return switch (treeId.toLowerCase(Locale.ROOT)) {
            case "ranger" -> "BOW";
            case "warrior" -> "DIAMOND_SWORD";
            case "merchant" -> "EMERALD";
            case "mage" -> "ENCHANTED_BOOK";
            case "rogue" -> "IRON_SWORD";
            case "cleric" -> "GOLDEN_APPLE";
            default -> "EXPERIENCE_BOTTLE";
        };
    }

    private static @NotNull String resolveSkillNameByCategory(
        final @NotNull String integrationId,
        final @NotNull String category
    ) {
        final boolean isMcMMO = "mcmmo".equals(integrationId);
        if (isMcMMO) {
            return switch (category) {
                case "COMBAT" -> "SWORDS";
                case "MOVEMENT" -> "ACROBATICS";
                case "SURVIVAL" -> "HERBALISM";
                case "ECONOMY" -> "MINING";
                case "SOCIAL", "COSMETIC" -> "FISHING";
                case "SPECIAL" -> "ALCHEMY";
                default -> "MINING";
            };
        }

        return switch (category) {
            case "COMBAT" -> "fighting";
            case "MOVEMENT" -> "endurance";
            case "SURVIVAL" -> "farming";
            case "ECONOMY" -> "excavation";
            case "SOCIAL", "COSMETIC" -> "fishing";
            case "SPECIAL" -> "alchemy";
            default -> "mining";
        };
    }

    private static @NotNull String resolveSkillNameByTree(
        final @NotNull String integrationId,
        final @NotNull String treeId,
        final @NotNull String rankKey
    ) {
        final boolean isMcMMO = "mcmmo".equals(integrationId);
        if (isMcMMO) {
            return switch (treeId) {
                case "ranger" -> "ARCHERY";
                case "warrior" -> "SWORDS";
                case "rogue" -> "ACROBATICS";
                case "merchant" -> "MINING";
                case "mage" -> "ALCHEMY";
                case "cleric" -> "HERBALISM";
                default -> rankKey.contains("fish") ? "FISHING" : "MINING";
            };
        }

        return switch (treeId) {
            case "ranger" -> "archery";
            case "warrior" -> "fighting";
            case "rogue" -> "excavation";
            case "merchant" -> "foraging";
            case "mage" -> "alchemy";
            case "cleric" -> "farming";
            default -> rankKey.contains("fish") ? "fishing" : "mining";
        };
    }

    private static @NotNull String resolveJobNameByCategory(final @NotNull String category) {
        return switch (category) {
            case "COMBAT" -> "hunter";
            case "MOVEMENT" -> "explorer";
            case "SURVIVAL" -> "farmer";
            case "ECONOMY" -> "builder";
            case "SOCIAL", "COSMETIC" -> "fisherman";
            case "SPECIAL" -> "enchanter";
            default -> "miner";
        };
    }

    private static @NotNull String resolveJobNameByTree(
        final @NotNull String treeId,
        final @NotNull String rankKey
    ) {
        return switch (treeId) {
            case "ranger", "warrior" -> "hunter";
            case "rogue" -> "explorer";
            case "merchant" -> "builder";
            case "mage" -> "enchanter";
            case "cleric" -> "farmer";
            default -> rankKey.contains("fish") ? "fisherman" : "miner";
        };
    }

    private static @NotNull File[] listYamlFiles(final @NotNull File directory) {
        if (!directory.exists() || !directory.isDirectory()) {
            return new File[0];
        }

        final File[] files = directory.listFiles((dir, name) -> name.toLowerCase(Locale.ROOT).endsWith(FILE_EXTENSION));
        if (files == null || files.length == 0) {
            return new File[0];
        }

        Arrays.sort(files, (left, right) -> left.getName().compareToIgnoreCase(right.getName()));
        return files;
    }

    private static @NotNull String normalizeIntegrationId(final @Nullable String integrationId) {
        if (integrationId == null || integrationId.isBlank()) {
            return "";
        }
        return AdminPluginIntegrationSupport.normalizePluginKey(integrationId);
    }

    private static @NotNull String toIdentifier(final @NotNull String fileName) {
        return fileName
            .replace(FILE_EXTENSION, "")
            .replace("-", "_")
            .replace(" ", "_")
            .toLowerCase(Locale.ROOT);
    }

    private enum RequirementKind {
        SKILLS("SKILLS", "skillRequirement", "skillPlugin", "requiredSkills", "integration_skill"),
        JOBS("JOBS", "jobRequirement", "jobPlugin", "requiredJobs", "integration_job");

        private final String typeId;
        private final String nestedPath;
        private final String pluginKey;
        private final String valuesKey;
        private final String keyPrefix;

        RequirementKind(
            final @NotNull String typeId,
            final @NotNull String nestedPath,
            final @NotNull String pluginKey,
            final @NotNull String valuesKey,
            final @NotNull String keyPrefix
        ) {
            this.typeId = Objects.requireNonNull(typeId, "typeId");
            this.nestedPath = Objects.requireNonNull(nestedPath, "nestedPath");
            this.pluginKey = Objects.requireNonNull(pluginKey, "pluginKey");
            this.valuesKey = Objects.requireNonNull(valuesKey, "valuesKey");
            this.keyPrefix = Objects.requireNonNull(keyPrefix, "keyPrefix");
        }
    }

    private record InjectionStats(
        int entriesAdded,
        int filesUpdated
    ) {
    }

    static record InjectionResult(
        int perkRequirementsAdded,
        int rankRequirementsAdded,
        int perkFilesUpdated,
        int rankFilesUpdated
    ) {
        private static final InjectionResult EMPTY = new InjectionResult(0, 0, 0, 0);

        int totalRequirementsAdded() {
            return this.perkRequirementsAdded + this.rankRequirementsAdded;
        }

        boolean hasChanges() {
            return this.totalRequirementsAdded() > 0;
        }
    }
}
