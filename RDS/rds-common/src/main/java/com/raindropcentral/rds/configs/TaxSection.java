/*
 * TaxSection.java
 *
 * @author ItsRainingHP
 * @version 5.0.0
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
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Represents the tax configuration section.
 */
@CSAlways
@SuppressWarnings("unused")
public class TaxSection extends AConfigSection {

    private static final long DEFAULT_DURATION_TICKS = 24L * 60L * 60L * 20L;
    private static final DateTimeFormatter TWENTY_FOUR_HOUR_FORMAT = DateTimeFormatter.ofPattern("H:mm");
    private static final DateTimeFormatter TWELVE_HOUR_FORMAT = DateTimeFormatter.ofPattern("h[:mm]a", Locale.US);

    private Map<String, TaxCurrencySection> currencies;
    private Long duration;
    private String start_time;
    private String time_zone;
    private Boolean join_notification;

    /**
     * Creates a new tax section.
     *
     * @param baseEnvironment evaluation environment used for config expressions
     */
    public TaxSection(
            final EvaluationEnvironmentBuilder baseEnvironment
    ) {
        super(baseEnvironment);
    }

    /**
     * Returns the currencies.
     *
     * @return the currencies
     */
    public @NotNull Map<String, TaxCurrencySection> getCurrencies() {
        return this.currencies == null
                ? new LinkedHashMap<>()
                : new LinkedHashMap<>(this.currencies);
    }

    public @NotNull TaxCurrencySection getTaxCurrency(
            final @Nullable String currencyType
    ) {
        final String normalizedType = currencyType == null || currencyType.isBlank()
                ? "vault"
                : currencyType.trim().toLowerCase(Locale.ROOT);

        if (this.currencies != null && !this.currencies.isEmpty()) {
            final TaxCurrencySection directMatch = this.currencies.get(normalizedType);
            if (directMatch != null) {
                return directMatch;
            }

            for (final Map.Entry<String, TaxCurrencySection> entry : this.currencies.entrySet()) {
                if (entry.getKey().equalsIgnoreCase(normalizedType)) {
                    return entry.getValue();
                }
            }

            return this.currencies.values().iterator().next();
        }

        return createFallbackCurrency(normalizedType);
    }

    /**
     * Returns the duration ticks.
     *
     * @return the duration ticks
     */
    public long getDurationTicks() {
        if (this.duration == null) {
            return DEFAULT_DURATION_TICKS;
        }

        return Math.max(1L, this.duration);
    }

    /**
     * Returns the start time.
     *
     * @return the start time
     */
    public @NotNull LocalTime getStartTime() {
        return this.parseStartTime(this.start_time);
    }

    /**
     * Returns the time zone id.
     *
     * @return the time zone id
     */
    public @NotNull ZoneId getTimeZoneId() {
        return this.parseTimeZone(this.time_zone);
    }

    /**
     * Executes should notify on join.
     *
     * @return {@code true} if notify on join; otherwise {@code false}
     */
    public boolean shouldNotifyOnJoin() {
        return this.join_notification == null || this.join_notification;
    }

