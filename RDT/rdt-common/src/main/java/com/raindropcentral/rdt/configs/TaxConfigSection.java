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

package com.raindropcentral.rdt.configs;

import com.raindropcentral.rdt.utils.ChunkType;
import com.raindropcentral.rdt.utils.TownArchetype;
import de.jexcellence.gpeee.GPEEE;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.DateTimeException;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * Parsed configuration snapshot for scheduled town and nation taxes.
 *
 * <p>The tax configuration keeps schedule, currency-rate expression, item-tax tables, and debt
 * policy in one immutable snapshot so the runtime can reload the file safely whenever needed.</p>
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public final class TaxConfigSection {

    private static final Logger LOGGER = Logger.getLogger(TaxConfigSection.class.getName());
    private static final long DEFAULT_DURATION_TICKS = 24L * 60L * 60L * 20L;
    private static final DateTimeFormatter TWENTY_FOUR_HOUR_FORMAT = DateTimeFormatter.ofPattern("H:mm");
    private static final DateTimeFormatter TWELVE_HOUR_FORMAT = DateTimeFormatter.ofPattern("h[:mm]a", Locale.US);
    private static final double DEFAULT_BASE_TAX_PERCENT = 2.0D;
    private static final double DEFAULT_MINIMUM_TAX_PERCENT = 0.0D;
    private static final double DEFAULT_MAXIMUM_TAX_PERCENT = 25.0D;
    private static final String DEFAULT_RATE_EXPRESSION = "base_tax_percent";
    private static final List<String> DEFAULT_CURRENCY_IDS = List.of("vault", "raindrops");
    private static final long DEFAULT_GRACE_PERIOD_TICKS = 7L * 24L * 60L * 60L * 20L;
    private static final long DEFAULT_WARNING_INTERVAL_TICKS = 60L * 60L * 20L;

    private final TaxSchedule schedule;
    private final CurrencyTaxSettings currency;
    private final Map<TownArchetype, Double> archetypeModifiers;
    private final Map<ChunkType, Map<String, Integer>> townItemTaxes;
    private final Map<ChunkType, Map<String, Integer>> nationItemTaxes;
    private final DebtSettings debt;

    private TaxConfigSection(
        final @NotNull TaxSchedule schedule,
        final @NotNull CurrencyTaxSettings currency,
        final @NotNull Map<TownArchetype, Double> archetypeModifiers,
        final @NotNull Map<ChunkType, Map<String, Integer>> townItemTaxes,
        final @NotNull Map<ChunkType, Map<String, Integer>> nationItemTaxes,
        final @NotNull DebtSettings debt
    ) {
        this.schedule = Objects.requireNonNull(schedule, "schedule");
        this.currency = Objects.requireNonNull(currency, "currency");
        this.archetypeModifiers = Map.copyOf(Objects.requireNonNull(archetypeModifiers, "archetypeModifiers"));
        this.townItemTaxes = copyItemTaxes(townItemTaxes);
        this.nationItemTaxes = copyItemTaxes(nationItemTaxes);
        this.debt = Objects.requireNonNull(debt, "debt");
    }

    /**
     * Returns the configured tax schedule.
     *
     * @return immutable schedule snapshot
     */
    public @NotNull TaxSchedule getSchedule() {
        return this.schedule;
    }

    /**
     * Returns the configured currency-tax settings.
     *
     * @return immutable currency-tax settings
     */
    public @NotNull CurrencyTaxSettings getCurrency() {
        return this.currency;
    }

    /**
     * Returns the configured archetype modifiers used by the rate expression.
     *
     * @return immutable archetype modifier map
     */
    public @NotNull Map<TownArchetype, Double> getArchetypeModifiers() {
        return this.archetypeModifiers;
    }

    /**
     * Returns the configured town item taxes keyed by chunk type.
     *
     * @return immutable town item-tax table
     */
    public @NotNull Map<ChunkType, Map<String, Integer>> getTownItemTaxes() {
        return this.townItemTaxes;
    }

    /**
     * Returns the configured nation item taxes keyed by chunk type.
     *
     * @return immutable nation item-tax table
     */
    public @NotNull Map<ChunkType, Map<String, Integer>> getNationItemTaxes() {
        return this.nationItemTaxes;
    }

    /**
     * Returns the configured debt-policy settings.
     *
     * @return immutable debt settings
     */
    public @NotNull DebtSettings getDebt() {
        return this.debt;
    }

    /**
     * Returns the configured archetype modifier for one town archetype.
     *
     * @param archetype archetype to resolve
     * @return configured modifier, or {@code 0.0} when the archetype is not configured
     */
    public double getArchetypeModifier(final @Nullable TownArchetype archetype) {
        return archetype == null ? 0.0D : this.archetypeModifiers.getOrDefault(archetype, 0.0D);
    }

    /**
     * Parses a tax config snapshot from one YAML file.
     *
     * @param file source config file
     * @return parsed tax config snapshot
     */
    public static @NotNull TaxConfigSection fromFile(final @NotNull File file) {
        Objects.requireNonNull(file, "file");
        return fromConfiguration(YamlConfiguration.loadConfiguration(file));
    }

    /**
     * Parses a tax config snapshot from a UTF-8 YAML stream.
     *
     * @param inputStream source config stream
     * @return parsed tax config snapshot
     */
    public static @NotNull TaxConfigSection fromInputStream(final @NotNull InputStream inputStream) {
        Objects.requireNonNull(inputStream, "inputStream");
        try (InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
            return fromConfiguration(YamlConfiguration.loadConfiguration(reader));
        } catch (final Exception exception) {
            throw new IllegalStateException("Failed to read tax config stream", exception);
        }
    }

    /**
     * Returns a tax config snapshot populated with built-in defaults.
     *
     * @return default tax config snapshot
     */
    public static @NotNull TaxConfigSection createDefault() {
        return new TaxConfigSection(
            TaxSchedule.createDefault(),
            CurrencyTaxSettings.createDefault(),
            createDefaultArchetypeModifiers(),
            Map.of(),
            Map.of(),
            DebtSettings.createDefault()
        );
    }

    private static @NotNull TaxConfigSection fromConfiguration(final @NotNull YamlConfiguration configuration) {
        return new TaxConfigSection(
            TaxSchedule.fromConfiguration(configuration),
            CurrencyTaxSettings.fromConfiguration(configuration.getConfigurationSection("currency_tax")),
            parseArchetypeModifiers(configuration.getConfigurationSection("archetype_modifiers")),
            parseItemTaxes(configuration.getConfigurationSection("town_item_taxes")),
            parseItemTaxes(configuration.getConfigurationSection("nation_item_taxes")),
            DebtSettings.fromConfiguration(configuration.getConfigurationSection("debt"))
        );
    }

    private static @NotNull Map<TownArchetype, Double> createDefaultArchetypeModifiers() {
        final Map<TownArchetype, Double> modifiers = new EnumMap<>(TownArchetype.class);
        for (final TownArchetype archetype : TownArchetype.values()) {
            modifiers.put(archetype, 0.0D);
        }
        return modifiers;
    }

    private static @NotNull Map<TownArchetype, Double> parseArchetypeModifiers(final @Nullable ConfigurationSection section) {
        final Map<TownArchetype, Double> modifiers = new EnumMap<>(createDefaultArchetypeModifiers());
        if (section == null) {
            return Map.copyOf(modifiers);
        }

        for (final String rawKey : section.getKeys(false)) {
            final TownArchetype archetype = TownArchetype.fromString(rawKey);
            if (archetype == null) {
                continue;
            }
            modifiers.put(archetype, section.getDouble(rawKey, 0.0D));
        }
        return Map.copyOf(modifiers);
    }

    private static @NotNull Map<ChunkType, Map<String, Integer>> parseItemTaxes(final @Nullable ConfigurationSection section) {
        if (section == null) {
            return Map.of();
        }

        final Map<ChunkType, Map<String, Integer>> parsed = new EnumMap<>(ChunkType.class);
        for (final String rawChunkType : section.getKeys(false)) {
            final ChunkType chunkType = parseChunkType(rawChunkType);
            final ConfigurationSection itemSection = section.getConfigurationSection(rawChunkType);
            if (chunkType == null || itemSection == null) {
                continue;
            }

            final Map<String, Integer> itemTaxes = new LinkedHashMap<>();
            for (final String rawItemId : itemSection.getKeys(false)) {
                final Material material = Material.matchMaterial(rawItemId == null ? "" : rawItemId.trim().toUpperCase(Locale.ROOT));
                final int amount = itemSection.getInt(rawItemId, 0);
                if (material == null || material.isAir() || amount <= 0) {
                    continue;
                }
                itemTaxes.put(material.name().toLowerCase(Locale.ROOT), amount);
            }
            if (!itemTaxes.isEmpty()) {
                parsed.put(chunkType, Map.copyOf(itemTaxes));
            }
        }
        return copyItemTaxes(parsed);
    }

    private static @NotNull Map<ChunkType, Map<String, Integer>> copyItemTaxes(
        final @Nullable Map<ChunkType, Map<String, Integer>> source
    ) {
        if (source == null || source.isEmpty()) {
            return Map.of();
        }

        final Map<ChunkType, Map<String, Integer>> copied = new EnumMap<>(ChunkType.class);
        for (final Map.Entry<ChunkType, Map<String, Integer>> entry : source.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null || entry.getValue().isEmpty()) {
                continue;
            }
            copied.put(entry.getKey(), Map.copyOf(entry.getValue()));
        }
        return copied.isEmpty() ? Map.of() : Map.copyOf(copied);
    }

    private static @Nullable ChunkType parseChunkType(final @Nullable String rawChunkType) {
        if (rawChunkType == null || rawChunkType.isBlank()) {
            return null;
        }
        try {
            return ChunkType.valueOf(rawChunkType.trim().toUpperCase(Locale.ROOT));
        } catch (final IllegalArgumentException ignored) {
            return null;
        }
    }

    /**
     * Immutable schedule settings for one tax cadence.
     *
     * @param durationTicks interval between tax runs in ticks
     * @param startTime wall-clock start time in the configured time zone
     * @param timeZoneId configured time zone
     */
    public record TaxSchedule(
        long durationTicks,
        @NotNull LocalTime startTime,
        @NotNull ZoneId timeZoneId
    ) {

        /**
         * Creates one immutable tax schedule snapshot.
         *
         * @param durationTicks interval between tax runs in ticks
         * @param startTime wall-clock start time in the configured time zone
         * @param timeZoneId configured time zone
         */
        public TaxSchedule {
            durationTicks = Math.max(1L, durationTicks);
            startTime = Objects.requireNonNull(startTime, "startTime");
            timeZoneId = Objects.requireNonNull(timeZoneId, "timeZoneId");
        }

        private static @NotNull TaxSchedule createDefault() {
            return new TaxSchedule(DEFAULT_DURATION_TICKS, LocalTime.NOON, ZoneId.systemDefault());
        }

        private static @NotNull TaxSchedule fromConfiguration(final @NotNull YamlConfiguration configuration) {
            return new TaxSchedule(
                Math.max(1L, configuration.getLong("duration_ticks", DEFAULT_DURATION_TICKS)),
                parseStartTime(configuration.getString("start_time")),
                parseTimeZone(configuration.getString("time_zone"))
            );
        }
    }

    /**
     * Immutable currency-tax settings for towns and nations.
     *
     * @param baseTaxPercent baseline rate expression input
     * @param minimumTaxPercent lower clamp applied after evaluating the expression
     * @param maximumTaxPercent upper clamp applied after evaluating the expression
     * @param currencyIds charged bank currency identifiers
     * @param rateExpression GPEEE expression that resolves the final tax percent
     */
    public record CurrencyTaxSettings(
        double baseTaxPercent,
        double minimumTaxPercent,
        double maximumTaxPercent,
        @NotNull List<String> currencyIds,
        @NotNull String rateExpression
    ) {

        /**
         * Creates one immutable currency-tax settings snapshot.
         *
         * @param baseTaxPercent baseline rate expression input
         * @param minimumTaxPercent lower clamp applied after evaluating the expression
         * @param maximumTaxPercent upper clamp applied after evaluating the expression
         * @param currencyIds charged bank currency identifiers
         * @param rateExpression GPEEE expression that resolves the final tax percent
         */
        public CurrencyTaxSettings {
            currencyIds = copyCurrencyIds(currencyIds);
            rateExpression = validateRateExpression(rateExpression);
            minimumTaxPercent = Math.max(0.0D, minimumTaxPercent);
            maximumTaxPercent = Math.max(minimumTaxPercent, maximumTaxPercent);
        }

        /**
         * Clamps one calculated tax rate into the configured bounds.
         *
         * @param taxRatePercent calculated tax rate in percent
         * @return bounded tax rate in percent
         */
        public double clampRate(final double taxRatePercent) {
            return Math.clamp(taxRatePercent, this.minimumTaxPercent, this.maximumTaxPercent);
        }

        private static @NotNull CurrencyTaxSettings createDefault() {
            return new CurrencyTaxSettings(
                DEFAULT_BASE_TAX_PERCENT,
                DEFAULT_MINIMUM_TAX_PERCENT,
                DEFAULT_MAXIMUM_TAX_PERCENT,
                DEFAULT_CURRENCY_IDS,
                DEFAULT_RATE_EXPRESSION
            );
        }

        private static @NotNull CurrencyTaxSettings fromConfiguration(final @Nullable ConfigurationSection section) {
            if (section == null) {
                return createDefault();
            }
            return new CurrencyTaxSettings(
                section.getDouble("base_tax_percent", DEFAULT_BASE_TAX_PERCENT),
                section.getDouble("minimum_tax_percent", DEFAULT_MINIMUM_TAX_PERCENT),
                section.getDouble("maximum_tax_percent", DEFAULT_MAXIMUM_TAX_PERCENT),
                section.getStringList("currency_ids"),
                section.getString("rate_expression", DEFAULT_RATE_EXPRESSION)
            );
        }
    }

    /**
     * Immutable debt-policy settings for tax collection.
     *
     * @param gracePeriodTicks maximum debt age before enforcement
     * @param warningIntervalTicks cadence used for debt warnings and grace checks
     * @param joinReminderEnabled whether online join reminders are sent while debt exists
     * @param broadcastTownFall whether town fall events are broadcast server-wide
     * @param broadcastNationDisband whether tax-driven nation disbands are broadcast server-wide
     */
    public record DebtSettings(
        long gracePeriodTicks,
        long warningIntervalTicks,
        boolean joinReminderEnabled,
        boolean broadcastTownFall,
        boolean broadcastNationDisband
    ) {

        /**
         * Creates one immutable debt-policy settings snapshot.
         *
         * @param gracePeriodTicks maximum debt age before enforcement
         * @param warningIntervalTicks cadence used for debt warnings and grace checks
         * @param joinReminderEnabled whether online join reminders are sent while debt exists
         * @param broadcastTownFall whether town fall events are broadcast server-wide
         * @param broadcastNationDisband whether tax-driven nation disbands are broadcast server-wide
         */
        public DebtSettings {
            gracePeriodTicks = Math.max(1L, gracePeriodTicks);
            warningIntervalTicks = Math.max(20L, warningIntervalTicks);
        }

        private static @NotNull DebtSettings createDefault() {
            return new DebtSettings(
                DEFAULT_GRACE_PERIOD_TICKS,
                DEFAULT_WARNING_INTERVAL_TICKS,
                true,
                true,
                true
            );
        }

        private static @NotNull DebtSettings fromConfiguration(final @Nullable ConfigurationSection section) {
            if (section == null) {
                return createDefault();
            }
            return new DebtSettings(
                section.getLong("grace_period_ticks", DEFAULT_GRACE_PERIOD_TICKS),
                section.getLong("warning_interval_ticks", DEFAULT_WARNING_INTERVAL_TICKS),
                section.getBoolean("join_reminder_enabled", true),
                section.getBoolean("broadcast_town_fall", true),
                section.getBoolean("broadcast_nation_disband", true)
            );
        }
    }

    private static @NotNull List<String> copyCurrencyIds(final @Nullable Iterable<String> currencyIds) {
        final LinkedHashSet<String> normalized = new LinkedHashSet<>();
        if (currencyIds != null) {
            for (final String currencyId : currencyIds) {
                if (currencyId == null || currencyId.isBlank()) {
                    continue;
                }
                normalized.add(currencyId.trim().toLowerCase(Locale.ROOT));
            }
        }
        return normalized.isEmpty() ? List.copyOf(DEFAULT_CURRENCY_IDS) : List.copyOf(normalized);
    }

    private static @NotNull String validateRateExpression(final @Nullable String rawExpression) {
        final String expression = rawExpression == null || rawExpression.isBlank()
            ? DEFAULT_RATE_EXPRESSION
            : rawExpression.trim();
        try {
            new GPEEE(LOGGER).parseString(expression);
            return expression;
        } catch (final Exception exception) {
            LOGGER.warning("Invalid RDT tax rate expression '" + expression + "'. Falling back to " + DEFAULT_RATE_EXPRESSION + '.');
            return DEFAULT_RATE_EXPRESSION;
        }
    }

    private static @NotNull LocalTime parseStartTime(final @Nullable String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return LocalTime.NOON;
        }

        final String normalized = rawValue.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "noon" -> LocalTime.NOON;
            case "midnight" -> LocalTime.MIDNIGHT;
            default -> parseExplicitTime(normalized);
        };
    }

    private static @NotNull LocalTime parseExplicitTime(final @NotNull String normalized) {
        final String compact = normalized.replace(" ", "");
        try {
            return LocalTime.parse(compact, TWENTY_FOUR_HOUR_FORMAT);
        } catch (final DateTimeParseException ignored) {
        }

        final String meridiemNormalized = compact
            .replace(".", "")
            .replace("am", "am")
            .replace("pm", "pm");
        try {
            return LocalTime.parse(meridiemNormalized.toUpperCase(Locale.US), TWELVE_HOUR_FORMAT);
        } catch (final DateTimeParseException ignored) {
        }

        try {
            return LocalTime.parse(compact);
        } catch (final DateTimeParseException ignored) {
            return LocalTime.NOON;
        }
    }

    private static @NotNull ZoneId parseTimeZone(final @Nullable String rawValue) {
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
        } catch (final DateTimeException ignored) {
        }

        try {
            return ZoneId.of(normalized.toUpperCase(Locale.ROOT));
        } catch (final DateTimeException ignored) {
            return ZoneId.systemDefault();
        }
    }
}
