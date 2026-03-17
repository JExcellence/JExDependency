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

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Shared detection support for RDQ admin plugin integration views.
 *
 * <p>This helper centralizes supported integration definitions and plugin-name matching so
 * skills/jobs views can render consistent detection status and unit tests can validate behavior.</p>
 *
 * @author ItsRainingHP
 * @since 6.0.0
 * @version 6.0.0
 */
final class AdminPluginIntegrationSupport {

    private static final List<SupportedPluginDefinition> SKILL_DEFINITIONS = List.of(
        new SupportedPluginDefinition(
            "mcmmo",
            "mcMMO",
            List.of("mcMMO"),
            Material.IRON_PICKAXE
        ),
        new SupportedPluginDefinition(
            "auraskills",
            "Aura",
            List.of("Aura", "AuraSkills", "AureliumSkills"),
            Material.AMETHYST_SHARD
        ),
        new SupportedPluginDefinition(
            "ecoskills",
            "EcoSkills",
            List.of("EcoSkills"),
            Material.EMERALD
        )
    );

    private static final List<SupportedPluginDefinition> JOB_DEFINITIONS = List.of(
        new SupportedPluginDefinition(
            "ecojobs",
            "EcoJobs",
            List.of("EcoJobs", "EcoJobsPlugin"),
            Material.DIAMOND_PICKAXE
        ),
        new SupportedPluginDefinition(
            "jobsreborn",
            "JobsReborn",
            List.of("Jobs", "JobsReborn"),
            Material.GOLDEN_PICKAXE
        )
    );

    private AdminPluginIntegrationSupport() {
    }

    /**
     * Returns detected plugin entries for configured skill integrations.
     *
     * @param installedPlugins currently installed plugins
     * @return skill integration entries with detection status
     */
    static @NotNull List<PluginDetectionEntry> detectSkillPlugins(
        final @Nullable Plugin[] installedPlugins
    ) {
        return detectPlugins(SKILL_DEFINITIONS, installedPlugins);
    }

    /**
     * Returns detected plugin entries for configured job integrations.
     *
     * @param installedPlugins currently installed plugins
     * @return job integration entries with detection status
     */
    static @NotNull List<PluginDetectionEntry> detectJobPlugins(
        final @Nullable Plugin[] installedPlugins
    ) {
        return detectPlugins(JOB_DEFINITIONS, installedPlugins);
    }

    /**
     * Counts how many integration entries are currently detected.
     *
     * @param entries integration entries
     * @return number of detected integrations
     */
    static int countDetectedEntries(
        final @NotNull List<PluginDetectionEntry> entries
    ) {
        int count = 0;
        for (final PluginDetectionEntry entry : entries) {
            if (entry.detected()) {
                count++;
            }
        }
        return count;
    }

    /**
     * Resolves currently installed plugins from the active Bukkit runtime.
     *
     * @return installed plugin array, or empty array when unavailable
     */
    static @NotNull Plugin[] resolveInstalledPlugins() {
        try {
            final PluginManager pluginManager = Bukkit.getPluginManager();
            if (pluginManager == null) {
                return new Plugin[0];
            }
            return pluginManager.getPlugins();
        } catch (final RuntimeException exception) {
            return new Plugin[0];
        }
    }

    /**
     * Determines whether any provided plugin alias is currently installed and enabled.
     *
     * @param pluginNames plugin aliases accepted for detection
     * @param installedPlugins currently installed plugins
     * @return {@code true} when at least one alias matches an enabled plugin
     */
    static boolean isPluginDetected(
        final @NotNull List<String> pluginNames,
        final @Nullable Plugin[] installedPlugins
    ) {
        if (installedPlugins == null || installedPlugins.length == 0) {
            return false;
        }

        for (final Plugin installedPlugin : installedPlugins) {
            if (installedPlugin == null || !installedPlugin.isEnabled()) {
                continue;
            }

            final String installedKey = normalizePluginKey(installedPlugin.getName());
            if (installedKey.isEmpty()) {
                continue;
            }

            for (final String pluginName : pluginNames) {
                if (installedKey.equals(normalizePluginKey(pluginName))) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Normalizes plugin names for consistent alias matching.
     *
     * @param pluginName raw plugin name
     * @return canonical plugin key used for detection
     */
    static @NotNull String normalizePluginKey(
        final @NotNull String pluginName
    ) {
        final String normalized = pluginName
            .trim()
            .toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9]+", "");

        if (normalized.isEmpty()) {
            return "";
        }

        if (List.of("aura", "auraskills", "aureliumskills").contains(normalized)) {
            return "auraskills";
        }
        if (List.of("jobs", "jobsreborn").contains(normalized)) {
            return "jobsreborn";
        }
        if (List.of("ecojobs", "ecojobsplugin").contains(normalized)) {
            return "ecojobs";
        }
        return normalized;
    }

    private static @NotNull List<PluginDetectionEntry> detectPlugins(
        final @NotNull List<SupportedPluginDefinition> definitions,
        final @Nullable Plugin[] installedPlugins
    ) {
        return definitions.stream()
            .map(definition -> new PluginDetectionEntry(
                definition.integrationId(),
                definition.displayName(),
                definition.iconType(),
                isPluginDetected(definition.detectedPluginNames(), installedPlugins)
            ))
            .toList();
    }

    /**
     * Supported plugin definition used for skills/jobs detection entries.
     *
     * @param integrationId canonical integration identifier
     * @param displayName display name rendered in admin views
     * @param detectedPluginNames plugin aliases accepted for detection
     * @param iconType icon material used when detected
     */
    record SupportedPluginDefinition(
        @NotNull String integrationId,
        @NotNull String displayName,
        @NotNull List<String> detectedPluginNames,
        @NotNull Material iconType
    ) {

        SupportedPluginDefinition {
            Objects.requireNonNull(integrationId, "integrationId");
            Objects.requireNonNull(displayName, "displayName");
            Objects.requireNonNull(detectedPluginNames, "detectedPluginNames");
            Objects.requireNonNull(iconType, "iconType");
            detectedPluginNames = List.copyOf(detectedPluginNames);
        }
    }

    /**
     * Detection payload consumed by admin integration views.
     *
     * @param integrationId canonical integration identifier
     * @param displayName display name rendered in the view
     * @param iconType icon material used when detected
     * @param detected whether this integration was detected at runtime
     */
    record PluginDetectionEntry(
        @NotNull String integrationId,
        @NotNull String displayName,
        @NotNull Material iconType,
        boolean detected
    ) {

        PluginDetectionEntry {
            Objects.requireNonNull(integrationId, "integrationId");
            Objects.requireNonNull(displayName, "displayName");
            Objects.requireNonNull(iconType, "iconType");
        }
    }
}
