package com.raindropcentral.rds.configs;

import de.jexcellence.configmapper.sections.AConfigSection;
import de.jexcellence.configmapper.sections.CSAlways;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@CSAlways
@SuppressWarnings("unused")
public class ConfigSection extends AConfigSection {
    
    private Map<String, StoreCurrencySection> store;
    private String default_currency_type;
    private List<String> blacklisted_currencies;
    private Integer max_shops;
    private TaxSection taxes;
    private BossBarSection boss_bar;

    public ConfigSection(EvaluationEnvironmentBuilder baseEnvironment) {
        super(baseEnvironment);
    }
    
    public @NotNull Map<String, StoreCurrencySection> getStore() {
        return this.store == null ? new LinkedHashMap<>() :
               new LinkedHashMap<>(this.store);
    }

    public @NotNull Map<String, StoreCurrencySection> getStoreCosts() {
        return this.getStore();
    }

    public @NotNull String getDefaultCurrencyType() {
        if (this.default_currency_type != null && !this.default_currency_type.isBlank()) {
            return this.default_currency_type.trim().toLowerCase(Locale.ROOT);
        }

        if (this.store != null && !this.store.isEmpty()) {
            return this.store.keySet().iterator().next();
        }

        return "vault";
    }

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

    public int getMaxShops() {
        return this.max_shops == null ? -1 : this.max_shops;
    }

    public boolean hasShopLimit() {
        return this.getMaxShops() > 0;
    }

    public @NotNull TaxSection getTaxes() {
        return this.taxes == null
                ? TaxSection.createDefault(this.getDefaultCurrencyType())
                : this.taxes;
    }

    public @NotNull BossBarSection getBossBar() {
        return this.boss_bar == null
                ? new BossBarSection(new EvaluationEnvironmentBuilder())
                : this.boss_bar;
    }

    public double getTaxInitialCost() {
        return this.getDefaultTaxCurrency().getInitialCost();
    }

    public double getTaxGrowthRate() {
        return this.getDefaultTaxCurrency().getGrowthRate();
    }

    public double getMaximumTax() {
        return this.getDefaultTaxCurrency().getMaximumTax();
    }

    public @NotNull TaxCurrencySection getDefaultTaxCurrency() {
        return this.getTaxes().getTaxCurrency(this.getDefaultCurrencyType());
    }

    public void setTaxes(
            final @NotNull TaxSection taxes
    ) {
        this.taxes = taxes;
    }

    public @NotNull StoreCurrencySection getDefaultStoreCurrency() {
        return this.getStoreCurrency(this.getDefaultCurrencyType());
    }

    public @NotNull StoreCurrencySection getStoreCurrency(
            final @Nullable String currencyType
    ) {
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

    public double getInitialCost() {
        return this.getDefaultStoreCurrency().getInitialCost();
    }

    public double getGrowthRate() {
        return this.getDefaultStoreCurrency().getGrowthRate();
    }
    
    @Override
    public void afterParsing(final List<Field> fields) throws Exception {
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

    private @NotNull StoreCurrencySection createFallbackStoreCurrency(
            final @NotNull String currencyType
    ) {
        final StoreCurrencySection fallback = new StoreCurrencySection(new EvaluationEnvironmentBuilder());
        fallback.setContext(currencyType, 1000.0, 1.125);
        return fallback;
    }
}
