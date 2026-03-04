/*
 * ConfigSection.java
 *
 * @author ItsRainingHP
 * @version 5.0.0
 */

package com.raindropcentral.rds.configs;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringJoiner;
import java.util.logging.Logger;

import de.jexcellence.configmapper.sections.AConfigSection;
import de.jexcellence.configmapper.sections.CSAlways;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Root configuration section for the RDS plugin.
 *
 * <p>This section keeps the existing tax, admin-shop, and currency defaults while exposing the new
 * purchase-tier requirement model used by the shop store.</p>
 *
 * @author ItsRainingHP
 * @since 5.0.0
 * @version 5.0.0
 */
@CSAlways
@SuppressWarnings("unused")
public class ConfigSection extends AConfigSection {

    private static final int DEFAULT_MAX_SHOPS = 10;
    private static final int DEFAULT_LEGACY_REQUIREMENT_TIERS = 10;

    private Map<String, StoreCurrencySection> store;
    private String default_currency_type;
    private List<String> blacklisted_currencies;
    private Integer max_shops;
    private Boolean warn_missing_requirements;
    private Map<Integer, Map<String, StoreRequirementSection>> requirements;
    private TaxSection taxes;
    private BossBarSection boss_bar;
    private AdminShopSection admin_shops;

    /**
     * Creates a configuration section bound to the provided evaluation environment.
     *
     * @param baseEnvironment base environment used by the config mapper
     */
    public ConfigSection(final @NotNull EvaluationEnvironmentBuilder baseEnvironment) {
        super(baseEnvironment);
    }

