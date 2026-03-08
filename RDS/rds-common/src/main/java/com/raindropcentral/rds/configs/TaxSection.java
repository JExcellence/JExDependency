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
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
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
    private Double never_item_penalty_rate;
    private Map<String, Double> maximum_bankruptcy_amount;
    private Double maximum_bankruptcy_amount_default;

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

    /**
     * Returns the configured per-item tax penalty rate for listings marked as never available.
     *
     * @return non-negative penalty rate multiplier applied per never-available item
     */
    public double getNeverItemPenaltyRate() {
        return this.never_item_penalty_rate == null
                ? 0.25D
                : Math.max(0D, this.never_item_penalty_rate);
    }

    /**
     * Returns the configured bankruptcy debt caps by currency.
     *
     * <p>Each returned value is either a positive cap or {@code -1.0} for unlimited debt.</p>
     *
     * @return normalized map of per-currency bankruptcy debt caps
     */
    public @NotNull Map<String, Double> getMaximumBankruptcyAmounts() {
        return this.maximum_bankruptcy_amount == null
                ? Map.of()
                : new LinkedHashMap<>(this.maximum_bankruptcy_amount);
    }

    /**
     * Returns the maximum bankruptcy debt tracked for one currency.
     *
     * <p>When a currency-specific cap is configured, that value is used. When no currency-specific
     * entry exists, a legacy global fallback cap is used when present. Non-positive values are
     * treated as unlimited ({@code -1.0}).</p>
     *
     * @param currencyType currency identifier
     * @return positive cap amount for that currency, or {@code -1.0} when uncapped
     */
    public double getMaximumBankruptcyAmount(
            final @Nullable String currencyType
    ) {
        final String normalizedCurrencyType = normalizeCurrencyType(currencyType);
        final Double configuredMaximum = this.maximum_bankruptcy_amount == null
                ? null
                : this.maximum_bankruptcy_amount.get(normalizedCurrencyType);

        if (configuredMaximum != null) {
            return normalizeMaximumBankruptcyAmount(configuredMaximum);
        }

        return this.maximum_bankruptcy_amount_default == null
                ? -1D
                : normalizeMaximumBankruptcyAmount(this.maximum_bankruptcy_amount_default);
    }

    /**
     * Returns the maximum bankruptcy debt tracked for the default {@code vault} currency.
     *
     * @return positive cap amount for {@code vault}, or {@code -1.0} when uncapped
     */
    public double getMaximumBankruptcyAmount() {
        return this.getMaximumBankruptcyAmount("vault");
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

    /**
     * Sets parsed tax-section values.
     *
     * @param currencies configured tax currencies
     * @param duration duration between tax runs in ticks
     * @param startTime configured tax start time expression
     * @param timeZone configured tax time zone expression
     * @param joinNotification whether join notifications are enabled
     * @param neverItemPenaltyRate multiplier applied per never-available item
     * @param maximumBankruptcyAmounts per-currency bankruptcy debt caps
     * @param defaultMaximumBankruptcyAmount optional fallback cap used when a currency-specific cap is not configured
     */
    public void setContext(
            final @NotNull Map<String, TaxCurrencySection> currencies,
            final long duration,
            final @Nullable String startTime,
            final @Nullable String timeZone,
            final boolean joinNotification,
            final double neverItemPenaltyRate,
            final @NotNull Map<String, Double> maximumBankruptcyAmounts,
            final @Nullable Double defaultMaximumBankruptcyAmount
    ) {
        this.currencies = new LinkedHashMap<>(currencies);
        this.duration = duration;
        this.start_time = startTime;
        this.time_zone = timeZone;
        this.join_notification = joinNotification;
        this.never_item_penalty_rate = Math.max(0D, neverItemPenaltyRate);
        final Map<String, Double> normalizedMaximums = new LinkedHashMap<>();
        for (final Map.Entry<String, Double> entry : maximumBankruptcyAmounts.entrySet()) {
            if (entry.getKey() == null || entry.getKey().isBlank() || entry.getValue() == null) {
                continue;
            }

            normalizedMaximums.put(
                    normalizeCurrencyType(entry.getKey()),
                    normalizeMaximumBankruptcyAmount(entry.getValue())
            );
        }
        this.maximum_bankruptcy_amount = normalizedMaximums;
        this.maximum_bankruptcy_amount_default = defaultMaximumBankruptcyAmount == null
                ? null
                : normalizeMaximumBankruptcyAmount(defaultMaximumBankruptcyAmount);
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
        final MaximumBankruptcyConfiguration maximumBankruptcyConfiguration =
                parseMaximumBankruptcyConfiguration(taxesSection);
        section.setContext(
                currencies,
                Math.max(1L, taxesSection.getLong("duration", DEFAULT_DURATION_TICKS)),
                taxesSection.getString("start_time", "noon"),
                taxesSection.getString("time_zone", ZoneId.systemDefault().getId()),
                taxesSection.getBoolean("join_notification", true),
                Math.max(0D, taxesSection.getDouble("never_item_penalty_rate", 0.25D)),
                maximumBankruptcyConfiguration.maximumsByCurrency(),
                maximumBankruptcyConfiguration.defaultMaximum()
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
                true,
                0.25D,
                createDefaultMaximumBankruptcyAmounts(),
                null
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
                    || "join_notification".equalsIgnoreCase(key)
                    || "never_item_penalty_rate".equalsIgnoreCase(key)
                    || "maximum_bankruptcy_amount".equalsIgnoreCase(key)) {
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

    private static @NotNull Map<String, Double> createDefaultMaximumBankruptcyAmounts() {
        final Map<String, Double> defaults = new LinkedHashMap<>();
        defaults.put("vault", -1D);
        defaults.put("raindrops", -1D);
        return defaults;
    }

    private static @NotNull MaximumBankruptcyConfiguration parseMaximumBankruptcyConfiguration(
            final @NotNull ConfigurationSection taxesSection
    ) {
        final ConfigurationSection maximumSection =
                taxesSection.getConfigurationSection("maximum_bankruptcy_amount");
        if (maximumSection == null) {
            final Object legacyMaximum = taxesSection.get("maximum_bankruptcy_amount");
            final Double parsedLegacyMaximum = parseMaximumBankruptcyValue(legacyMaximum);
            return new MaximumBankruptcyConfiguration(
                    Map.of(),
                    parsedLegacyMaximum == null ? null : normalizeMaximumBankruptcyAmount(parsedLegacyMaximum)
            );
        }

        final Map<String, Double> parsedMaximums = new LinkedHashMap<>();
        for (final String rawCurrencyType : maximumSection.getKeys(false)) {
            if (rawCurrencyType == null || rawCurrencyType.isBlank()) {
                continue;
            }

            final Double parsedMaximum = parseMaximumBankruptcyValue(maximumSection.get(rawCurrencyType));
            if (parsedMaximum == null) {
                continue;
            }

            parsedMaximums.put(
                    normalizeCurrencyType(rawCurrencyType),
                    normalizeMaximumBankruptcyAmount(parsedMaximum)
            );
        }
        return new MaximumBankruptcyConfiguration(parsedMaximums, null);
    }

    private static @Nullable Double parseMaximumBankruptcyValue(
            final @Nullable Object rawValue
    ) {
        if (rawValue instanceof Number numberValue) {
            return numberValue.doubleValue();
        }

        if (rawValue instanceof String stringValue) {
            final String normalizedValue = stringValue.trim();
            if (normalizedValue.isEmpty()) {
                return null;
            }

            try {
                return Double.parseDouble(normalizedValue);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }

        return null;
    }

    private static double normalizeMaximumBankruptcyAmount(
            final double maximumBankruptcyAmount
    ) {
        return maximumBankruptcyAmount > 0D ? maximumBankruptcyAmount : -1D;
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

    private record MaximumBankruptcyConfiguration(
            @NotNull Map<String, Double> maximumsByCurrency,
            @Nullable Double defaultMaximum
    ) {
    }
}
