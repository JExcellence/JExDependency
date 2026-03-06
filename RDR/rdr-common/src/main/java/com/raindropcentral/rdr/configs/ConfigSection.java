package com.raindropcentral.rdr.configs;

import de.jexcellence.configmapper.sections.AConfigSection;
import de.jexcellence.configmapper.sections.CSAlways;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringJoiner;
import java.util.logging.Logger;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Root configuration section for the RDR plugin.
 *
 * <p>This section exposes storage-related defaults used when a player first joins the server and when
 * gameplay systems decide whether the player may unlock more storage containers.</p>
 *
 * @author ItsRainingHP
 * @since 5.0.0
 * @version 5.0.0
 */
@CSAlways
@SuppressWarnings("unused")
public class ConfigSection extends AConfigSection {

    private Integer starting_storages;
    private Integer max_storages;
    private Integer max_hotkeys;
    private Boolean warn_missing_requirements;
    private List<String> global_blacklist;
    private Map<Integer, Map<String, StoreRequirementSection>> requirements;
    private String protection_restricted_storages;
    private String protection_taxed_storages;
    private Map<String, Double> protection_open_storage_taxes;
    private Long protection_filled_storage_tax_interval_ticks;
    private Integer protection_filled_storage_maximum_freeze;
    private Map<String, Double> protection_filled_storage_maximum_debt;
    private Map<String, Double> protection_filled_storage_taxes;

    /**
     * Creates a configuration section bound to the provided evaluation environment.
     *
     * @param baseEnvironment base environment used by the config mapper
     * @throws NullPointerException if {@code baseEnvironment} is {@code null}
     */
    public ConfigSection(final @NotNull EvaluationEnvironmentBuilder baseEnvironment) {
        super(baseEnvironment);
    }

    /**
     * Returns how many storages should be created for a brand-new player profile.
     *
     * @return configured starting storage count, defaulting to {@code 1}
     */
    public int getStartingStorages() {
        return this.normalizePositive(this.starting_storages, 1);
    }

    /**
     * Returns the global maximum number of storages a player may own.
     *
     * @return configured storage cap, defaulting to {@code 1}
     */
    public int getMaxStorages() {
        return this.normalizePositive(this.max_storages, 1);
    }

    /**
     * Returns the highest numeric hotkey players may bind for direct storage access.
     *
     * @return configured hotkey cap, defaulting to {@code 9}
     */
    public int getMaxHotkeys() {
        return this.normalizePositive(this.max_hotkeys, 9);
    }

    /**
     * Returns whether missing purchase requirement sets should be logged at startup.
     *
     * @return {@code true} when startup warnings should be emitted
     */
    public boolean shouldWarnMissingRequirements() {
        return this.warn_missing_requirements == null || this.warn_missing_requirements;
    }

    /**
     * Returns the globally blacklisted item material keys that may not be stored in any RDR storage.
     *
     * @return normalized blacklist entries as uppercase material names
     */
    public @NotNull List<String> getGlobalBlacklist() {
        final List<String> blacklist = new ArrayList<>();
        if (this.global_blacklist == null) {
            blacklist.add("NETHER_STAR");
            return blacklist;
        }

        for (final String entry : this.global_blacklist) {
            if (entry == null || entry.isBlank()) {
                continue;
            }
            blacklist.add(entry.trim().toUpperCase(Locale.ROOT));
        }
        return blacklist;
    }

