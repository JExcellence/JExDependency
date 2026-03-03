/*
 * StorageStorePricingSupport.java
 *
 * @author RaindropCentral
 * @version 5.0.0
 */

package com.raindropcentral.rdr.view;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.raindropcentral.rdr.RDR;
import com.raindropcentral.rdr.configs.ConfigSection;
import com.raindropcentral.rdr.configs.StoreCurrencySection;
import com.raindropcentral.rdr.database.entity.RDRPlayer;
import com.raindropcentral.rplatform.economy.JExEconomyBridge;
import me.devnatan.inventoryframework.context.Context;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

final class StorageStorePricingSupport {

    private StorageStorePricingSupport() {
    }

    static @NotNull List<StorageCurrencyCost> getConfiguredStoreCosts(
        final @NotNull RDR plugin,
        final @NotNull ConfigSection config,
        final int ownedStorages
    ) {
        final List<StorageCurrencyCost> costs = new ArrayList<>();
        for (final Map.Entry<String, StoreCurrencySection> entry : config.getStore().entrySet()) {
            final String currencyType = entry.getKey();
            final StoreCurrencySection section = entry.getValue();
            costs.add(new StorageCurrencyCost(
                currencyType,
                getCurrencyDisplayName(currencyType),
                section.getInitialCost(),
                section.getGrowthRate(),
                calculateCost(section, ownedStorages)
            ));
        }

        return costs;
    }

    static @NotNull PurchaseResult purchaseStorage(
        final @NotNull Context context,
        final @NotNull RDR plugin,
        final @NotNull RDRPlayer playerData,
        final @NotNull ConfigSection config
    ) {
        final List<StorageCurrencyCost> costs = getConfiguredStoreCosts(plugin, config, playerData.getStorages().size());
        if (costs.isEmpty()) {
            return PurchaseResult.failure("feedback.no_currencies", "", "", "", "");
        }

        for (final StorageCurrencyCost cost : costs) {
            if (!isCurrencyOperational(plugin, cost.currencyType())) {
                return PurchaseResult.failure(
                    "feedback.currency_unavailable",
                    cost.currencyType(),
                    cost.currencyName(),
                    formatCurrency(plugin, cost.currencyType(), cost.currentCost()),
                    formatCostSummary(plugin, costs)
                );
            }

            if (!hasFunds(context.getPlayer(), plugin, cost)) {
                return PurchaseResult.failure(
                    "feedback.insufficient_funds",
                    cost.currencyType(),
                    cost.currencyName(),
                    formatCurrency(plugin, cost.currencyType(), cost.currentCost()),
                    formatCostSummary(plugin, costs)
                );
            }
        }

        final List<StorageCurrencyCost> charged = new ArrayList<>();
        for (final StorageCurrencyCost cost : costs) {
            if (withdraw(context.getPlayer(), plugin, cost)) {
                charged.add(cost);
                continue;
            }

            rollback(context.getPlayer(), plugin, charged);
            return PurchaseResult.failure(
                "feedback.purchase_failed",
                cost.currencyType(),
                cost.currencyName(),
                formatCurrency(plugin, cost.currencyType(), cost.currentCost()),
                formatCostSummary(plugin, costs)
            );
        }

        return PurchaseResult.successful(formatCostSummary(plugin, costs));
    }

    static double calculateCost(
        final @NotNull StoreCurrencySection config,
        final int ownedStorages
    ) {
        final double rawCost = config.getInitialCost() * Math.pow(config.getGrowthRate(), Math.max(ownedStorages, 0));
        if (!Double.isFinite(rawCost)) {
            return Math.max(0D, config.getInitialCost());
        }

        return Math.max(0D, rawCost);
    }

    static @NotNull String formatCurrency(
        final @NotNull RDR plugin,
        final @NotNull String currencyType,
        final double amount
    ) {
        if (usesVaultCurrency(currencyType)) {
            return plugin.formatVaultCurrency(amount);
        }

        return formatAmount(amount) + " " + getCurrencyDisplayName(currencyType);
    }

