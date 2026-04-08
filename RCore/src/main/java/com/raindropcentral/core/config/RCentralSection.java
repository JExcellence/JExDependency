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

import de.jexcellence.configmapper.sections.AConfigSection;
import de.jexcellence.configmapper.sections.CSAlways;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Snapshot of the {@code rcentral.yml} connection settings.
 */
@CSAlways
public class RCentralSection extends AConfigSection {

    private String backendUrl;
    private Boolean developmentMode;
    private Boolean autoDetect;

    /**
     * Creates a section with default backend connection settings.
     *
     * @param baseEnvironment expression environment used by the config mapper base class
     */
    public RCentralSection(final @NotNull EvaluationEnvironmentBuilder baseEnvironment) {
        super(baseEnvironment);
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
        return section;
    }

    /**
     * Returns the configured backend base URL.
     *
     * @return backend URL, or {@code null} when auto-detection should be used
     */
    @Nullable
    public String getBackendUrl() {
        return backendUrl != null && !backendUrl.isEmpty() ? backendUrl : null;
    }

    /**
     * Returns whether development-mode endpoints should be preferred.
     *
     * @return {@code true} when development mode is enabled
     */
    public boolean isDevelopmentMode() {
        return developmentMode != null && developmentMode;
    }

    /**
     * Returns whether backend auto-detection should be used.
     *
     * @return {@code true} when auto-detection is enabled or left unspecified
     */
    public boolean isAutoDetect() {
        return autoDetect == null || autoDetect;
    }
}
