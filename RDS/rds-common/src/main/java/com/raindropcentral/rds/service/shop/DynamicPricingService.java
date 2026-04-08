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

package com.raindropcentral.rds.service.shop;

import com.raindropcentral.rds.RDS;
import com.raindropcentral.rds.configs.ConfigSection;
import com.raindropcentral.rds.configs.DynamicPricingMissingBasePriceMode;
import com.raindropcentral.rds.configs.DynamicPricingRecentSalesMode;
import com.raindropcentral.rds.configs.DynamicPricingSection;
import com.raindropcentral.rds.database.entity.Shop;
import com.raindropcentral.rds.database.entity.ShopLedgerEntry;
import com.raindropcentral.rds.database.entity.ShopLedgerType;
import com.raindropcentral.rds.items.AbstractItem;
import com.raindropcentral.rds.items.ShopItem;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Resolves market-aware dynamic prices for shop items.
 *
 * <p>When globally enabled and the target item opts in, this service uses material base prices
 * from {@code material-prices.yml}, then applies configurable market formula weights and
 * stability controls using server-wide market snapshots.</p>
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public class DynamicPricingService {

    private static final long TICK_DURATION_MILLIS = 50L;
    private static final String MATERIALS_SECTION = "materials";

    private final RDS plugin;
    private final DynamicPricingFormulaStrategy formulaStrategy;
    private final Object refreshLock = new Object();
    private final Map<MarketIdentifier, Double> previousPriceByMarket = new HashMap<>();
    private volatile Map<MarketIdentifier, DynamicPricingMarketStats> marketSnapshot = Map.of();
    private volatile Map<String, Map<String, Double>> materialPrices = Map.of();
    private volatile long materialPricesLastModified = Long.MIN_VALUE;
    private volatile long lastRefreshEpochMillis = 0L;

    /**
     * Creates a new dynamic-pricing service.
     *
     * @param plugin plugin instance
     */
    public DynamicPricingService(
            final @NotNull RDS plugin
    ) {
        this(plugin, new WeightedDynamicPricingFormulaStrategy());
    }

    /**
     * Creates a new dynamic-pricing service.
     *
     * @param plugin plugin instance
     * @param formulaStrategy market formula strategy used for multiplier computation
     */
    public DynamicPricingService(
            final @NotNull RDS plugin,
            final @NotNull DynamicPricingFormulaStrategy formulaStrategy
    ) {
        this.plugin = plugin;
        this.formulaStrategy = formulaStrategy;
    }

    /**
     * Performs an initial market refresh.
     */
    public void start() {
        this.refreshNow();
    }

    /**
     * Clears cached dynamic-pricing state.
     */
    public void shutdown() {
        synchronized (this.refreshLock) {
            this.marketSnapshot = Map.of();
            this.previousPriceByMarket.clear();
            this.materialPrices = Map.of();
            this.materialPricesLastModified = Long.MIN_VALUE;
            this.lastRefreshEpochMillis = 0L;
        }
    }

    /**
     * Rebuilds market and material-price caches immediately.
     */
    public void refreshNow() {
        synchronized (this.refreshLock) {
            this.refreshSnapshot(System.currentTimeMillis());
        }
    }

    /**
     * Resolves the current unit price for a shop item.
     *
     * @param item target shop item
     * @return resolved dynamic price quote, or a static quote when dynamic pricing is disabled
     * globally or for the item itself
     */
    public @NotNull DynamicPriceQuote resolvePrice(
            final @NotNull ShopItem item
    ) {
        final ConfigSection config = this.plugin.getDefaultConfig();
        final DynamicPricingSection dynamicPricing = config.getDynamicPricing();
        this.refreshIfStale(dynamicPricing);

        final String currencyType = normalizeCurrencyType(item.getCurrencyType());
        final String materialType = item.getItem().getType().name().toUpperCase(Locale.ROOT);
        final MarketIdentifier marketIdentifier = new MarketIdentifier(materialType, currencyType);
        final DynamicPricingMarketStats marketStats = this.marketSnapshot.getOrDefault(
                marketIdentifier,
                DynamicPricingMarketStats.empty()
        );

        if (!dynamicPricing.isEnabled() || !item.isDynamicPricingEnabled()) {
            final double staticPrice = sanitizeNonNegativePrice(item.getValue());
            return DynamicPriceQuote.staticPrice(staticPrice, marketStats);
        }

        final BasePriceResolution basePriceResolution = this.resolveBasePrice(config, dynamicPricing, item);
        final Double previousPrice = this.previousPriceByMarket.get(marketIdentifier);
        final DynamicPriceQuote quote = computeQuote(
                basePriceResolution.basePrice(),
                marketStats,
                previousPrice,
                dynamicPricing,
                this.formulaStrategy,
                basePriceResolution.usedFallbackBasePrice()
        );
        this.previousPriceByMarket.put(marketIdentifier, quote.unitPrice());
        return quote;
    }

    /**
     * Returns a snapshot of currently cached market stats.
     *
     * @return current market snapshot
     */
    public @NotNull Map<MarketIdentifier, DynamicPricingMarketStats> getMarketSnapshot() {
        return this.marketSnapshot;
    }

    static @NotNull DynamicPriceQuote computeQuote(
            final double basePrice,
            final @NotNull DynamicPricingMarketStats marketStats,
            final @Nullable Double previousPrice,
            final @NotNull DynamicPricingSection dynamicPricing,
            final @NotNull DynamicPricingFormulaStrategy formulaStrategy,
            final boolean usedFallbackBasePrice
    ) {
        final double sanitizedBasePrice = sanitizeNonNegativePrice(basePrice);
        final DynamicPricingSection.FormulaSettings formulaSettings = dynamicPricing.getFormulaSettings();
        final DynamicPricingSection.StabilitySettings stabilitySettings = dynamicPricing.getStabilitySettings();

        double rawMultiplier = formulaStrategy.computeRawMultiplier(marketStats, formulaSettings);
        if (!Double.isFinite(rawMultiplier)) {
            rawMultiplier = formulaSettings.baseMultiplier();
        }
        final double boundedMultiplier = clamp(
                rawMultiplier,
                formulaSettings.minimumMultiplier(),
                formulaSettings.maximumMultiplier()
        );

        final double rawPrice = applyAbsolutePriceBounds(sanitizedBasePrice * boundedMultiplier, stabilitySettings);
        final double smoothedPrice = applySmoothing(rawPrice, previousPrice, stabilitySettings);
        final double finalPrice = roundPrice(
                applyAbsolutePriceBounds(smoothedPrice, stabilitySettings),
                stabilitySettings.roundingScale()
        );

        return new DynamicPriceQuote(
                finalPrice,
                sanitizedBasePrice,
                boundedMultiplier,
                marketStats,
                usedFallbackBasePrice,
                true
        );
    }

    static @NotNull Map<MarketIdentifier, DynamicPricingMarketStats> buildMarketSnapshot(
            final @NotNull List<Shop> shops,
            final @NotNull DynamicPricingSection dynamicPricing,
            final @NotNull LocalDateTime now
    ) {
        if (shops.isEmpty()) {
            return Map.of();
        }

        final DynamicPricingSection.InfiniteStockSettings infiniteStockSettings =
                dynamicPricing.getInfiniteStockSettings();
        final DynamicPricingRecentSalesMode recentSalesMode = dynamicPricing.getRecentSalesMode();
        final LocalDateTime salesCutoff = now.minusMinutes(dynamicPricing.getRecentSalesWindowMinutes());

        final Map<MarketIdentifier, Long> highestFiniteByMarket = new HashMap<>();
        final Map<MarketIdentifier, Long> recentSalesByMarket = new HashMap<>();
        final List<ListingObservation> listingObservations = new ArrayList<>();

        for (final Shop shop : shops) {
            if (shop == null) {
                continue;
            }

            final Object shopIdentity = resolveShopIdentity(shop);
            final Set<MarketIdentifier> dynamicMarketsForShop = new HashSet<>();
            final Set<java.util.UUID> dynamicEntryIdsForShop = new HashSet<>();
            for (final AbstractItem abstractItem : shop.getItems()) {
                if (!(abstractItem instanceof ShopItem shopItem)
                        || !shopItem.isAvailableNow()
                        || !shopItem.isDynamicPricingEnabled()) {
                    continue;
                }

                final MarketIdentifier marketIdentifier = new MarketIdentifier(
                        shopItem.getItem().getType().name(),
                        normalizeCurrencyType(shopItem.getCurrencyType())
                );
                dynamicMarketsForShop.add(marketIdentifier);
                dynamicEntryIdsForShop.add(shopItem.getEntryId());
                final boolean infiniteListing = AdminShopStockSupport.isUnlimitedAdminStock(shop, shopItem);
                final long finiteAmount = Math.max(0L, AdminShopStockSupport.getVisibleStockAmount(shop, shopItem));
                listingObservations.add(
                        new ListingObservation(marketIdentifier, shopIdentity, infiniteListing, finiteAmount)
                );

                if (!infiniteListing && finiteAmount > 0L) {
                    highestFiniteByMarket.merge(marketIdentifier, finiteAmount, Math::max);
                }
            }

            for (final ShopLedgerEntry ledgerEntry : shop.getLedgerEntries()) {
                if (!isRecentPurchaseLedgerEntry(ledgerEntry, salesCutoff)) {
                    continue;
                }

                final MarketIdentifier marketIdentifier = resolveLedgerMarket(ledgerEntry);
                if (marketIdentifier == null) {
                    continue;
                }

                final java.util.UUID itemEntryId = ledgerEntry.getItemEntryId();
                if (itemEntryId != null) {
                    if (!dynamicEntryIdsForShop.contains(itemEntryId)) {
                        continue;
                    }
                } else if (!dynamicMarketsForShop.contains(marketIdentifier)) {
                    continue;
                }

                final long increment = recentSalesMode == DynamicPricingRecentSalesMode.TRANSACTIONS
                        ? 1L
                        : Math.max(1L, ledgerEntry.getItemAmount() == null ? 1L : ledgerEntry.getItemAmount());
                recentSalesByMarket.merge(marketIdentifier, increment, Long::sum);
            }
        }

        final Map<MarketIdentifier, Long> listedItemsByMarket = new HashMap<>();
        final Map<MarketIdentifier, Set<Object>> sellersByMarket = new HashMap<>();
        for (final ListingObservation listing : listingObservations) {
            final long effectiveListedAmount = listing.infiniteListing()
                    ? resolveInfiniteListedAmount(
                            listing.marketIdentifier(),
                            highestFiniteByMarket,
                            infiniteStockSettings
                    )
                    : listing.finiteAmount();

            final boolean countsAsSeller = listing.infiniteListing()
                    ? infiniteStockSettings.countAsSeller() || effectiveListedAmount > 0L
                    : listing.finiteAmount() > 0L;
            if (countsAsSeller) {
                sellersByMarket.computeIfAbsent(listing.marketIdentifier(), key -> new HashSet<>())
                        .add(listing.shopIdentity());
            }

            if (effectiveListedAmount > 0L) {
                listedItemsByMarket.merge(listing.marketIdentifier(), effectiveListedAmount, Long::sum);
            }
        }

        final Set<MarketIdentifier> markets = new HashSet<>();
        markets.addAll(sellersByMarket.keySet());
        markets.addAll(listedItemsByMarket.keySet());
        markets.addAll(recentSalesByMarket.keySet());
        if (markets.isEmpty()) {
            return Map.of();
        }

        final Map<MarketIdentifier, DynamicPricingMarketStats> snapshot = new LinkedHashMap<>();
        for (final MarketIdentifier marketIdentifier : markets) {
            snapshot.put(
                    marketIdentifier,
                    new DynamicPricingMarketStats(
                            sellersByMarket.getOrDefault(marketIdentifier, Set.of()).size(),
                            listedItemsByMarket.getOrDefault(marketIdentifier, 0L),
                            recentSalesByMarket.getOrDefault(marketIdentifier, 0L)
                    )
            );
        }
        return Map.copyOf(snapshot);
    }

    static double resolveMissingBasePrice(
            final @NotNull DynamicPricingMissingBasePriceMode mode,
            final double listingValue,
            final double configDefaultItemPrice,
            final double fixedFallback
    ) {
        final double resolved = switch (mode) {
            case CONFIG_DEFAULT -> configDefaultItemPrice;
            case LISTING_VALUE -> listingValue;
            case FIXED_FALLBACK -> fixedFallback;
        };
        return sanitizeNonNegativePrice(resolved);
    }

    private void refreshIfStale(
            final @NotNull DynamicPricingSection dynamicPricing
    ) {
        final long refreshIntervalMillis = Math.max(1L, dynamicPricing.getRefreshIntervalTicks()) * TICK_DURATION_MILLIS;
        final long now = System.currentTimeMillis();
        if ((now - this.lastRefreshEpochMillis) < refreshIntervalMillis) {
            return;
        }

        synchronized (this.refreshLock) {
            final long syncedNow = System.currentTimeMillis();
            if ((syncedNow - this.lastRefreshEpochMillis) < refreshIntervalMillis) {
                return;
            }
            this.refreshSnapshot(syncedNow);
        }
    }

    private void refreshSnapshot(
            final long nowEpochMillis
    ) {
        final ConfigSection config = this.plugin.getDefaultConfig();
        final DynamicPricingSection dynamicPricing = config.getDynamicPricing();
        this.refreshMaterialPriceCache();

        if (!dynamicPricing.isEnabled()) {
            this.marketSnapshot = Map.of();
            this.previousPriceByMarket.clear();
            this.lastRefreshEpochMillis = nowEpochMillis;
            return;
        }

        final var shopRepository = this.plugin.getShopRepository();
        if (shopRepository == null) {
            this.marketSnapshot = Map.of();
            this.previousPriceByMarket.clear();
            this.lastRefreshEpochMillis = nowEpochMillis;
            return;
        }

        try {
            this.marketSnapshot = buildMarketSnapshot(
                    shopRepository.findAllShops(),
                    dynamicPricing,
                    LocalDateTime.now()
            );
        } catch (Exception exception) {
            this.plugin.getLogger().warning(
                    "Dynamic pricing market refresh failed: " + exception.getMessage()
            );
        }
        this.lastRefreshEpochMillis = nowEpochMillis;
    }

    private void refreshMaterialPriceCache() {
        final File materialPricesFile = this.plugin.getMaterialPricesFile();
        final long currentLastModified = materialPricesFile.exists() ? materialPricesFile.lastModified() : Long.MIN_VALUE;
        if (currentLastModified == this.materialPricesLastModified) {
            return;
        }

        this.materialPrices = loadMaterialPrices(materialPricesFile);
        this.materialPricesLastModified = currentLastModified;
    }

    private @NotNull BasePriceResolution resolveBasePrice(
            final @NotNull ConfigSection config,
            final @NotNull DynamicPricingSection dynamicPricing,
            final @NotNull ShopItem item
    ) {
        final String materialType = item.getItem().getType().name().toUpperCase(Locale.ROOT);
        final String currencyType = normalizeCurrencyType(item.getCurrencyType());
        final Map<String, Double> materialCurrencyPrices = this.materialPrices.get(materialType);
        if (materialCurrencyPrices != null && !materialCurrencyPrices.isEmpty()) {
            final Double configuredPrice = findCurrencyPrice(materialCurrencyPrices, currencyType);
            if (configuredPrice != null && Double.isFinite(configuredPrice) && configuredPrice >= 0.0D) {
                return new BasePriceResolution(configuredPrice, false);
            }
        }

        final double fallbackPrice = resolveMissingBasePrice(
                dynamicPricing.getMissingBasePriceMode(),
                item.getValue(),
                config.getDefaultItemPrice(),
                dynamicPricing.getMissingBasePriceFallback()
        );
        return new BasePriceResolution(fallbackPrice, true);
    }

    private static @NotNull Map<String, Map<String, Double>> loadMaterialPrices(
            final @NotNull File materialPricesFile
    ) {
        if (!materialPricesFile.exists()) {
            return Map.of();
        }

        final YamlConfiguration configuration = YamlConfiguration.loadConfiguration(materialPricesFile);
        final ConfigurationSection materialsSection = configuration.getConfigurationSection(MATERIALS_SECTION);
        if (materialsSection == null) {
            return Map.of();
        }

        final Map<String, Map<String, Double>> loadedMaterialPrices = new LinkedHashMap<>();
        for (final String materialKey : materialsSection.getKeys(false)) {
            if (materialKey == null || materialKey.isBlank()) {
                continue;
            }

            final ConfigurationSection materialSection = materialsSection.getConfigurationSection(materialKey);
            if (materialSection == null) {
                continue;
            }

            final Map<String, Double> currencyPrices = new LinkedHashMap<>();
            for (final String currencyKey : materialSection.getKeys(false)) {
                if (currencyKey == null || currencyKey.isBlank()) {
                    continue;
                }

                final Double parsedPrice = parsePrice(materialSection.get(currencyKey));
                if (parsedPrice == null || !Double.isFinite(parsedPrice) || parsedPrice < 0.0D) {
                    continue;
                }

                currencyPrices.put(normalizeCurrencyType(currencyKey), parsedPrice);
            }

            if (!currencyPrices.isEmpty()) {
                loadedMaterialPrices.put(materialKey.trim().toUpperCase(Locale.ROOT), Map.copyOf(currencyPrices));
            }
        }

        return Map.copyOf(loadedMaterialPrices);
    }

    private static @Nullable Double parsePrice(
            final @Nullable Object rawValue
    ) {
        if (rawValue instanceof Number number) {
            return number.doubleValue();
        }
        if (rawValue instanceof String text) {
            final String trimmed = text.trim();
            if (trimmed.isEmpty()) {
                return null;
            }
            try {
                return Double.parseDouble(trimmed);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private static @Nullable Double findCurrencyPrice(
            final @NotNull Map<String, Double> currencyPrices,
            final @NotNull String currencyType
    ) {
        final Double directMatch = currencyPrices.get(currencyType);
        if (directMatch != null) {
            return directMatch;
        }

        for (final Map.Entry<String, Double> entry : currencyPrices.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(currencyType)) {
                return entry.getValue();
            }
        }
        return null;
    }

    static long resolveInfiniteListedAmount(
            final @NotNull MarketIdentifier marketIdentifier,
            final @NotNull Map<MarketIdentifier, Long> highestFiniteByMarket,
            final @NotNull DynamicPricingSection.InfiniteStockSettings infiniteStockSettings
    ) {
        return switch (infiniteStockSettings.mode()) {
            case IGNORE -> 0L;
            case FIXED_CAP -> Math.max(0L, infiniteStockSettings.fixedCapAmount());
            case USE_HIGHEST_FINITE -> Math.max(
                    0L,
                    highestFiniteByMarket.getOrDefault(
                            marketIdentifier,
                            infiniteStockSettings.fallbackAmountWhenNoFinite()
                    )
            );
        };
    }

    private static boolean isRecentPurchaseLedgerEntry(
            final @Nullable ShopLedgerEntry ledgerEntry,
            final @NotNull LocalDateTime salesCutoff
    ) {
        if (ledgerEntry == null || ledgerEntry.getEntryType() != ShopLedgerType.PURCHASE) {
            return false;
        }
        if (ledgerEntry.getCreatedAt() == null) {
            return false;
        }
        return !ledgerEntry.getCreatedAt().isBefore(salesCutoff);
    }

    private static @Nullable MarketIdentifier resolveLedgerMarket(
            final @NotNull ShopLedgerEntry ledgerEntry
    ) {
        final String itemType = ledgerEntry.getItemType();
        final String currencyType = ledgerEntry.getCurrencyType();
        if (itemType == null || itemType.isBlank() || currencyType == null || currencyType.isBlank()) {
            return null;
        }

        final Material material = resolveMaterial(itemType);
        if (material == null) {
            return null;
        }

        return new MarketIdentifier(material.name(), normalizeCurrencyType(currencyType));
    }

    private static @Nullable Material resolveMaterial(
            final @NotNull String rawMaterial
    ) {
        final String normalized = rawMaterial.trim().toUpperCase(Locale.ROOT);
        final Material direct = Material.matchMaterial(normalized);
        if (direct != null) {
            return direct;
        }

        try {
            return Material.valueOf(normalized);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static @NotNull Object resolveShopIdentity(
            final @NotNull Shop shop
    ) {
        if (shop.getId() != null) {
            return "id:" + shop.getId();
        }

        final var location = shop.getShopLocation();
        if (location != null) {
            final String worldName = location.getWorld() == null ? "unknown" : location.getWorld().getName();
            return "loc:" + worldName + ":" + location.getBlockX() + ":" + location.getBlockY() + ":" + location.getBlockZ();
        }

        return "owner:" + shop.getOwner();
    }

    private static double applySmoothing(
            final double targetPrice,
            final @Nullable Double previousPrice,
            final @NotNull DynamicPricingSection.StabilitySettings stabilitySettings
    ) {
        if (previousPrice == null || !Double.isFinite(previousPrice) || previousPrice < 0.0D) {
            return targetPrice;
        }

        final double alpha = stabilitySettings.smoothingAlpha();
        double smoothed = (targetPrice * alpha) + (previousPrice * (1.0D - alpha));
        if (previousPrice > 0.0D && stabilitySettings.maxStepChangePercent() > 0.0D) {
            final double minStepPrice = previousPrice * (1.0D - stabilitySettings.maxStepChangePercent());
            final double maxStepPrice = previousPrice * (1.0D + stabilitySettings.maxStepChangePercent());
            smoothed = clamp(smoothed, Math.min(minStepPrice, maxStepPrice), Math.max(minStepPrice, maxStepPrice));
        }
        return smoothed;
    }

    private static double applyAbsolutePriceBounds(
            final double price,
            final @NotNull DynamicPricingSection.StabilitySettings stabilitySettings
    ) {
        double boundedPrice = Math.max(0.0D, price);
        boundedPrice = Math.max(stabilitySettings.minimumPrice(), boundedPrice);
        if (stabilitySettings.maximumPrice() > 0.0D) {
            boundedPrice = Math.min(stabilitySettings.maximumPrice(), boundedPrice);
        }
        return boundedPrice;
    }

    private static double roundPrice(
            final double amount,
            final int scale
    ) {
        if (!Double.isFinite(amount) || amount < 0.0D) {
            return 0.0D;
        }
        return BigDecimal.valueOf(amount)
                .setScale(Math.max(0, scale), RoundingMode.HALF_UP)
                .doubleValue();
    }

    private static double clamp(
            final double value,
            final double minimum,
            final double maximum
    ) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private static double sanitizeNonNegativePrice(
            final double price
    ) {
        if (!Double.isFinite(price) || price < 0.0D) {
            return 0.0D;
        }
        return price;
    }

    private static @NotNull String normalizeCurrencyType(
            final @Nullable String currencyType
    ) {
        if (currencyType == null || currencyType.isBlank()) {
            return "vault";
        }
        return currencyType.trim().toLowerCase(Locale.ROOT);
    }

    private record BasePriceResolution(
            double basePrice,
            boolean usedFallbackBasePrice
    ) {
    }

    private record ListingObservation(
            @NotNull MarketIdentifier marketIdentifier,
            @NotNull Object shopIdentity,
            boolean infiniteListing,
            long finiteAmount
    ) {
    }

    /**
     * Represents one market identity keyed by material type and currency type.
     *
     * @param materialType item material type (for example {@code DIAMOND})
     * @param currencyType normalized currency identifier (for example {@code vault})
     *
     * @author ItsRainingHP
     * @since 1.0.0
     * @version 1.0.0
     */
    public record MarketIdentifier(
            @NotNull String materialType,
            @NotNull String currencyType
    ) {

        /**
         * Creates a normalized market identifier.
         *
         * @param materialType item material type (for example {@code DIAMOND})
         * @param currencyType normalized currency identifier (for example {@code vault})
         */
        public MarketIdentifier {
            materialType = materialType == null || materialType.isBlank()
                    ? "AIR"
                    : materialType.trim().toUpperCase(Locale.ROOT);
            currencyType = normalizeCurrencyType(currencyType);
        }
    }

    /**
     * Represents one resolved dynamic price quote.
     *
     * @param unitPrice final resolved unit price
     * @param basePrice base material price used as formula foundation
     * @param multiplier applied bounded market multiplier
     * @param marketStats market signals used for this calculation
     * @param usedFallbackBasePrice whether fallback base pricing was used
     * @param dynamicPricingEnabled whether dynamic pricing was enabled when calculated
     *
     * @author ItsRainingHP
     * @since 1.0.0
     * @version 1.0.0
     */
    public record DynamicPriceQuote(
            double unitPrice,
            double basePrice,
            double multiplier,
            @NotNull DynamicPricingMarketStats marketStats,
            boolean usedFallbackBasePrice,
            boolean dynamicPricingEnabled
    ) {

        /**
         * Creates a static-price quote used when dynamic pricing is disabled.
         *
         * @param staticPrice static listing price
         * @param marketStats current market stats snapshot
         * @return static-price quote
         */
        public static @NotNull DynamicPriceQuote staticPrice(
                final double staticPrice,
                final @NotNull DynamicPricingMarketStats marketStats
        ) {
            final double sanitized = sanitizeNonNegativePrice(staticPrice);
            return new DynamicPriceQuote(
                    sanitized,
                    sanitized,
                    1.0D,
                    marketStats,
                    false,
                    false
            );
        }
    }
}