    static @NotNull String getCurrencyDisplayName(final @NotNull String currencyType) {
        if (usesVaultCurrency(currencyType)) {
            return "Vault";
        }

        final JExEconomyBridge bridge = JExEconomyBridge.getBridge();
        return bridge == null ? currencyType : bridge.getCurrencyDisplayName(currencyType);
    }

    static boolean isCurrencyOperational(
        final @NotNull RDR plugin,
        final @NotNull String currencyType
    ) {
        if (usesVaultCurrency(currencyType)) {
            return plugin.hasVaultEconomy();
        }

        return JExEconomyBridge.getBridge() != null;
    }

    static @NotNull String formatCostSummary(
        final @NotNull RDR plugin,
        final @NotNull List<StorageCurrencyCost> costs
    ) {
        final List<String> parts = new ArrayList<>();
        for (final StorageCurrencyCost cost : costs) {
            parts.add(cost.currencyName() + ": " + formatCurrency(plugin, cost.currencyType(), cost.currentCost()));
        }
        return String.join(", ", parts);
    }

    static @NotNull String formatAmount(final double amount) {
        return String.format(Locale.US, "%.2f", amount);
    }

    private static boolean hasFunds(
        final @NotNull OfflinePlayer player,
        final @NotNull RDR plugin,
        final @NotNull StorageCurrencyCost cost
    ) {
        if (cost.currentCost() <= 0D) {
            return true;
        }

        if (usesVaultCurrency(cost.currencyType())) {
            return plugin.hasVaultFunds(player, cost.currentCost());
        }

        final JExEconomyBridge bridge = JExEconomyBridge.getBridge();
        return bridge != null && bridge.has(player, cost.currencyType(), cost.currentCost());
    }

    private static boolean withdraw(
        final @NotNull OfflinePlayer player,
        final @NotNull RDR plugin,
        final @NotNull StorageCurrencyCost cost
    ) {
        if (cost.currentCost() <= 0D) {
            return true;
        }

        if (usesVaultCurrency(cost.currencyType())) {
            return plugin.withdrawVault(player, cost.currentCost());
        }

        final JExEconomyBridge bridge = JExEconomyBridge.getBridge();
        return bridge != null && bridge.withdraw(player, cost.currencyType(), cost.currentCost()).join();
    }

    private static void rollback(
        final @NotNull OfflinePlayer player,
        final @NotNull RDR plugin,
        final @NotNull List<StorageCurrencyCost> charged
    ) {
        final List<StorageCurrencyCost> reversed = new ArrayList<>(charged);
        Collections.reverse(reversed);
        for (final StorageCurrencyCost cost : reversed) {
            if (cost.currentCost() <= 0D) {
                continue;
            }

            if (usesVaultCurrency(cost.currencyType())) {
                plugin.depositVault(player, cost.currentCost());
                continue;
            }

            final JExEconomyBridge bridge = JExEconomyBridge.getBridge();
            if (bridge != null) {
                bridge.deposit(player, cost.currencyType(), cost.currentCost()).join();
            }
        }
    }

    private static boolean usesVaultCurrency(final @NotNull String currencyType) {
        return "vault".equalsIgnoreCase(currencyType);
    }

    record StorageCurrencyCost(
        @NotNull String currencyType,
        @NotNull String currencyName,
        double initialCost,
        double growthRate,
        double currentCost
    ) {
    }

    record PurchaseResult(
        boolean success,
        @NotNull String failureKey,
        @NotNull String currencyType,
        @NotNull String currencyName,
        @NotNull String formattedCost,
        @NotNull String costSummary
    ) {
        static @NotNull PurchaseResult successful(final @NotNull String costSummary) {
            return new PurchaseResult(true, "", "", "", "", costSummary);
        }

        static @NotNull PurchaseResult failure(
            final @NotNull String failureKey,
            final @NotNull String currencyType,
            final @NotNull String currencyName,
            final @NotNull String formattedCost,
            final @NotNull String costSummary
        ) {
            return new PurchaseResult(
                false,
                failureKey,
                currencyType,
                currencyName,
                formattedCost,
                costSummary
            );
        }
    }
}
