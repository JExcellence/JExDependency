package com.raindropcentral.rds.service.tax;

import com.raindropcentral.rds.RDS;
import com.raindropcentral.rds.configs.TaxCurrencySection;
import com.raindropcentral.rds.configs.TaxSection;
import com.raindropcentral.rplatform.economy.JExEconomyBridge;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Locale;

/**
 * Provides support utilities for shop tax.
 */
final class ShopTaxSupport {

    private ShopTaxSupport() {
    }

    static double calculateTax(
            final @NotNull TaxCurrencySection config,
            final int ownedShops
    ) {
        if (ownedShops <= 0) {
            return 0D;
        }

        final int tier = Math.max(ownedShops - 1, 0);
        final double rawTax = config.getInitialCost() * Math.pow(config.getGrowthRate(), tier);
        final double normalizedTax;
        if (!Double.isFinite(rawTax)) {
            normalizedTax = Math.max(0D, config.getInitialCost());
        } else {
            normalizedTax = Math.max(0D, rawTax);
        }

        return config.hasTaxCap()
                ? Math.min(normalizedTax, Math.max(0D, config.getMaximumTax()))
                : normalizedTax;
    }

    static long calculateInitialDelayTicks(
            final @NotNull TaxSection config,
            final @NotNull Instant now
    ) {
        final Instant nextRun = calculateNextRun(config, now);
        final Duration delay = Duration.between(now, nextRun);
        return Math.max(1L, (long) Math.ceil(Math.max(1D, delay.toMillis()) / 50.0D));
    }

    static @NotNull Instant calculateNextRun(
            final @NotNull TaxSection config,
            final @NotNull Instant now
    ) {
        final ZonedDateTime zonedNow = now.atZone(config.getTimeZoneId());
        final ZonedDateTime anchor = zonedNow.toLocalDate()
                .atTime(config.getStartTime())
                .atZone(config.getTimeZoneId());
        if (anchor.isAfter(zonedNow)) {
            return anchor.toInstant();
        }

        final long periodMillis = thisPeriodDuration(config).toMillis();
        if (periodMillis <= 0L) {
            return now.plusSeconds(1);
        }

        final long elapsedMillis = Duration.between(anchor.toInstant(), now).toMillis();
        final long periodsElapsed = (elapsedMillis / periodMillis) + 1L;
        return anchor.toInstant().plusMillis(periodMillis * periodsElapsed);
    }

    static boolean isCurrencyAvailable(
            final @NotNull RDS plugin,
            final @NotNull String currencyType
    ) {
        if (usesVaultCurrency(currencyType)) {
            return plugin.hasVaultEconomy();
        }

        final JExEconomyBridge bridge = JExEconomyBridge.getBridge();
        return bridge != null && hasCustomCurrency(bridge, currencyType);
    }

    static boolean hasFunds(
            final @NotNull RDS plugin,
            final @NotNull OfflinePlayer player,
            final @NotNull String currencyType,
            final double amount
    ) {
        if (amount <= 0D) {
            return true;
        }

        if (usesVaultCurrency(currencyType)) {
            return plugin.hasVaultFunds(player, amount);
        }

        final JExEconomyBridge bridge = JExEconomyBridge.getBridge();
        return bridge != null
                && hasCustomCurrency(bridge, currencyType)
                && bridge.has(player, currencyType, amount);
    }

    static boolean withdraw(
            final @NotNull RDS plugin,
            final @NotNull OfflinePlayer player,
            final @NotNull String currencyType,
            final double amount
    ) {
        if (amount <= 0D) {
            return true;
        }

        if (usesVaultCurrency(currencyType)) {
            return plugin.withdrawVault(player, amount);
        }

        final JExEconomyBridge bridge = JExEconomyBridge.getBridge();
        return bridge != null
                && hasCustomCurrency(bridge, currencyType)
                && bridge.withdraw(player, currencyType, amount).join();
    }

    static @NotNull String getCurrencyDisplayName(
            final @NotNull String currencyType
    ) {
        if (usesVaultCurrency(currencyType)) {
            return "Vault";
        }

        final JExEconomyBridge bridge = JExEconomyBridge.getBridge();
        return bridge == null ? currencyType : bridge.getCurrencyDisplayName(currencyType);
    }

    static @NotNull String formatCurrency(
            final @NotNull RDS plugin,
            final @NotNull String currencyType,
            final double amount
    ) {
        if (usesVaultCurrency(currencyType)) {
            return plugin.formatVaultCurrency(amount);
        }

        return String.format(Locale.US, "%.2f", amount) + " " + getCurrencyDisplayName(currencyType);
    }

    static @NotNull Duration getPeriodDuration(
            final @NotNull TaxSection config
    ) {
        return thisPeriodDuration(config);
    }

    private static @NotNull Duration thisPeriodDuration(
            final @NotNull TaxSection config
    ) {
        return Duration.ofMillis(Math.max(1L, config.getDurationTicks()) * 50L);
    }

    private static boolean hasCustomCurrency(
            final @NotNull JExEconomyBridge bridge,
            final @NotNull String currencyType
    ) {
        try {
            final Method hasCurrencyMethod = JExEconomyBridge.class.getMethod("hasCurrency", String.class);
            return Boolean.TRUE.equals(hasCurrencyMethod.invoke(bridge, currencyType));
        } catch (ReflectiveOperationException ignored) {
            try {
                final Method findCurrencyMethod = JExEconomyBridge.class.getDeclaredMethod("findCurrency", String.class);
                findCurrencyMethod.setAccessible(true);
                return findCurrencyMethod.invoke(bridge, currencyType) != null;
            } catch (ReflectiveOperationException exception) {
                return false;
            }
        }
    }

    private static boolean usesVaultCurrency(
            final @NotNull String currencyType
    ) {
        return "vault".equalsIgnoreCase(currencyType);
    }
}