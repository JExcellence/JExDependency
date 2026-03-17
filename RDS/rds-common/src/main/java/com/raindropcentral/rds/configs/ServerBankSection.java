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

package com.raindropcentral.rds.configs;

import de.jexcellence.configmapper.sections.AConfigSection;
import de.jexcellence.configmapper.sections.CSAlways;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;

/**
 * Represents the server bank configuration section.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
@CSAlways
@SuppressWarnings("unused")
public class ServerBankSection extends AConfigSection {

    private static final long DEFAULT_TRANSFER_INTERVAL_TICKS = 1200L;
    private static final long MINIMUM_TRANSFER_INTERVAL_TICKS = 20L;

    private Boolean enabled;
    private Long transfer_interval_ticks;

    /**
     * Creates a new server bank section.
     *
     * @param baseEnvironment evaluation environment used for config expressions
     */
    public ServerBankSection(
            final @NotNull EvaluationEnvironmentBuilder baseEnvironment
    ) {
        super(baseEnvironment);
    }

    /**
     * Loads the server bank configuration from the plugin config file.
     *
     * @param configFile plugin config file
     * @return parsed server bank section
     */
    public static @NotNull ServerBankSection fromFile(
            final @NotNull File configFile
    ) {
        final ServerBankSection section = new ServerBankSection(new EvaluationEnvironmentBuilder());
        final ConfigurationSection serverBankSection = YamlConfiguration.loadConfiguration(configFile)
                .getConfigurationSection("server_bank");
        if (serverBankSection == null) {
            return section;
        }

        section.enabled = serverBankSection.contains("enabled")
                ? serverBankSection.getBoolean("enabled")
                : null;
        section.transfer_interval_ticks = serverBankSection.contains("transfer_interval_ticks")
                ? serverBankSection.getLong("transfer_interval_ticks")
                : null;
        return section;
    }

    /**
     * Indicates whether automatic server bank transfers are enabled.
     *
     * @return {@code true} when automatic admin-shop transfer to server bank is enabled
     */
    public boolean isEnabled() {
        return this.enabled == null || this.enabled;
    }

    /**
     * Returns the configured transfer interval in ticks.
     *
     * @return transfer interval in ticks
     */
    public long getTransferIntervalTicks() {
        if (this.transfer_interval_ticks == null) {
            return DEFAULT_TRANSFER_INTERVAL_TICKS;
        }

        return Math.max(MINIMUM_TRANSFER_INTERVAL_TICKS, this.transfer_interval_ticks);
    }
}
