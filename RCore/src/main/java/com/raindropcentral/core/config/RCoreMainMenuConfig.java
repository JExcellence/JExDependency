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

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Snapshot of the {@code configs/config.yml} module-hub settings used by {@code /rc main}.
 */
public final class RCoreMainMenuConfig {

    private static final List<String> DEFAULT_PLACEMENT_ORDER = List.of("RDA", "RDQ", "RDR", "RDS", "RDT");
    private static final Map<String, Material> DEFAULT_BUTTON_MATERIALS = createDefaultButtonMaterials();

    private final List<String> placementOrder;
    private final Map<String, Material> buttonMaterials;

    private RCoreMainMenuConfig(
        final @NotNull List<String> placementOrder,
        final @NotNull Map<String, Material> buttonMaterials
    ) {
        this.placementOrder = List.copyOf(placementOrder);
        this.buttonMaterials = Map.copyOf(buttonMaterials);
    }

    /**
     * Creates the default module-hub configuration used when the config file is missing or invalid.
     *
     * @return default module-hub configuration
     */
    public static @NotNull RCoreMainMenuConfig defaults() {
        return new RCoreMainMenuConfig(DEFAULT_PLACEMENT_ORDER, DEFAULT_BUTTON_MATERIALS);
    }

    /**
     * Creates a normalized module-hub configuration snapshot from a Bukkit configuration tree.
     *
     * @param configuration loaded YAML configuration
     * @return normalized module-hub configuration
     */
    public static @NotNull RCoreMainMenuConfig fromConfiguration(final @NotNull ConfigurationSection configuration) {
        final ConfigurationSection mainSection = configuration.getConfigurationSection("rc_main");
        if (mainSection == null) {
            return defaults();
        }

        return new RCoreMainMenuConfig(
            resolvePlacementOrder(mainSection.getStringList("placement_order")),
            resolveButtonMaterials(mainSection.getConfigurationSection("button_materials"))
        );
    }

    /**
     * Returns the normalized module-id placement order for the five hub buttons.
     *
     * @return module ids in render order
     */
    public @NotNull List<String> getPlacementOrder() {
        return this.placementOrder;
    }

    /**
     * Resolves the configured material for the requested module button.
     *
     * @param moduleId logical module id such as {@code RDA} or {@code RDT}
     * @return configured button material, or {@link Material#BARRIER} when the id is unknown
     */
    public @NotNull Material getButtonMaterial(final @NotNull String moduleId) {
        final String normalizedModuleId = normalizeModuleId(moduleId);
        if (normalizedModuleId == null) {
            return Material.BARRIER;
        }
        return this.buttonMaterials.getOrDefault(
            normalizedModuleId,
            DEFAULT_BUTTON_MATERIALS.get(normalizedModuleId)
        );
    }

    private static @NotNull List<String> resolvePlacementOrder(final @NotNull List<String> configuredOrder) {
        final ArrayList<String> resolvedOrder = new ArrayList<>();
        for (final String rawModuleId : configuredOrder) {
            final String normalizedModuleId = normalizeModuleId(rawModuleId);
            if (normalizedModuleId == null || resolvedOrder.contains(normalizedModuleId)) {
                continue;
            }
            resolvedOrder.add(normalizedModuleId);
        }

        for (final String defaultModuleId : DEFAULT_PLACEMENT_ORDER) {
            if (!resolvedOrder.contains(defaultModuleId)) {
                resolvedOrder.add(defaultModuleId);
            }
        }

        return resolvedOrder;
    }

    private static @NotNull Map<String, Material> resolveButtonMaterials(
        final ConfigurationSection buttonMaterialsSection
    ) {
        final LinkedHashMap<String, Material> resolvedMaterials = new LinkedHashMap<>();
        for (final Map.Entry<String, Material> entry : DEFAULT_BUTTON_MATERIALS.entrySet()) {
            final String moduleId = entry.getKey();
            final Material defaultMaterial = entry.getValue();
            final String configuredMaterialName = buttonMaterialsSection == null
                ? null
                : buttonMaterialsSection.getString(moduleId);
            resolvedMaterials.put(moduleId, resolveMaterial(configuredMaterialName, defaultMaterial));
        }
        return resolvedMaterials;
    }

    private static @NotNull Material resolveMaterial(
        final String configuredMaterialName,
        final @NotNull Material defaultMaterial
    ) {
        if (configuredMaterialName == null || configuredMaterialName.isBlank()) {
            return defaultMaterial;
        }

        final Material material = Material.getMaterial(configuredMaterialName.trim().toUpperCase(Locale.ROOT));
        return material == null ? defaultMaterial : material;
    }

    private static @Nullable String normalizeModuleId(final String rawModuleId) {
        if (rawModuleId == null || rawModuleId.isBlank()) {
            return null;
        }

        final String normalizedModuleId = rawModuleId.trim().toUpperCase(Locale.ROOT);
        return DEFAULT_PLACEMENT_ORDER.contains(normalizedModuleId) ? normalizedModuleId : null;
    }

    private static @NotNull Map<String, Material> createDefaultButtonMaterials() {
        final LinkedHashMap<String, Material> defaults = new LinkedHashMap<>();
        defaults.put("RDA", Material.NETHER_STAR);
        defaults.put("RDQ", Material.BOOK);
        defaults.put("RDR", Material.BARREL);
        defaults.put("RDS", Material.CHEST);
        defaults.put("RDT", Material.SHIELD);
        return defaults;
    }
}