    private @NotNull LocalTime parseStartTime(
            final @Nullable String rawValue
    ) {
        if (rawValue == null || rawValue.isBlank()) {
            return LocalTime.NOON;
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

        final String meridiemNormalized = compact
                .replace(".", "")
                .replace("am", "am")
                .replace("pm", "pm");
        try {
            return LocalTime.parse(meridiemNormalized.toUpperCase(Locale.US), TWELVE_HOUR_FORMAT);
        } catch (DateTimeParseException ignored) {
        }

        try {
            return LocalTime.parse(compact);
        } catch (DateTimeParseException ignored) {
            return LocalTime.NOON;
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

    public void setContext(
            final @NotNull Map<String, TaxCurrencySection> currencies,
            final long duration,
            final @Nullable String startTime,
            final @Nullable String timeZone,
            final boolean joinNotification
    ) {
        this.currencies = new LinkedHashMap<>(currencies);
        this.duration = duration;
        this.start_time = startTime;
        this.time_zone = timeZone;
        this.join_notification = joinNotification;
    }

    public static @NotNull TaxSection fromFile(
            final @NotNull File configFile,
            final @Nullable String defaultCurrencyType
    ) {
        final TaxSection section = createDefault(defaultCurrencyType);
        final YamlConfiguration configuration = YamlConfiguration.loadConfiguration(configFile);
        final ConfigurationSection taxesSection = configuration.getConfigurationSection("taxes");
        if (taxesSection == null) {
            return section;
        }

        final String fallbackCurrencyType = normalizeCurrencyType(defaultCurrencyType);
        final Map<String, TaxCurrencySection> currencies = parseCurrencies(taxesSection, fallbackCurrencyType);
        section.setContext(
                currencies,
                Math.max(1L, taxesSection.getLong("duration", DEFAULT_DURATION_TICKS)),
                taxesSection.getString("start_time", "noon"),
                taxesSection.getString("time_zone", ZoneId.systemDefault().getId()),
                taxesSection.getBoolean("join_notification", true)
        );
        return section;
    }

    public static @NotNull TaxSection createDefault(
            final @Nullable String defaultCurrencyType
    ) {
        final TaxSection section = new TaxSection(new EvaluationEnvironmentBuilder());
        section.setContext(
                createFallbackCurrencies(normalizeCurrencyType(defaultCurrencyType)),
                DEFAULT_DURATION_TICKS,
                "noon",
                ZoneId.systemDefault().getId(),
                true
        );
        return section;
    }

    private static @NotNull Map<String, TaxCurrencySection> parseCurrencies(
            final @NotNull ConfigurationSection taxesSection,
            final @NotNull String fallbackCurrencyType
    ) {
        final Map<String, TaxCurrencySection> currencies = new LinkedHashMap<>();
        for (final String key : taxesSection.getKeys(false)) {
            if (key == null || key.isBlank()) {
                continue;
            }

            if ("duration".equalsIgnoreCase(key)
                    || "start_time".equalsIgnoreCase(key)
                    || "time_zone".equalsIgnoreCase(key)
                    || "join_notification".equalsIgnoreCase(key)) {
                continue;
            }

            if (!taxesSection.isConfigurationSection(key)) {
                continue;
            }

            final ConfigurationSection currencySection = taxesSection.getConfigurationSection(key);
            if (currencySection == null) {
                continue;
            }

            final String normalizedType = normalizeCurrencyType(key);
            currencies.put(normalizedType, createCurrencySection(normalizedType, currencySection));
        }

        if (!currencies.isEmpty()) {
            return currencies;
        }

        if (taxesSection.contains("initial_cost")
                || taxesSection.contains("growth_rate")
                || taxesSection.contains("maximum_tax")) {
            currencies.put(
                    fallbackCurrencyType,
                    createCurrencySection(fallbackCurrencyType, taxesSection)
            );
            return currencies;
        }

        return createFallbackCurrencies(fallbackCurrencyType);
    }

    private static @NotNull Map<String, TaxCurrencySection> createFallbackCurrencies(
            final @NotNull String currencyType
    ) {
        final Map<String, TaxCurrencySection> fallback = new LinkedHashMap<>();
        fallback.put(currencyType, createFallbackCurrency(currencyType));
        return fallback;
    }

    private static @NotNull TaxCurrencySection createCurrencySection(
            final @NotNull String currencyType,
            final @NotNull ConfigurationSection section
    ) {
        final TaxCurrencySection currencySection = new TaxCurrencySection(new EvaluationEnvironmentBuilder());
        currencySection.setContext(
                currencyType,
                section.getDouble("initial_cost", 100.0D),
                section.getDouble("growth_rate", 1.125D),
                section.contains("maximum_tax") ? section.getDouble("maximum_tax") : -1D
        );
        return currencySection;
    }

    private static @NotNull TaxCurrencySection createFallbackCurrency(
            final @NotNull String currencyType
    ) {
        final TaxCurrencySection fallback = new TaxCurrencySection(new EvaluationEnvironmentBuilder());
        fallback.setContext(currencyType, 100.0D, 1.125D, -1D);
        return fallback;
    }

    private static @NotNull String normalizeCurrencyType(
            final @Nullable String currencyType
    ) {
        if (currencyType == null || currencyType.isBlank()) {
            return "vault";
        }

        return currencyType.trim().toLowerCase(Locale.ROOT);
    }
}