package com.raindropcentral.rdr.configs;

import de.jexcellence.configmapper.sections.AConfigSection;
import de.jexcellence.configmapper.sections.CSAlways;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Root configuration section for the RDR plugin.
 *
 * <p>This section exposes storage-related defaults used when a player first joins the server and when
 * gameplay systems decide whether the player may unlock more storage containers.</p>
 *
 * @author RaindropCentral
 * @since 5.0.0
 * @version 5.0.0
 */
@CSAlways
@SuppressWarnings("unused")
public class ConfigSection extends AConfigSection {

    private Integer starting_storages;
    private Integer max_storages;
    private Integer max_hotkeys;
    private Map<String, StoreCurrencySection> store;

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
     * Returns the configured storage purchase currencies.
     *
     * <p>When the configuration does not define a store section, a fallback Vault entry is returned so the
     * storage store remains usable with default pricing.</p>
     *
     * @return ordered map of normalized currency identifiers to pricing sections
     */
    public @NotNull Map<String, StoreCurrencySection> getStore() {
        if (this.store == null || this.store.isEmpty()) {
            final StoreCurrencySection fallback = this.createFallbackStoreCurrency("vault");
            return new LinkedHashMap<>(Map.of(fallback.getType(), fallback));
        }

        return new LinkedHashMap<>(this.store);
    }

    /**
     * Returns the configured storage purchase currencies.
     *
     * @return ordered map of normalized currency identifiers to pricing sections
     */
    public @NotNull Map<String, StoreCurrencySection> getStoreCosts() {
        return this.getStore();
    }

    /**
     * Returns the default storage purchase currency type.
     *
     * @return first configured store currency, or {@code "vault"} when no store currencies are configured
     */
    public @NotNull String getDefaultCurrencyType() {
        return this.getStore().keySet().iterator().next();
    }

    /**
     * Returns the default storage purchase pricing section.
     *
     * @return pricing section for the default storage purchase currency
     */
    public @NotNull StoreCurrencySection getDefaultStoreCurrency() {
        return this.getStoreCurrency(this.getDefaultCurrencyType());
    }

    /**
     * Returns the pricing section for the requested storage purchase currency.
     *
     * @param currencyType currency identifier to resolve, or {@code null} to use the default entry
     * @return matching pricing section, falling back to the first configured currency when necessary
     */
    public @NotNull StoreCurrencySection getStoreCurrency(final @Nullable String currencyType) {
        final String normalizedType = currencyType == null || currencyType.isBlank()
            ? this.getDefaultCurrencyType()
            : currencyType.trim().toLowerCase(Locale.ROOT);

        final Map<String, StoreCurrencySection> storeCurrencies = this.getStore();
        final StoreCurrencySection directMatch = storeCurrencies.get(normalizedType);
        if (directMatch != null) {
            return directMatch;
        }

        for (final Map.Entry<String, StoreCurrencySection> entry : storeCurrencies.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(normalizedType)) {
                return entry.getValue();
            }
        }

        return storeCurrencies.values().iterator().next();
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
        section.store = parseStore(configuration.getConfigurationSection("store"));
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
        section.store = createFallbackStore();
        return section;
    }

    /**
     * Normalizes configured store currencies after config parsing completes.
     *
     * @param fields mapped config fields
     * @throws Exception if the underlying config mapper fails while finishing parse processing
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

    private int normalizePositive(
        final Integer value,
        final int defaultValue
    ) {
        if (value == null || value < 1) {
            return defaultValue;
        }
        return value;
    }

    private @NotNull StoreCurrencySection createFallbackStoreCurrency(final @NotNull String currencyType) {
        final StoreCurrencySection fallback = new StoreCurrencySection(new EvaluationEnvironmentBuilder());
        fallback.setContext(currencyType, 1000.0D, 1.125D);
        return fallback;
    }

    private static @NotNull Map<String, StoreCurrencySection> parseStore(
        final @Nullable ConfigurationSection storeSection
    ) {
        if (storeSection == null) {
            return createFallbackStore();
        }

        final Map<String, StoreCurrencySection> storeCurrencies = new LinkedHashMap<>();
        for (final String key : storeSection.getKeys(false)) {
            if (key == null || key.isBlank() || !storeSection.isConfigurationSection(key)) {
                continue;
            }

            final ConfigurationSection currencySection = storeSection.getConfigurationSection(key);
            if (currencySection == null) {
                continue;
            }

            final String normalizedType = normalizeCurrencyType(key);
            final StoreCurrencySection section = new StoreCurrencySection(new EvaluationEnvironmentBuilder());
            section.setContext(
                normalizedType,
                currencySection.getDouble("initial_cost", 1000.0D),
                currencySection.getDouble("growth_rate", 1.125D)
            );
            storeCurrencies.put(normalizedType, section);
        }

        return storeCurrencies.isEmpty() ? createFallbackStore() : storeCurrencies;
    }

    private static @NotNull Map<String, StoreCurrencySection> createFallbackStore() {
        final Map<String, StoreCurrencySection> fallback = new LinkedHashMap<>();
        final StoreCurrencySection fallbackSection = new StoreCurrencySection(new EvaluationEnvironmentBuilder());
        fallbackSection.setContext("vault", 1000.0D, 1.125D);
        fallback.put("vault", fallbackSection);
        return fallback;
    }

    private static @NotNull String normalizeCurrencyType(final @Nullable String currencyType) {
        if (currencyType == null || currencyType.isBlank()) {
            return "vault";
        }

        return currencyType.trim().toLowerCase(Locale.ROOT);
    }
}
