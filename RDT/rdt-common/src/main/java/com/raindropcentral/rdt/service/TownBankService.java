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
import com.raindropcentral.rdt.configs.BankConfigSection;
import com.raindropcentral.rdt.database.entity.RDTPlayer;
import com.raindropcentral.rdt.database.entity.RTown;
import com.raindropcentral.rdt.database.entity.RTownChunk;
import com.raindropcentral.rdt.items.CacheChest;
import com.raindropcentral.rdt.utils.ChunkType;
import com.raindropcentral.rdt.utils.TownPermissions;
import com.raindropcentral.rplatform.economy.JExEconomyBridge;
import com.raindropcentral.rplatform.proxy.ProxyActionEnvelope;
import com.raindropcentral.rplatform.proxy.ProxyActionResult;
import com.raindropcentral.rplatform.serializer.ItemStackSerializer;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
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
import java.util.logging.Level;

/**
 * Centralizes config-driven town-bank capabilities, cache placement state, storage persistence, and
 * cross-cluster remote cache deposits.
 *
 * <p>Persistent bank and cache data lives on {@link RTown}, while session locking is delegated to
 * {@link TownBankSessionRegistry} so every local bank entry point shares one town-wide viewer
 * lock.</p>
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public final class TownBankService {

    private static final String PROXY_MODULE_ID = "rdt";
    private static final String ACTION_ACQUIRE_LOCK = "bank_acquire_lock";
    private static final String ACTION_APPLY_REMOTE_DEPOSIT = "bank_apply_remote_cache_deposit";
    private static final String ACTION_RELEASE_LOCK = "bank_release_lock";

    private static final String PAYLOAD_TOWN_UUID = "town_uuid";
    private static final String PAYLOAD_ITEM_ARRAY = "item_array";

    private final RDT plugin;
    private final ItemStackSerializer itemStackSerializer;
    private boolean running;

    /**
     * Creates the town-bank service.
     *
     * @param plugin active RDT runtime
     */
    public TownBankService(final @NotNull RDT plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.itemStackSerializer = new ItemStackSerializer();
    }

    /**
     * Registers cross-cluster proxy handlers used by level 5 remote cache deposits.
     */
    public void start() {
        if (this.running) {
            return;
        }

        this.plugin.getProxyService().registerActionHandler(
            PROXY_MODULE_ID,
            ACTION_ACQUIRE_LOCK,
            this::handleAcquireLock
        );
        this.plugin.getProxyService().registerActionHandler(
            PROXY_MODULE_ID,
            ACTION_APPLY_REMOTE_DEPOSIT,
            this::handleApplyRemoteDeposit
        );
        this.plugin.getProxyService().registerActionHandler(
            PROXY_MODULE_ID,
            ACTION_RELEASE_LOCK,
            this::handleReleaseLock
        );
        this.running = true;
    }

    /**
     * Unregisters proxy handlers and clears any lingering local bank locks.
     */
    public void shutdown() {
        this.running = false;
        this.plugin.getProxyService().unregisterActionHandler(PROXY_MODULE_ID, ACTION_ACQUIRE_LOCK);
        this.plugin.getProxyService().unregisterActionHandler(PROXY_MODULE_ID, ACTION_APPLY_REMOTE_DEPOSIT);
        this.plugin.getProxyService().unregisterActionHandler(PROXY_MODULE_ID, ACTION_RELEASE_LOCK);
        TownBankSessionRegistry.clear();
    }

    /**
     * Returns one town by UUID.
     *
     * @param townUuid town UUID
     * @return live town, or {@code null} when unavailable
     */
    public @Nullable RTown getTown(final @NotNull UUID townUuid) {
        final TownRuntimeService runtimeService = this.plugin.getTownRuntimeService();
        return runtimeService == null ? null : runtimeService.getTown(townUuid);
    }

    /**
     * Returns the highest unlocked BANK chunk level owned by the town.
     *
     * @param town town to inspect
     * @return highest BANK level, or {@code 0} when the town has no BANK chunk
     */
    public int getHighestBankLevel(final @Nullable RTown town) {
        if (town == null) {
            return 0;
        }
        return town.getChunks().stream()
            .filter(chunk -> chunk.getChunkType() == ChunkType.BANK)
            .mapToInt(RTownChunk::getChunkLevel)
            .max()
            .orElse(0);
    }

    /**
     * Returns the configured bank currencies shown in the bank UI.
     *
     * @return normalized currency identifiers
     */
    public @NotNull List<String> getConfiguredCurrencies() {
        return this.bankConfig().getCurrencyStorage().currencies();
    }

    /**
     * Returns the shared town-bank storage size in slots.
     *
     * @return configured shared-storage size
     */
    public int getSharedStorageSize() {
        return Math.max(9, Math.min(54, this.bankConfig().getItemStorage().rows() * 9));
    }

    /**
     * Returns the cache inventory size in slots.
     *
     * @return configured cache size
     */
    public int getCacheSize() {
        return Math.max(9, Math.min(54, this.bankConfig().getCache().rows() * 9));
    }

    /**
     * Returns whether level 1 currency storage is currently unlocked for the town.
     *
     * @param town town to inspect
     * @return {@code true} when currency storage is unlocked
     */
    public boolean isCurrencyStorageUnlocked(final @Nullable RTown town) {
        return this.bankConfig().getCurrencyStorage().isUnlocked(this.getHighestBankLevel(town));
    }

    /**
     * Returns whether level 2 shared item storage is currently unlocked for the town.
     *
     * @param town town to inspect
     * @return {@code true} when shared item storage is unlocked
     */
    public boolean isItemStorageUnlocked(final @Nullable RTown town) {
        return this.bankConfig().getItemStorage().isUnlocked(this.getHighestBankLevel(town));
    }

    /**
     * Returns whether level 3 remote bank command access is currently unlocked for the town.
     *
     * @param town town to inspect
     * @return {@code true} when {@code /rt bank} local access is unlocked
     */
    public boolean isRemoteCommandUnlocked(final @Nullable RTown town) {
        return this.bankConfig().getRemoteAccess().isUnlocked(this.getHighestBankLevel(town));
    }

    /**
     * Returns whether level 4 cache access is currently unlocked for the town.
     *
     * @param town town to inspect
     * @return {@code true} when the town cache is unlocked
     */
    public boolean isCacheUnlocked(final @Nullable RTown town) {
        return this.bankConfig().getCache().isUnlocked(this.getHighestBankLevel(town));
    }

    /**
     * Returns whether level 5 remote cache deposit is currently unlocked for the town.
     *
     * @param town town to inspect
     * @return {@code true} when cluster-remote cache deposit is unlocked
     */
    public boolean supportsRemoteCacheDeposit(final @Nullable RTown town) {
        return this.bankConfig().getRemoteAccess().supportsCrossClusterCacheDeposit(this.getHighestBankLevel(town));
    }

    /**
     * Returns whether the town currently has a placed cache chest binding.
     *
     * @param town town to inspect
     * @return {@code true} when the cache chest is placed
     */
    public boolean hasPlacedCache(final @Nullable RTown town) {
        return town != null && town.hasBankCacheLocation();
    }

    /**
     * Returns whether the player belongs to the town and may view bank UI or cache storage.
     *
     * @param player player to inspect
     * @param town target town
     * @return {@code true} when the player may view bank storage
     */
    public boolean canViewBank(final @NotNull Player player, final @Nullable RTown town) {
        final TownRuntimeService runtimeService = this.plugin.getTownRuntimeService();
        final RDTPlayer playerData = runtimeService == null ? null : runtimeService.getPlayerData(player.getUniqueId());
        return playerData != null
            && town != null
            && Objects.equals(playerData.getTownUUID(), town.getTownUUID())
            && playerData.hasTownPermission(TownPermissions.VIEW_BANK);
    }

    /**
     * Returns whether the player may deposit configured currencies into the town bank.
     *
     * @param player player to inspect
     * @param town target town
     * @return {@code true} when deposits are allowed
     */
    public boolean canDepositCurrency(final @NotNull Player player, final @Nullable RTown town) {
        final TownRuntimeService runtimeService = this.plugin.getTownRuntimeService();
        final RDTPlayer playerData = runtimeService == null ? null : runtimeService.getPlayerData(player.getUniqueId());
        return playerData != null
            && town != null
            && Objects.equals(playerData.getTownUUID(), town.getTownUUID())
            && playerData.hasTownPermission(TownPermissions.TOWN_DEPOSIT);
    }

    /**
     * Returns whether the player may withdraw configured currencies from the town bank.
     *
     * @param player player to inspect
     * @param town target town
     * @return {@code true} when withdrawals are allowed
     */
    public boolean canWithdrawCurrency(final @NotNull Player player, final @Nullable RTown town) {
        final TownRuntimeService runtimeService = this.plugin.getTownRuntimeService();
        final RDTPlayer playerData = runtimeService == null ? null : runtimeService.getPlayerData(player.getUniqueId());
        return playerData != null
            && town != null
            && Objects.equals(playerData.getTownUUID(), town.getTownUUID())
            && playerData.hasTownPermission(TownPermissions.TOWN_WITHDRAW);
    }

    /**
     * Returns whether the player may place or pick up the bound cache chest item for the town.
     *
     * @param player player to inspect
     * @param town target town
     * @return {@code true} when the player may manage the placed cache chest
     */
    public boolean canManageCachePlacement(final @NotNull Player player, final @Nullable RTown town) {
        final TownRuntimeService runtimeService = this.plugin.getTownRuntimeService();
        final RDTPlayer playerData = runtimeService == null ? null : runtimeService.getPlayerData(player.getUniqueId());
        return playerData != null
            && town != null
            && Objects.equals(playerData.getTownUUID(), town.getTownUUID())
            && playerData.hasTownPermission(TownPermissions.CHANGE_CHUNK_TYPE);
    }

    /**
     * Returns whether one bank chunk entry may open the actionable bank UI.
     *
     * @param player player attempting to open the bank
     * @param townChunk BANK chunk entry point
     * @return {@code true} when the player may open the actionable bank UI
     */
    public boolean canOpenBankChunkView(final @NotNull Player player, final @Nullable RTownChunk townChunk) {
        return townChunk != null
            && townChunk.getChunkType() == ChunkType.BANK
            && this.canViewBank(player, townChunk.getTown())
            && this.isCurrencyStorageUnlocked(townChunk.getTown());
    }

    /**
     * Resolves whether the player can use {@code /rt bank} right now and which access mode should
     * open when the command succeeds.
     *
     * @param player player executing or tab-completing the command
     * @return resolved command-access outcome
     */
    public @NotNull BankCommandAccess resolveCommandAccess(final @NotNull Player player) {
        final TownRuntimeService runtimeService = this.plugin.getTownRuntimeService();
        if (runtimeService == null) {
            return new BankCommandAccess(BankCommandStatus.INVALID_TARGET, null, null);
        }

        final RTown town = runtimeService.getTownFor(player.getUniqueId());
        if (town == null) {
            return new BankCommandAccess(BankCommandStatus.NO_TOWN, null, null);
        }
        if (!this.canViewBank(player, town)) {
            return new BankCommandAccess(BankCommandStatus.NO_VIEW_PERMISSION, town, null);
        }

        final RDTPlayer playerData = runtimeService.getPlayerData(player.getUniqueId());
        if (playerData == null || !playerData.hasTownPermission(TownPermissions.TOWN_BANK_REMOTE)) {
            return new BankCommandAccess(BankCommandStatus.NO_REMOTE_PERMISSION, town, null);
        }
        if (!this.isRemoteCommandUnlocked(town)) {
            return new BankCommandAccess(BankCommandStatus.LOCKED, town, null);
        }

        final RTownChunk currentChunk = runtimeService.getChunkAt(player.getLocation());
        final boolean inOwnClaim = currentChunk != null && Objects.equals(currentChunk.getTown().getTownUUID(), town.getTownUUID());
        if (!this.bankConfig().getRemoteAccess().requireOwnClaim() || inOwnClaim) {
            return new BankCommandAccess(BankCommandStatus.LOCAL_BANK, town, currentChunk);
        }
        if (this.supportsRemoteCacheDeposit(town)) {
            return this.hasPlacedCache(town)
                ? new BankCommandAccess(BankCommandStatus.REMOTE_CACHE_DEPOSIT, town, null)
                : new BankCommandAccess(BankCommandStatus.CACHE_UNAVAILABLE, town, null);
        }
        return new BankCommandAccess(BankCommandStatus.LOCATION_REQUIRED, town, null);
    }

    /**
     * Returns whether one bank command should appear in tab completion for the player.
     *
     * @param player player tab-completing the command
     * @return {@code true} when at least one bank command access mode is usable right now
     */
    public boolean isBankCommandVisible(final @NotNull Player player) {
        final BankCommandStatus status = this.resolveCommandAccess(player).status();
        return status == BankCommandStatus.LOCAL_BANK || status == BankCommandStatus.REMOTE_CACHE_DEPOSIT;
    }

    /**
     * Resolves the player's current balance for one configured currency identifier.
     *
     * @param player player to inspect
     * @param currencyId configured or display currency identifier
     * @return current player balance
     */
    public double getPlayerCurrencyBalance(final @NotNull Player player, final @Nullable String currencyId) {
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
     * Returns the town's stored balance for one configured currency identifier.
     *
     * @param town target town
     * @param currencyId configured or display currency identifier
     * @return stored town balance
     */
    public double getTownCurrencyBalance(final @NotNull RTown town, final @NotNull String currencyId) {
        return town.getBankAmount(this.resolveSupportedCurrencyId(currencyId));
    }

    /**
     * Returns a player-facing label for one configured currency identifier.
     *
     * @param currencyId configured or display currency identifier
     * @return resolved display label
     */
    public @NotNull String resolveCurrencyDisplayName(final @NotNull String currencyId) {
        final String resolvedCurrencyId = this.resolveSupportedCurrencyId(currencyId);
        final JExEconomyBridge bridge = JExEconomyBridge.getBridge();
        if (bridge != null && bridge.hasCurrency(resolvedCurrencyId)) {
            final String displayName = bridge.getCurrencyDisplayName(resolvedCurrencyId);
            if (displayName != null && !displayName.isBlank()) {
                return displayName;
            }
        }
        if (this.isVaultCurrency(resolvedCurrencyId)) {
            return "Vault";
        }
        return toDisplayLabel(resolvedCurrencyId);
    }

    /**
     * Returns whether the configured currency can currently be resolved on this server.
     *
     * @param currencyId configured or display currency identifier
     * @return {@code true} when deposits and withdrawals can be processed
     */
    public boolean isSupportedCurrency(final @Nullable String currencyId) {
        if (currencyId == null || currencyId.isBlank()) {
            return false;
        }

        final String resolvedCurrencyId = this.resolveSupportedCurrencyId(currencyId);
        final JExEconomyBridge bridge = JExEconomyBridge.getBridge();
        if (bridge != null && bridge.hasCurrency(resolvedCurrencyId)) {
            return true;
        }
        return this.plugin.getEco() != null && this.isVaultCurrency(resolvedCurrencyId);
    }

    /**
     * Deposits one configured currency from the player into the town bank.
     *
     * @param player contributing player
     * @param townUuid target town UUID
     * @param currencyId configured currency identifier
     * @param amount requested deposit amount
     * @return structured deposit result
     */
    public @NotNull CurrencyTransactionResult depositCurrency(
        final @NotNull Player player,
        final @NotNull UUID townUuid,
        final @NotNull String currencyId,
        final double amount
    ) {
        final RTown town = this.getTown(townUuid);
        if (town == null || !this.isCurrencyStorageUnlocked(town) || this.plugin.getTownRepository() == null) {
            return new CurrencyTransactionResult(CurrencyTransactionStatus.INVALID_TARGET, currencyId, 0.0D, 0.0D);
        }
        if (!this.canDepositCurrency(player, town)) {
            return new CurrencyTransactionResult(CurrencyTransactionStatus.NO_PERMISSION, currencyId, 0.0D, 0.0D);
        }
        if (amount <= 0.0D || !Double.isFinite(amount)) {
            return new CurrencyTransactionResult(CurrencyTransactionStatus.INVALID_AMOUNT, currencyId, 0.0D, town.getBankAmount(currencyId));
        }

        final String resolvedCurrencyId = this.resolveSupportedCurrencyId(currencyId);
        if (!this.isSupportedCurrency(resolvedCurrencyId)) {
            return new CurrencyTransactionResult(CurrencyTransactionStatus.INVALID_CURRENCY, currencyId, 0.0D, town.getBankAmount(resolvedCurrencyId));
        }
        if (!this.withdrawPlayerCurrency(player, resolvedCurrencyId, amount)) {
            return new CurrencyTransactionResult(
                CurrencyTransactionStatus.NOT_ENOUGH_RESOURCES,
                resolvedCurrencyId,
                0.0D,
                town.getBankAmount(resolvedCurrencyId)
            );
        }

        final double newBalance = town.depositBank(resolvedCurrencyId, amount);
        this.plugin.getTownRepository().update(town);
        return new CurrencyTransactionResult(CurrencyTransactionStatus.SUCCESS, resolvedCurrencyId, amount, newBalance);
    }

    /**
     * Withdraws one configured currency from the town bank to the player.
     *
     * @param player receiving player
     * @param townUuid target town UUID
     * @param currencyId configured currency identifier
     * @param amount requested withdrawal amount
     * @return structured withdrawal result
     */
    public @NotNull CurrencyTransactionResult withdrawCurrency(
        final @NotNull Player player,
        final @NotNull UUID townUuid,
        final @NotNull String currencyId,
        final double amount
    ) {
        final RTown town = this.getTown(townUuid);
        if (town == null || !this.isCurrencyStorageUnlocked(town) || this.plugin.getTownRepository() == null) {
            return new CurrencyTransactionResult(CurrencyTransactionStatus.INVALID_TARGET, currencyId, 0.0D, 0.0D);
        }
        if (!this.canWithdrawCurrency(player, town)) {
            return new CurrencyTransactionResult(CurrencyTransactionStatus.NO_PERMISSION, currencyId, 0.0D, 0.0D);
        }
        if (amount <= 0.0D || !Double.isFinite(amount)) {
            return new CurrencyTransactionResult(CurrencyTransactionStatus.INVALID_AMOUNT, currencyId, 0.0D, town.getBankAmount(currencyId));
        }

        final String resolvedCurrencyId = this.resolveSupportedCurrencyId(currencyId);
        if (!this.isSupportedCurrency(resolvedCurrencyId)) {
            return new CurrencyTransactionResult(CurrencyTransactionStatus.INVALID_CURRENCY, currencyId, 0.0D, town.getBankAmount(resolvedCurrencyId));
        }
        if (!town.withdrawBank(resolvedCurrencyId, amount)) {
            return new CurrencyTransactionResult(
                CurrencyTransactionStatus.NOT_ENOUGH_RESOURCES,
                resolvedCurrencyId,
                0.0D,
                town.getBankAmount(resolvedCurrencyId)
            );
        }
        if (!this.depositPlayerCurrency(player, resolvedCurrencyId, amount)) {
            town.depositBank(resolvedCurrencyId, amount);
            return new CurrencyTransactionResult(CurrencyTransactionStatus.FAILED, resolvedCurrencyId, 0.0D, town.getBankAmount(resolvedCurrencyId));
        }

        final double newBalance = town.getBankAmount(resolvedCurrencyId);
        this.plugin.getTownRepository().update(town);
        return new CurrencyTransactionResult(CurrencyTransactionStatus.SUCCESS, resolvedCurrencyId, amount, newBalance);
    }

    /**
     * Returns whether the single-viewer town bank lock may be acquired by the player.
     *
     * @param townUuid target town UUID
     * @param ownerUuid viewing player UUID
     * @return {@code true} when the town bank can be opened now
     */
    public boolean canOpenBankAccess(final @NotNull UUID townUuid, final @NotNull UUID ownerUuid) {
        return !this.bankConfig().getLocking().singleViewer() || TownBankSessionRegistry.canOpen(townUuid, ownerUuid);
    }

    /**
     * Acquires the single-viewer town bank lock for one player.
     *
     * @param townUuid target town UUID
     * @param ownerUuid viewing player UUID
     * @return {@code true} when the lock is now held by the player
     */
    public boolean acquireBankAccess(final @NotNull UUID townUuid, final @NotNull UUID ownerUuid) {
        return !this.bankConfig().getLocking().singleViewer() || TownBankSessionRegistry.acquire(townUuid, ownerUuid);
    }

    /**
     * Releases one previously acquired town bank lock hold for the player.
     *
     * @param townUuid target town UUID
     * @param ownerUuid viewing player UUID
     */
    public void releaseBankAccess(final @NotNull UUID townUuid, final @NotNull UUID ownerUuid) {
        if (!this.bankConfig().getLocking().singleViewer()) {
            return;
        }
        TownBankSessionRegistry.release(townUuid, ownerUuid);
    }

    /**
     * Returns whether any local viewer currently holds the town-wide bank lock.
     *
     * @param townUuid target town UUID
     * @return {@code true} when local bank access is locked
     */
    public boolean isBankAccessLocked(final @NotNull UUID townUuid) {
        return this.bankConfig().getLocking().singleViewer() && TownBankSessionRegistry.isLocked(townUuid);
    }

    /**
     * Replaces the shared town-bank storage contents with the supplied inventory snapshot.
     *
     * @param townUuid target town UUID
     * @param contents latest inventory contents
     * @return {@code true} when the shared storage was saved
     */
    public boolean saveSharedStorage(final @NotNull UUID townUuid, final ItemStack @Nullable [] contents) {
        final RTown town = this.getTown(townUuid);
        if (town == null || !this.isItemStorageUnlocked(town) || this.plugin.getTownRepository() == null) {
            return false;
        }
        town.setSharedBankStorage(this.snapshotInventory(contents, this.getSharedStorageSize()));
        this.plugin.getTownRepository().update(town);
        return true;
    }

    /**
     * Replaces the persistent cache contents with the supplied inventory snapshot and refreshes the
     * live placed chest when it is currently loaded on the authoritative host server.
     *
     * @param townUuid target town UUID
     * @param contents latest inventory contents
     * @return {@code true} when the cache contents were saved
     */
    public boolean saveCacheStorage(final @NotNull UUID townUuid, final ItemStack @Nullable [] contents) {
        final RTown town = this.getTown(townUuid);
        if (town == null || !this.isCacheUnlocked(town) || this.plugin.getTownRepository() == null) {
            return false;
        }
        town.setBankCacheContents(this.snapshotInventory(contents, this.getCacheSize()));
        this.plugin.getTownRepository().update(town);
        this.syncLiveCacheInventory(town);
        return true;
    }

    /**
     * Restores one sparse storage map into a chest inventory array.
     *
     * @param storedContents sparse storage contents
     * @param size target inventory size
     * @return full inventory array for the UI
     */
    public @NotNull ItemStack[] expandInventory(final @NotNull Map<String, ItemStack> storedContents, final int size) {
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
     * Captures a sparse slot map from one chest-style inventory.
     *
     * @param contents live inventory contents
     * @param maxSlots maximum number of slots to persist
     * @return sparse slot map keyed by slot index strings
     */
    public @NotNull Map<String, ItemStack> snapshotInventory(final ItemStack @Nullable [] contents, final int maxSlots) {
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
            snapshot.put(Integer.toString(slot), itemStack.clone());
        }
        return snapshot;
    }

    /**
     * Validates whether the supplied location can host the town cache chest.
     *
     * @param town target town
     * @param location requested cache location
     * @return {@code true} when the location is in an eligible level 4+ BANK chunk and within the
     *         configured radius of its chunk marker
     */
    public boolean isValidCachePlacement(final @NotNull RTown town, final @NotNull Location location) {
        return this.resolveEligibleCacheHostChunk(town, location) != null && !town.hasBankCacheLocation();
    }

    /**
     * Registers the placed cache chest binding for the town.
     *
     * @param townUuid target town UUID
     * @param cacheLocation placed cache chest location
     * @return {@code true} when the cache binding was persisted
     */
    public boolean registerCacheChest(final @NotNull UUID townUuid, final @NotNull Location cacheLocation) {
        final RTown town = this.getTown(townUuid);
        if (town == null || !this.isCacheUnlocked(town) || this.plugin.getTownRepository() == null) {
            return false;
        }
        if (!this.isValidCachePlacement(town, cacheLocation)) {
            return false;
        }

        town.setBankCacheLocation(cacheLocation);
        town.setBankCacheServerId(this.plugin.getServerRouteId());
        this.plugin.getTownRepository().update(town);
        this.syncLiveCacheInventory(town);
        return true;
    }

    /**
     * Returns the town that owns the cache chest placed at one exact world location.
     *
     * @param location candidate cache location
     * @return owning town, or {@code null} when the location is not the bound cache chest
     */
    public @Nullable RTown resolveCacheTown(final @Nullable Location location) {
        if (location == null || location.getWorld() == null) {
            return null;
        }

        final TownRuntimeService runtimeService = this.plugin.getTownRuntimeService();
        if (runtimeService == null) {
            return null;
        }

        for (final RTown town : runtimeService.getTowns()) {
            if (this.sameBlock(location, town.getBankCacheLocation())) {
                return town;
            }
        }
        return null;
    }

    /**
     * Restores the authoritative persisted cache contents into the live placed chest when it is
     * currently loaded on the cache host server.
     *
     * @param town target town
     */
    public void syncLiveCacheInventory(final @Nullable RTown town) {
        if (town == null || !this.hasPlacedCache(town) || !this.isCacheHostedLocally(town)) {
            return;
        }

        final Container cacheContainer = this.resolveLiveCacheContainer(town);
        if (cacheContainer == null) {
            return;
        }

        final Inventory inventory = cacheContainer.getInventory();
        inventory.clear();
        final ItemStack[] contents = this.expandInventory(town.getBankCacheContents(), inventory.getSize());
        for (int slot = 0; slot < inventory.getSize() && slot < contents.length; slot++) {
            inventory.setItem(slot, contents[slot] == null ? null : contents[slot].clone());
        }
        cacheContainer.update(true, false);
    }

    /**
     * Safely removes the placed cache chest and returns the bound item to the player while
     * preserving the persisted town cache contents.
     *
     * @param player player picking up the cache chest
     * @param townUuid target town UUID
     * @return structured pickup result
     */
    public @NotNull CachePickupResult pickupCacheChest(final @NotNull Player player, final @NotNull UUID townUuid) {
        final RTown town = this.getTown(townUuid);
        if (town == null || this.plugin.getTownRepository() == null || !this.isCacheUnlocked(town)) {
            return new CachePickupResult(CacheOperationStatus.INVALID_TARGET, false);
        }
        if (!this.canManageCachePlacement(player, town)) {
            return new CachePickupResult(CacheOperationStatus.NO_PERMISSION, false);
        }
        if (!town.hasBankCacheLocation()) {
            return new CachePickupResult(CacheOperationStatus.NOT_PLACED, false);
        }

        final Location cacheLocation = town.getBankCacheLocation();
        if (cacheLocation != null
            && cacheLocation.getWorld() != null
            && this.isCacheHostedLocally(town)
            && cacheLocation.getWorld().isChunkLoaded(
            TownRuntimeService.toChunkCoordinate(cacheLocation.getBlockX()),
            TownRuntimeService.toChunkCoordinate(cacheLocation.getBlockZ())
        )) {
            cacheLocation.getBlock().setType(Material.AIR, false);
        }

        town.clearBankCachePlacement();
        this.plugin.getTownRepository().update(town);
        this.giveItemOrDrop(player, CacheChest.getCacheChestItem(this.plugin, player, town.getTownUUID(), town.getTownName()));
        return new CachePickupResult(CacheOperationStatus.SUCCESS, false);
    }

    /**
     * Drops the cache contents and bound chest item at the placed cache location when the host BANK
     * plot is changed away from BANK or unclaimed.
     *
     * @param removedBankChunk BANK chunk being removed or retargeted
     * @return {@code true} when the cache binding was cleared
     */
    public boolean clearCacheForHostLoss(final @NotNull RTownChunk removedBankChunk) {
        final RTown town = this.getTown(removedBankChunk.getTown().getTownUUID());
        if (town == null || !town.hasBankCacheLocation() || this.plugin.getTownRepository() == null) {
            return false;
        }

        final Location cacheLocation = town.getBankCacheLocation();
        if (cacheLocation == null
            || cacheLocation.getWorld() == null
            || !Objects.equals(cacheLocation.getWorld().getName(), removedBankChunk.getWorldName())
            || TownRuntimeService.toChunkCoordinate(cacheLocation.getBlockX()) != removedBankChunk.getX()
            || TownRuntimeService.toChunkCoordinate(cacheLocation.getBlockZ()) != removedBankChunk.getZ()) {
            return false;
        }

        final World world = cacheLocation.getWorld();
        for (final ItemStack itemStack : town.getBankCacheContents().values()) {
            if (itemStack == null || itemStack.isEmpty()) {
                continue;
            }
            world.dropItemNaturally(cacheLocation, itemStack.clone());
        }
        world.dropItemNaturally(
            cacheLocation,
            CacheChest.createUnlocalizedBoundItem(this.plugin, town.getTownUUID(), town.getTownName())
        );
        if (this.isCacheHostedLocally(town) && cacheLocation.getBlock().getType() != Material.AIR) {
            cacheLocation.getBlock().setType(Material.AIR, false);
        }

        town.clearBankCacheState();
        this.plugin.getTownRepository().update(town);
        return true;
    }

    /**
     * Returns whether reaching the new BANK level should grant the town its one bound cache chest
     * item.
     *
     * @param town owning town
     * @param previousChunkLevel chunk level before the change
     * @param newChunkLevel chunk level after the change
     * @return {@code true} when the town just crossed the configured cache unlock threshold
     */
    public boolean shouldGrantCacheChestOnLevelGain(
        final @NotNull RTown town,
        final int previousChunkLevel,
        final int newChunkLevel
    ) {
        final int unlockLevel = this.bankConfig().getCache().unlockLevel();
        final int previousTownMax = town.getChunks().stream()
            .filter(chunk -> chunk.getChunkType() == ChunkType.BANK)
            .mapToInt(chunk -> chunk.getChunkLevel() == newChunkLevel ? previousChunkLevel : chunk.getChunkLevel())
            .max()
            .orElse(0);
        final int newTownMax = town.getChunks().stream()
            .filter(chunk -> chunk.getChunkType() == ChunkType.BANK)
            .mapToInt(RTownChunk::getChunkLevel)
            .max()
            .orElse(0);
        return previousTownMax < unlockLevel && newTownMax >= unlockLevel;
    }

    /**
     * Returns whether changing a chunk into a BANK plot at its current level should grant the
     * town's bound cache chest item.
     *
     * @param town owning town
     * @param enteringBankLevel level carried by the new BANK chunk
     * @return {@code true} when the town just unlocked cache access through this BANK chunk
     */
    public boolean shouldGrantCacheChestOnBankEntry(final @NotNull RTown town, final int enteringBankLevel) {
        final int unlockLevel = this.bankConfig().getCache().unlockLevel();
        final int previousTownMax = town.getChunks().stream()
            .filter(chunk -> chunk.getChunkType() == ChunkType.BANK)
            .mapToInt(RTownChunk::getChunkLevel)
            .max()
            .orElse(0);
        final int newTownMax = Math.max(previousTownMax, enteringBankLevel);
        return previousTownMax < unlockLevel && newTownMax >= unlockLevel;
    }

    /**
     * Gives the one bound cache chest item to the player.
     *
     * @param player receiving player
     * @param town owning town
     * @return {@code true} when the item was delivered or dropped
     */
    public boolean giveCacheChest(final @NotNull Player player, final @NotNull RTown town) {
        this.giveItemOrDrop(player, CacheChest.getCacheChestItem(this.plugin, player, town.getTownUUID(), town.getTownName()));
        return true;
    }

    /**
     * Starts one deposit-only remote cache session for level 5 {@code /rt bank} access.
     *
     * @param player player opening the deposit view
     * @param townUuid target town UUID
     * @return resolved session-start outcome
     */
    public @NotNull RemoteCacheSessionStartResult openRemoteCacheDepositSession(
        final @NotNull Player player,
        final @NotNull UUID townUuid
    ) {
        final RTown town = this.getTown(townUuid);
        if (town == null || !this.supportsRemoteCacheDeposit(town) || !this.hasPlacedCache(town)) {
            return new RemoteCacheSessionStartResult(CacheOperationStatus.INVALID_TARGET, false, "");
        }

        final String hostServerId = this.resolveCacheHostServerId(town);
        if (Objects.equals(hostServerId, this.plugin.getServerRouteId())) {
            final boolean acquired = this.acquireBankAccess(townUuid, player.getUniqueId());
            return new RemoteCacheSessionStartResult(acquired ? CacheOperationStatus.SUCCESS : CacheOperationStatus.LOCKED, false, hostServerId);
        }
        if (!this.plugin.getProxyService().isAvailable()) {
            return new RemoteCacheSessionStartResult(CacheOperationStatus.FAILED, true, hostServerId);
        }

        final ProxyActionResult result = this.plugin.getProxyService().sendAction(new ProxyActionEnvelope(
            UUID.randomUUID(),
            this.plugin.getProxyService().protocolVersion(),
            PROXY_MODULE_ID,
            ACTION_ACQUIRE_LOCK,
            player.getUniqueId(),
            this.plugin.getServerRouteId(),
            hostServerId,
            "",
            Map.of(PAYLOAD_TOWN_UUID, townUuid.toString()),
            System.currentTimeMillis()
        )).join();
        return new RemoteCacheSessionStartResult(result.success() ? CacheOperationStatus.SUCCESS : CacheOperationStatus.LOCKED, true, hostServerId);
    }

    /**
     * Merges one deposit-only cache inventory into the authoritative town cache and releases the
     * held local or remote lock.
     *
     * @param player closing player
     * @param townUuid target town UUID
     * @param contents deposit-buffer inventory contents
     * @param proxyBacked whether the host lock is held through proxy messaging
     * @param hostServerId authoritative cache host server route identifier
     * @return merge result including leftover items that did not fit in the cache
     */
    public @NotNull RemoteCacheDepositResult closeRemoteCacheDepositSession(
        final @NotNull Player player,
        final @NotNull UUID townUuid,
        final ItemStack @Nullable [] contents,
        final boolean proxyBacked,
        final @NotNull String hostServerId
    ) {
        final ItemStack[] depositContents = contents == null ? new ItemStack[this.getCacheSize()] : sanitizeContents(contents, this.getCacheSize());
        try {
            if (!proxyBacked || Objects.equals(hostServerId, this.plugin.getServerRouteId())) {
                final RTown town = this.getTown(townUuid);
                if (town == null) {
                    return new RemoteCacheDepositResult(CacheOperationStatus.INVALID_TARGET, depositContents);
                }
                final ItemStack[] leftovers = this.mergeIntoTownCache(town, depositContents);
                return new RemoteCacheDepositResult(CacheOperationStatus.SUCCESS, leftovers);
            }
            if (!this.plugin.getProxyService().isAvailable()) {
                return new RemoteCacheDepositResult(CacheOperationStatus.FAILED, depositContents);
            }

            final ProxyActionResult result = this.plugin.getProxyService().sendAction(new ProxyActionEnvelope(
                UUID.randomUUID(),
                this.plugin.getProxyService().protocolVersion(),
                PROXY_MODULE_ID,
                ACTION_APPLY_REMOTE_DEPOSIT,
                player.getUniqueId(),
                this.plugin.getServerRouteId(),
                hostServerId,
                "",
                Map.of(
                    PAYLOAD_TOWN_UUID, townUuid.toString(),
                    PAYLOAD_ITEM_ARRAY, this.itemStackSerializer.arrayToBase64(depositContents)
                ),
                System.currentTimeMillis()
            )).join();
            final ItemStack[] leftovers = result.success() && result.payload().containsKey(PAYLOAD_ITEM_ARRAY)
                ? this.deserializeItemArray(result.payload().get(PAYLOAD_ITEM_ARRAY), depositContents.length)
                : depositContents;
            return new RemoteCacheDepositResult(result.success() ? CacheOperationStatus.SUCCESS : CacheOperationStatus.FAILED, leftovers);
        } finally {
            if (!proxyBacked || Objects.equals(hostServerId, this.plugin.getServerRouteId())) {
                this.releaseBankAccess(townUuid, player.getUniqueId());
            } else if (this.plugin.getProxyService().isAvailable()) {
                this.plugin.getProxyService().sendAction(new ProxyActionEnvelope(
                    UUID.randomUUID(),
                    this.plugin.getProxyService().protocolVersion(),
                    PROXY_MODULE_ID,
                    ACTION_RELEASE_LOCK,
                    player.getUniqueId(),
                    this.plugin.getServerRouteId(),
                    hostServerId,
                    "",
                    Map.of(PAYLOAD_TOWN_UUID, townUuid.toString()),
                    System.currentTimeMillis()
                ));
            }
        }
    }

    private @NotNull CompletableFuture<ProxyActionResult> handleAcquireLock(final @NotNull ProxyActionEnvelope envelope) {
        final UUID townUuid = this.parseTownUuid(envelope.payload().get(PAYLOAD_TOWN_UUID));
        if (townUuid == null) {
            return CompletableFuture.completedFuture(ProxyActionResult.failure("invalid_payload", "Missing town UUID."));
        }
        return CompletableFuture.completedFuture(
            this.acquireBankAccess(townUuid, envelope.playerUuid())
                ? ProxyActionResult.success("Bank lock acquired.")
                : ProxyActionResult.failure("locked", "Town bank is already in use.")
        );
    }

    private @NotNull CompletableFuture<ProxyActionResult> handleReleaseLock(final @NotNull ProxyActionEnvelope envelope) {
        final UUID townUuid = this.parseTownUuid(envelope.payload().get(PAYLOAD_TOWN_UUID));
        if (townUuid == null) {
            return CompletableFuture.completedFuture(ProxyActionResult.failure("invalid_payload", "Missing town UUID."));
        }
        this.releaseBankAccess(townUuid, envelope.playerUuid());
        return CompletableFuture.completedFuture(ProxyActionResult.success("Bank lock released."));
    }

    private @NotNull CompletableFuture<ProxyActionResult> handleApplyRemoteDeposit(final @NotNull ProxyActionEnvelope envelope) {
        final UUID townUuid = this.parseTownUuid(envelope.payload().get(PAYLOAD_TOWN_UUID));
        if (townUuid == null || !envelope.payload().containsKey(PAYLOAD_ITEM_ARRAY)) {
            return CompletableFuture.completedFuture(
                ProxyActionResult.failure("invalid_payload", "Missing remote cache deposit payload.")
            );
        }

        final RTown town = this.getTown(townUuid);
        if (town == null || !this.hasPlacedCache(town)) {
            return CompletableFuture.completedFuture(
                ProxyActionResult.failure("invalid_target", "Town cache is unavailable.")
            );
        }

        final ItemStack[] depositedContents = this.deserializeItemArray(envelope.payload().get(PAYLOAD_ITEM_ARRAY), this.getCacheSize());
        final ItemStack[] leftovers = this.mergeIntoTownCache(town, depositedContents);
        return CompletableFuture.completedFuture(
            ProxyActionResult.success(
                "Remote cache deposit applied.",
                Map.of(PAYLOAD_ITEM_ARRAY, this.itemStackSerializer.arrayToBase64(leftovers))
            )
        );
    }

    private @NotNull ItemStack[] mergeIntoTownCache(final @NotNull RTown town, final ItemStack @NotNull [] depositedContents) {
        final ItemStack[] cacheContents = this.expandInventory(town.getBankCacheContents(), this.getCacheSize());
        final ItemStack[] leftovers = new ItemStack[depositedContents.length];

        for (int slot = 0; slot < depositedContents.length; slot++) {
            ItemStack remaining = depositedContents[slot] == null ? null : depositedContents[slot].clone();
            if (remaining == null || remaining.isEmpty()) {
                continue;
            }

            remaining = mergeIntoSimilarStacks(cacheContents, remaining);
            remaining = placeIntoEmptySlot(cacheContents, remaining);
            leftovers[slot] = remaining == null || remaining.isEmpty() ? null : remaining;
        }

        town.setBankCacheContents(this.snapshotInventory(cacheContents, cacheContents.length));
        if (this.plugin.getTownRepository() != null) {
            this.plugin.getTownRepository().update(town);
        }
        this.syncLiveCacheInventory(town);
        return leftovers;
    }

    private static @Nullable ItemStack mergeIntoSimilarStacks(final ItemStack @NotNull [] targetContents, final @Nullable ItemStack source) {
        if (source == null || source.isEmpty()) {
            return null;
        }

        ItemStack remaining = source.clone();
        for (final ItemStack existing : targetContents) {
            if (existing == null || existing.isEmpty() || !existing.isSimilar(remaining)) {
                continue;
            }

            final int maxStackSize = Math.max(existing.getMaxStackSize(), remaining.getMaxStackSize());
            final int transferable = Math.min(maxStackSize - existing.getAmount(), remaining.getAmount());
            if (transferable <= 0) {
                continue;
            }
            existing.setAmount(existing.getAmount() + transferable);
            remaining.setAmount(remaining.getAmount() - transferable);
            if (remaining.getAmount() <= 0) {
                return null;
            }
        }
        return remaining;
    }

    private static @Nullable ItemStack placeIntoEmptySlot(final ItemStack @NotNull [] targetContents, final @Nullable ItemStack source) {
        if (source == null || source.isEmpty()) {
            return null;
        }

        ItemStack remaining = source.clone();
        for (int slot = 0; slot < targetContents.length; slot++) {
            if (targetContents[slot] != null && !targetContents[slot].isEmpty()) {
                continue;
            }

            final ItemStack placed = remaining.clone();
            final int maxStackSize = Math.max(1, placed.getMaxStackSize());
            final int amount = Math.min(maxStackSize, remaining.getAmount());
            placed.setAmount(amount);
            targetContents[slot] = placed;
            remaining.setAmount(remaining.getAmount() - amount);
            if (remaining.getAmount() <= 0) {
                return null;
            }
        }
        return remaining;
    }

    private @Nullable RTownChunk resolveEligibleCacheHostChunk(final @NotNull RTown town, final @NotNull Location cacheLocation) {
        if (cacheLocation.getWorld() == null) {
            return null;
        }

        final int unlockLevel = this.bankConfig().getCache().unlockLevel();
        final int radius = this.bankConfig().getCache().placementRadiusBlocks();
        for (final RTownChunk townChunk : town.getChunks()) {
            if (townChunk.getChunkType() != ChunkType.BANK || townChunk.getChunkLevel() < unlockLevel) {
                continue;
            }
            if (!Objects.equals(townChunk.getWorldName(), cacheLocation.getWorld().getName())
                || TownRuntimeService.toChunkCoordinate(cacheLocation.getBlockX()) != townChunk.getX()
                || TownRuntimeService.toChunkCoordinate(cacheLocation.getBlockZ()) != townChunk.getZ()) {
                continue;
            }

            final Location markerLocation = townChunk.getChunkBlockLocation();
            if (markerLocation == null
                || markerLocation.getWorld() == null
                || !Objects.equals(markerLocation.getWorld().getName(), cacheLocation.getWorld().getName())) {
                continue;
            }
            if (markerLocation.distanceSquared(cacheLocation) <= (double) radius * (double) radius) {
                return townChunk;
            }
        }
        return null;
    }

    private @Nullable Container resolveLiveCacheContainer(final @NotNull RTown town) {
        final Location cacheLocation = town.getBankCacheLocation();
        if (cacheLocation == null || cacheLocation.getWorld() == null) {
            return null;
        }
        final int chunkX = TownRuntimeService.toChunkCoordinate(cacheLocation.getBlockX());
        final int chunkZ = TownRuntimeService.toChunkCoordinate(cacheLocation.getBlockZ());
        if (!cacheLocation.getWorld().isChunkLoaded(chunkX, chunkZ)) {
            return null;
        }
        if (!(cacheLocation.getBlock().getState() instanceof Container cacheContainer)
            || !CacheChest.isPlacedCacheChest(this.plugin, cacheContainer)) {
            return null;
        }
        return cacheContainer;
    }

    private boolean isCacheHostedLocally(final @NotNull RTown town) {
        return Objects.equals(this.resolveCacheHostServerId(town), this.plugin.getServerRouteId());
    }

    private @NotNull String resolveCacheHostServerId(final @NotNull RTown town) {
        final String configuredServerId = town.getBankCacheServerId();
        return configuredServerId == null || configuredServerId.isBlank()
            ? this.plugin.getServerRouteId()
            : configuredServerId.trim();
    }

    private void giveItemOrDrop(final @NotNull Player player, final @NotNull ItemStack itemStack) {
        final Map<Integer, ItemStack> leftovers = player.getInventory().addItem(itemStack);
        for (final ItemStack leftover : leftovers.values()) {
            if (leftover == null || leftover.isEmpty()) {
                continue;
            }
            player.getWorld().dropItemNaturally(player.getLocation(), leftover);
        }
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

    private @Nullable UUID parseTownUuid(final @Nullable String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(value.trim());
        } catch (final IllegalArgumentException exception) {
            return null;
        }
    }

    private ItemStack @NotNull [] deserializeItemArray(final @Nullable String base64, final int expectedSize) {
        if (base64 == null || base64.isBlank()) {
            return new ItemStack[Math.max(9, expectedSize)];
        }
        try {
            return sanitizeContents(this.itemStackSerializer.arrayFromBase64(base64), expectedSize);
        } catch (final Exception exception) {
            this.plugin.getLogger().log(Level.WARNING, "Failed to deserialize remote cache deposit payload.", exception);
            return new ItemStack[Math.max(9, expectedSize)];
        }
    }

    private static ItemStack @NotNull [] sanitizeContents(final ItemStack @Nullable [] contents, final int expectedSize) {
        final ItemStack[] sanitized = new ItemStack[Math.max(9, expectedSize)];
        if (contents == null) {
            return sanitized;
        }
        final int slotLimit = Math.min(contents.length, sanitized.length);
        for (int slot = 0; slot < slotLimit; slot++) {
            sanitized[slot] = contents[slot] == null || contents[slot].isEmpty() ? null : contents[slot].clone();
        }
        return sanitized;
    }

    private boolean sameBlock(final @Nullable Location first, final @Nullable Location second) {
        return first != null
            && second != null
            && first.getWorld() != null
            && second.getWorld() != null
            && Objects.equals(first.getWorld().getName(), second.getWorld().getName())
            && first.getBlockX() == second.getBlockX()
            && first.getBlockY() == second.getBlockY()
            && first.getBlockZ() == second.getBlockZ();
    }

    private @NotNull BankConfigSection bankConfig() {
        return this.plugin.getBankConfig();
    }

    /**
     * Resolved access outcome for {@code /rt bank}.
     *
     * @param status resolved access status or resulting mode
     * @param town resolved town, when available
     * @param localChunk resolved own claim chunk for local access, when applicable
     */
    public record BankCommandAccess(
        @NotNull BankCommandStatus status,
        @Nullable RTown town,
        @Nullable RTownChunk localChunk
    ) {

        /**
         * Returns whether the command may open a bank UI right now.
         *
         * @return {@code true} when the command resolves to a concrete bank access mode
         */
        public boolean allowed() {
            return this.status == BankCommandStatus.LOCAL_BANK || this.status == BankCommandStatus.REMOTE_CACHE_DEPOSIT;
        }
    }

    /**
     * Stable command-access outcomes for {@code /rt bank}.
     */
    public enum BankCommandStatus {
        INVALID_TARGET,
        NO_TOWN,
        NO_VIEW_PERMISSION,
        NO_REMOTE_PERMISSION,
        LOCKED,
        LOCATION_REQUIRED,
        CACHE_UNAVAILABLE,
        LOCAL_BANK,
        REMOTE_CACHE_DEPOSIT
    }

    /**
     * Stable outcomes for town-bank currency transactions.
     */
    public enum CurrencyTransactionStatus {
        SUCCESS,
        INVALID_TARGET,
        NO_PERMISSION,
        INVALID_AMOUNT,
        INVALID_CURRENCY,
        NOT_ENOUGH_RESOURCES,
        FAILED
    }

    /**
     * Result for one town-bank currency deposit or withdrawal action.
     *
     * @param status resolved transaction outcome
     * @param currencyId resolved currency identifier
     * @param amount processed amount
     * @param newTownBalance new stored town balance
     */
    public record CurrencyTransactionResult(
        @NotNull CurrencyTransactionStatus status,
        @NotNull String currencyId,
        double amount,
        double newTownBalance
    ) {
    }

    /**
     * Stable outcomes for cache placement, pickup, and remote deposit actions.
     */
    public enum CacheOperationStatus {
        SUCCESS,
        INVALID_TARGET,
        NO_PERMISSION,
        NOT_PLACED,
        LOCKED,
        FAILED
    }

    /**
     * Result for safely picking up one placed cache chest.
     *
     * @param status resolved pickup outcome
     * @param droppedContents whether contents were dropped during the pickup flow
     */
    public record CachePickupResult(@NotNull CacheOperationStatus status, boolean droppedContents) {
    }

    /**
     * Result for opening one deposit-only remote cache session.
     *
     * @param status resolved open outcome
     * @param proxyBacked whether the host lock lives on another server
     * @param hostServerId authoritative cache host server identifier
     */
    public record RemoteCacheSessionStartResult(
        @NotNull CacheOperationStatus status,
        boolean proxyBacked,
        @NotNull String hostServerId
    ) {

        /**
         * Returns whether the session lock was acquired and the deposit view may open.
         *
         * @return {@code true} when the remote deposit view may proceed
         */
        public boolean opened() {
            return this.status == CacheOperationStatus.SUCCESS;
        }
    }

    /**
     * Result for closing one deposit-only remote cache session.
     *
     * @param status resolved close outcome
     * @param leftoverContents items that could not fit in the authoritative cache
     */
    public record RemoteCacheDepositResult(
        @NotNull CacheOperationStatus status,
        ItemStack @NotNull [] leftoverContents
    ) {
    }
}
