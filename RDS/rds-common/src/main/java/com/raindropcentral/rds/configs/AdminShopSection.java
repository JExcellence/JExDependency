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
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.time.DateTimeException;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

/**
 * Represents the admin shop configuration section.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
@CSAlways
@SuppressWarnings("unused")
public class AdminShopSection extends AConfigSection {

    private static final long DEFAULT_RESTOCK_CHECK_PERIOD_TICKS = 20L;
    private static final long DEFAULT_RESET_TIMER_TICKS = 1200L;
    private static final DateTimeFormatter TWENTY_FOUR_HOUR_FORMAT = DateTimeFormatter.ofPattern("H:mm");
    private static final DateTimeFormatter TWELVE_HOUR_FORMAT = DateTimeFormatter.ofPattern("h[:mm]a", Locale.US);

    private String restock_mode;
    private Long restock_check_period_ticks;
    private Long default_reset_timer_ticks;
    private String full_restock_time;
    private String time_zone;

    /**
     * Creates a new admin shop section.
     *
     * @param baseEnvironment evaluation environment used for config expressions
     */
    public AdminShopSection(
            final EvaluationEnvironmentBuilder baseEnvironment
    ) {
        super(baseEnvironment);
    }

    /**
     * Loads the admin-shop restock configuration from the plugin config file.
     *
     * @param configFile plugin config file
     * @return parsed admin-shop section
     */
    public static @NotNull AdminShopSection fromFile(final @NotNull File configFile) {
        final AdminShopSection section = new AdminShopSection(new EvaluationEnvironmentBuilder());
        final ConfigurationSection adminShopSection = YamlConfiguration.loadConfiguration(configFile)
            .getConfigurationSection("admin_shops");
        if (adminShopSection == null) {
            return section;
        }

        section.restock_mode = adminShopSection.getString("restock_mode");
        section.restock_check_period_ticks = adminShopSection.contains("restock_check_period_ticks")
            ? adminShopSection.getLong("restock_check_period_ticks")
            : null;
        section.default_reset_timer_ticks = adminShopSection.contains("default_reset_timer_ticks")
            ? adminShopSection.getLong("default_reset_timer_ticks")
            : null;
        section.full_restock_time = adminShopSection.getString("full_restock_time");
        section.time_zone = adminShopSection.getString("time_zone");
        return section;
    }

    /**
     * Returns the restock mode.
     *
     * @return the restock mode
     */
    public @NotNull AdminShopRestockMode getRestockMode() {
        if (this.restock_mode == null || this.restock_mode.isBlank()) {
            return AdminShopRestockMode.GRADUAL;
        }

        return switch (this.restock_mode.trim().toLowerCase(Locale.ROOT)) {
            case "full", "full_at_time", "scheduled_full" -> AdminShopRestockMode.FULL_AT_TIME;
            default -> AdminShopRestockMode.GRADUAL;
        };
    }

    /**
     * Returns the restock check period ticks.
     *
     * @return the restock check period ticks
     */
    public long getRestockCheckPeriodTicks() {
        if (this.restock_check_period_ticks == null) {
            return DEFAULT_RESTOCK_CHECK_PERIOD_TICKS;
        }

        return Math.max(1L, this.restock_check_period_ticks);
    }

    /**
     * Returns the default reset timer ticks.
     *
     * @return the default reset timer ticks
     */
    public long getDefaultResetTimerTicks() {
        if (this.default_reset_timer_ticks == null) {
            return DEFAULT_RESET_TIMER_TICKS;
        }

        return Math.max(1L, this.default_reset_timer_ticks);
    }

    /**
     * Returns the full restock time.
     *
     * @return the full restock time
     */
    public @NotNull LocalTime getFullRestockTime() {
        return this.parseTime(this.full_restock_time);
    }

    /**
     * Returns the time zone id.
     *
     * @return the time zone id
     */
    public @NotNull ZoneId getTimeZoneId() {
        return this.parseTimeZone(this.time_zone);
    }

    private @NotNull LocalTime parseTime(
            final @Nullable String rawValue
    ) {
        if (rawValue == null || rawValue.isBlank()) {
            return LocalTime.MIDNIGHT;
        }

        final String normalized = rawValue.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "noon" -> LocalTime.NOON;
            case "midnight" -> LocalTime.MIDNIGHT;
            default -> this.parseExplicitTime(normalized);
        };
    }

    private @NotNull LocalTime parseExplicitTime(
            final @NotNull String normalized
    ) {
        final String compact = normalized.replace(" ", "");
        try {
            return LocalTime.parse(compact, TWENTY_FOUR_HOUR_FORMAT);
        } catch (DateTimeParseException ignored) {
        }

        try {
            return LocalTime.parse(compact.toUpperCase(Locale.US), TWELVE_HOUR_FORMAT);
        } catch (DateTimeParseException ignored) {
        }

        try {
            return LocalTime.parse(compact);
        } catch (DateTimeParseException ignored) {
            return LocalTime.MIDNIGHT;
        }
    }

    private @NotNull ZoneId parseTimeZone(
            final @Nullable String rawValue
    ) {
        if (rawValue == null || rawValue.isBlank()) {
            return ZoneId.systemDefault();
        }

        final String normalized = rawValue.trim();
        if (normalized.equalsIgnoreCase("system")
                || normalized.equalsIgnoreCase("server")
                || normalized.equalsIgnoreCase("default")) {
            return ZoneId.systemDefault();
        }

        try {
            return ZoneId.of(normalized);
        } catch (DateTimeException ignored) {
        }

        try {
            return ZoneId.of(normalized.toUpperCase(Locale.ROOT));
        } catch (DateTimeException ignored) {
            return ZoneId.systemDefault();
        }
    }
}
