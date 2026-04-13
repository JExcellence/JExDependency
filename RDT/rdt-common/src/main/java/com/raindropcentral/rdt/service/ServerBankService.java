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

package com.raindropcentral.rdt.service;

import com.raindropcentral.rdt.RDT;
import com.raindropcentral.rdt.database.entity.RServerBank;
import com.raindropcentral.rplatform.economy.JExEconomyBridge;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Centralizes access to the singleton admin-only server bank.
 *
 * <p>The server bank stores both collected tax currencies and collected item taxes. Currency
 * balances can be manipulated through admin commands, while the shared item storage is exposed
 * through the admin GUI.</p>
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public final class ServerBankService {

    private static final int DEFAULT_SHARED_STORAGE_SIZE = 54;

    private final RDT plugin;

    /**
     * Creates the server-bank service.
     *
     * @param plugin active RDT runtime
     */
    public ServerBankService(final @NotNull RDT plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    /**
     * Returns the singleton server-bank aggregate, creating it when necessary.
     *
     * @return singleton server-bank aggregate
     */
    public @NotNull RServerBank getOrCreateBank() {
        if (this.plugin.getServerBankRepository() == null) {
            throw new IllegalStateException("Server bank repository is unavailable.");
        }
        final RServerBank existing = this.plugin.getServerBankRepository().findDefaultBank();
        if (existing != null) {
            return existing;
        }

        final RServerBank created = new RServerBank(RServerBank.DEFAULT_BANK_KEY);
        this.plugin.getServerBankRepository().create(created);
        return created;
    }

    /**
     * Returns the configured server-bank currencies.
     *
     * @return configured currency identifiers
     */
    public @NotNull List<String> getConfiguredCurrencies() {
        return this.plugin.getTaxConfig().getCurrency().currencyIds();
    }

    /**
     * Returns the shared server-bank storage size in slots.
     *
     * @return shared server-bank storage size
     */
    public int getSharedStorageSize() {
        return DEFAULT_SHARED_STORAGE_SIZE;
    }

    /**
     * Acquires the singleton server-bank viewer lock.
     *
     * @param ownerUuid viewer UUID
     * @return {@code true} when the lock was acquired
     */
    public boolean acquireBankAccess(final @NotNull UUID ownerUuid) {
        return ServerBankSessionRegistry.acquire(ownerUuid);
    }

    /**
     * Releases the singleton server-bank viewer lock.
     *
     * @param ownerUuid viewer UUID
     */
    public void releaseBankAccess(final @NotNull UUID ownerUuid) {
        ServerBankSessionRegistry.release(ownerUuid);
    }

    /**
     * Returns whether the singleton server bank is currently locked by a viewer.
     *
     * @return {@code true} when the bank is locked
     */
    public boolean isBankAccessLocked() {
        return ServerBankSessionRegistry.isLocked();
    }

    /**
     * Returns the player's current balance for one configured currency identifier.
     *
     * @param player player to inspect
     * @param currencyId configured or display currency identifier
     * @return current player balance
     */
    public double getPlayerCurrencyBalance(final @NotNull Player player, final @Nullable String currencyId) {
        final TownBankService townBankService = this.plugin.getTownBankService();
        if (townBankService != null) {
            return townBankService.getPlayerCurrencyBalance(player, currencyId);
        }

        if (currencyId == null || currencyId.isBlank()) {
            return 0.0D;
        }
        final String resolvedCurrencyId = this.resolveSupportedCurrencyId(currencyId);
        final JExEconomyBridge bridge = JExEconomyBridge.getBridge();
        if (bridge != null && bridge.hasCurrency(resolvedCurrencyId)) {
            return bridge.getBalance(player, resolvedCurrencyId);
        }

        final Economy economy = this.plugin.getEco();
        if (economy != null && this.isVaultCurrency(resolvedCurrencyId)) {
            return economy.getBalance(player);
        }
        return 0.0D;
    }

    /**
     * Returns the stored balance for one configured currency identifier.
     *
     * @param currencyId configured or display currency identifier
     * @return stored server-bank balance
     */
    public double getCurrencyBalance(final @NotNull String currencyId) {
        return this.getOrCreateBank().getCurrencyBalance(this.resolveSupportedCurrencyId(currencyId));
    }

    /**
     * Replaces the stored balance for one configured currency identifier.
     *
     * @param currencyId configured or display currency identifier
     * @param amount replacement amount
     * @return updated stored balance
     */
    public double setCurrencyBalance(final @NotNull String currencyId, final double amount) {
        final RServerBank bank = this.getOrCreateBank();
        final String resolvedCurrencyId = this.resolveSupportedCurrencyId(currencyId);
        bank.setCurrencyBalance(resolvedCurrencyId, amount);
        this.plugin.getServerBankRepository().update(bank);
        return bank.getCurrencyBalance(resolvedCurrencyId);
    }

    /**
     * Deposits one amount into a stored server-bank balance.
     *
     * @param currencyId configured or display currency identifier
     * @param amount amount to add
     * @return updated stored balance
     */
    public double addCurrency(final @NotNull String currencyId, final double amount) {
        final RServerBank bank = this.getOrCreateBank();
        final String resolvedCurrencyId = this.resolveSupportedCurrencyId(currencyId);
        final double updatedBalance = bank.depositCurrency(resolvedCurrencyId, amount);
        this.plugin.getServerBankRepository().update(bank);
        return updatedBalance;
    }

    /**
     * Withdraws one amount from a stored server-bank balance.
     *
     * @param currencyId configured or display currency identifier
     * @param amount amount to subtract
     * @return {@code true} when the balance covered the withdrawal
     */
    public boolean takeCurrency(final @NotNull String currencyId, final double amount) {
        final RServerBank bank = this.getOrCreateBank();
        final String resolvedCurrencyId = this.resolveSupportedCurrencyId(currencyId);
        final boolean withdrawn = bank.withdrawCurrency(resolvedCurrencyId, amount);
        if (withdrawn) {
            this.plugin.getServerBankRepository().update(bank);
        }
        return withdrawn;
    }

    /**
     * Deposits one configured currency from the player into the server bank.
     *
     * @param player contributing admin
     * @param currencyId configured currency identifier
     * @param amount requested deposit amount
     * @return structured transaction result
     */
    public @NotNull TownBankService.CurrencyTransactionResult depositCurrency(
        final @NotNull Player player,
        final @NotNull String currencyId,
        final double amount
    ) {
        if (amount <= 0.0D || !Double.isFinite(amount)) {
            return new TownBankService.CurrencyTransactionResult(
                TownBankService.CurrencyTransactionStatus.INVALID_AMOUNT,
                currencyId,
                0.0D,
                this.getCurrencyBalance(currencyId)
            );
        }

        final String resolvedCurrencyId = this.resolveSupportedCurrencyId(currencyId);
        if (!this.isSupportedCurrency(resolvedCurrencyId)) {
            return new TownBankService.CurrencyTransactionResult(
                TownBankService.CurrencyTransactionStatus.INVALID_CURRENCY,
                currencyId,
                0.0D,
                this.getCurrencyBalance(resolvedCurrencyId)
            );
        }
        if (!this.withdrawPlayerCurrency(player, resolvedCurrencyId, amount)) {
            return new TownBankService.CurrencyTransactionResult(
                TownBankService.CurrencyTransactionStatus.NOT_ENOUGH_RESOURCES,
                resolvedCurrencyId,
                0.0D,
                this.getCurrencyBalance(resolvedCurrencyId)
            );
        }

        final double newBalance = this.addCurrency(resolvedCurrencyId, amount);
        return new TownBankService.CurrencyTransactionResult(
            TownBankService.CurrencyTransactionStatus.SUCCESS,
            resolvedCurrencyId,
            amount,
            newBalance
        );
    }

    /**
     * Withdraws one configured currency from the server bank to the player.
     *
     * @param player withdrawing admin
     * @param currencyId configured currency identifier
     * @param amount requested withdrawal amount
     * @return structured transaction result
     */
    public @NotNull TownBankService.CurrencyTransactionResult withdrawCurrency(
        final @NotNull Player player,
        final @NotNull String currencyId,
        final double amount
    ) {
        if (amount <= 0.0D || !Double.isFinite(amount)) {
            return new TownBankService.CurrencyTransactionResult(
                TownBankService.CurrencyTransactionStatus.INVALID_AMOUNT,
                currencyId,
                0.0D,
                this.getCurrencyBalance(currencyId)
            );
        }

        final String resolvedCurrencyId = this.resolveSupportedCurrencyId(currencyId);
        if (!this.isSupportedCurrency(resolvedCurrencyId)) {
            return new TownBankService.CurrencyTransactionResult(
                TownBankService.CurrencyTransactionStatus.INVALID_CURRENCY,
                currencyId,
                0.0D,
                this.getCurrencyBalance(resolvedCurrencyId)
            );
        }
        if (!this.takeCurrency(resolvedCurrencyId, amount)) {
            return new TownBankService.CurrencyTransactionResult(
                TownBankService.CurrencyTransactionStatus.NOT_ENOUGH_RESOURCES,
                resolvedCurrencyId,
                0.0D,
                this.getCurrencyBalance(resolvedCurrencyId)
            );
        }
        if (!this.depositPlayerCurrency(player, resolvedCurrencyId, amount)) {
            this.addCurrency(resolvedCurrencyId, amount);
            return new TownBankService.CurrencyTransactionResult(
                TownBankService.CurrencyTransactionStatus.FAILED,
                resolvedCurrencyId,
                0.0D,
                this.getCurrencyBalance(resolvedCurrencyId)
            );
        }

        return new TownBankService.CurrencyTransactionResult(
            TownBankService.CurrencyTransactionStatus.SUCCESS,
            resolvedCurrencyId,
            amount,
            this.getCurrencyBalance(resolvedCurrencyId)
        );
    }

    /**
     * Returns the persisted shared server-bank item storage.
     *
     * @return copied shared storage snapshot
     */
    public @NotNull Map<String, ItemStack> getSharedStorage() {
        return this.getOrCreateBank().getSharedStorage();
    }

    /**
     * Replaces the shared server-bank item storage using one inventory snapshot.
     *
     * @param contents latest inventory contents
     * @return {@code true} when the shared storage was saved
     */
    public boolean saveSharedStorage(final ItemStack @Nullable [] contents) {
        final RServerBank bank = this.getOrCreateBank();
        bank.setSharedStorage(this.snapshotInventory(contents, this.getSharedStorageSize()));
        this.plugin.getServerBankRepository().update(bank);
        return true;
    }

    /**
     * Returns whether the configured currency can currently be resolved on this server.
     *
     * @param currencyId configured or display currency identifier
     * @return {@code true} when the currency is currently supported
     */
    public boolean isSupportedCurrency(final @Nullable String currencyId) {
        final TownBankService townBankService = this.plugin.getTownBankService();
        if (townBankService != null) {
            return townBankService.isSupportedCurrency(currencyId);
        }
        if (currencyId == null || currencyId.isBlank()) {
            return false;
        }
        final String resolvedCurrencyId = this.resolveSupportedCurrencyId(currencyId);
        final JExEconomyBridge bridge = JExEconomyBridge.getBridge();
        return (bridge != null && bridge.hasCurrency(resolvedCurrencyId))
            || (this.plugin.getEco() != null && this.isVaultCurrency(resolvedCurrencyId));
    }

    /**
     * Returns a player-facing label for one configured currency identifier.
     *
     * @param currencyId configured or display currency identifier
     * @return resolved display label
     */
    public @NotNull String resolveCurrencyDisplayName(final @NotNull String currencyId) {
        final TownBankService townBankService = this.plugin.getTownBankService();
        return townBankService == null
            ? toDisplayLabel(this.resolveSupportedCurrencyId(currencyId))
            : townBankService.resolveCurrencyDisplayName(currencyId);
    }

    /**
     * Restores one sparse storage map into an inventory array.
     *
     * @param storedContents sparse storage contents
     * @param size target inventory size
     * @return inventory contents sized for a chest UI
     */
    public @NotNull ItemStack[] expandInventory(final @NotNull Map<String, ItemStack> storedContents, final int size) {
        final TownBankService townBankService = this.plugin.getTownBankService();
        if (townBankService != null) {
            return townBankService.expandInventory(storedContents, size);
        }

        final ItemStack[] contents = new ItemStack[Math.max(9, size)];
        for (final Map.Entry<String, ItemStack> entry : storedContents.entrySet()) {
            try {
                final int slot = Integer.parseInt(entry.getKey());
                final ItemStack itemStack = entry.getValue();
                if (slot >= 0 && slot < contents.length && itemStack != null && !itemStack.isEmpty()) {
                    contents[slot] = itemStack.clone();
                }
            } catch (final NumberFormatException ignored) {
            }
        }
        return contents;
    }

    /**
     * Converts one inventory snapshot into a sparse persisted storage map.
     *
     * @param contents latest inventory contents
     * @param maxSlots maximum number of persisted slots
     * @return sparse persisted inventory snapshot
     */
    public @NotNull Map<String, ItemStack> snapshotInventory(final ItemStack @Nullable [] contents, final int maxSlots) {
        final TownBankService townBankService = this.plugin.getTownBankService();
        if (townBankService != null) {
            return townBankService.snapshotInventory(contents, maxSlots);
        }

        final Map<String, ItemStack> snapshot = new LinkedHashMap<>();
        if (contents == null) {
            return snapshot;
        }
        final int slotLimit = Math.min(contents.length, Math.max(0, maxSlots));
        for (int slot = 0; slot < slotLimit; slot++) {
            final ItemStack itemStack = contents[slot];
            if (itemStack == null || itemStack.isEmpty()) {
                continue;
            }
            snapshot.put(String.valueOf(slot), itemStack.clone());
        }
        return snapshot;
    }

    /**
     * Clears any lingering local server-bank locks.
     */
    public void shutdown() {
        ServerBankSessionRegistry.clear();
    }

    private boolean withdrawPlayerCurrency(
        final @NotNull Player player,
        final @NotNull String currencyId,
        final double amount
    ) {
        final JExEconomyBridge bridge = JExEconomyBridge.getBridge();
        if (bridge != null && bridge.hasCurrency(currencyId)) {
            return safeJoin(bridge.withdraw(player, currencyId, amount));
        }
        final Economy economy = this.plugin.getEco();
        return economy != null && this.isVaultCurrency(currencyId) && economy.withdrawPlayer(player, amount).transactionSuccess();
    }

    private boolean depositPlayerCurrency(
        final @NotNull Player player,
        final @NotNull String currencyId,
        final double amount
    ) {
        final JExEconomyBridge bridge = JExEconomyBridge.getBridge();
        if (bridge != null && bridge.hasCurrency(currencyId)) {
            return safeJoin(bridge.deposit(player, currencyId, amount));
        }
        final Economy economy = this.plugin.getEco();
        return economy != null && this.isVaultCurrency(currencyId) && economy.depositPlayer(player, amount).transactionSuccess();
    }

    private @NotNull String resolveSupportedCurrencyId(final @Nullable String currencyId) {
        if (currencyId == null || currencyId.isBlank()) {
            return "";
        }

        final String trimmedCurrencyId = currencyId.trim();
        final JExEconomyBridge bridge = JExEconomyBridge.getBridge();
        if (bridge == null) {
            return trimmedCurrencyId;
        }
        if (bridge.hasCurrency(trimmedCurrencyId)) {
            return trimmedCurrencyId;
        }

        final String normalizedLookupToken = normalizeCurrencyLookupToken(trimmedCurrencyId);
        final Map<String, String> availableCurrencies = bridge.getAvailableCurrencies();
        if (availableCurrencies == null || availableCurrencies.isEmpty()) {
            return trimmedCurrencyId;
        }
        for (final Map.Entry<String, String> entry : availableCurrencies.entrySet()) {
            final String candidateCurrencyId = entry.getKey();
            final String candidateDisplayName = entry.getValue();
            if (trimmedCurrencyId.equalsIgnoreCase(candidateCurrencyId)
                || trimmedCurrencyId.equalsIgnoreCase(candidateDisplayName)
                || normalizedLookupToken.equals(normalizeCurrencyLookupToken(candidateCurrencyId))
                || normalizedLookupToken.equals(normalizeCurrencyLookupToken(candidateDisplayName))) {
                return candidateCurrencyId;
            }
        }
        return trimmedCurrencyId;
    }

    private boolean isVaultCurrency(final @NotNull String currencyId) {
        final String normalized = currencyId.trim().toLowerCase(Locale.ROOT);
        return Objects.equals(normalized, "vault")
            || Objects.equals(normalized, "money")
            || Objects.equals(normalized, "dollars");
    }

    private static @NotNull String normalizeCurrencyLookupToken(final @Nullable String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    private static @NotNull String toDisplayLabel(final @NotNull String value) {
        final String[] words = value.toLowerCase(Locale.ROOT).split("_");
        final StringBuilder builder = new StringBuilder();
        for (final String word : words) {
            if (word.isBlank()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return builder.length() == 0 ? value : builder.toString();
    }

    private static boolean safeJoin(final @NotNull CompletableFuture<Boolean> future) {
        try {
            return Boolean.TRUE.equals(future.join());
        } catch (final Exception exception) {
            return false;
        }
    }
}
