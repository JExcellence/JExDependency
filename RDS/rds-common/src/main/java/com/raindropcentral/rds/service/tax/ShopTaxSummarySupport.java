package com.raindropcentral.rds.service.tax;

import com.raindropcentral.rds.RDS;
import com.raindropcentral.rds.configs.TaxCurrencySection;
import com.raindropcentral.rds.configs.TaxSection;
import com.raindropcentral.rds.database.entity.Shop;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Provides support utilities for shop tax summary.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public final class ShopTaxSummarySupport {

    private static final DateTimeFormatter NEXT_TAX_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm z");

    private ShopTaxSummarySupport() {
    }

    public static @NotNull ShopTaxSummary summarize(
            final @NotNull RDS plugin,
            final @NotNull UUID ownerId
    ) {
        return summarize(plugin, ownerId, Instant.now());
    }

    static @NotNull ShopTaxSummary summarize(
            final @NotNull RDS plugin,
            final @NotNull UUID ownerId,
            final @NotNull Instant now
    ) {
        final TaxSection taxes = plugin.getDefaultConfig().getTaxes();
        final List<Shop> taxableShops = getTaxableShops(plugin, ownerId);
        final int taxedShops = taxableShops.size();
        final int neverAvailabilityItems = ShopTaxSupport.countNeverAvailabilityItems(taxableShops);
        final Instant nextTaxAt = ShopTaxSupport.calculateNextRun(taxes, now);
        final ZoneId zoneId = taxes.getTimeZoneId();

        final List<String> amounts = new ArrayList<>();
        if (taxedShops > 0) {
            for (final Map.Entry<String, TaxCurrencySection> entry : taxes.getCurrencies().entrySet()) {
                final String currencyType = entry.getKey();
                if (!ShopTaxSupport.isCurrencyAvailable(plugin, currencyType)) {
                    continue;
                }

                final double baseAmount = ShopTaxSupport.calculateTax(entry.getValue(), taxedShops);
                final double amount = ShopTaxSupport.applyNeverItemPenalty(
                        baseAmount,
                        taxes.getNeverItemPenaltyRate(),
                        neverAvailabilityItems
                );
                if (amount <= 0D) {
                    continue;
                }

                amounts.add(
                        ShopTaxSupport.formatCurrency(plugin, currencyType, amount)
                );
            }
        }

        final String amountSummary = amounts.isEmpty()
                ? "None"
                : String.join(", ", amounts);
        final ZonedDateTime zonedNextTax = ZonedDateTime.ofInstant(nextTaxAt, zoneId);
        final String nextTaxDisplay = zonedNextTax.format(NEXT_TAX_FORMAT);
        final String timeUntil = formatRemainingTime(Duration.between(now, nextTaxAt));

        return new ShopTaxSummary(
                taxedShops,
                amountSummary,
                nextTaxAt,
                zoneId,
                nextTaxDisplay,
                timeUntil
        );
    }

    private static @NotNull List<Shop> getTaxableShops(
            final @NotNull RDS plugin,
            final @NotNull UUID ownerId
    ) {
        final List<Shop> taxableShops = new ArrayList<>();
        for (final Shop shop : plugin.getShopRepository().findAllShops()) {
            if (!ShopTaxSupport.isTaxableShopForOwner(shop, ownerId)) {
                continue;
            }

            taxableShops.add(shop);
        }

        return taxableShops;
    }

    private static @NotNull String formatRemainingTime(
            final @NotNull Duration duration
    ) {
        long totalMinutes = Math.max(0L, duration.toMinutes());
        final long days = totalMinutes / (24L * 60L);
        totalMinutes %= 24L * 60L;
        final long hours = totalMinutes / 60L;
        final long minutes = totalMinutes % 60L;

        final List<String> parts = new ArrayList<>();
        if (days > 0L) {
            parts.add(days + "d");
        }
        if (hours > 0L) {
            parts.add(hours + "h");
        }
        if (minutes > 0L) {
            parts.add(minutes + "m");
        }

        if (parts.isEmpty()) {
            return "<1m";
        }

        final int limit = Math.min(parts.size(), 3);
        return String.join(" ", parts.subList(0, limit));
    }

    /**
     * Represents shop tax summary.
     *
     * @param taxedShops taxed shops
     * @param amountSummary amount summary
     * @param nextTaxAt next tax at
     * @param timeZone time zone
     * @param nextTaxDisplay next tax display
     * @param timeUntilDisplay time until display
     */
    public record ShopTaxSummary(
            int taxedShops,
            @NotNull String amountSummary,
            @NotNull Instant nextTaxAt,
            @NotNull ZoneId timeZone,
            @NotNull String nextTaxDisplay,
            @NotNull String timeUntilDisplay
    ) {
        /**
         * Indicates whether the summary contains any taxable shops.
         *
         * @return {@code true} if taxable shops are present; otherwise {@code false}
         */
        public boolean hasTaxableShops() {
            return this.taxedShops > 0;
        }

        /**
         * Indicates whether configured charges is available.
         *
         * @return {@code true} if configured charges; otherwise {@code false}
         */
        public boolean hasConfiguredCharges() {
            return !"None".equalsIgnoreCase(this.amountSummary);
        }
    }
}
