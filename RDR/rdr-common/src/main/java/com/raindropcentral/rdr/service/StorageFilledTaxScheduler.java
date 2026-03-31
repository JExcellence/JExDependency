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

package com.raindropcentral.rdr.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

import com.raindropcentral.rdr.RDR;
import com.raindropcentral.rdr.configs.ConfigSection;
import com.raindropcentral.rdr.database.entity.RStorage;
import com.raindropcentral.rdr.database.repository.RRStorage;
import com.raindropcentral.rplatform.economy.JExEconomyBridge;
import de.jexcellence.jextranslate.i18n.I18n;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Schedules recurring storage taxes for non-empty player storages.
 *
 * <p>Each run counts storages that currently contain items and charges configured per-storage
 * tax amounts per currency from the owning player.</p>
 *
 * @author ItsRainingHP
 * @since 5.0.0
 * @version 5.0.0
 */
public class StorageFilledTaxScheduler {

    private static final Logger LOGGER = Logger.getLogger("RDR");
    private static final double EPSILON = 1.0E-6D;

    private final RDR plugin;
    private boolean running;

    /**
     * Creates a recurring non-empty-storage tax scheduler.
     *
     * @param plugin active RDR runtime
     * @throws NullPointerException if {@code plugin} is {@code null}
     */
    public StorageFilledTaxScheduler(final @NotNull RDR plugin) {
        this.plugin = plugin;
    }

    /**
     * Starts recurring tax collection using the configured interval.
     */
    public void start() {
        if (this.running) {
            return;
        }

        final ConfigSection config = this.plugin.getDefaultConfig();
        final long intervalTicks = config.getProtectionFilledStorageTaxIntervalTicks();
        if (intervalTicks < 1L) {
            LOGGER.info("Filled-storage tax scheduler is disabled because protection.filled_storage_taxes.interval_ticks <= 0.");
            return;
        }

        LOGGER.info(
                "Scheduling filled-storage taxes every "
                        + intervalTicks
                        + " ticks using currencies "
                        + config.getProtectionFilledStorageTaxes().keySet()
        );
        this.plugin.getScheduler().runRepeating(this::collectTaxes, intervalTicks, intervalTicks);
        this.running = true;
    }

    /**
     * Marks this scheduler as stopped.
     *
     * <p>Bukkit cancels plugin tasks on disable, so this method primarily guards against
     * any late invocations during shutdown.</p>
     */
    public void shutdown() {
        this.running = false;
    }

    /**
     * Runs one immediate filled-storage tax collection cycle.
     */
    public void collectTaxesNow() {
        this.collectTaxes(true);
    }

    private void collectTaxes() {
        this.collectTaxes(false);
    }

    private void collectTaxes(final boolean forced) {
        if (!forced && !this.running) {
            return;
        }

        final RRStorage storageRepository = this.plugin.getStorageRepository();
        if (storageRepository == null) {
            return;
        }

        final ConfigSection config = this.plugin.getDefaultConfig();
        final List<TaxCharge> configuredCharges = this.resolveTaxCharges(config.getProtectionFilledStorageTaxes());
        final Map<String, Double> maximumDebtByCurrency = config.getProtectionFilledStorageMaximumDebtByCurrency();
        if (configuredCharges.isEmpty()) {
            return;
        }

        final List<RStorage> allStorages = storageRepository.findAllWithPlayer();
        if (allStorages.isEmpty()) {
            return;
        }

        final Map<UUID, List<RStorage>> storagesByOwner = this.groupStoragesByOwner(allStorages);
        if (storagesByOwner.isEmpty()) {
            return;
        }

        final JExEconomyBridge economyBridge = JExEconomyBridge.getBridge();
        for (final Map.Entry<UUID, List<RStorage>> ownerEntry : storagesByOwner.entrySet()) {
            final OfflinePlayer owner = Bukkit.getOfflinePlayer(ownerEntry.getKey());
            final List<RStorage> ownerStorages = ownerEntry.getValue();
            final List<RStorage> nonEmptyStorages = this.filterNonEmptyStorages(ownerStorages);
            final int taxedStorageCount = nonEmptyStorages.size();
            if (taxedStorageCount < 1 && !hasAnyDebt(ownerStorages)) {
                continue;
            }

            final List<TaxCharge> chargedTaxes = new ArrayList<>();
            final Set<RStorage> changedStorages = new HashSet<>();
            final Map<String, Double> existingDebtByCurrency = this.summarizeDebtByCurrency(ownerStorages);
            for (final TaxCharge configuredCharge : configuredCharges) {
                final double baseTaxAmount = configuredCharge.amount() * taxedStorageCount;
                final double debtAmount = existingDebtByCurrency.getOrDefault(configuredCharge.currencyId(), 0.0D);
                final double totalAmount = baseTaxAmount + debtAmount;
                if (totalAmount <= EPSILON) {
                    continue;
                }

                final TaxCharge effectiveCharge = new TaxCharge(configuredCharge.currencyId(), totalAmount);
                final ChargeStatus status = this.collectTaxForOwner(owner, economyBridge, effectiveCharge);
                if (status == ChargeStatus.SUCCESS) {
                    this.clearDebtForCurrency(ownerStorages, configuredCharge.currencyId(), changedStorages);
                    this.recordCollectedTownTax(owner, effectiveCharge);
                    chargedTaxes.add(effectiveCharge);
                    continue;
                }

                this.recordFailedTaxDebt(
                    ownerStorages,
                    nonEmptyStorages,
                    configuredCharge.currencyId(),
                    baseTaxAmount,
                    config.getProtectionFilledStorageMaximumFreeze(),
                    maximumDebtByCurrency,
                    changedStorages
                );
                this.notifyFailedCharge(owner, taxedStorageCount, effectiveCharge, status, economyBridge);
            }

            this.persistStorageUpdates(storageRepository, changedStorages);
            this.notifySuccessfulCharge(owner, taxedStorageCount, chargedTaxes, economyBridge);
        }
    }

