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
import com.raindropcentral.rdt.database.entity.RDTPlayer;
import com.raindropcentral.rdt.database.entity.RNation;
import com.raindropcentral.rdt.database.entity.RTown;
import com.raindropcentral.rdt.utils.TownPermissions;
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
 * Centralizes nation-bank access rules, shared storage persistence, and currency transactions.
 *
 * <p>Nation-bank permissions are derived from each viewer's own town role. The service therefore
 * validates both the player's current town membership and that town's active nation before allowing
 * any bank interaction.</p>
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public final class NationBankService {

    private static final int DEFAULT_SHARED_STORAGE_SIZE = 54;

    private final RDT plugin;

    /**
     * Creates the nation-bank service.
     *
     * @param plugin active RDT runtime
     */
    public NationBankService(final @NotNull RDT plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    /**
     * Returns one active nation by UUID.
     *
     * @param nationUuid nation UUID
     * @return active nation, or {@code null} when unavailable
     */
    public @Nullable RNation getNation(final @NotNull UUID nationUuid) {
        final TownRuntimeService runtimeService = this.plugin.getTownRuntimeService();
        final RNation nation = runtimeService == null ? null : runtimeService.getNation(nationUuid);
        return nation != null && nation.isActive() ? nation : null;
    }

    /**
     * Returns the active nation currently associated with one player.
     *
     * @param player player to inspect
     * @return active nation, or {@code null} when the player is not in one
     */
    public @Nullable RNation getNationForPlayer(final @NotNull Player player) {
        final TownRuntimeService runtimeService = this.plugin.getTownRuntimeService();
        final RTown town = runtimeService == null ? null : runtimeService.getTownFor(player.getUniqueId());
        return town == null || runtimeService == null ? null : runtimeService.getNationForTown(town);
    }

    /**
     * Returns the configured nation-bank currencies charged by taxes and shown in the UI.
     *
     * @return configured currency identifiers
     */
    public @NotNull List<String> getConfiguredCurrencies() {
        return this.plugin.getTaxConfig().getCurrency().currencyIds();
    }

    /**
     * Returns the shared nation-bank storage size in slots.
     *
     * @return shared nation-bank storage size
     */
    public int getSharedStorageSize() {
        return DEFAULT_SHARED_STORAGE_SIZE;
    }

    /**
     * Returns whether the player may view the active nation bank.
     *
     * @param player player to inspect
     * @param nation target nation
     * @return {@code true} when the player may open the nation bank
     */
    public boolean canViewBank(final @NotNull Player player, final @Nullable RNation nation) {
        final Membership membership = this.resolveMembership(player, nation);
        return membership != null && membership.playerData().hasTownPermission(TownPermissions.VIEW_NATION_BANK);
    }

    /**
     * Returns whether the player may deposit currencies into the active nation bank.
     *
     * @param player player to inspect
     * @param nation target nation
     * @return {@code true} when deposits are allowed
     */
    public boolean canDepositCurrency(final @NotNull Player player, final @Nullable RNation nation) {
        final Membership membership = this.resolveMembership(player, nation);
        return membership != null && membership.playerData().hasTownPermission(TownPermissions.NATION_DEPOSIT);
    }

    /**
     * Returns whether the player may withdraw currencies from the active nation bank.
     *
     * @param player player to inspect
     * @param nation target nation
     * @return {@code true} when withdrawals are allowed
     */
    public boolean canWithdrawCurrency(final @NotNull Player player, final @Nullable RNation nation) {
        final Membership membership = this.resolveMembership(player, nation);
        return membership != null && membership.playerData().hasTownPermission(TownPermissions.NATION_WITHDRAW);
    }

    /**
     * Acquires one nation-bank viewer lock.
     *
     * @param nationUuid nation UUID
     * @param ownerUuid viewer UUID
     * @return {@code true} when the lock was acquired
     */
    public boolean acquireBankAccess(final @NotNull UUID nationUuid, final @NotNull UUID ownerUuid) {
        return NationBankSessionRegistry.acquire(nationUuid, ownerUuid);
    }

    /**
     * Releases one nation-bank viewer lock.
     *
     * @param nationUuid nation UUID
     * @param ownerUuid viewer UUID
     */
    public void releaseBankAccess(final @NotNull UUID nationUuid, final @NotNull UUID ownerUuid) {
        NationBankSessionRegistry.release(nationUuid, ownerUuid);
    }

    /**
     * Returns whether the nation bank is currently locked by one viewer.
     *
     * @param nationUuid nation UUID
     * @return {@code true} when the bank is locked
     */
    public boolean isBankAccessLocked(final @NotNull UUID nationUuid) {
        return NationBankSessionRegistry.isLocked(nationUuid);
    }

    /**
     * Returns the player's current balance for one configured currency identifier.
     *
     * @param player player to inspect
     * @param currencyId configured currency identifier
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
     * Returns the nation's stored balance for one configured currency identifier.
     *
     * @param nation target nation
     * @param currencyId configured or display currency identifier
     * @return stored nation-bank balance
     */
    public double getNationCurrencyBalance(final @NotNull RNation nation, final @NotNull String currencyId) {
        return nation.getBankAmount(this.resolveSupportedCurrencyId(currencyId));
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
     * Deposits one configured currency from the player into the nation bank.
     *
     * @param player contributing player
     * @param nationUuid target nation UUID
     * @param currencyId configured currency identifier
     * @param amount requested deposit amount
     * @return structured transaction result
     */
    public @NotNull TownBankService.CurrencyTransactionResult depositCurrency(
        final @NotNull Player player,
        final @NotNull UUID nationUuid,
        final @NotNull String currencyId,
        final double amount
    ) {
        final RNation nation = this.getNation(nationUuid);
        if (nation == null || this.plugin.getNationRepository() == null) {
            return new TownBankService.CurrencyTransactionResult(
                TownBankService.CurrencyTransactionStatus.INVALID_TARGET,
                currencyId,
                0.0D,
                0.0D
            );
        }
        if (!this.canDepositCurrency(player, nation)) {
            return new TownBankService.CurrencyTransactionResult(
                TownBankService.CurrencyTransactionStatus.NO_PERMISSION,
                currencyId,
                0.0D,
                nation.getBankAmount(currencyId)
            );
        }
        if (amount <= 0.0D || !Double.isFinite(amount)) {
            return new TownBankService.CurrencyTransactionResult(
                TownBankService.CurrencyTransactionStatus.INVALID_AMOUNT,
                currencyId,
                0.0D,
                nation.getBankAmount(currencyId)
            );
        }

        final String resolvedCurrencyId = this.resolveSupportedCurrencyId(currencyId);
        if (!this.isSupportedCurrency(resolvedCurrencyId)) {
            return new TownBankService.CurrencyTransactionResult(
                TownBankService.CurrencyTransactionStatus.INVALID_CURRENCY,
                currencyId,
                0.0D,
                nation.getBankAmount(resolvedCurrencyId)
            );
        }
        if (!this.withdrawPlayerCurrency(player, resolvedCurrencyId, amount)) {
            return new TownBankService.CurrencyTransactionResult(
                TownBankService.CurrencyTransactionStatus.NOT_ENOUGH_RESOURCES,
                resolvedCurrencyId,
                0.0D,
                nation.getBankAmount(resolvedCurrencyId)
            );
        }

        final double newBalance = nation.depositBank(resolvedCurrencyId, amount);
        this.plugin.getNationRepository().update(nation);
        return new TownBankService.CurrencyTransactionResult(
            TownBankService.CurrencyTransactionStatus.SUCCESS,
            resolvedCurrencyId,
            amount,
            newBalance
        );
    }

    /**
     * Withdraws one configured currency from the nation bank to the player.
     *
     * @param player withdrawing player
     * @param nationUuid target nation UUID
     * @param currencyId configured currency identifier
     * @param amount requested withdrawal amount
     * @return structured transaction result
     */
    public @NotNull TownBankService.CurrencyTransactionResult withdrawCurrency(
        final @NotNull Player player,
        final @NotNull UUID nationUuid,
        final @NotNull String currencyId,
        final double amount
    ) {
        final RNation nation = this.getNation(nationUuid);
        if (nation == null || this.plugin.getNationRepository() == null) {
            return new TownBankService.CurrencyTransactionResult(
                TownBankService.CurrencyTransactionStatus.INVALID_TARGET,
                currencyId,
                0.0D,
                0.0D
            );
        }
        if (!this.canWithdrawCurrency(player, nation)) {
            return new TownBankService.CurrencyTransactionResult(
                TownBankService.CurrencyTransactionStatus.NO_PERMISSION,
                currencyId,
                0.0D,
                nation.getBankAmount(currencyId)
            );
        }
        if (amount <= 0.0D || !Double.isFinite(amount)) {
            return new TownBankService.CurrencyTransactionResult(
                TownBankService.CurrencyTransactionStatus.INVALID_AMOUNT,
                currencyId,
                0.0D,
                nation.getBankAmount(currencyId)
            );
        }

        final String resolvedCurrencyId = this.resolveSupportedCurrencyId(currencyId);
        if (!this.isSupportedCurrency(resolvedCurrencyId)) {
            return new TownBankService.CurrencyTransactionResult(
                TownBankService.CurrencyTransactionStatus.INVALID_CURRENCY,
                currencyId,
                0.0D,
                nation.getBankAmount(resolvedCurrencyId)
            );
        }
        if (!nation.withdrawBank(resolvedCurrencyId, amount)) {
            return new TownBankService.CurrencyTransactionResult(
                TownBankService.CurrencyTransactionStatus.NOT_ENOUGH_RESOURCES,
                resolvedCurrencyId,
                0.0D,
                nation.getBankAmount(resolvedCurrencyId)
            );
        }
        if (!this.depositPlayerCurrency(player, resolvedCurrencyId, amount)) {
            nation.depositBank(resolvedCurrencyId, amount);
            return new TownBankService.CurrencyTransactionResult(
                TownBankService.CurrencyTransactionStatus.FAILED,
                resolvedCurrencyId,
                0.0D,
                nation.getBankAmount(resolvedCurrencyId)
            );
        }

        final double newBalance = nation.getBankAmount(resolvedCurrencyId);
        this.plugin.getNationRepository().update(nation);
        return new TownBankService.CurrencyTransactionResult(
            TownBankService.CurrencyTransactionStatus.SUCCESS,
            resolvedCurrencyId,
            amount,
            newBalance
        );
    }

    /**
     * Replaces the shared nation-bank storage contents with one inventory snapshot.
     *
     * @param nationUuid target nation UUID
     * @param contents latest inventory contents
     * @return {@code true} when the shared storage was saved
     */
    public boolean saveSharedStorage(final @NotNull UUID nationUuid, final ItemStack @Nullable [] contents) {
        final RNation nation = this.getNation(nationUuid);
        if (nation == null || this.plugin.getNationRepository() == null) {
            return false;
        }
        nation.setSharedBankStorage(this.snapshotInventory(contents, this.getSharedStorageSize()));
        this.plugin.getNationRepository().update(nation);
        return true;
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
     * Clears any lingering local nation-bank locks.
     */
    public void shutdown() {
        NationBankSessionRegistry.clear();
    }

    private @Nullable Membership resolveMembership(final @NotNull Player player, final @Nullable RNation nation) {
        if (nation == null) {
            return null;
        }

        final TownRuntimeService runtimeService = this.plugin.getTownRuntimeService();
        if (runtimeService == null) {
            return null;
        }
        final RDTPlayer playerData = runtimeService.getPlayerData(player.getUniqueId());
        final RTown town = runtimeService.getTownFor(player.getUniqueId());
        if (playerData == null || town == null || !Objects.equals(town.getNationUuid(), nation.getNationUuid())) {
            return null;
        }
        return new Membership(playerData, town);
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

    private record Membership(@NotNull RDTPlayer playerData, @NotNull RTown town) {
    }
}
