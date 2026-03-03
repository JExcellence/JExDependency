package com.raindropcentral.rds.configs;

import de.jexcellence.configmapper.sections.AConfigSection;
import de.jexcellence.configmapper.sections.CSAlways;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.DateTimeException;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

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

    public AdminShopSection(
            final EvaluationEnvironmentBuilder baseEnvironment
    ) {
        super(baseEnvironment);
    }

    public @NotNull AdminShopRestockMode getRestockMode() {
        if (this.restock_mode == null || this.restock_mode.isBlank()) {
            return AdminShopRestockMode.GRADUAL;
        }

        return switch (this.restock_mode.trim().toLowerCase(Locale.ROOT)) {
            case "full", "full_at_time", "scheduled_full" -> AdminShopRestockMode.FULL_AT_TIME;
            default -> AdminShopRestockMode.GRADUAL;
        };
    }

    public long getRestockCheckPeriodTicks() {
        if (this.restock_check_period_ticks == null) {
            return DEFAULT_RESTOCK_CHECK_PERIOD_TICKS;
        }

        return Math.max(1L, this.restock_check_period_ticks);
    }

    public long getDefaultResetTimerTicks() {
        if (this.default_reset_timer_ticks == null) {
            return DEFAULT_RESET_TIMER_TICKS;
        }

        return Math.max(1L, this.default_reset_timer_ticks);
    }

    public @NotNull LocalTime getFullRestockTime() {
        return this.parseTime(this.full_restock_time);
    }

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
