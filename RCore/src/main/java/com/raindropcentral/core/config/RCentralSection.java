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
import de.jexcellence.configmapper.sections.AConfigSection;
import de.jexcellence.configmapper.sections.CSAlways;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Configuration section for RaindropCentral backend connection settings.
 */
@CSAlways
public class RCentralSection extends AConfigSection {

    private static final String DROPLETS_STORE_ENABLED_PATH = "droplets_store.enabled";
    private static final String DROPLETS_STORE_REWARDS_PATH = "droplets_store.rewards";

    private String backendUrl;
    private Boolean developmentMode;
    private Boolean autoDetect;
    private Boolean dropletsStoreEnabled;
    private final Map<String, Boolean> dropletStoreRewardStates;

    /**
     * Executes RCentralSection.
     */
    public RCentralSection(EvaluationEnvironmentBuilder baseEnvironment) {
        super(baseEnvironment);
        this.dropletStoreRewardStates = new LinkedHashMap<>();
        for (final String itemCode : DropletCookieDefinitions.allItemCodes()) {
            this.dropletStoreRewardStates.put(normalize(itemCode), true);
        }
    }

    /**
     * Creates a section snapshot from a Bukkit configuration tree.
     *
     * @param configuration loaded YAML configuration
     * @return populated configuration section
     */
    public static @NotNull RCentralSection fromConfiguration(final @NotNull ConfigurationSection configuration) {
        final RCentralSection section = new RCentralSection(new EvaluationEnvironmentBuilder());
        section.backendUrl = configuration.getString("backendUrl");
        section.developmentMode = configuration.contains("developmentMode")
                ? configuration.getBoolean("developmentMode")
                : null;
        section.autoDetect = configuration.contains("autoDetect")
                ? configuration.getBoolean("autoDetect")
                : null;
        section.dropletsStoreEnabled = configuration.contains(DROPLETS_STORE_ENABLED_PATH)
                ? configuration.getBoolean(DROPLETS_STORE_ENABLED_PATH)
                : null;
        final ConfigurationSection rewardsSection = configuration.getConfigurationSection(DROPLETS_STORE_REWARDS_PATH);
        for (final String itemCode : DropletCookieDefinitions.allItemCodes()) {
            final String rewardPath = DROPLETS_STORE_REWARDS_PATH + "." + itemCode;
            if (configuration.contains(rewardPath)) {
                section.dropletStoreRewardStates.put(normalize(itemCode), configuration.getBoolean(rewardPath));
                continue;
            }
            if (rewardsSection != null && rewardsSection.contains(itemCode)) {
                section.dropletStoreRewardStates.put(normalize(itemCode), rewardsSection.getBoolean(itemCode));
            }
        }
        return section;
    }

    /**
     * Gets the backend URL. Returns null if not explicitly set or empty.
     */
    @Nullable
    public String getBackendUrl() {
        return backendUrl != null && !backendUrl.isEmpty() ? backendUrl : null;
    }

    /**
     * Checks if development mode is explicitly enabled.
     */
    public boolean isDevelopmentMode() {
        return developmentMode != null && developmentMode;
    }

    /**
     * Checks if auto-detection should be used.
     * Defaults to true if not specified.
     */
    public boolean isAutoDetect() {
        return autoDetect == null || autoDetect;
    }

    /**
     * Checks if droplet-store claiming is enabled for this server.
     *
     * @return {@code true} when the claim command should be available
     */
    public boolean isDropletsStoreEnabled() {
        return dropletsStoreEnabled == null || dropletsStoreEnabled;
    }

    /**
     * Checks if a supported droplet-store reward can be claimed on this server.
     *
     * @param itemCode backend item code
     * @return {@code true} when the reward is enabled in config
     */
    public boolean isDropletStoreRewardEnabled(final @NotNull String itemCode) {
        return this.dropletStoreRewardStates.getOrDefault(normalize(itemCode), false);
    }

    private static @NotNull String normalize(final @NotNull String itemCode) {
        return itemCode.trim().toLowerCase(Locale.ROOT);
    }
}