    private @NotNull Map<UUID, List<RStorage>> groupStoragesByOwner(final @NotNull List<RStorage> storages) {
        final Map<UUID, List<RStorage>> grouped = new LinkedHashMap<>();
        for (final RStorage storage : storages) {
            if (storage == null || storage.getPlayer() == null) {
                continue;
            }

            grouped.computeIfAbsent(storage.getPlayer().getIdentifier(), key -> new ArrayList<>()).add(storage);
        }
        return grouped;
    }

    private @NotNull List<RStorage> filterNonEmptyStorages(final @NotNull List<RStorage> storages) {
        final List<RStorage> nonEmpty = new ArrayList<>();
        for (final RStorage storage : storages) {
            if (storage != null && !storage.isEmpty()) {
                nonEmpty.add(storage);
            }
        }
        return nonEmpty;
    }

    private static boolean hasAnyDebt(final @NotNull List<RStorage> storages) {
        for (final RStorage storage : storages) {
            if (storage != null && storage.hasTaxDebt()) {
                return true;
            }
        }
        return false;
    }

    private @NotNull Map<String, Double> summarizeDebtByCurrency(final @NotNull List<RStorage> storages) {
        final Map<String, Double> summary = new LinkedHashMap<>();
        for (final RStorage storage : storages) {
            if (storage == null || !storage.hasTaxDebt()) {
                continue;
            }

            for (final Map.Entry<String, Double> debtEntry : storage.getTaxDebtEntries().entrySet()) {
                if (debtEntry.getKey() == null || debtEntry.getKey().isBlank() || debtEntry.getValue() == null) {
                    continue;
                }

                summary.merge(debtEntry.getKey(), Math.max(0.0D, debtEntry.getValue()), Double::sum);
            }
        }
        return summary;
    }

    private void clearDebtForCurrency(
        final @NotNull List<RStorage> storages,
        final @NotNull String currencyId,
        final @NotNull Set<RStorage> changedStorages
    ) {
        for (final RStorage storage : storages) {
            if (storage == null || !storage.hasTaxDebt()) {
                continue;
            }
            if (!storage.getTaxDebtEntries().containsKey(currencyId)) {
                continue;
            }

            final Map<String, Double> debt = storage.getTaxDebtEntries();
            debt.remove(currencyId);
            storage.setTaxDebtEntries(debt);
            changedStorages.add(storage);
        }
    }

    private void recordFailedTaxDebt(
        final @NotNull List<RStorage> ownerStorages,
        final @NotNull List<RStorage> nonEmptyStorages,
        final @NotNull String currencyId,
        final double baseTaxAmount,
        final int maximumFreeze,
        final @NotNull Map<String, Double> maximumDebtByCurrency,
        final @NotNull Set<RStorage> changedStorages
    ) {
        if (baseTaxAmount <= EPSILON) {
            return;
        }

        final List<RStorage> targetStorages = this.resolveFreezeTargets(ownerStorages, nonEmptyStorages, maximumFreeze);
        if (targetStorages.isEmpty()) {
            return;
        }

        final double maximumDebt = resolveMaximumDebtCap(currencyId, maximumDebtByCurrency);
        double remaining = baseTaxAmount;
        for (int index = 0; index < targetStorages.size(); index++) {
            if (remaining <= EPSILON) {
                break;
            }

            final RStorage storage = targetStorages.get(index);
            final int remainingTargets = targetStorages.size() - index;
            final double requestedShare = remainingTargets <= 1 ? remaining : remaining / remainingTargets;
            if (requestedShare <= EPSILON) {
                continue;
            }

            final double currentDebt = storage.getTaxDebtEntries().getOrDefault(currencyId, 0.0D);
            final double availableCapacity = resolveAvailableDebtCapacity(currentDebt, maximumDebt);
            if (availableCapacity <= EPSILON) {
                continue;
            }

            final double debtShare = Math.min(Math.min(requestedShare, availableCapacity), remaining);
            if (debtShare <= EPSILON) {
                continue;
            }

            storage.addTaxDebt(currencyId, debtShare);
            changedStorages.add(storage);
            remaining -= debtShare;
        }
    }