    /**
     * Returns whether the supplied material name is blocked from every RDR storage.
     *
     * @param materialName material name to evaluate
     * @return {@code true} when the material is globally blacklisted
     */
    public boolean isGloballyBlacklisted(final @Nullable String materialName) {
        if (materialName == null || materialName.isBlank()) {
            return false;
        }

        final String normalizedMaterialName = materialName.trim().toUpperCase(Locale.ROOT);
        for (final String entry : this.getGlobalBlacklist()) {
            if (entry.equals(normalizedMaterialName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns all configured purchase-indexed storage requirements.
     *
     * <p>The outer key is the one-based shop purchase number and the inner map contains the
     * requirement entries for that purchase.</p>
     *
     * @return defensive copy of the configured purchase requirement map
     */
    public @NotNull Map<Integer, Map<String, StoreRequirementSection>> getRequirements() {
        final Map<Integer, Map<String, StoreRequirementSection>> copy = new LinkedHashMap<>();
        if (this.requirements == null) {
            return copy;
        }

        for (final Map.Entry<Integer, Map<String, StoreRequirementSection>> entry : this.requirements.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                continue;
            }
            copy.put(entry.getKey(), new LinkedHashMap<>(entry.getValue()));
        }
        return copy;
    }

    /**
     * Returns the requirement entries for the requested shop purchase number.
     *
     * @param purchaseNumber one-based shop purchase number
     * @return requirement map for the requested purchase, or an empty map when none is configured
     */
    public @NotNull Map<String, StoreRequirementSection> getRequirementsForPurchase(final int purchaseNumber) {
        final Map<String, StoreRequirementSection> purchaseRequirements = this.getRequirements().get(purchaseNumber);
        return purchaseRequirements == null ? new LinkedHashMap<>() : new LinkedHashMap<>(purchaseRequirements);
    }

    /**
     * Returns the storage selector used for protection-area access restrictions.
     *
     * <p>Supported selectors are {@code none}, {@code all}, or one-based range lists such as
     * {@code 1-3,5}.</p>
     *
     * @return normalized selector for protection access rules
     */
    public @NotNull String getProtectionRestrictedStorages() {
        return normalizeStorageSelector(this.protection_restricted_storages, "none");
    }

    /**
     * Returns the storage selector used to decide which storages should charge open taxes.
     *
     * <p>Supported selectors are {@code none}, {@code all}, or one-based range lists such as
     * {@code 1-3,5}.</p>
     *
     * @return normalized selector for storage tax rules
     */
    public @NotNull String getProtectionTaxedStorages() {
        return normalizeStorageSelector(this.protection_taxed_storages, "all");
    }

    /**
     * Returns configured per-open storage taxes keyed by currency identifier.
     *
     * <p>Tax amounts are normalized to be non-negative and always include defaults for
     * {@code vault} and {@code raindrops}.</p>
     *
     * @return normalized currency tax map
     */
    public @NotNull Map<String, Double> getProtectionOpenStorageTaxes() {
        return normalizeProtectionTaxes(this.protection_open_storage_taxes);
    }

    /**
     * Returns the configured recurring tax interval for non-empty storages.
     *
     * @return recurring tax period in Bukkit ticks, or {@code 0} when disabled
     */
    public long getProtectionFilledStorageTaxIntervalTicks() {
        return normalizeNonNegative(this.protection_filled_storage_tax_interval_ticks, 1_728_000L);
    }

    /**
     * Returns the maximum number of storages per owner that may be frozen for unpaid tax debt.
     *
     * <p>Positive values cap frozen storages, {@code 0} disallows creating new frozen storages, and
     * negative values allow unlimited freezes.</p>
     *
     * @return configured maximum freeze cap
     */
    public int getProtectionFilledStorageMaximumFreeze() {
        return this.protection_filled_storage_maximum_freeze == null
            ? -1
            : this.protection_filled_storage_maximum_freeze;
    }

    /**
     * Returns the per-storage debt cap for recurring filled-storage taxes keyed by currency identifier.
     *
     * <p>Positive values cap how much debt a single storage may hold for that currency. Values less than
     * or equal to {@code 0} are treated as unlimited ({@code -1}). The returned map always includes
     * defaults for {@code vault} and {@code raindrops}.</p>
     *
     * @return normalized per-currency maximum storage debt map
     */
    public @NotNull Map<String, Double> getProtectionFilledStorageMaximumDebtByCurrency() {
        return normalizeProtectionMaximumDebt(this.protection_filled_storage_maximum_debt);
    }

    /**
     * Returns configured recurring taxes for non-empty storages keyed by currency identifier.
     *
     * <p>Tax amounts are normalized to be non-negative and always include defaults for
     * {@code vault} and {@code raindrops}.</p>
     *
     * @return normalized recurring currency tax map
     */
    public @NotNull Map<String, Double> getProtectionFilledStorageTaxes() {
        return normalizeProtectionTaxes(this.protection_filled_storage_taxes);
    }

    /**
     * Returns whether the supplied storage key is covered by protection-area restrictions.
     *
     * @param storageKey storage key to evaluate
     * @return {@code true} when the storage key matches the protection selector
     */
    public boolean isProtectionRestrictedStorage(final @Nullable String storageKey) {
        return this.matchesStorageSelector(this.getProtectionRestrictedStorages(), storageKey);
    }

    /**
     * Returns whether the supplied storage key is covered by storage tax rules.
     *
     * @param storageKey storage key to evaluate
     * @return {@code true} when the storage key matches the tax selector
     */
    public boolean isProtectionTaxedStorage(final @Nullable String storageKey) {
        return this.matchesStorageSelector(this.getProtectionTaxedStorages(), storageKey);
    }

    /**
     * Returns the shop purchase numbers required by the current storage limits that have no requirement entries.
     *
     * @return ordered list of missing purchase numbers
     */
    public @NotNull List<Integer> getMissingRequirementPurchases() {
        final List<Integer> missing = new ArrayList<>();
        final Map<Integer, Map<String, StoreRequirementSection>> configuredRequirements = this.getRequirements();
        for (int purchaseNumber = 1; purchaseNumber <= this.getRequiredPurchaseCount(); purchaseNumber++) {
            final Map<String, StoreRequirementSection> purchaseRequirements = configuredRequirements.get(purchaseNumber);
            if (purchaseRequirements == null || purchaseRequirements.isEmpty()) {
                missing.add(purchaseNumber);
            }
        }
        return missing;
    }

    /**
     * Logs a startup warning for any storage purchases that do not have explicit requirements.
     *
     * @param logger logger that should receive the warning
     * @throws NullPointerException if {@code logger} is {@code null}
     */
    public void logMissingRequirementWarnings(final @NotNull Logger logger) {
        if (!this.shouldWarnMissingRequirements()) {
            return;
        }

        final List<Integer> missingPurchases = this.getMissingRequirementPurchases();
        if (missingPurchases.isEmpty()) {
            return;
        }

        final StringJoiner joiner = new StringJoiner(", ");
        for (final Integer purchaseNumber : missingPurchases) {
            joiner.add(String.valueOf(purchaseNumber));
        }

        logger.warning(
            "Storage shop purchase tiers without configured requirements: " + joiner
                + ". These purchases will be available with no requirements."
        );
    }

    /**
     * Returns the number of storages that should actually be provisioned during first-join creation.
     *
     * <p>The provisioned amount is always clamped to the configured maximum so invalid configurations do
     * not create more storages than the global cap allows.</p>
     *
     * @return starting storage count clamped to the configured maximum
     */
    public int getInitialProvisionedStorages() {
        return Math.min(this.getStartingStorages(), this.getMaxStorages());
    }

    /**
     * Creates a configuration section by reading the plugin config file directly.
     *
     * @param configFile config file to parse
     * @return parsed config section with fallback defaults when file values are absent
     * @throws NullPointerException if {@code configFile} is {@code null}
     */
    public static @NotNull ConfigSection fromFile(final @NotNull File configFile) {
        final ConfigSection section = createDefault();
        final YamlConfiguration configuration = YamlConfiguration.loadConfiguration(configFile);

        section.starting_storages = configuration.contains("starting_storages")
            ? configuration.getInt("starting_storages")
            : section.starting_storages;
        section.max_storages = configuration.contains("max_storages")
            ? configuration.getInt("max_storages")
            : section.max_storages;
        section.max_hotkeys = configuration.contains("max_hotkeys")
            ? configuration.getInt("max_hotkeys")
            : section.max_hotkeys;
        section.warn_missing_requirements = configuration.contains("warn_missing_requirements")
            ? configuration.getBoolean("warn_missing_requirements")
            : section.warn_missing_requirements;
        section.global_blacklist = configuration.contains("global_blacklist")
            ? new ArrayList<>(configuration.getStringList("global_blacklist"))
            : section.global_blacklist;
        section.requirements = parseRequirements(configuration.getConfigurationSection("requirements"));
        final ConfigurationSection protectionSection = configuration.getConfigurationSection("protection");
        if (protectionSection != null) {
            section.protection_restricted_storages = protectionSection.contains("restricted_storages")
                ? protectionSection.getString("restricted_storages")
                : section.protection_restricted_storages;
            section.protection_taxed_storages = protectionSection.contains("taxed_storages")
                ? protectionSection.getString("taxed_storages")
                : section.protection_taxed_storages;
            final ConfigurationSection openStorageTaxesSection = firstNonNullSection(
                protectionSection.getConfigurationSection("open_storage_taxes"),
                protectionSection.getConfigurationSection("storage_taxes")
            );
            section.protection_open_storage_taxes = parseProtectionTaxes(openStorageTaxesSection);

            final ConfigurationSection filledStorageTaxesSection = protectionSection.getConfigurationSection("filled_storage_taxes");
            if (filledStorageTaxesSection != null) {
                section.protection_filled_storage_tax_interval_ticks = filledStorageTaxesSection.contains("interval_ticks")
                    ? filledStorageTaxesSection.getLong("interval_ticks")
                    : section.protection_filled_storage_tax_interval_ticks;
                section.protection_filled_storage_maximum_freeze = filledStorageTaxesSection.contains("maximum_freeze")
                    ? filledStorageTaxesSection.getInt("maximum_freeze")
                    : section.protection_filled_storage_maximum_freeze;
                section.protection_filled_storage_maximum_debt = parseProtectionMaximumDebt(
                    filledStorageTaxesSection.getConfigurationSection("maximum_debt")
                );

                final ConfigurationSection filledCurrencySection = firstNonNullSection(
                    filledStorageTaxesSection.getConfigurationSection("currencies"),
                    filledStorageTaxesSection
                );
                section.protection_filled_storage_taxes = parseProtectionTaxes(filledCurrencySection);
            }
        }
        return section;
    }

    /**
     * Creates the default RDR config section used when no config file is available.
     *
     * @return config section populated with bundled fallback defaults
     */
    public static @NotNull ConfigSection createDefault() {
        final ConfigSection section = new ConfigSection(new EvaluationEnvironmentBuilder());
        section.starting_storages = 1;
        section.max_storages = 1;
        section.max_hotkeys = 9;
        section.warn_missing_requirements = true;
        section.global_blacklist = new ArrayList<>(List.of("NETHER_STAR"));
        section.requirements = createEmptyRequirements();
        section.protection_restricted_storages = "none";
        section.protection_taxed_storages = "all";
        section.protection_open_storage_taxes = createDefaultProtectionTaxes();
        section.protection_filled_storage_tax_interval_ticks = 1_728_000L;
        section.protection_filled_storage_maximum_freeze = -1;
        section.protection_filled_storage_maximum_debt = createDefaultProtectionMaximumDebt();
        section.protection_filled_storage_taxes = createDefaultProtectionTaxes();
        return section;
    }

    /**
     * Normalizes configured storage requirements after config parsing completes.
     *
     * @param fields mapped config fields
     * @throws Exception if the underlying config mapper fails while finishing parse processing
     */
    @Override
    public void afterParsing(final @NotNull List<Field> fields) throws Exception {
        super.afterParsing(fields);

        if (this.requirements == null || this.requirements.isEmpty()) {
            this.global_blacklist = this.getGlobalBlacklist();
            this.protection_restricted_storages = this.getProtectionRestrictedStorages();
            this.protection_taxed_storages = this.getProtectionTaxedStorages();
            this.protection_open_storage_taxes = this.getProtectionOpenStorageTaxes();
            this.protection_filled_storage_tax_interval_ticks = this.getProtectionFilledStorageTaxIntervalTicks();
            this.protection_filled_storage_maximum_freeze = this.getProtectionFilledStorageMaximumFreeze();
            this.protection_filled_storage_maximum_debt = this.getProtectionFilledStorageMaximumDebtByCurrency();
            this.protection_filled_storage_taxes = this.getProtectionFilledStorageTaxes();
            return;
        }

        this.global_blacklist = this.getGlobalBlacklist();

        final Map<Integer, Map<String, StoreRequirementSection>> normalizedRequirements = new LinkedHashMap<>();
        for (final Map.Entry<Integer, Map<String, StoreRequirementSection>> purchaseEntry : this.requirements.entrySet()) {
            if (purchaseEntry.getKey() == null || purchaseEntry.getValue() == null || purchaseEntry.getKey() < 1) {
                continue;
            }

            final Map<String, StoreRequirementSection> normalizedPurchaseRequirements = new LinkedHashMap<>();
            for (final Map.Entry<String, StoreRequirementSection> requirementEntry : purchaseEntry.getValue().entrySet()) {
                if (requirementEntry.getKey() == null || requirementEntry.getValue() == null) {
                    continue;
                }

                normalizedPurchaseRequirements.put(
                    normalizeRequirementKey(requirementEntry.getKey()),
                    requirementEntry.getValue()
                );
            }

            normalizedRequirements.put(purchaseEntry.getKey(), normalizedPurchaseRequirements);
        }

        this.requirements = normalizedRequirements;
        this.protection_restricted_storages = this.getProtectionRestrictedStorages();
        this.protection_taxed_storages = this.getProtectionTaxedStorages();
        this.protection_open_storage_taxes = this.getProtectionOpenStorageTaxes();
        this.protection_filled_storage_tax_interval_ticks = this.getProtectionFilledStorageTaxIntervalTicks();
        this.protection_filled_storage_maximum_freeze = this.getProtectionFilledStorageMaximumFreeze();
        this.protection_filled_storage_maximum_debt = this.getProtectionFilledStorageMaximumDebtByCurrency();
        this.protection_filled_storage_taxes = this.getProtectionFilledStorageTaxes();
    }

    private int normalizePositive(
        final Integer value,
        final int defaultValue
    ) {
        if (value == null || value < 1) {
            return defaultValue;
        }
        return value;
    }

    private long normalizeNonNegative(
        final Long value,
        final long defaultValue
    ) {
        if (value == null) {
            return defaultValue;
        }
        return Math.max(0L, value);
    }

    private boolean matchesStorageSelector(
        final @NotNull String selector,
        final @Nullable String storageKey
    ) {
        if ("none".equalsIgnoreCase(selector)) {
            return false;
        }
        if ("all".equalsIgnoreCase(selector)) {
            return true;
        }

        final Integer storageIndex = parseStorageIndex(storageKey);
        if (storageIndex == null || storageIndex < 1) {
            return false;
        }

        for (final String rawToken : selector.split(",")) {
            final String token = rawToken.trim();
            if (token.isEmpty()) {
                continue;
            }

            final int rangeSeparator = token.indexOf('-');
            if (rangeSeparator > 0 && rangeSeparator < token.length() - 1) {
                final Integer start = parsePositiveInteger(token.substring(0, rangeSeparator));
                final Integer end = parsePositiveInteger(token.substring(rangeSeparator + 1));
                if (start == null || end == null) {
                    continue;
                }

                final int minimum = Math.min(start, end);
                final int maximum = Math.max(start, end);
                if (storageIndex >= minimum && storageIndex <= maximum) {
                    return true;
                }
                continue;
            }

            final Integer singleIndex = parsePositiveInteger(token);
            if (singleIndex != null && singleIndex.intValue() == storageIndex.intValue()) {
                return true;
            }
        }

        return false;
    }

    private static @NotNull Map<Integer, Map<String, StoreRequirementSection>> parseRequirements(
        final @Nullable ConfigurationSection requirementsSection
    ) {
        if (requirementsSection == null) {
            return createEmptyRequirements();
        }

        final Map<Integer, Map<String, StoreRequirementSection>> parsedRequirements = new LinkedHashMap<>();
        for (final String purchaseKey : requirementsSection.getKeys(false)) {
            final Integer purchaseNumber = parsePurchaseNumber(purchaseKey);
            if (purchaseNumber == null || purchaseNumber < 1) {
                continue;
            }

            final ConfigurationSection purchaseSection = requirementsSection.getConfigurationSection(purchaseKey);
            if (purchaseSection == null) {
                continue;
            }

            final Map<String, StoreRequirementSection> purchaseRequirements = parsePurchaseRequirementSet(purchaseSection);
            if (!purchaseRequirements.isEmpty()) {
                parsedRequirements.put(purchaseNumber, purchaseRequirements);
            }
        }

        return parsedRequirements;
    }

    private static @NotNull Map<String, Double> parseProtectionTaxes(
        final @Nullable ConfigurationSection storageTaxesSection
    ) {
        if (storageTaxesSection == null) {
            return createDefaultProtectionTaxes();
        }

        final Map<String, Double> parsedTaxes = new LinkedHashMap<>();
        for (final String rawCurrencyId : storageTaxesSection.getKeys(false)) {
            if ("interval_ticks".equalsIgnoreCase(rawCurrencyId)
                || "maximum_freeze".equalsIgnoreCase(rawCurrencyId)
                || "maximum_debt".equalsIgnoreCase(rawCurrencyId)
                || "currencies".equalsIgnoreCase(rawCurrencyId)) {
                continue;
            }
            if (rawCurrencyId == null || rawCurrencyId.isBlank()) {
                continue;
            }

            final Double amount = parseDoubleValue(storageTaxesSection.get(rawCurrencyId));
            if (amount == null) {
                continue;
            }

            parsedTaxes.put(rawCurrencyId, amount);
        }

        return normalizeProtectionTaxes(parsedTaxes);
    }

    private static @NotNull Map<String, Double> parseProtectionMaximumDebt(
        final @Nullable ConfigurationSection maximumDebtSection
    ) {
        if (maximumDebtSection == null) {
            return createDefaultProtectionMaximumDebt();
        }

        final Map<String, Double> parsedDebtCaps = new LinkedHashMap<>();
        for (final String rawCurrencyId : maximumDebtSection.getKeys(false)) {
            if (rawCurrencyId == null || rawCurrencyId.isBlank()) {
                continue;
            }

            final Double amount = parseDoubleValue(maximumDebtSection.get(rawCurrencyId));
            if (amount == null) {
                continue;
            }

            parsedDebtCaps.put(rawCurrencyId, amount);
        }

        return normalizeProtectionMaximumDebt(parsedDebtCaps);
    }

    private static @NotNull Map<String, StoreRequirementSection> parsePurchaseRequirementSet(
        final @NotNull ConfigurationSection purchaseSection
    ) {
        final Map<String, StoreRequirementSection> purchaseRequirements = new LinkedHashMap<>();
        for (final String key : purchaseSection.getKeys(false)) {
            if (key == null || key.isBlank() || !purchaseSection.isConfigurationSection(key)) {
                continue;
            }

            final ConfigurationSection requirementSection = purchaseSection.getConfigurationSection(key);
            if (requirementSection == null) {
                continue;
            }

            purchaseRequirements.put(
                normalizeRequirementKey(key),
                StoreRequirementSection.fromConfigurationSection(key, requirementSection)
            );
        }
        return purchaseRequirements;
    }

    private static @NotNull Map<Integer, Map<String, StoreRequirementSection>> createEmptyRequirements() {
        return new LinkedHashMap<>();
    }

    private static @NotNull Map<String, Double> createDefaultProtectionTaxes() {
        final Map<String, Double> defaults = new LinkedHashMap<>();
        defaults.put("vault", 0.0D);
        defaults.put("raindrops", 0.0D);
        return defaults;
    }

    private static @NotNull Map<String, Double> createDefaultProtectionMaximumDebt() {
        final Map<String, Double> defaults = new LinkedHashMap<>();
        defaults.put("vault", -1.0D);
        defaults.put("raindrops", -1.0D);
        return defaults;
    }

    private int getRequiredPurchaseCount() {
        return Math.max(this.getMaxStorages() - this.getStartingStorages(), 0);
    }

    private static @Nullable Integer parsePurchaseNumber(final @Nullable String purchaseKey) {
        if (purchaseKey == null || purchaseKey.isBlank()) {
            return null;
        }

        try {
            return Integer.parseInt(purchaseKey.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static @Nullable ConfigurationSection firstNonNullSection(
        final @Nullable ConfigurationSection primary,
        final @Nullable ConfigurationSection fallback
    ) {
        return primary != null ? primary : fallback;
    }

    private static @Nullable Integer parseStorageIndex(final @Nullable String storageKey) {
        if (storageKey == null || storageKey.isBlank()) {
            return null;
        }

        final String normalizedKey = storageKey.trim().toLowerCase(Locale.ROOT);
        if (!normalizedKey.startsWith("storage-")) {
            return null;
        }

        return parsePositiveInteger(normalizedKey.substring("storage-".length()));
    }

    private static @Nullable Integer parsePositiveInteger(final @Nullable String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            final int parsed = Integer.parseInt(value.trim());
            return parsed > 0 ? parsed : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static @NotNull String normalizeStorageSelector(
        final @Nullable String selector,
        final @NotNull String fallback
    ) {
        if (selector == null || selector.isBlank()) {
            return fallback;
        }

        final String normalized = selector.trim().toLowerCase(Locale.ROOT).replace(" ", "");
        if ("all".equals(normalized) || "none".equals(normalized)) {
            return normalized;
        }
        return normalized;
    }

    private static @NotNull Map<String, Double> normalizeProtectionTaxes(
        final @Nullable Map<String, Double> taxes
    ) {
        final Map<String, Double> normalizedTaxes = createDefaultProtectionTaxes();
        if (taxes == null || taxes.isEmpty()) {
            return normalizedTaxes;
        }

        for (final Map.Entry<String, Double> entry : taxes.entrySet()) {
            if (entry.getKey() == null || entry.getKey().isBlank()) {
                continue;
            }

            final String currencyId = entry.getKey().trim().toLowerCase(Locale.ROOT);
            final double amount = entry.getValue() == null ? 0.0D : Math.max(0.0D, entry.getValue());
            normalizedTaxes.put(currencyId, amount);
        }

        return normalizedTaxes;
    }

    private static @NotNull Map<String, Double> normalizeProtectionMaximumDebt(
        final @Nullable Map<String, Double> maximumDebt
    ) {
        final Map<String, Double> normalizedMaximumDebt = createDefaultProtectionMaximumDebt();
        if (maximumDebt == null || maximumDebt.isEmpty()) {
            return normalizedMaximumDebt;
        }

        for (final Map.Entry<String, Double> entry : maximumDebt.entrySet()) {
            if (entry.getKey() == null || entry.getKey().isBlank()) {
                continue;
            }

            final String currencyId = entry.getKey().trim().toLowerCase(Locale.ROOT);
            final double rawAmount = entry.getValue() == null ? -1.0D : entry.getValue();
            final double normalizedAmount = rawAmount > 0.0D ? rawAmount : -1.0D;
            normalizedMaximumDebt.put(currencyId, normalizedAmount);
        }

        return normalizedMaximumDebt;
    }

    private static @Nullable Double parseDoubleValue(final @Nullable Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value instanceof String text) {
            try {
                return Double.parseDouble(text.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private static @NotNull String normalizeRequirementKey(final @Nullable String requirementKey) {
        if (requirementKey == null || requirementKey.isBlank()) {
            return "requirement";
        }

        return requirementKey.trim().toLowerCase(Locale.ROOT);
    }
}
