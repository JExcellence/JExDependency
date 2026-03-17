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

package com.raindropcentral.rds.service.tax;

import com.raindropcentral.rds.RDS;
import com.raindropcentral.rds.configs.ConfigSection;
import com.raindropcentral.rds.configs.TaxCurrencySection;
import com.raindropcentral.rds.configs.TaxSection;
import com.raindropcentral.rds.database.entity.Shop;
import com.raindropcentral.rds.database.entity.ShopLedgerEntry;
import de.jexcellence.jextranslate.i18n.I18n;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Schedules shop tax operations.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public class ShopTaxScheduler {

    private static final Logger LOGGER = Logger.getLogger("RDS");
    private static final DateTimeFormatter START_TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private static final String PAYMENT_SOURCE_PLAYER = "Player Balance";
    private static final String PAYMENT_SOURCE_PLAYER_FALLBACK = "Player Balance (Fallback)";
    private static final String PAYMENT_SOURCE_TOWN_BANK = "Town Bank";

    private final RDS plugin;
    private boolean running = false;

    /**
     * Creates a new shop tax scheduler.
     *
     * @param plugin plugin instance
     */
    public ShopTaxScheduler(
            final @NotNull RDS plugin
    ) {
        this.plugin = plugin;
    }

    /**
     * Starts shop tax scheduler processing.
     */
    public void start() {
        if (this.running) {
            return;
        }

        final ConfigSection config = this.plugin.getDefaultConfig();
        final TaxSection taxes = config.getTaxes();
        final long initialDelayTicks = ShopTaxSupport.calculateInitialDelayTicks(taxes, Instant.now());
        final long periodTicks = taxes.getDurationTicks();
        final Map<String, TaxCurrencySection> configuredTaxCurrencies = this.resolveConfiguredTaxes(config, taxes);
        final List<String> configuredCurrencies = new ArrayList<>();
        for (final String currencyType : configuredTaxCurrencies.keySet()) {
            configuredCurrencies.add(currencyType);
            if (ShopTaxSupport.isCurrencyAvailable(this.plugin, currencyType)) {
                continue;
            }

            LOGGER.warning(
                    "Shop tax scheduler started with unavailable currency "
                            + currencyType
                            + ". Charges in that currency will be skipped until it becomes available."
            );
        }

        LOGGER.info(
                "Scheduling shop taxes every "
                        + periodTicks
                        + " ticks (~"
                        + String.format(Locale.US, "%.2f", periodTicks / 72000.0D)
                        + " hours) starting from "
                        + taxes.getStartTime().format(START_TIME_FORMAT)
                        + " "
                        + taxes.getTimeZoneId().getId()
                        + " using currencies "
                        + configuredCurrencies
        );

        this.plugin.getScheduler().runRepeating(this::collectTaxes, initialDelayTicks, periodTicks);
        this.running = true;
    }

    /**
     * Collects taxes immediately without modifying the configured repeating timer schedule.
     *
     * <p>This method is intended for admin-triggered verification runs and does not alter
     * the scheduler cadence configured in {@code taxes.duration}.</p>
     */
    public void collectTaxesNow() {
        this.collectTaxes();
    }

    /**
     * Pays all outstanding tax debt currently tracked on a shop.
     *
     * <p>Debt collection is attempted per currency from {@code payer}. Any currency that cannot be
     * charged remains as debt and keeps the shop in bankrupt mode.</p>
     *
     * @param shop target shop with tracked debt
     * @param payer payer charged for the outstanding debt
     * @return payment result including paid and remaining debt amounts
     */
    public @NotNull TaxDebtPaymentResult payOutstandingDebt(
            final @NotNull Shop shop,
            final @NotNull OfflinePlayer payer
    ) {
        final Map<String, Double> outstandingDebt = shop.getTaxDebtEntries();
        if (outstandingDebt.isEmpty()) {
            return new TaxDebtPaymentResult(Map.of(), Map.of());
        }

        final Map<String, Double> paidDebts = new LinkedHashMap<>();
        for (final Map.Entry<String, Double> debtEntry : outstandingDebt.entrySet()) {
            final String currencyType = debtEntry.getKey();
            final double debtAmount = debtEntry.getValue() == null ? 0D : Math.max(0D, debtEntry.getValue());
            if (currencyType == null || currencyType.isBlank() || debtAmount <= 1.0E-6D) {
                continue;
            }

            if (!ShopTaxSupport.isCurrencyAvailable(this.plugin, currencyType)) {
                continue;
            }
            if (!ShopTaxSupport.hasFunds(this.plugin, payer, currencyType, debtAmount)) {
                continue;
            }
            if (!ShopTaxSupport.withdraw(this.plugin, payer, currencyType, debtAmount)) {
                continue;
            }

            paidDebts.put(currencyType, debtAmount);
            shop.reduceTaxDebt(currencyType, debtAmount);
            shop.addLedgerEntry(
                    ShopLedgerEntry.taxation(
                            shop,
                            payer.getUniqueId(),
                            this.resolveActorName(payer),
                            currencyType,
                            debtAmount,
                            1
                    )
            );
        }

        if (!paidDebts.isEmpty()) {
            this.plugin.getShopRepository().update(shop);
        }

        return new TaxDebtPaymentResult(
                paidDebts,
                shop.getTaxDebtEntries()
        );
    }

    /**
     * Formats a currency map into a compact amount summary string.
     *
     * @param amountsByCurrency amounts keyed by currency type
     * @return formatted summary (for example: {@code 100.00 Vault, 5.00 Raindrops})
     */
    public @NotNull String formatCurrencySummary(
            final @NotNull Map<String, Double> amountsByCurrency
    ) {
        final List<String> parts = new ArrayList<>();
        for (final Map.Entry<String, Double> entry : amountsByCurrency.entrySet()) {
            if (entry.getKey() == null || entry.getKey().isBlank() || entry.getValue() == null) {
                continue;
            }

            final double amount = Math.max(0D, entry.getValue());
            if (amount <= 1.0E-6D) {
                continue;
            }

            parts.add(ShopTaxSupport.formatCurrency(this.plugin, entry.getKey(), amount));
        }

        return parts.isEmpty() ? "None" : String.join(", ", parts);
    }

    private void collectTaxes() {
        final ConfigSection config = this.plugin.getDefaultConfig();
        final TaxSection taxes = config.getTaxes();
        final Map<UUID, List<Shop>> ownedShopsByOwner = this.groupTaxableShopsByOwner();
        if (ownedShopsByOwner.isEmpty()) {
            return;
        }

        for (final Map.Entry<UUID, List<Shop>> entry : ownedShopsByOwner.entrySet()) {
            final List<Shop> ownedShops = entry.getValue();
            if (ownedShops.isEmpty()) {
                continue;
            }

            final OfflinePlayer owner = Bukkit.getOfflinePlayer(entry.getKey());
            this.collectTaxesForOwner(config, taxes, owner, ownedShops);
        }
    }

    private void collectTaxesForOwner(
            final @NotNull ConfigSection config,
            final @NotNull TaxSection taxes,
            final @NotNull OfflinePlayer owner,
            final @NotNull List<Shop> ownedShops
    ) {
        final List<Shop> taxableOwnedShops = new ArrayList<>();
        for (final Shop shop : ownedShops) {
            if (ShopTaxSupport.isTaxableShop(shop)) {
                taxableOwnedShops.add(shop);
            }
        }
        if (taxableOwnedShops.isEmpty()) {
            return;
        }

        final int ownedShopCount = taxableOwnedShops.size();
        final int neverAvailabilityItems = ShopTaxSupport.countNeverAvailabilityItems(taxableOwnedShops);
            final Map<String, TaxCurrencySection> configuredTaxes = this.resolveConfiguredTaxes(config, taxes);
        for (final Map.Entry<String, TaxCurrencySection> taxEntry : configuredTaxes.entrySet()) {
            final String currencyType = taxEntry.getKey();
            if (!ShopTaxSupport.isCurrencyAvailable(this.plugin, currencyType)) {
                continue;
            }

            final boolean protectionTaxCurrency =
                    ShopTaxSupport.usesProtectionTax(config.getProtection(), currencyType);
            final double baseTax = ShopTaxSupport.calculateTax(taxEntry.getValue(), ownedShopCount);
            final double taxAmount = ShopTaxSupport.applyNeverItemPenalty(
                    baseTax,
                    taxes.getNeverItemPenaltyRate(),
                    neverAvailabilityItems
            );
            final double cappedTaxAmount = this.applyProtectionTaxMaximum(
                    config,
                    currencyType,
                    taxAmount,
                    protectionTaxCurrency
            );
            if (cappedTaxAmount <= 0D) {
                continue;
            }

            final TaxChargeResult chargeResult = this.chargeTax(
                    config,
                    owner,
                    currencyType,
                    cappedTaxAmount,
                    protectionTaxCurrency
            );
            final String formattedTax = ShopTaxSupport.formatCurrency(this.plugin, currencyType, cappedTaxAmount);
            final String currencyName = ShopTaxSupport.getCurrencyDisplayName(currencyType);
            final Map<String, Object> placeholders = new LinkedHashMap<>();
            placeholders.put("amount", formattedTax);
            placeholders.put("owned_shops", ownedShopCount);
            placeholders.put("never_item_count", neverAvailabilityItems);
            placeholders.put("currency_type", currencyType);
            placeholders.put("currency_name", currencyName);
            placeholders.put("payment_source", chargeResult.paymentSource());

            if (!chargeResult.successful()) {
                this.applyFailedTaxDebt(
                        taxableOwnedShops,
                        currencyType,
                        cappedTaxAmount,
                        taxes.getMaximumBankruptcyAmount(currencyType)
                );
                placeholders.put("debt_summary", this.formatCurrencySummary(this.summarizeDebtByCurrency(taxableOwnedShops)));
                this.notifyOwner(owner, chargeResult.failureMessageKey(), placeholders);
                this.notifyOwner(owner, "tax_scheduler.debt_recorded", placeholders);
                continue;
            }

            this.recordTaxLedgerEntries(taxableOwnedShops, owner, currencyType, cappedTaxAmount, ownedShopCount);
            if (protectionTaxCurrency) {
                ShopTownTaxBankService.recordCollectedTax(this.plugin, owner, currencyType, cappedTaxAmount);
            }
            this.notifyOwner(owner, "tax_scheduler.paid", placeholders);
        }
    }

    private @NotNull TaxChargeResult chargeTax(
            final @NotNull ConfigSection config,
            final @NotNull OfflinePlayer owner,
            final @NotNull String currencyType,
            final double amount,
            final boolean protectionTaxCurrency
    ) {
        if (!protectionTaxCurrency) {
            return this.chargeFromPlayerBalance(owner, currencyType, amount, PAYMENT_SOURCE_PLAYER);
        }

        final TaxChargeResult townBankChargeResult = this.chargeFromTownBank(owner, amount);
        if (townBankChargeResult.successful()) {
            return townBankChargeResult;
        }

        if (!config.getProtection().isShopTaxesFallbackToPlayer()) {
            return townBankChargeResult;
        }

        return this.chargeFromPlayerBalance(
                owner,
                currencyType,
                amount,
                PAYMENT_SOURCE_PLAYER_FALLBACK
        );
    }

    private @NotNull TaxChargeResult chargeFromTownBank(
            final @NotNull OfflinePlayer owner,
            final double amount
    ) {
        if (!ShopTaxSupport.canWithdrawFromTownBank(owner)) {
            return new TaxChargeResult(false, PAYMENT_SOURCE_TOWN_BANK, "tax_scheduler.charge_failed");
        }

        if (!ShopTaxSupport.withdrawFromTownBank(owner, amount)) {
            return new TaxChargeResult(false, PAYMENT_SOURCE_TOWN_BANK, "tax_scheduler.charge_failed");
        }

        return new TaxChargeResult(true, PAYMENT_SOURCE_TOWN_BANK, null);
    }

    private @NotNull TaxChargeResult chargeFromPlayerBalance(
            final @NotNull OfflinePlayer owner,
            final @NotNull String currencyType,
            final double amount,
            final @NotNull String paymentSource
    ) {
        if (!ShopTaxSupport.hasFunds(this.plugin, owner, currencyType, amount)) {
            return new TaxChargeResult(false, paymentSource, "tax_scheduler.insufficient_funds");
        }

        if (!ShopTaxSupport.withdraw(this.plugin, owner, currencyType, amount)) {
            return new TaxChargeResult(false, paymentSource, "tax_scheduler.charge_failed");
        }

        return new TaxChargeResult(true, paymentSource, null);
    }

    private void applyFailedTaxDebt(
            final @NotNull List<Shop> taxableOwnedShops,
            final @NotNull String currencyType,
            final double totalDebtAmount,
            final double maximumBankruptcyAmount
    ) {
        if (taxableOwnedShops.isEmpty() || totalDebtAmount <= 0D) {
            return;
        }

        final double baseShare = totalDebtAmount / taxableOwnedShops.size();
        for (int index = 0; index < taxableOwnedShops.size(); index++) {
            final Shop shop = taxableOwnedShops.get(index);
            final double debtShare = index == taxableOwnedShops.size() - 1
                    ? Math.max(0D, totalDebtAmount - (baseShare * index))
                    : baseShare;
            if (debtShare <= 0D) {
                continue;
            }

            shop.addTaxDebt(currencyType, debtShare, maximumBankruptcyAmount);
            shop.forceItemsNeverAvailability();
            this.plugin.getShopRepository().update(shop);
        }
    }

    private @NotNull Map<String, Double> summarizeDebtByCurrency(
            final @NotNull List<Shop> shops
    ) {
        final Map<String, Double> summary = new LinkedHashMap<>();
        for (final Shop shop : shops) {
            for (final Map.Entry<String, Double> debtEntry : shop.getTaxDebtEntries().entrySet()) {
                if (debtEntry.getKey() == null || debtEntry.getValue() == null) {
                    continue;
                }

                final double amount = Math.max(0D, debtEntry.getValue());
                if (amount <= 1.0E-6D) {
                    continue;
                }

                summary.merge(debtEntry.getKey(), amount, Double::sum);
            }
        }
        return summary;
    }

    private void recordTaxLedgerEntries(
            final @NotNull List<Shop> ownedShops,
            final @NotNull OfflinePlayer owner,
            final @NotNull String currencyType,
            final double totalAmount,
            final int countedShops
    ) {
        if (ownedShops.isEmpty() || totalAmount <= 0D) {
            return;
        }

        final String ownerName = this.resolveActorName(owner);
        final double baseShare = totalAmount / ownedShops.size();

        for (int index = 0; index < ownedShops.size(); index++) {
            final Shop shop = ownedShops.get(index);
            final double shareAmount = index == ownedShops.size() - 1
                    ? Math.max(0D, totalAmount - (baseShare * index))
                    : baseShare;

            shop.addLedgerEntry(
                    ShopLedgerEntry.taxation(
                            shop,
                            owner.getUniqueId(),
                            ownerName,
                            currencyType,
                            shareAmount,
                            countedShops
                    )
            );
            this.plugin.getShopRepository().update(shop);
        }
    }

    private @NotNull String resolveActorName(
            final @NotNull OfflinePlayer actor
    ) {
        return actor.getName() == null || actor.getName().isBlank()
                ? actor.getUniqueId().toString()
                : actor.getName();
    }

    private @NotNull Map<UUID, List<Shop>> groupTaxableShopsByOwner() {
        final Map<UUID, List<Shop>> groupedShops = new HashMap<>();
        for (final Shop shop : this.plugin.getShopRepository().findAllShops()) {
            if (!ShopTaxSupport.isTaxableShop(shop)) {
                continue;
            }

            groupedShops.computeIfAbsent(shop.getOwner(), ignored -> new ArrayList<>()).add(shop);
        }

        return groupedShops;
    }

    private @NotNull Map<String, TaxCurrencySection> resolveConfiguredTaxes(
            final @NotNull ConfigSection config,
            final @NotNull TaxSection taxes
    ) {
        final Map<String, TaxCurrencySection> resolvedTaxes = new LinkedHashMap<>();
        final Set<String> orderedCurrencyTypes = new LinkedHashSet<>();
        orderedCurrencyTypes.addAll(taxes.getCurrencies().keySet());
        orderedCurrencyTypes.addAll(config.getProtection().getShopTaxes().keySet());

        for (final String rawCurrencyType : orderedCurrencyTypes) {
            if (rawCurrencyType == null || rawCurrencyType.isBlank()) {
                continue;
            }

            final String currencyType = rawCurrencyType.trim().toLowerCase(Locale.ROOT);
            final TaxCurrencySection standardCurrency = taxes.getTaxCurrency(currencyType);
            if (standardCurrency != null) {
                resolvedTaxes.put(currencyType, standardCurrency);
            }
        }

        return resolvedTaxes;
    }

    private void notifyOwner(
            final @NotNull OfflinePlayer owner,
            final @NotNull String key,
            final @NotNull Map<String, Object> placeholders
    ) {
        final Player onlinePlayer = owner.getPlayer();
        if (onlinePlayer == null) {
            return;
        }

        new I18n.Builder(key, onlinePlayer)
                .withPlaceholders(placeholders)
                .includePrefix()
                .build()
                .sendMessage();
    }

    /**
     * Represents the result of paying outstanding shop tax debt.
     *
     * @param paidDebts amounts that were successfully charged by currency
     * @param remainingDebts debt amounts still owed after payment attempt
     */
    public record TaxDebtPaymentResult(
            @NotNull Map<String, Double> paidDebts,
            @NotNull Map<String, Double> remainingDebts
    ) {

        /**
         * Creates a normalized tax-debt payment result.
         *
         * @param paidDebts amounts that were successfully charged by currency
         * @param remainingDebts debt amounts still owed after payment attempt
         */
        public TaxDebtPaymentResult {
            paidDebts = normalizeDebtMap(paidDebts);
            remainingDebts = normalizeDebtMap(remainingDebts);
        }

        /**
         * Indicates whether any debt amount was successfully paid.
         *
         * @return {@code true} when at least one currency payment succeeded
         */
        public boolean hasPaidAny() {
            return !this.paidDebts.isEmpty();
        }

        /**
         * Indicates whether all outstanding debt has been cleared.
         *
         * @return {@code true} when no debt remains
         */
        public boolean isFullyPaid() {
            return this.remainingDebts.isEmpty();
        }
    }

    private static @NotNull Map<String, Double> normalizeDebtMap(
            final @Nullable Map<String, Double> source
    ) {
        if (source == null || source.isEmpty()) {
            return Map.of();
        }

        final Map<String, Double> normalized = new LinkedHashMap<>();
        for (final Map.Entry<String, Double> entry : source.entrySet()) {
            if (entry.getKey() == null || entry.getKey().isBlank() || entry.getValue() == null) {
                continue;
            }

            final double amount = Math.max(0D, entry.getValue());
            if (amount <= 1.0E-6D) {
                continue;
            }

            normalized.put(entry.getKey().trim().toLowerCase(Locale.ROOT), amount);
        }

        return normalized.isEmpty() ? Map.of() : Map.copyOf(normalized);
    }

    private record TaxChargeResult(
            boolean successful,
            @NotNull String paymentSource,
            @Nullable String failureMessageKey
    ) {

        private TaxChargeResult {
            if (paymentSource == null || paymentSource.isBlank()) {
                paymentSource = PAYMENT_SOURCE_PLAYER;
            }
            if (successful) {
                failureMessageKey = null;
            } else if (failureMessageKey == null || failureMessageKey.isBlank()) {
                failureMessageKey = "tax_scheduler.charge_failed";
            }
        }
    }

    private double applyProtectionTaxMaximum(
            final @NotNull ConfigSection config,
            final @NotNull String currencyType,
            final double calculatedTax,
            final boolean protectionTaxCurrency
    ) {
        if (!protectionTaxCurrency) {
            return calculatedTax;
        }

        final Double configuredMaximum = config.getProtection().getShopTaxMaximum(currencyType);
        if (configuredMaximum == null || configuredMaximum <= 0D) {
            return calculatedTax;
        }

        return Math.min(calculatedTax, configuredMaximum);
    }
}