    private @NotNull List<RStorage> resolveFreezeTargets(
        final @NotNull List<RStorage> ownerStorages,
        final @NotNull List<RStorage> nonEmptyStorages,
        final int maximumFreeze
    ) {
        final List<RStorage> existingFrozen = new ArrayList<>();
        for (final RStorage storage : ownerStorages) {
            if (storage != null && storage.hasTaxDebt()) {
                existingFrozen.add(storage);
            }
        }

        if (maximumFreeze == 0) {
            return existingFrozen;
        }

        if (maximumFreeze < 0) {
            final List<RStorage> targets = new ArrayList<>(existingFrozen);
            for (final RStorage storage : nonEmptyStorages) {
                if (!targets.contains(storage)) {
                    targets.add(storage);
                }
            }
            return targets;
        }

        final List<RStorage> targets = new ArrayList<>(existingFrozen);
        int availableSlots = maximumFreeze - existingFrozen.size();
        if (availableSlots <= 0) {
            return targets;
        }

        for (final RStorage storage : nonEmptyStorages) {
            if (availableSlots <= 0) {
                break;
            }
            if (targets.contains(storage)) {
                continue;
            }

            targets.add(storage);
            availableSlots--;
        }
        return targets;
    }

    private void persistStorageUpdates(
        final @NotNull RRStorage storageRepository,
        final @NotNull Set<RStorage> changedStorages
    ) {
        if (changedStorages.isEmpty()) {
            return;
        }

        for (final RStorage storage : changedStorages) {
            storageRepository.update(storage);
        }
    }

    private @NotNull ChargeStatus collectTaxForOwner(
        final @NotNull OfflinePlayer owner,
        final @Nullable JExEconomyBridge economyBridge,
        final @NotNull TaxCharge taxCharge
    ) {
        if (isVaultCurrency(taxCharge.currencyId())) {
            if (!this.plugin.hasVaultEconomy()) {
                return ChargeStatus.CURRENCY_UNAVAILABLE;
            }
            if (!this.plugin.hasVaultFunds(owner, taxCharge.amount())) {
                return ChargeStatus.INSUFFICIENT_FUNDS;
            }
            return this.plugin.withdrawVault(owner, taxCharge.amount())
                ? ChargeStatus.SUCCESS
                : ChargeStatus.CHARGE_FAILED;
        }

        if (economyBridge == null || !economyBridge.hasCurrency(taxCharge.currencyId())) {
            return ChargeStatus.CURRENCY_UNAVAILABLE;
        }
        if (!economyBridge.has(owner, taxCharge.currencyId(), taxCharge.amount())) {
            return ChargeStatus.INSUFFICIENT_FUNDS;
        }
        return safeJoin(economyBridge.withdraw(owner, taxCharge.currencyId(), taxCharge.amount()))
            ? ChargeStatus.SUCCESS
            : ChargeStatus.CHARGE_FAILED;
    }

    private void notifySuccessfulCharge(
        final @NotNull OfflinePlayer owner,
        final int taxedStorageCount,
        final @NotNull List<TaxCharge> chargedTaxes,
        final @Nullable JExEconomyBridge economyBridge
    ) {
        if (chargedTaxes.isEmpty()) {
            return;
        }

        final Player onlineOwner = this.resolveOnlineOwner(owner);
        if (onlineOwner == null) {
            return;
        }

        new I18n.Builder("storage.message.filled_tax_charged_header", onlineOwner)
            .withPlaceholder("taxed_storages", taxedStorageCount)
            .build()
            .sendMessage();

        for (final TaxCharge chargedTax : chargedTaxes) {
            new I18n.Builder("storage.message.filled_tax_charged_line", onlineOwner)
                .withPlaceholder("currency", this.getCurrencyDisplayName(chargedTax.currencyId(), economyBridge))
                .withPlaceholder("amount", this.formatCurrency(chargedTax.currencyId(), chargedTax.amount(), economyBridge))
                .build()
                .sendMessage();
        }
    }

