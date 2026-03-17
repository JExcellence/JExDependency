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

import java.io.File;

import de.jexcellence.configmapper.sections.AConfigSection;
import de.jexcellence.configmapper.sections.CSAlways;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

/**
 * Represents the boss bar configuration section.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
@CSAlways
@SuppressWarnings("unused")
public class BossBarSection extends AConfigSection {

    private Integer update_period_ticks;
    private Integer view_distance;

    /**
     * Creates a new boss bar section.
     *
     * @param baseEnvironment evaluation environment used for config expressions
     */
    public BossBarSection(
            final EvaluationEnvironmentBuilder baseEnvironment
    ) {
        super(baseEnvironment);
    }

    /**
     * Loads the boss-bar configuration from the plugin config file.
     *
     * @param configFile plugin config file
     * @return parsed boss-bar section
     */
    public static @NotNull BossBarSection fromFile(final @NotNull File configFile) {
        final BossBarSection section = new BossBarSection(new EvaluationEnvironmentBuilder());
        final ConfigurationSection bossBarSection = YamlConfiguration.loadConfiguration(configFile)
            .getConfigurationSection("boss_bar");
        if (bossBarSection == null) {
            return section;
        }

        section.update_period_ticks = bossBarSection.contains("update_period_ticks")
            ? bossBarSection.getInt("update_period_ticks")
            : null;
        section.view_distance = bossBarSection.contains("view_distance")
            ? bossBarSection.getInt("view_distance")
            : null;
        return section;
    }

    /**
     * Returns the update period ticks.
     *
     * @return the update period ticks
     */
    public long getUpdatePeriodTicks() {
        if (this.update_period_ticks == null) {
            return 10L;
        }

        return Math.max(1L, this.update_period_ticks.longValue());
    }

    /**
     * Returns the view distance.
     *
     * @return the view distance
     */
    public int getViewDistance() {
        if (this.view_distance == null) {
            return 12;
        }

        return Math.max(1, this.view_distance);
    }
}