    /**
     * Returns the legacy store section when present.
     *
     * @return copied legacy store map
     */
    public @NotNull Map<String, StoreCurrencySection> getStore() {
        final Map<String, StoreCurrencySection> normalizedStore = new LinkedHashMap<>();
        if (this.store == null) {
            return normalizedStore;
        }

        for (final Map.Entry<String, StoreCurrencySection> entry : this.store.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                continue;
            }
            normalizedStore.put(entry.getKey().trim().toLowerCase(Locale.ROOT), entry.getValue());
        }
        return normalizedStore;
    }

    /**
     * Returns the legacy store costs.
     *
     * @return copied legacy store map
     */
    public @NotNull Map<String, StoreCurrencySection> getStoreCosts() {
        return this.getStore();
    }

    /**
     * Returns the default currency type used by shop item pricing.
     *
     * @return normalized default currency identifier
     */
    public @NotNull String getDefaultCurrencyType() {
        if (this.default_currency_type != null && !this.default_currency_type.isBlank()) {
            return this.default_currency_type.trim().toLowerCase(Locale.ROOT);
        }

        if (this.store != null && !this.store.isEmpty()) {
            return this.store.keySet().iterator().next().trim().toLowerCase(Locale.ROOT);
        }

        return "vault";
    }

    /**
     * Returns the list of shop item currencies players may not choose.
     *
     * @return normalized blacklisted currency identifiers
     */
    public @NotNull List<String> getBlacklistedCurrencies() {
        if (this.blacklisted_currencies == null || this.blacklisted_currencies.isEmpty()) {
            return List.of();
        }

        final List<String> normalized = new ArrayList<>();
        for (final String currencyType : this.blacklisted_currencies) {
            if (currencyType == null || currencyType.isBlank()) {
                continue;
            }

            normalized.add(currencyType.trim().toLowerCase(Locale.ROOT));
        }
        return normalized;
    }

    /**
     * Returns the configured maximum placed shops per player.
     *
     * @return maximum placed shops, or {@code -1} when unlimited
     */
    public int getMaxShops() {
        return this.max_shops == null ? DEFAULT_MAX_SHOPS : this.max_shops;
    }

    /**
     * Returns whether the placed-shop cap is finite.
     *
     * @return {@code true} when a finite shop limit is configured
     */
    public boolean hasShopLimit() {
        return this.getMaxShops() > 0;
    }

    /**
     * Returns whether startup warnings should be emitted for missing purchase requirement tiers.
     *
     * @return {@code true} when missing requirement warnings should be logged
     */
    public boolean shouldWarnMissingRequirements() {
        return this.warn_missing_requirements == null || this.warn_missing_requirements;
    }

    /**
     * Returns the configured purchase-indexed shop requirements.
     *
     * @return defensive copy of the configured requirement map
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
     * Returns the requirements configured for a one-based shop purchase tier.
     *
     * @param purchaseNumber one-based shop purchase number
     * @return copied requirement map for that tier, or an empty map when no tier is configured
     */
    public @NotNull Map<String, StoreRequirementSection> getRequirementsForPurchase(final int purchaseNumber) {
        final Map<String, StoreRequirementSection> purchaseRequirements = this.getRequirements().get(purchaseNumber);
        return purchaseRequirements == null ? new LinkedHashMap<>() : new LinkedHashMap<>(purchaseRequirements);
    }

    /**
     * Returns the missing purchase tiers within the currently knowable range.
     *
     * <p>When the shop limit is finite, this covers every required purchase tier from {@code 1} to
     * {@code max_shops}. When the shop limit is unlimited, the range is limited to the highest
     * configured requirement tier so only explicit gaps are enumerated.</p>
     *
     * @return ordered list of missing purchase tiers
     */
    public @NotNull List<Integer> getMissingRequirementPurchases() {
        final List<Integer> missing = new ArrayList<>();
        final int knownPurchaseCount = this.getKnownPurchaseCountForWarnings();
        if (knownPurchaseCount <= 0) {
            return missing;
        }

        final Map<Integer, Map<String, StoreRequirementSection>> configuredRequirements = this.getRequirements();
        for (int purchaseNumber = 1; purchaseNumber <= knownPurchaseCount; purchaseNumber++) {
            final Map<String, StoreRequirementSection> purchaseRequirements = configuredRequirements.get(purchaseNumber);
            if (purchaseRequirements == null || purchaseRequirements.isEmpty()) {
                missing.add(purchaseNumber);
            }
        }
        return missing;
    }

    /**
     * Returns the highest configured purchase tier with requirement entries.
     *
     * @return highest configured purchase tier, or {@code 0} when no requirements exist
     */
    public int getHighestConfiguredRequirementPurchase() {
        int highestPurchase = 0;
        for (final Integer purchaseNumber : this.getRequirements().keySet()) {
            if (purchaseNumber != null && purchaseNumber > highestPurchase) {
                highestPurchase = purchaseNumber;
            }
        }
        return highestPurchase;
    }

    /**
     * Logs startup warnings describing missing requirement tiers.
     *
     * @param logger logger receiving any warning messages
     */
    public void logMissingRequirementWarnings(final @NotNull Logger logger) {
        if (!this.shouldWarnMissingRequirements()) {
            return;
        }

        final List<Integer> missingPurchases = this.getMissingRequirementPurchases();
        if (!missingPurchases.isEmpty()) {
            final StringJoiner joiner = new StringJoiner(", ");
            for (final Integer purchaseNumber : missingPurchases) {
                joiner.add(String.valueOf(purchaseNumber));
            }

            logger.warning(
                "Shop store purchase tiers without configured requirements: " + joiner
                    + ". These purchases will be available with no requirements."
            );
        }

        if (this.hasShopLimit()) {
            return;
        }

        final int highestConfiguredPurchase = this.getHighestConfiguredRequirementPurchase();
        if (highestConfiguredPurchase < 1) {
            logger.warning(
                "Shop store max_shops is unlimited and no purchase requirements are configured. "
                    + "All shop purchases will be available with no requirements."
            );
            return;
        }

        logger.warning(
            "Shop store max_shops is unlimited and purchase requirements are only configured through tier "
                + highestConfiguredPurchase + ". Purchases above that tier will be available with no requirements."
        );
    }

    /**
     * Returns the configured tax section.
     *
     * @return tax configuration, or defaults when no tax section was mapped
     */
    public @NotNull TaxSection getTaxes() {
        return this.taxes == null
            ? TaxSection.createDefault(this.getDefaultCurrencyType())
            : this.taxes;
    }

    /**
     * Returns the configured shop boss bar section.
     *
     * @return boss bar configuration
     */
    public @NotNull BossBarSection getBossBar() {
        return this.boss_bar == null
            ? new BossBarSection(new EvaluationEnvironmentBuilder())
            : this.boss_bar;
    }

    /**
     * Returns the configured admin shop section.
     *
     * @return admin shop configuration
     */
    public @NotNull AdminShopSection getAdminShops() {
        return this.admin_shops == null
            ? new AdminShopSection(new EvaluationEnvironmentBuilder())
            : this.admin_shops;
    }

    /**
     * Returns the initial tax amount for the default tax currency.
     *
     * @return initial tax amount
     */
    public double getTaxInitialCost() {
        return this.getDefaultTaxCurrency().getInitialCost();
    }

    /**
     * Returns the growth rate for the default tax currency.
     *
     * @return tax growth rate
     */
    public double getTaxGrowthRate() {
        return this.getDefaultTaxCurrency().getGrowthRate();
    }

    /**
     * Returns the configured maximum tax for the default tax currency.
     *
     * @return maximum tax, or {@code -1.0} when uncapped
     */
    public double getMaximumTax() {
        return this.getDefaultTaxCurrency().getMaximumTax();
    }

    /**
     * Returns the tax currency matching the configured default currency type.
     *
     * @return default tax currency section
     */
    public @NotNull TaxCurrencySection getDefaultTaxCurrency() {
        return this.getTaxes().getTaxCurrency(this.getDefaultCurrencyType());
    }

    /**
     * Replaces the parsed tax configuration.
     *
     * @param taxes replacement tax section
     */
    public void setTaxes(final @NotNull TaxSection taxes) {
        this.taxes = taxes;
    }

    /**
     * Returns the legacy store currency matching the configured default currency type.
     *
     * @return default legacy store currency section
     */
    public @NotNull StoreCurrencySection getDefaultStoreCurrency() {
        return this.getStoreCurrency(this.getDefaultCurrencyType());
    }

    /**
     * Returns the legacy store currency definition for the provided currency type.
     *
     * @param currencyType currency identifier to resolve
     * @return matching store currency section, or a fallback when none is configured
     */
    public @NotNull StoreCurrencySection getStoreCurrency(final @Nullable String currencyType) {
        final String normalizedType = currencyType == null || currencyType.isBlank()
            ? this.getDefaultCurrencyType()
            : currencyType.trim().toLowerCase(Locale.ROOT);

        if (this.store != null && !this.store.isEmpty()) {
            final StoreCurrencySection directMatch = this.store.get(normalizedType);
            if (directMatch != null) {
                return directMatch;
            }

            for (final Map.Entry<String, StoreCurrencySection> entry : this.store.entrySet()) {
                if (entry.getKey().equalsIgnoreCase(normalizedType)) {
                    return entry.getValue();
                }
            }

            return this.store.values().iterator().next();
        }

        return this.createFallbackStoreCurrency(normalizedType);
    }

    /**
     * Returns the initial legacy store price for the default currency.
     *
     * @return initial legacy store cost
     */
    public double getInitialCost() {
        return this.getDefaultStoreCurrency().getInitialCost();
    }

    /**
     * Returns the legacy growth rate for the default store currency.
     *
     * @return legacy store growth rate
     */
    public double getGrowthRate() {
        return this.getDefaultStoreCurrency().getGrowthRate();
    }

    /**
     * Loads the RDS root config directly from a file.
     *
     * <p>This helper mirrors the plugin's runtime loading flow closely enough for tests and other
     * non-plugin callers that need the requirement-backed store configuration.</p>
     *
     * @param configFile config file to parse
     * @return parsed config section
     */
    public static @NotNull ConfigSection fromFile(final @NotNull File configFile) {
        final ConfigSection section = new ConfigSection(new EvaluationEnvironmentBuilder());
        final YamlConfiguration configuration = YamlConfiguration.loadConfiguration(configFile);

        if (configuration.contains("default_currency_type")) {
            section.default_currency_type = configuration.getString("default_currency_type");
        }
        if (configuration.contains("blacklisted_currencies")) {
            section.blacklisted_currencies = new ArrayList<>(configuration.getStringList("blacklisted_currencies"));
        }
        if (configuration.contains("max_shops")) {
            section.max_shops = configuration.getInt("max_shops");
        }
        if (configuration.contains("warn_missing_requirements")) {
            section.warn_missing_requirements = configuration.getBoolean("warn_missing_requirements");
        }

        section.loadStoreConfiguration(configFile);
        section.setTaxes(TaxSection.fromFile(configFile, section.getDefaultCurrencyType()));
        section.boss_bar = BossBarSection.fromFile(configFile);
        section.admin_shops = AdminShopSection.fromFile(configFile);
        return section;
    }

    /**
     * Loads the purchase-tier shop requirement configuration directly from the config file.
     *
     * @param configFile plugin config file
     */
    public void loadStoreConfiguration(final @NotNull File configFile) {
        final YamlConfiguration configuration = YamlConfiguration.loadConfiguration(configFile);
        this.warn_missing_requirements = configuration.contains("warn_missing_requirements")
            ? configuration.getBoolean("warn_missing_requirements")
            : this.warn_missing_requirements;

        Map<Integer, Map<String, StoreRequirementSection>> parsedRequirements =
            parseRequirements(configuration.getConfigurationSection("requirements"));

        if (parsedRequirements.isEmpty()) {
            final Map<String, StoreCurrencySection> legacyStore = parseLegacyStore(
                configuration.getConfigurationSection("store")
            );
            if (!legacyStore.isEmpty()) {
                this.store = legacyStore;
                parsedRequirements = createLegacyRequirements(
                    legacyStore,
                    this.resolveLegacyRequirementTierCount()
                );
            }
        }

        this.requirements = parsedRequirements;
    }

    /**
     * Executes after parsing.
     *
     * @param fields parsed fields to post-process
     * @throws Exception if the operation fails
     */
    @Override
    public void afterParsing(final @NotNull List<Field> fields) throws Exception {
        super.afterParsing(fields);

        if (this.store == null || this.store.isEmpty()) {
            return;
        }

        final Map<String, StoreCurrencySection> normalizedStore = new LinkedHashMap<>();
        for (final Map.Entry<String, StoreCurrencySection> entry : this.store.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                continue;
            }

            final String normalizedKey = entry.getKey().trim().toLowerCase(Locale.ROOT);
            final StoreCurrencySection section = entry.getValue();
            section.setContext(
                normalizedKey,
                section.getInitialCost(),
                section.getGrowthRate()
            );
            normalizedStore.put(normalizedKey, section);
        }

        this.store = normalizedStore;
    }

    private int getKnownPurchaseCountForWarnings() {
        if (this.hasShopLimit()) {
            return Math.max(this.getMaxShops(), 0);
        }
        return this.getHighestConfiguredRequirementPurchase();
    }

    private int resolveLegacyRequirementTierCount() {
        if (this.hasShopLimit()) {
            return Math.max(this.getMaxShops(), 0);
        }
        return DEFAULT_LEGACY_REQUIREMENT_TIERS;
    }

    private @NotNull StoreCurrencySection createFallbackStoreCurrency(final @NotNull String currencyType) {
        final StoreCurrencySection fallback = new StoreCurrencySection(new EvaluationEnvironmentBuilder());
        fallback.setContext(currencyType, 1000.0D, 1.125D);
        return fallback;
    }

    private static @NotNull Map<Integer, Map<String, StoreRequirementSection>> parseRequirements(
        final @Nullable ConfigurationSection requirementsSection
    ) {
        if (requirementsSection == null) {
            return new LinkedHashMap<>();
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

            final Map<String, StoreRequirementSection> purchaseRequirements =
                parsePurchaseRequirementSet(purchaseSection);
            if (!purchaseRequirements.isEmpty()) {
                parsedRequirements.put(purchaseNumber, purchaseRequirements);
            }
        }

        return parsedRequirements;
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

    private static @NotNull Map<String, StoreCurrencySection> parseLegacyStore(
        final @Nullable ConfigurationSection storeSection
    ) {
        final Map<String, StoreCurrencySection> legacyStore = new LinkedHashMap<>();
        if (storeSection == null) {
            return legacyStore;
        }

        for (final String currencyKey : storeSection.getKeys(false)) {
            if (currencyKey == null || currencyKey.isBlank()) {
                continue;
            }

            final ConfigurationSection currencySection = storeSection.getConfigurationSection(currencyKey);
            if (currencySection == null) {
                continue;
            }

            final String normalizedCurrency = currencyKey.trim().toLowerCase(Locale.ROOT);
            final StoreCurrencySection section = new StoreCurrencySection(new EvaluationEnvironmentBuilder());
            section.setContext(
                normalizedCurrency,
                currencySection.getDouble("initial_cost", 1000.0D),
                currencySection.getDouble("growth_rate", 1.125D)
            );
            legacyStore.put(normalizedCurrency, section);
        }

        return legacyStore;
    }

    private static @NotNull Map<Integer, Map<String, StoreRequirementSection>> createLegacyRequirements(
        final @NotNull Map<String, StoreCurrencySection> legacyStore,
        final int purchaseCount
    ) {
        final Map<Integer, Map<String, StoreRequirementSection>> legacyRequirements = new LinkedHashMap<>();
        if (purchaseCount <= 0 || legacyStore.isEmpty()) {
            return legacyRequirements;
        }

        for (int purchaseNumber = 1; purchaseNumber <= purchaseCount; purchaseNumber++) {
            final Map<String, StoreRequirementSection> purchaseRequirements = new LinkedHashMap<>();
            for (final Map.Entry<String, StoreCurrencySection> entry : legacyStore.entrySet()) {
                final String currencyType = entry.getKey();
                final double amount = calculateLegacyCost(entry.getValue(), purchaseNumber - 1);
                purchaseRequirements.put(
                    normalizeRequirementKey(currencyType + "_purchase"),
                    StoreRequirementSection.currency(
                        currencyType + "_purchase",
                        currencyType,
                        amount,
                        resolveLegacyCurrencyIcon(currencyType)
                    )
                );
            }

            if (!purchaseRequirements.isEmpty()) {
                legacyRequirements.put(purchaseNumber, purchaseRequirements);
            }
        }

        return legacyRequirements;
    }

    private static double calculateLegacyCost(
        final @NotNull StoreCurrencySection section,
        final int ownedShops
    ) {
        final double rawCost = section.getInitialCost() * Math.pow(section.getGrowthRate(), Math.max(ownedShops, 0));
        if (!Double.isFinite(rawCost)) {
            return Math.max(0D, section.getInitialCost());
        }
        return Math.max(0D, rawCost);
    }

    private static @NotNull String resolveLegacyCurrencyIcon(final @NotNull String currencyType) {
        return "vault".equalsIgnoreCase(currencyType) ? "GOLD_INGOT" : "EMERALD";
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

    private static @NotNull String normalizeRequirementKey(final @Nullable String requirementKey) {
        if (requirementKey == null || requirementKey.isBlank()) {
            return "requirement";
        }

        return requirementKey.trim().toLowerCase(Locale.ROOT);
    }
}