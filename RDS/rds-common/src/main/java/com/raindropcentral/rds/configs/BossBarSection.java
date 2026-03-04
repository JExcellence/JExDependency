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