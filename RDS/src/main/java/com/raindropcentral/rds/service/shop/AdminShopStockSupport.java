package com.raindropcentral.rds.service.shop;

import com.raindropcentral.rds.RDS;
import com.raindropcentral.rds.configs.AdminShopRestockMode;
import com.raindropcentral.rds.configs.AdminShopSection;
import com.raindropcentral.rds.database.entity.Shop;
import com.raindropcentral.rds.items.ShopItem;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public final class AdminShopStockSupport {

    private static final long TICK_DURATION_MILLIS = 50L;

    private AdminShopStockSupport() {
    }

    public static boolean isUnlimitedAdminStock(
            final @NotNull Shop shop,
            final @NotNull ShopItem item
    ) {
        return shop.isAdminShop() && !item.hasAdminStockLimit();
    }

    public static boolean usesLimitedAdminStock(
            final @NotNull Shop shop,
            final @NotNull ShopItem item
    ) {
        return shop.isAdminShop() && item.hasAdminStockLimit();
    }

    public static int getVisibleStockAmount(
            final @NotNull Shop shop,
            final @NotNull ShopItem item
    ) {
        if (usesLimitedAdminStock(shop, item)) {
            return Math.max(0, item.getAmount());
        }

        return item.getAmount();
    }

    public static @NotNull ShopItem configureStockLimit(
            final @NotNull RDS plugin,
            final @NotNull ShopItem item,
            final int stockLimit
    ) {
        if (stockLimit < 1) {
            return item.clearAdminStockSettings();
        }

        final AdminShopSection config = plugin.getDefaultConfig().getAdminShops();
        final int updatedAmount = item.hasAdminStockLimit()
                ? Math.min(stockLimit, Math.max(0, item.getAmount()))
                : stockLimit;

        return item.withAdminStockState(
                updatedAmount,
                stockLimit,
                resolveConfiguredIntervalTicks(config, item),
                resolveInitialReferenceTime(config)
        );
    }

    public static @NotNull ShopItem configureResetTimer(
            final @NotNull RDS plugin,
            final @NotNull ShopItem item,
            final long restockIntervalTicks
    ) {
        final AdminShopSection config = plugin.getDefaultConfig().getAdminShops();
        final long effectiveInterval = Math.max(1L, restockIntervalTicks);
        return item.withAdminStockSettings(
                item.hasAdminStockLimit() ? item.getAdminStockLimit() : null,
                effectiveInterval,
                item.getAdminStockReferenceTime() >= 0L
                        ? item.getAdminStockReferenceTime()
                        : resolveInitialReferenceTime(config)
        );
    }

    public static @NotNull ShopItem consumeLimitedStock(
            final @NotNull RDS plugin,
            final @NotNull ShopItem item,
            final int updatedAmount
    ) {
        final AdminShopSection config = plugin.getDefaultConfig().getAdminShops();
        final Long updatedReference = config.getRestockMode() == AdminShopRestockMode.GRADUAL
                ? System.currentTimeMillis()
                : item.getAdminStockReferenceTime() >= 0L
                        ? item.getAdminStockReferenceTime()
                        : resolveInitialReferenceTime(config);

        return item.withAdminStockState(
                Math.max(0, updatedAmount),
                item.getAdminStockLimit(),
                resolveConfiguredIntervalTicks(config, item),
                updatedReference
        );
    }

    public static boolean applyRestock(
            final @NotNull RDS plugin,
            final @NotNull Shop shop
    ) {
        if (!shop.isAdminShop()) {
            return false;
        }

        boolean changed = false;
        final var updatedItems = new java.util.ArrayList<com.raindropcentral.rds.items.AbstractItem>();
        for (final var item : shop.getItems()) {
            if (!(item instanceof ShopItem shopItem) || !shopItem.hasAdminStockLimit()) {
                updatedItems.add(item);
                continue;
            }

            final ShopItem updatedItem = applyRestock(plugin, shopItem);
            if (!areEquivalent(shopItem, updatedItem)) {
                changed = true;
            }
            updatedItems.add(updatedItem);
        }

        if (changed) {
            shop.setItems(updatedItems);
        }

        return changed;
    }

    public static @NotNull ShopItem applyRestock(
            final @NotNull RDS plugin,
            final @NotNull ShopItem item
    ) {
        if (!item.hasAdminStockLimit()) {
            return item;
        }

        final AdminShopSection config = plugin.getDefaultConfig().getAdminShops();
        return switch (config.getRestockMode()) {
            case FULL_AT_TIME -> applyFullRestock(config, item);
            case GRADUAL -> applyGradualRestock(config, item);
        };
    }

    public static long resolveReferenceTimeForMode(
            final @NotNull AdminShopSection config,
            final @NotNull AdminShopRestockMode restockMode
    ) {
        return restockMode == AdminShopRestockMode.FULL_AT_TIME
                ? resolveLatestScheduledRestockTime(config)
                : System.currentTimeMillis();
    }

    public static int countVisibleStock(
            final @NotNull Shop shop
    ) {
        int total = 0;
        for (final var item : shop.getItems()) {
            if (item instanceof ShopItem shopItem && getVisibleStockAmount(shop, shopItem) > 0) {
                total += getVisibleStockAmount(shop, shopItem);
            }
        }
        return total;
    }

    private static @NotNull ShopItem applyGradualRestock(
            final @NotNull AdminShopSection config,
            final @NotNull ShopItem item
    ) {
        final int stockLimit = item.getAdminStockLimit();
        final int currentAmount = Math.max(0, Math.min(stockLimit, item.getAmount()));
        if (currentAmount >= stockLimit) {
            return item.withAdminStockState(
                    stockLimit,
                    stockLimit,
                    resolveConfiguredIntervalTicks(config, item),
                    item.getAdminStockReferenceTime() >= 0L
                            ? item.getAdminStockReferenceTime()
                            : System.currentTimeMillis()
            );
        }

        final long intervalTicks = resolveConfiguredIntervalTicks(config, item);
        final long referenceTime = item.getAdminStockReferenceTime() >= 0L
                ? item.getAdminStockReferenceTime()
                : System.currentTimeMillis();
        final long elapsedMillis = Math.max(0L, System.currentTimeMillis() - referenceTime);
        final long elapsedIntervals = elapsedMillis / (intervalTicks * TICK_DURATION_MILLIS);
        if (elapsedIntervals < 1L) {
            return item.withAdminStockState(
                    currentAmount,
                    stockLimit,
                    intervalTicks,
                    referenceTime
            );
        }

        final int restoredItems = (int) Math.min(stockLimit - currentAmount, elapsedIntervals);
        final long updatedReferenceTime = referenceTime + (restoredItems * intervalTicks * TICK_DURATION_MILLIS);

        return item.withAdminStockState(
                Math.min(stockLimit, currentAmount + restoredItems),
                stockLimit,
                intervalTicks,
                updatedReferenceTime
        );
    }

    private static @NotNull ShopItem applyFullRestock(
            final @NotNull AdminShopSection config,
            final @NotNull ShopItem item
    ) {
        final int stockLimit = item.getAdminStockLimit();
        final long latestScheduledRestock = resolveLatestScheduledRestockTime(config);
        final long currentReferenceTime = item.getAdminStockReferenceTime() >= 0L
                ? item.getAdminStockReferenceTime()
                : 0L;
        if (latestScheduledRestock <= currentReferenceTime) {
            return item.withAdminStockState(
                    Math.max(0, Math.min(stockLimit, item.getAmount())),
                    stockLimit,
                    resolveConfiguredIntervalTicks(config, item),
                    currentReferenceTime
            );
        }

        return item.withAdminStockState(
                stockLimit,
                stockLimit,
                resolveConfiguredIntervalTicks(config, item),
                latestScheduledRestock
        );
    }

    private static long resolveConfiguredIntervalTicks(
            final @NotNull AdminShopSection config,
            final @NotNull ShopItem item
    ) {
        return item.getAdminRestockIntervalTicks() > 0L
                ? item.getAdminRestockIntervalTicks()
                : config.getDefaultResetTimerTicks();
    }

    private static long resolveInitialReferenceTime(
            final @NotNull AdminShopSection config
    ) {
        return resolveReferenceTimeForMode(config, config.getRestockMode());
    }

    private static long resolveLatestScheduledRestockTime(
            final @NotNull AdminShopSection config
    ) {
        final Instant now = Instant.now();
        final ZoneId timeZone = config.getTimeZoneId();
        ZonedDateTime scheduled = now.atZone(timeZone)
                .with(config.getFullRestockTime());
        if (scheduled.toInstant().isAfter(now)) {
            scheduled = scheduled.minusDays(1);
        }
        return scheduled.toInstant().toEpochMilli();
    }

    private static boolean areEquivalent(
            final @NotNull ShopItem left,
            final @NotNull ShopItem right
    ) {
        return left.getAmount() == right.getAmount()
                && left.getAdminStockLimit() == right.getAdminStockLimit()
                && left.getAdminRestockIntervalTicks() == right.getAdminRestockIntervalTicks()
                && left.getAdminStockReferenceTime() == right.getAdminStockReferenceTime()
                && left.getCurrencyType().equalsIgnoreCase(right.getCurrencyType())
                && Double.compare(left.getValue(), right.getValue()) == 0
                && left.getItem().isSimilar(right.getItem());
    }
}
