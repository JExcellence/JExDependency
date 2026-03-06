package com.raindropcentral.rds.configs;

import de.jexcellence.configmapper.sections.AConfigSection;
import de.jexcellence.configmapper.sections.CSAlways;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Represents optional protection integration settings.
 *
 * <p>The section controls where player shops may be placed and which tax currencies
 * should be charged through a supported town-protection plugin bank.</p>
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
@CSAlways
@SuppressWarnings("unused")
public class ProtectionSection extends AConfigSection {

    private Boolean only_player_shops;
    private Boolean shop_taxes_fallback_to_player;
    private Map<String, TaxCurrencySection> shop_taxes;

    /**
     * Creates a new protection config section.
     *
     * @param baseEnvironment evaluation environment used for config expressions
     */
    public ProtectionSection(
            final @NotNull EvaluationEnvironmentBuilder baseEnvironment
    ) {
        super(baseEnvironment);
    }

    /**
     * Loads the protection section from the plugin config file.
     *
     * @param configFile plugin config file
     * @return parsed protection section
     * @throws NullPointerException if {@code configFile} is {@code null}
     */
    public static @NotNull ProtectionSection fromFile(
            final @NotNull File configFile
    ) {
        final ProtectionSection section = new ProtectionSection(new EvaluationEnvironmentBuilder());
        final YamlConfiguration configuration = YamlConfiguration.loadConfiguration(configFile);
        final ConfigurationSection protectionSection = configuration.getConfigurationSection("protection");
        if (protectionSection == null) {
            return section;
        }

        section.only_player_shops = protectionSection.contains("only_player_shops")
                ? protectionSection.getBoolean("only_player_shops")
                : null;
        section.shop_taxes_fallback_to_player = protectionSection.contains("shop_taxes_fallback_to_player")
                ? protectionSection.getBoolean("shop_taxes_fallback_to_player")
                : null;
        section.shop_taxes = parseShopTaxes(configuration, protectionSection);
        return section;
    }

    /**
     * Indicates whether player shops can only be placed in the owner's town territory.
     *
     * @return {@code true} when own-town placement is required
     */
    public boolean isOnlyPlayerShops() {
        return Boolean.TRUE.equals(this.only_player_shops);
    }

    /**
     * Indicates whether failed protection-town tax charges should fall back to player balances.
     *
     * @return {@code true} when player-balance fallback is enabled
     */
    public boolean isShopTaxesFallbackToPlayer() {
        return Boolean.TRUE.equals(this.shop_taxes_fallback_to_player);
    }

    /**
     * Returns configured tax definitions that should be charged from town banks.
     *
     * @return normalized map of currency tax definitions
     */
    public @NotNull Map<String, TaxCurrencySection> getShopTaxes() {
        if (this.shop_taxes == null || this.shop_taxes.isEmpty()) {
            return Map.of();
        }
        return new LinkedHashMap<>(this.shop_taxes);
    }

    /**
     * Indicates whether any tax currencies are configured to use town-bank charging.
     *
     * @return {@code true} when at least one protected tax currency is configured
     */
    public boolean hasShopTaxes() {
        return !this.getShopTaxes().isEmpty();
    }

    /**
     * Returns a configured tax definition that should be charged from a town bank.
     *
     * @param currencyType currency identifier to resolve
     * @return matching tax definition, or {@code null} when not configured
     */
    public @Nullable TaxCurrencySection getShopTaxCurrency(
            final @Nullable String currencyType
    ) {
        if (currencyType == null || currencyType.isBlank()) {
            return null;
        }

        final String normalizedCurrencyType = normalizeCurrencyType(currencyType);
        if (this.shop_taxes == null || this.shop_taxes.isEmpty()) {
            return null;
        }

        final TaxCurrencySection directMatch = this.shop_taxes.get(normalizedCurrencyType);
        if (directMatch != null) {
            return directMatch;
        }

        for (final Map.Entry<String, TaxCurrencySection> entry : this.shop_taxes.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(normalizedCurrencyType)) {
                return entry.getValue();
            }
        }