    private void notifyFailedCharge(
        final @NotNull OfflinePlayer owner,
        final int taxedStorageCount,
        final @NotNull TaxCharge failedCharge,
        final @NotNull ChargeStatus status,
        final @Nullable JExEconomyBridge economyBridge
    ) {
        final Player onlineOwner = this.resolveOnlineOwner(owner);
        if (onlineOwner == null) {
            return;
        }

        final String messageKey = switch (status) {
            case CURRENCY_UNAVAILABLE -> "storage.message.filled_tax_currency_unavailable";
            case INSUFFICIENT_FUNDS -> "storage.message.filled_tax_insufficient";
            case CHARGE_FAILED -> "storage.message.filled_tax_charge_failed";
            case SUCCESS -> "";
        };
        if (messageKey.isEmpty()) {
            return;
        }

        new I18n.Builder(messageKey, onlineOwner)
            .withPlaceholder("taxed_storages", taxedStorageCount)
            .withPlaceholder("currency", this.getCurrencyDisplayName(failedCharge.currencyId(), economyBridge))
            .withPlaceholder("amount", this.formatCurrency(failedCharge.currencyId(), failedCharge.amount(), economyBridge))
            .build()
            .sendMessage();
    }

    private @NotNull List<TaxCharge> resolveTaxCharges(final @NotNull Map<String, Double> configuredTaxes) {
        final List<TaxCharge> charges = new ArrayList<>();
        for (final Map.Entry<String, Double> entry : configuredTaxes.entrySet()) {
            if (entry.getKey() == null || entry.getKey().isBlank()) {
                continue;
            }

            final double amount = entry.getValue() == null ? 0.0D : Math.max(0.0D, entry.getValue());
            if (amount <= EPSILON) {
                continue;
            }

            charges.add(new TaxCharge(entry.getKey().trim().toLowerCase(Locale.ROOT), amount));
        }
        return charges;
    }

    private @Nullable Player resolveOnlineOwner(final @NotNull OfflinePlayer owner) {
        final Player onlineOwner = owner.getPlayer();
        if (onlineOwner == null || !onlineOwner.isOnline()) {
            return null;
        }
        return onlineOwner;
    }

    private @NotNull String formatCurrency(
        final @NotNull String currencyId,
        final double amount,
        final @Nullable JExEconomyBridge economyBridge
    ) {
        if (isVaultCurrency(currencyId)) {
            return this.plugin.formatVaultCurrency(amount);
        }
        return String.format(Locale.US, "%.2f %s", amount, this.getCurrencyDisplayName(currencyId, economyBridge));
    }

    private @NotNull String getCurrencyDisplayName(
        final @NotNull String currencyId,
        final @Nullable JExEconomyBridge economyBridge
    ) {
        if (isVaultCurrency(currencyId)) {
            return "Vault";
        }
        return economyBridge == null ? currencyId : economyBridge.getCurrencyDisplayName(currencyId);
    }

    private static boolean isVaultCurrency(final @NotNull String currencyId) {
        return "vault".equalsIgnoreCase(currencyId);
    }

    private static double resolveMaximumDebtCap(
        final @NotNull String currencyId,
        final @NotNull Map<String, Double> maximumDebtByCurrency
    ) {
        final Double configuredCap = maximumDebtByCurrency.get(currencyId.toLowerCase(Locale.ROOT));
        if (configuredCap == null || configuredCap <= EPSILON) {
            return -1.0D;
        }
        return configuredCap;
    }

    private static double resolveAvailableDebtCapacity(
        final double currentDebt,
        final double maximumDebt
    ) {
        if (maximumDebt <= EPSILON) {
            return Double.POSITIVE_INFINITY;
        }
        return Math.max(0.0D, maximumDebt - Math.max(0.0D, currentDebt));
    }

    private static boolean safeJoin(final @NotNull java.util.concurrent.CompletableFuture<Boolean> future) {
        try {
            return Boolean.TRUE.equals(future.join());
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private void recordCollectedTownTax(
        final @NotNull OfflinePlayer owner,
        final @NotNull TaxCharge chargedTax
    ) {
        final Player onlineOwner = owner.getPlayer();
        if (onlineOwner == null || !onlineOwner.isOnline()) {
            return;
        }
        StorageTownTaxBankService.recordCollectedTax(
            this.plugin,
            onlineOwner,
            chargedTax.currencyId(),
            chargedTax.amount()
        );
    }

    private record TaxCharge(@NotNull String currencyId, double amount) {
    }

    private enum ChargeStatus {
        SUCCESS,
        CURRENCY_UNAVAILABLE,
        INSUFFICIENT_FUNDS,
        CHARGE_FAILED
    }
}
