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
 * <p>The section controls where player shops may be placed and which tax currencies should be
 * treated as protection-tax currencies with optional global maximum caps.</p>
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
    private Map<String, Double> shop_taxes;

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
        section.shop_taxes = parseShopTaxMaximums(configuration, protectionSection);
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
     * Returns configured global tax caps for protection-tax currencies.
     *
     * <p>Positive values are hard caps. Non-positive values are treated as unlimited and normalized
     * to {@code -1.0}.</p>
     *
     * @return normalized map of protection-tax currency caps
     */
    public @NotNull Map<String, Double> getShopTaxes() {
        if (this.shop_taxes == null || this.shop_taxes.isEmpty()) {
            return Map.of();
        }
        return new LinkedHashMap<>(this.shop_taxes);
    }

    /**
     * Indicates whether any tax currencies are configured as protection-tax currencies.
     *
     * @return {@code true} when at least one protection tax currency is configured
     */
    public boolean hasShopTaxes() {
        return !this.getShopTaxes().isEmpty();
    }

    /**
     * Returns the configured global maximum for a protection-tax currency.
     *
     * <p>When the returned value is positive, it is the applied cap. A value of {@code -1.0}
     * indicates unlimited.</p>
     *
     * @param currencyType currency identifier to resolve
     * @return configured global maximum, or {@code null} when the currency is not configured
     */
    public @Nullable Double getShopTaxMaximum(
            final @Nullable String currencyType
    ) {
        if (currencyType == null || currencyType.isBlank()) {
            return null;
        }

        final String normalizedCurrencyType = normalizeCurrencyType(currencyType);
        if (this.shop_taxes == null || this.shop_taxes.isEmpty()) {
            return null;
        }

        final Double directMatch = this.shop_taxes.get(normalizedCurrencyType);
        if (directMatch != null) {
            return directMatch;
        }

        for (final Map.Entry<String, Double> entry : this.shop_taxes.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(normalizedCurrencyType)) {
                return entry.getValue();
            }
        }

        return null;
    }

    /**
     * Indicates whether a specific currency is configured as a protection-tax currency.
     *
     * @param currencyType currency identifier to check
     * @return {@code true} when that currency is configured in {@code protection.shop_taxes}
     */
    public boolean isShopTaxCurrency(
            final @Nullable String currencyType
    ) {
        return this.getShopTaxMaximum(currencyType) != null;
    }

    /**
     * Sets the parsed section context.
     *
     * @param onlyPlayerShops whether own-town placement is required
     * @param shopTaxMaximums global max definitions for protection-tax currencies
     * @throws NullPointerException if {@code shopTaxMaximums} is {@code null}
     */
    public void setContext(
            final boolean onlyPlayerShops,
            final @NotNull Map<String, Double> shopTaxMaximums
    ) {
        this.setContext(onlyPlayerShops, false, shopTaxMaximums);
    }

    /**
     * Sets the parsed section context.
     *
     * @param onlyPlayerShops whether own-town placement is required
     * @param shopTaxesFallbackToPlayer whether failed town-bank taxes should fall back to player balances
     * @param shopTaxMaximums global max definitions for protection-tax currencies
     * @throws NullPointerException if {@code shopTaxMaximums} is {@code null}
     */
    public void setContext(
            final boolean onlyPlayerShops,
            final boolean shopTaxesFallbackToPlayer,
            final @NotNull Map<String, Double> shopTaxMaximums
    ) {
        this.only_player_shops = onlyPlayerShops;
        this.shop_taxes_fallback_to_player = shopTaxesFallbackToPlayer;
        final Map<String, Double> normalizedMaximums = new LinkedHashMap<>();
        for (final Map.Entry<String, Double> entry : shopTaxMaximums.entrySet()) {
            if (entry.getKey() == null || entry.getKey().isBlank() || entry.getValue() == null) {
                continue;
            }
            normalizedMaximums.put(normalizeCurrencyType(entry.getKey()), normalizeMaximum(entry.getValue()));
        }
        this.shop_taxes = normalizedMaximums;
    }

    private static @NotNull Map<String, Double> parseShopTaxMaximums(
            final @NotNull YamlConfiguration configuration,
            final @NotNull ConfigurationSection protectionSection
    ) {
        final Map<String, Double> parsedMaximums = new LinkedHashMap<>();
        final ConfigurationSection taxesSection = configuration.getConfigurationSection("taxes");

        final ConfigurationSection structuredShopTaxes = protectionSection.getConfigurationSection("shop_taxes");
        if (structuredShopTaxes != null) {
            for (final String rawCurrencyType : structuredShopTaxes.getKeys(false)) {
                if (rawCurrencyType == null || rawCurrencyType.isBlank()) {
                    continue;
                }

                final String normalizedCurrencyType = normalizeCurrencyType(rawCurrencyType);
                final Object rawValue = structuredShopTaxes.get(rawCurrencyType);
                final Double parsedMaximum = parseMaximumValue(rawValue);
                if (parsedMaximum != null) {
                    parsedMaximums.put(normalizedCurrencyType, normalizeMaximum(parsedMaximum));
                    continue;
                }

                final ConfigurationSection currencySection =
                        structuredShopTaxes.getConfigurationSection(rawCurrencyType);
                if (currencySection == null) {
                    continue;
                }

                if (currencySection.contains("maximum_tax")) {
                    parsedMaximums.put(
                            normalizedCurrencyType,
                            normalizeMaximum(currencySection.getDouble("maximum_tax"))
                    );
                    continue;
                }

                final Double taxesSectionMaximum = resolveMaximumFromTaxesSection(taxesSection, normalizedCurrencyType);
                parsedMaximums.put(
                        normalizedCurrencyType,
                        taxesSectionMaximum == null ? -1D : normalizeMaximum(taxesSectionMaximum)
                );
            }
            return parsedMaximums;
        }

        for (final String rawCurrencyType : protectionSection.getStringList("shop_taxes")) {
            if (rawCurrencyType == null || rawCurrencyType.isBlank()) {
                continue;
            }

            final String normalizedCurrencyType = normalizeCurrencyType(rawCurrencyType);
            final Double taxesSectionMaximum = resolveMaximumFromTaxesSection(taxesSection, normalizedCurrencyType);
            parsedMaximums.put(
                    normalizedCurrencyType,
                    taxesSectionMaximum == null ? -1D : normalizeMaximum(taxesSectionMaximum)
            );
        }

        return parsedMaximums;
    }

    private static @Nullable Double resolveMaximumFromTaxesSection(
            final @Nullable ConfigurationSection taxesSection,
            final @NotNull String normalizedCurrencyType
    ) {
        final ConfigurationSection currencySection = findCurrencySection(taxesSection, normalizedCurrencyType);
        if (currencySection == null) {
            return null;
        }

        if (!currencySection.contains("maximum_tax")) {
            return -1D;
        }
        return currencySection.getDouble("maximum_tax");
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

    private static @Nullable Double parseMaximumValue(
            final @Nullable Object rawValue
    ) {
        if (rawValue instanceof Number number) {
            return number.doubleValue();
        }
        if (rawValue instanceof String text) {
            try {
                return Double.parseDouble(text.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private static double normalizeMaximum(
            final double maximum
    ) {
        return maximum > 0D ? maximum : -1D;
    }

    private static @NotNull String normalizeCurrencyType(
            final @NotNull String currencyType
    ) {
        return currencyType.trim().toLowerCase(Locale.ROOT);
    }
}