        return null;
    }

    /**
     * Indicates whether a specific tax currency should be charged from a town bank.
     *
     * @param currencyType currency identifier to check
     * @return {@code true} when that currency is configured in {@code protection.shop_taxes}
     */
    public boolean isShopTaxCurrency(
            final @Nullable String currencyType
    ) {
        return this.getShopTaxCurrency(currencyType) != null;
    }

    /**
     * Sets the parsed section context.
     *
     * @param onlyPlayerShops whether own-town placement is required
     * @param shopTaxes tax definitions charged through town banks
     * @throws NullPointerException if {@code shopTaxes} is {@code null}
     */
    public void setContext(
            final boolean onlyPlayerShops,
            final @NotNull Map<String, TaxCurrencySection> shopTaxes
    ) {
        this.setContext(onlyPlayerShops, false, shopTaxes);
    }

    /**
     * Sets the parsed section context.
     *
     * @param onlyPlayerShops whether own-town placement is required
     * @param shopTaxesFallbackToPlayer whether failed town-bank taxes should fall back to player balances
     * @param shopTaxes tax definitions charged through town banks
     * @throws NullPointerException if {@code shopTaxes} is {@code null}
     */
    public void setContext(
            final boolean onlyPlayerShops,
            final boolean shopTaxesFallbackToPlayer,
            final @NotNull Map<String, TaxCurrencySection> shopTaxes
    ) {
        this.only_player_shops = onlyPlayerShops;
        this.shop_taxes_fallback_to_player = shopTaxesFallbackToPlayer;
        final Map<String, TaxCurrencySection> normalizedShopTaxes = new LinkedHashMap<>();
        for (final Map.Entry<String, TaxCurrencySection> entry : shopTaxes.entrySet()) {
            if (entry.getKey() == null || entry.getKey().isBlank() || entry.getValue() == null) {
                continue;
            }
            normalizedShopTaxes.put(normalizeCurrencyType(entry.getKey()), entry.getValue());
        }
        this.shop_taxes = normalizedShopTaxes;
    }

    private static @NotNull Map<String, TaxCurrencySection> parseShopTaxes(
            final @NotNull YamlConfiguration configuration,
            final @NotNull ConfigurationSection protectionSection
    ) {
        final Map<String, TaxCurrencySection> parsedShopTaxes = new LinkedHashMap<>();

        final ConfigurationSection structuredShopTaxes = protectionSection.getConfigurationSection("shop_taxes");
        if (structuredShopTaxes != null) {
            for (final String rawCurrencyType : structuredShopTaxes.getKeys(false)) {
                if (rawCurrencyType == null || rawCurrencyType.isBlank()) {
                    continue;
                }

                final ConfigurationSection currencySection =
                        structuredShopTaxes.getConfigurationSection(rawCurrencyType);
                if (currencySection == null) {
                    continue;
                }

                final String normalizedCurrencyType = normalizeCurrencyType(rawCurrencyType);
                parsedShopTaxes.put(normalizedCurrencyType, createCurrencySection(normalizedCurrencyType, currencySection));
            }
            return parsedShopTaxes;
        }

        for (final String rawCurrencyType : protectionSection.getStringList("shop_taxes")) {
            if (rawCurrencyType == null || rawCurrencyType.isBlank()) {
                continue;
            }

            final String normalizedCurrencyType = normalizeCurrencyType(rawCurrencyType);
            final ConfigurationSection taxesSection = configuration.getConfigurationSection("taxes");
            final ConfigurationSection taxCurrencySection = findCurrencySection(taxesSection, normalizedCurrencyType);
            parsedShopTaxes.put(
                    normalizedCurrencyType,
                    taxCurrencySection == null
                            ? createFallbackCurrency(normalizedCurrencyType)
                            : createCurrencySection(normalizedCurrencyType, taxCurrencySection)
            );
        }

        return parsedShopTaxes;
    }

    private static @Nullable ConfigurationSection findCurrencySection(
            final @Nullable ConfigurationSection taxesSection,
            final @NotNull String normalizedCurrencyType
    ) {
        if (taxesSection == null) {
            return null;
        }

        final ConfigurationSection directMatch = taxesSection.getConfigurationSection(normalizedCurrencyType);
        if (directMatch != null) {
            return directMatch;
        }

        for (final String key : taxesSection.getKeys(false)) {
            if (key != null && key.equalsIgnoreCase(normalizedCurrencyType)) {
                return taxesSection.getConfigurationSection(key);
            }
        }

        return null;
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
            final @NotNull String currencyType
    ) {
        return currencyType.trim().toLowerCase(Locale.ROOT);
    }
}
