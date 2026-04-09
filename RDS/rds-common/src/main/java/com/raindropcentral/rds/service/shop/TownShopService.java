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
import com.raindropcentral.rds.database.entity.Bank;
import com.raindropcentral.rds.database.entity.Shop;
import com.raindropcentral.rds.database.entity.ShopLedgerEntry;
import com.raindropcentral.rds.database.entity.TownShopOutpost;
import com.raindropcentral.rds.items.AbstractItem;
import com.raindropcentral.rds.items.ShopItem;
import com.raindropcentral.rds.items.TownShopBlock;
import com.raindropcentral.rplatform.protection.RProtectionBridge;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Coordinates town-owned Outpost shop capacity, access, and sale-tax behavior.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public class TownShopService {

    /** Permission key allowing players to stock town-owned shops. */
    public static final String SUPPLY_PERMISSION_KEY = "SUPPLY_TOWN_SHOPS";
    /** Permission key allowing players to manage town-owned shops. */
    public static final String MANAGE_PERMISSION_KEY = "MANAGE_TOWN_SHOPS";

    private final RDS plugin;

    /**
     * Creates the town-shop runtime service.
     *
     * @param plugin owning plugin runtime
     */
    public TownShopService(final @NotNull RDS plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    /**
     * Resolves one tracked outpost row by chunk UUID.
     *
     * @param chunkUuid outpost chunk UUID
     * @return tracked outpost row, or {@code null} when none exists
     */
    public @Nullable TownShopOutpost getOutpost(final @NotNull UUID chunkUuid) {
        return this.plugin.getTownShopOutpostRepository() == null
            ? null
            : this.plugin.getTownShopOutpostRepository().findByChunkUuid(chunkUuid);
    }

    /**
     * Resolves one tracked outpost row by shop location.
     *
     * @param location location to inspect
     * @return tracked outpost row, or {@code null} when none exists
     */
    public @Nullable TownShopOutpost getOutpost(final @Nullable Location location) {
        if (location == null || location.getWorld() == null || this.plugin.getTownShopOutpostRepository() == null) {
            return null;
        }
        return this.plugin.getTownShopOutpostRepository().findByChunkLocation(
            location.getWorld().getName(),
            location.getChunk().getX(),
            location.getChunk().getZ()
        );
    }

    /**
     * Upserts one tracked outpost row and force-closes any excess bound town shops.
     *
     * @param protectionPlugin source protection plugin id
     * @param townIdentifier stable town identifier
     * @param townDisplayName town display name
     * @param chunkUuid outpost chunk UUID
     * @param worldName world name
     * @param chunkX chunk x
     * @param chunkZ chunk z
     * @param chunkLevel current outpost level
     * @return synced outpost row, or {@code null} when repositories are unavailable
     */
    public @Nullable TownShopOutpost syncOutpost(
        final @NotNull String protectionPlugin,
        final @NotNull String townIdentifier,
        final @NotNull String townDisplayName,
        final @NotNull UUID chunkUuid,
        final @NotNull String worldName,
        final int chunkX,
        final int chunkZ,
        final int chunkLevel
    ) {
        if (this.plugin.getTownShopOutpostRepository() == null) {
            return null;
        }

        final TownShopOutpost existing = this.plugin.getTownShopOutpostRepository().findByChunkUuid(chunkUuid);
        final TownShopOutpost outpost;
        if (existing == null) {
            outpost = new TownShopOutpost(
                protectionPlugin,
                townIdentifier,
                townDisplayName,
                chunkUuid,
                worldName,
                chunkX,
                chunkZ,
                chunkLevel
            );
            this.plugin.getTownShopOutpostRepository().create(outpost);
        } else {
            existing.sync(townDisplayName, worldName, chunkX, chunkZ, chunkLevel);
            this.plugin.getTownShopOutpostRepository().update(existing);
            outpost = existing;
        }

        this.forceCloseExcessTownShops(outpost);
        return outpost;
    }

    /**
     * Removes one tracked outpost row and force-closes every bound town shop.
     *
     * @param chunkUuid outpost chunk UUID
     * @return number of force-closed town shops
     */
    public int removeOutpost(final @NotNull UUID chunkUuid) {
        if (this.plugin.getTownShopOutpostRepository() == null) {
            return 0;
        }

        final TownShopOutpost outpost = this.plugin.getTownShopOutpostRepository().findByChunkUuid(chunkUuid);
        if (outpost == null) {
            return 0;
        }

        final int removedShops = this.forceCloseTownShops(outpost, Integer.MAX_VALUE);
        this.plugin.getTownShopOutpostRepository().deleteEntity(outpost);
        return removedShops;
    }

    /**
     * Grants newly unlocked level-based town shops for one outpost level reward.
     *
     * @param target target player
     * @param protectionPlugin source protection plugin id
     * @param townIdentifier stable town identifier
     * @param townDisplayName town display name
     * @param chunkUuid outpost chunk UUID
     * @param worldName world name
     * @param chunkX chunk x
     * @param chunkZ chunk z
     * @param chunkLevel target outpost level
     * @return number of town-shop tokens granted
     */
    public int grantLevelReward(
        final @NotNull Player target,
        final @NotNull String protectionPlugin,
        final @NotNull String townIdentifier,
        final @NotNull String townDisplayName,
        final @NotNull UUID chunkUuid,
        final @NotNull String worldName,
        final int chunkX,
        final int chunkZ,
        final int chunkLevel
    ) {
        final TownShopOutpost previous = this.getOutpost(chunkUuid);
        final int previousBaseCapacity = previous == null ? 0 : previous.getBaseShopCapacity();
        final TownShopOutpost outpost = this.syncOutpost(
            protectionPlugin,
            townIdentifier,
            townDisplayName,
            chunkUuid,
            worldName,
            chunkX,
            chunkZ,
            chunkLevel
        );
        if (outpost == null) {
            return 0;
        }

        final int unlockedAmount = Math.max(0, outpost.getBaseShopCapacity() - previousBaseCapacity);
        this.giveTownShopBlocks(target, outpost, unlockedAmount);
        return unlockedAmount;
    }

    /**
     * Grants additional admin-configured town shops for one outpost.
     *
     * @param target target player
     * @param protectionPlugin source protection plugin id
     * @param townIdentifier stable town identifier
     * @param townDisplayName town display name
     * @param chunkUuid outpost chunk UUID
     * @param worldName world name
     * @param chunkX chunk x
     * @param chunkZ chunk z
     * @param chunkLevel current outpost level
     * @param amount number of bonus town shops to grant
     * @return number of town-shop tokens granted
     */
    public int grantBonusCapacity(
        final @NotNull Player target,
        final @NotNull String protectionPlugin,
        final @NotNull String townIdentifier,
        final @NotNull String townDisplayName,
        final @NotNull UUID chunkUuid,
        final @NotNull String worldName,
        final int chunkX,
        final int chunkZ,
        final int chunkLevel,
        final int amount
    ) {
        if (amount <= 0 || this.plugin.getTownShopOutpostRepository() == null) {
            return 0;
        }

        final TownShopOutpost outpost = this.syncOutpost(
            protectionPlugin,
            townIdentifier,
            townDisplayName,
            chunkUuid,
            worldName,
            chunkX,
            chunkZ,
            chunkLevel
        );
        if (outpost == null) {
            return 0;
        }

        outpost.addBonusShopCapacity(amount);
        this.plugin.getTownShopOutpostRepository().update(outpost);
        this.giveTownShopBlocks(target, outpost, amount);
        return amount;
    }

    /**
     * Reissues one town-shop token when the bound outpost still has unused capacity.
     *
     * @param target target player
     * @param outpost bound outpost row
     * @return {@code true} when a token was granted
     */
    public boolean claimTownShopToken(final @NotNull Player target, final @NotNull TownShopOutpost outpost) {
        if (!this.canManageTownShops(target, outpost)) {
            return false;
        }
        if (!outpost.matchesLocation(target.getLocation())) {
            return false;
        }
        if (this.getAvailablePlacementCapacity(outpost) <= 0) {
            return false;
        }

        this.giveTownShopBlocks(target, outpost, 1);
        return true;
    }

    /**
     * Returns whether the player may stock the supplied shop.
     *
     * @param player player to inspect
     * @param shop shop to inspect
     * @return {@code true} when the player may stock the supplied shop
     */
    public boolean canSupplyShop(final @NotNull Player player, final @NotNull Shop shop) {
        if (!shop.isTownShop()) {
            return shop.canSupply(player.getUniqueId());
        }
        final TownShopOutpost outpost = shop.getOutpostChunkUuid() == null ? null : this.getOutpost(shop.getOutpostChunkUuid());
        return outpost != null && this.isTownPermissionGranted(player, outpost, SUPPLY_PERMISSION_KEY);
    }

    /**
     * Returns whether the player should open the owner overview instead of customer browsing.
     *
     * @param player viewing player
     * @param shop target shop
     * @return {@code true} when the player should be treated as staff for the shop
     */
    public boolean canAccessOverview(final @NotNull Player player, final @NotNull Shop shop) {
        if (!shop.isTownShop()) {
            return shop.canAccessOverview(player.getUniqueId());
        }

        return this.canSupplyShop(player, shop) || this.canManageShop(player, shop);
    }

    /**
     * Returns whether the player may fully manage the supplied shop.
     *
     * @param player player to inspect
     * @param shop shop to inspect
     * @return {@code true} when the player may fully manage the supplied shop
     */
    public boolean canManageShop(final @NotNull Player player, final @NotNull Shop shop) {
        if (!shop.isTownShop()) {
            return shop.canManage(player.getUniqueId());
        }
        final TownShopOutpost outpost = shop.getOutpostChunkUuid() == null ? null : this.getOutpost(shop.getOutpostChunkUuid());
        return outpost != null && this.isTownPermissionGranted(player, outpost, MANAGE_PERMISSION_KEY);
    }

    /**
     * Returns whether the player may close the supplied shop.
     *
     * @param player player to inspect
     * @param shop shop to inspect
     * @return {@code true} when the player may close the shop
     */
    public boolean canCloseShop(final @NotNull Player player, final @NotNull Shop shop) {
        return shop.isTownShop() ? this.canManageShop(player, shop) : shop.isOwner(player.getUniqueId());
    }

    /**
     * Resolves the tracked outpost row bound to a town shop.
     *
     * @param shop town-owned shop
     * @return matching outpost row, or {@code null} when unavailable
     */
    public @Nullable TownShopOutpost getOutpost(final @NotNull Shop shop) {
        if (!shop.isTownShop()) {
            return null;
        }

        if (shop.getOutpostChunkUuid() != null) {
            return this.getOutpost(shop.getOutpostChunkUuid());
        }

        return this.getOutpost(shop.getShopLocation());
    }

    /**
     * Returns a user-facing owner label for the supplied shop.
     *
     * @param shop shop to inspect
     * @return owner label for UIs and messages
     */
    public @NotNull String resolveOwnerName(final @NotNull Shop shop) {
        if (shop.isTownShop()) {
            final String townDisplayName = shop.getTownDisplayName();
            if (townDisplayName != null && !townDisplayName.isBlank()) {
                return townDisplayName;
            }
            final String townIdentifier = shop.getTownIdentifier();
            return townIdentifier == null || townIdentifier.isBlank() ? "Unknown Town" : townIdentifier;
        }

        final UUID ownerId = shop.getOwner();
        final OfflinePlayer owner = Bukkit.getOfflinePlayer(ownerId);
        return owner.getName() == null || owner.getName().isBlank()
            ? ownerId.toString()
            : owner.getName();
    }

    /**
     * Returns whether the supplied token may place or extend a town shop right now.
     *
     * @param player placing player
     * @param metadata parsed token metadata
     * @return {@code true} when the token is currently valid for placement
     */
    public boolean canPlaceTownShop(
        final @NotNull Player player,
        final @NotNull TownShopBlock.Metadata metadata
    ) {
        return this.canPlaceTownShop(player.getLocation(), player, metadata);
    }

    /**
     * Returns whether the supplied token may place or extend a town shop at one location.
     *
     * @param placementLocation placed chest location
     * @param player placing player
     * @param metadata parsed token metadata
     * @return {@code true} when the token is currently valid for placement
     */
    public boolean canPlaceTownShop(
        final @Nullable Location placementLocation,
        final @NotNull Player player,
        final @NotNull TownShopBlock.Metadata metadata
    ) {
        final TownShopOutpost outpost = this.getOutpost(metadata.chunkUuid());
        return outpost != null
            && this.matchesMetadata(outpost, metadata)
            && outpost.matchesLocation(placementLocation)
            && this.canManageTownShops(player, outpost)
            && this.getAvailablePlacementCapacity(outpost) > 0;
    }

    /**
     * Returns whether one bound token may extend an existing town shop into a double chest.
     *
     * @param player acting player
     * @param shop adjacent town-owned shop
     * @param metadata parsed token metadata
     * @return {@code true} when the token may extend the existing town shop
     */
    public boolean canExtendTownShop(
        final @NotNull Player player,
        final @NotNull Shop shop,
        final @NotNull TownShopBlock.Metadata metadata
    ) {
        return this.canExtendTownShop(player.getLocation(), player, shop, metadata);
    }

    /**
     * Returns whether one bound token may extend an existing town shop into a double chest.
     *
     * @param placementLocation placed secondary chest location
     * @param player acting player
     * @param shop adjacent town-owned shop
     * @param metadata parsed token metadata
     * @return {@code true} when the token may extend the existing town shop
     */
    public boolean canExtendTownShop(
        final @Nullable Location placementLocation,
        final @NotNull Player player,
        final @NotNull Shop shop,
        final @NotNull TownShopBlock.Metadata metadata
    ) {
        if (!shop.isTownShop() || shop.isDoubleChest()) {
            return false;
        }

        final TownShopOutpost outpost = this.getOutpost(shop);
        return outpost != null
            && this.matchesMetadata(outpost, metadata)
            && shop.isBoundToOutpost(outpost)
            && outpost.matchesLocation(placementLocation)
            && this.canManageTownShops(player, outpost);
    }

    /**
     * Applies outpost sale tax for one successful non-admin shop sale.
     *
     * @param shop sold shop
     * @param currencyType sale currency id
     * @param grossAmount total charged sale amount before tax
     * @return tax result, or {@code null} when no outpost sale tax applied
     */
    public @Nullable SaleTaxResult applySaleTax(
        final @NotNull Shop shop,
        final @NotNull String currencyType,
        final double grossAmount
    ) {
        if (shop.isAdminShop() || grossAmount <= 0.0D) {
            return null;
        }

        final TownShopOutpost outpost = this.getOutpost(shop.getShopLocation());
        if (outpost == null) {
            return null;
        }

        final double taxRate = resolveSaleTaxRate(outpost.getChunkLevel());
        if (taxRate <= 0.0D) {
            return null;
        }

        final double taxAmount = Math.max(0.0D, grossAmount * taxRate);
        if (taxAmount <= 1.0E-6D) {
            return null;
        }

        final RProtectionBridge protectionBridge = resolveBridge(outpost);
        if (protectionBridge == null || !protectionBridge.depositToTownBank(outpost.getTownIdentifier(), currencyType, taxAmount)) {
            return null;
        }

        shop.addLedgerEntry(ShopLedgerEntry.outpostTax(
            shop,
            parseUuid(outpost.getTownIdentifier()),
            outpost.getTownDisplayName(),
            currencyType,
            taxAmount
        ));
        return new SaleTaxResult(outpost, taxRate, taxAmount);
    }

    /**
     * Returns the current available placement capacity for one outpost.
     *
     * @param outpost outpost row
     * @return currently available placement capacity
     */
    public int getAvailablePlacementCapacity(final @NotNull TownShopOutpost outpost) {
        return Math.max(0, outpost.getTotalShopCapacity() - this.countPlacedTownShops(outpost));
    }

    /**
     * Force-closes any bound town shops above the current capacity.
     *
     * @param outpost tracked outpost row
     * @return number of force-closed town shops
     */
    public int forceCloseExcessTownShops(final @NotNull TownShopOutpost outpost) {
        final int excess = this.countPlacedTownShops(outpost) - outpost.getTotalShopCapacity();
        return excess <= 0 ? 0 : this.forceCloseTownShops(outpost, excess);
    }

    /**
     * Force-closes one town-owned shop through the outpost cleanup path.
     *
     * @param shop town-owned shop to remove
     * @return {@code true} when the shop was removed
     */
    public boolean forceCloseTownShop(final @NotNull Shop shop) {
        if (!shop.isTownShop()) {
            return false;
        }

        this.forceCloseTownShop(shop, this.getOutpost(shop));
        return true;
    }

    /**
     * Reissues one token for the supplied chunk UUID when capacity is still available.
     *
     * @param target target player
     * @param chunkUuid outpost chunk UUID
     * @return {@code true} when a token was reissued
     */
    public boolean claimTownShopToken(final @NotNull Player target, final @NotNull UUID chunkUuid) {
        final TownShopOutpost outpost = this.getOutpost(chunkUuid);
        return outpost != null && this.claimTownShopToken(target, outpost);
    }

    /**
     * Resolves the live RDT outpost metadata for the supplied player location.
     *
     * @param player player standing in an outpost
     * @return resolved outpost metadata, or {@code null} when the player is not in an RDT outpost
     */
    public @Nullable LiveOutpostContext resolveLiveOutpost(final @NotNull Player player) {
        final RProtectionBridge protectionBridge = RProtectionBridge.getBridge();
        if (protectionBridge == null
            || !protectionBridge.isAvailable()
            || !"RDT".equalsIgnoreCase(protectionBridge.getPluginName())) {
            return null;
        }

        final Plugin plugin = Bukkit.getPluginManager().getPlugin("RDT");
        if (plugin == null || !plugin.isEnabled()) {
            return null;
        }

        try {
            final Object runtime = this.resolveRdtRuntime(plugin);
            if (runtime == null) {
                return null;
            }

            final Object townRuntimeService = this.invokeOptional(runtime, "getTownRuntimeService");
            if (townRuntimeService == null) {
                return null;
            }

            final Object townChunk = this.invokeOptional(townRuntimeService, "getChunkAt", player.getLocation());
            if (townChunk == null) {
                return null;
            }

            final Object chunkType = this.invokeOptional(townChunk, "getChunkType");
            if (chunkType == null || !"OUTPOST".equalsIgnoreCase(String.valueOf(chunkType))) {
                return null;
            }

            final Object identifier = this.invokeOptional(townChunk, "getIdentifier");
            final Object worldName = this.invokeOptional(townChunk, "getWorldName");
            final Object chunkX = this.invokeOptional(townChunk, "getX");
            final Object chunkZ = this.invokeOptional(townChunk, "getZ");
            final Object chunkLevel = this.invokeOptional(townChunk, "getChunkLevel");
            if (!(identifier instanceof UUID chunkUuid)
                || !(worldName instanceof String resolvedWorldName)
                || !(chunkX instanceof Number resolvedChunkX)
                || !(chunkZ instanceof Number resolvedChunkZ)
                || !(chunkLevel instanceof Number resolvedChunkLevel)) {
                return null;
            }

            String townIdentifier = protectionBridge.getPlayerTownIdentifier(player);
            String townDisplayName = this.invokeBridgeString(protectionBridge, "getPlayerTownDisplayName", player);
            final Object town = this.invokeOptional(townChunk, "getTown");
            if ((townIdentifier == null || townIdentifier.isBlank()) && town != null) {
                final Object townUuid = this.invokeOptional(town, "getTownUUID");
                if (townUuid instanceof UUID resolvedTownUuid) {
                    townIdentifier = resolvedTownUuid.toString();
                }
            }
            if ((townDisplayName == null || townDisplayName.isBlank()) && town != null) {
                final Object townName = this.invokeOptional(town, "getTownName");
                if (townName instanceof String resolvedTownName) {
                    townDisplayName = resolvedTownName;
                }
            }
            if (townIdentifier == null || townIdentifier.isBlank()) {
                return null;
            }
            if (townDisplayName == null || townDisplayName.isBlank()) {
                townDisplayName = townIdentifier;
            }

            return new LiveOutpostContext(
                protectionBridge.getPluginName(),
                townIdentifier,
                townDisplayName,
                chunkUuid,
                resolvedWorldName,
                resolvedChunkX.intValue(),
                resolvedChunkZ.intValue(),
                resolvedChunkLevel.intValue()
            );
        } catch (final ReflectiveOperationException ignored) {
            return null;
        }
    }

    private int countPlacedTownShops(final @NotNull TownShopOutpost outpost) {
        if (this.plugin.getShopRepository() == null) {
            return 0;
        }

        int count = 0;
        for (final Shop shop : this.plugin.getShopRepository().findAllShops()) {
            if (shop != null && shop.isTownShop() && shop.isBoundToOutpost(outpost)) {
                count++;
            }
        }
        return count;
    }

    private int forceCloseTownShops(final @NotNull TownShopOutpost outpost, final int limit) {
        if (this.plugin.getShopRepository() == null || limit <= 0) {
            return 0;
        }

        final List<Shop> townShops = new ArrayList<>();
        for (final Shop shop : this.plugin.getShopRepository().findAllShops()) {
            if (shop != null && shop.isTownShop() && shop.isBoundToOutpost(outpost)) {
                townShops.add(shop);
            }
        }

        townShops.sort(Comparator.comparing(Shop::getId, Comparator.nullsLast(Long::compareTo)).reversed());
        int closed = 0;
        for (final Shop shop : townShops) {
            if (closed >= limit) {
                break;
            }
            this.forceCloseTownShop(shop, outpost);
            closed++;
        }
        return closed;
    }

    private void forceCloseTownShop(final @NotNull Shop shop, final @Nullable TownShopOutpost outpost) {
        this.closeRelatedViews(shop);
        if (outpost != null) {
            this.transferShopBankToTown(shop, outpost);
        }
        this.dropShopStock(shop);
        if (this.plugin.getShopRepository() != null) {
            this.plugin.getShopRepository().deleteEntity(shop);
        }
        this.clearShopBlock(shop.getShopLocation());
        this.clearShopBlock(shop.getSecondaryShopLocation());
    }

    private void transferShopBankToTown(final @NotNull Shop shop, final @NotNull TownShopOutpost outpost) {
        final RProtectionBridge protectionBridge = resolveBridge(outpost);
        if (protectionBridge == null) {
            return;
        }

        for (final Bank bankEntry : shop.getBankEntries()) {
            if (bankEntry == null || bankEntry.getAmount() <= 0.0D) {
                continue;
            }
            protectionBridge.depositToTownBank(
                outpost.getTownIdentifier(),
                bankEntry.getCurrencyType(),
                bankEntry.getAmount()
            );
        }
    }

    private void dropShopStock(final @NotNull Shop shop) {
        final Location dropLocation = shop.getShopLocation();
        if (dropLocation == null || dropLocation.getWorld() == null) {
            return;
        }

        for (final AbstractItem item : shop.getItems()) {
            if (!(item instanceof ShopItem shopItem) || shopItem.getAmount() <= 0) {
                continue;
            }

            int remaining = shopItem.getAmount();
            while (remaining > 0) {
                final ItemStack stack = shopItem.getItem();
                final int stackAmount = Math.min(remaining, stack.getMaxStackSize());
                stack.setAmount(stackAmount);
                dropLocation.getWorld().dropItem(dropLocation.clone().add(0.5D, 0.5D, 0.5D), stack);
                remaining -= stackAmount;
            }
        }
    }

    private void closeRelatedViews(final @NotNull Shop shop) {
        if (this.plugin.getViewFrame() == null) {
            return;
        }

        for (final Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            final var viewer = this.plugin.getViewFrame().getViewer(onlinePlayer);
            if (viewer == null || viewer.getCurrentContext() == null || !(viewer.getCurrentContext().getInitialData() instanceof Map<?, ?> data)) {
                continue;
            }

            final Object locationObject = data.get("shopLocation");
            if (!(locationObject instanceof Location viewedLocation)) {
                continue;
            }

            if (Objects.equals(shop.getShopLocation(), viewedLocation) || Objects.equals(shop.getSecondaryShopLocation(), viewedLocation)) {
                viewer.close();
            }
        }
    }

    private void giveTownShopBlocks(
        final @NotNull Player target,
        final @NotNull TownShopOutpost outpost,
        final int amount
    ) {
        if (amount <= 0) {
            return;
        }

        final List<ItemStack> stacks = new ArrayList<>();
        int remaining = amount;
        while (remaining > 0) {
            final ItemStack stack = TownShopBlock.getTownShopBlock(this.plugin, target, outpost);
            final int stackAmount = Math.min(remaining, stack.getMaxStackSize());
            stack.setAmount(stackAmount);
            stacks.add(stack);
            remaining -= stackAmount;
        }

        target.getInventory().addItem(stacks.toArray(new ItemStack[0]))
            .forEach((slot, item) -> target.getWorld().dropItem(target.getLocation().clone().add(0, 0.5D, 0), item));
    }

    private boolean canManageTownShops(final @NotNull Player player, final @NotNull TownShopOutpost outpost) {
        return this.isTownPermissionGranted(player, outpost, MANAGE_PERMISSION_KEY);
    }

    private boolean matchesMetadata(
        final @NotNull TownShopOutpost outpost,
        final @NotNull TownShopBlock.Metadata metadata
    ) {
        return outpost.getTownIdentifier().equalsIgnoreCase(metadata.townIdentifier())
            && outpost.getProtectionPlugin().equalsIgnoreCase(metadata.protectionPlugin())
            && outpost.getWorldName().equalsIgnoreCase(metadata.worldName())
            && outpost.getChunkX() == metadata.chunkX()
            && outpost.getChunkZ() == metadata.chunkZ();
    }

    private boolean isTownPermissionGranted(
        final @NotNull Player player,
        final @NotNull TownShopOutpost outpost,
        final @NotNull String permissionKey
    ) {
        final RProtectionBridge protectionBridge = resolveBridge(outpost);
        return protectionBridge != null
            && outpost.getTownIdentifier().equalsIgnoreCase(normalizeTownIdentifier(protectionBridge.getPlayerTownIdentifier(player)))
            && protectionBridge.hasTownPermission(player, permissionKey);
    }

    private static @Nullable RProtectionBridge resolveBridge(final @NotNull TownShopOutpost outpost) {
        final RProtectionBridge protectionBridge = RProtectionBridge.getBridge();
        if (protectionBridge == null || !protectionBridge.isAvailable()) {
            return null;
        }
        return protectionBridge.getPluginName().equalsIgnoreCase(outpost.getProtectionPlugin()) ? protectionBridge : null;
    }

    private static @Nullable UUID parseUuid(final @Nullable String rawUuid) {
        if (rawUuid == null || rawUuid.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(rawUuid.trim());
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static @NotNull String normalizeTownIdentifier(final @Nullable String townIdentifier) {
        return townIdentifier == null ? "" : townIdentifier.trim().toLowerCase(Locale.ROOT);
    }

    private @Nullable Object resolveRdtRuntime(final @NotNull Plugin plugin) throws ReflectiveOperationException {
        if (this.hasZeroArgMethod(plugin.getClass(), "getTownRuntimeService")) {
            return plugin;
        }

        final Object directRuntime = this.firstNonNull(
            this.invokeOptional(plugin, "getRdt"),
            this.readFieldOptional(plugin, "rdt")
        );
        if (directRuntime != null && this.hasZeroArgMethod(directRuntime.getClass(), "getTownRuntimeService")) {
            return directRuntime;
        }

        final Object delegate = this.firstNonNull(
            this.readFieldOptional(plugin, "impl"),
            this.readFieldOptional(plugin, "delegate")
        );
        if (delegate == null) {
            return null;
        }

        final Object delegatedRuntime = this.firstNonNull(
            this.invokeOptional(delegate, "getRdt"),
            this.readFieldOptional(delegate, "rdt")
        );
        return delegatedRuntime != null && this.hasZeroArgMethod(delegatedRuntime.getClass(), "getTownRuntimeService")
            ? delegatedRuntime
            : null;
    }

    private boolean hasZeroArgMethod(final @NotNull Class<?> type, final @NotNull String methodName) {
        try {
            type.getMethod(methodName);
            return true;
        } catch (final NoSuchMethodException ignored) {
            return false;
        }
    }

    private @Nullable Object invokeOptional(
        final @NotNull Object target,
        final @NotNull String methodName,
        final Object... arguments
    ) throws ReflectiveOperationException {
        final Method method = this.findMethod(target.getClass(), methodName, arguments.length);
        if (method == null) {
            return null;
        }
        return method.invoke(target, arguments);
    }

    private @Nullable Method findMethod(
        final @NotNull Class<?> type,
        final @NotNull String methodName,
        final int parameterCount
    ) {
        for (final Method method : type.getMethods()) {
            if (method.getName().equals(methodName) && method.getParameterCount() == parameterCount) {
                return method;
            }
        }
        return null;
    }

    private @Nullable Object readFieldOptional(
        final @NotNull Object target,
        final @NotNull String fieldName
    ) throws ReflectiveOperationException {
        Class<?> current = target.getClass();
        while (current != null) {
            try {
                final Field field = current.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field.get(target);
            } catch (final NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    private @Nullable Object firstNonNull(final @Nullable Object... values) {
        if (values == null) {
            return null;
        }
        for (final Object value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private @Nullable String invokeBridgeString(
        final @NotNull RProtectionBridge bridge,
        final @NotNull String methodName,
        final @NotNull Player player
    ) {
        try {
            final Method method = bridge.getClass().getMethod(methodName, Player.class);
            final Object result = method.invoke(bridge, player);
            return result instanceof String value ? value : null;
        } catch (final ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static double resolveSaleTaxRate(final int chunkLevel) {
        return switch (Math.max(1, chunkLevel)) {
            case 1 -> 0.02D;
            case 2 -> 0.05D;
            case 3 -> 0.06D;
            case 4 -> 0.08D;
            default -> 0.10D;
        };
    }

    private void clearShopBlock(final @Nullable Location location) {
        if (location != null && location.getWorld() != null) {
            location.getBlock().setType(Material.AIR);
        }
    }

    /**
     * Immutable result for one applied outpost sale tax.
     *
     * @param outpost taxed outpost row
     * @param rate applied tax rate
     * @param amount deposited tax amount
     */
    public record SaleTaxResult(
        @NotNull TownShopOutpost outpost,
        double rate,
        double amount
    ) {
        /**
         * Creates a normalized sale-tax result.
         *
         * @param outpost taxed outpost row
         * @param rate applied tax rate
         * @param amount deposited tax amount
         */
        public SaleTaxResult {
            outpost = Objects.requireNonNull(outpost, "outpost");
            rate = Math.max(0.0D, rate);
            amount = Math.max(0.0D, amount);
        }
    }

    /**
     * Immutable snapshot of one live RDT outpost used for admin grants and interop.
     *
     * @param protectionPlugin source protection plugin id
     * @param townIdentifier stable town identifier
     * @param townDisplayName town display name
     * @param chunkUuid outpost chunk UUID
     * @param worldName outpost world name
     * @param chunkX outpost chunk x
     * @param chunkZ outpost chunk z
     * @param chunkLevel current outpost level
     */
    public record LiveOutpostContext(
        @NotNull String protectionPlugin,
        @NotNull String townIdentifier,
        @NotNull String townDisplayName,
        @NotNull UUID chunkUuid,
        @NotNull String worldName,
        int chunkX,
        int chunkZ,
        int chunkLevel
    ) {

        /**
         * Creates a normalized live-outpost snapshot.
         *
         * @param protectionPlugin source protection plugin id
         * @param townIdentifier stable town identifier
         * @param townDisplayName town display name
         * @param chunkUuid outpost chunk UUID
         * @param worldName outpost world name
         * @param chunkX outpost chunk x
         * @param chunkZ outpost chunk z
         * @param chunkLevel current outpost level
         */
        public LiveOutpostContext {
            protectionPlugin = Objects.requireNonNull(protectionPlugin, "protectionPlugin").trim();
            townIdentifier = Objects.requireNonNull(townIdentifier, "townIdentifier").trim();
            townDisplayName = Objects.requireNonNull(townDisplayName, "townDisplayName").trim();
            chunkUuid = Objects.requireNonNull(chunkUuid, "chunkUuid");
            worldName = Objects.requireNonNull(worldName, "worldName").trim();
            chunkLevel = Math.max(1, chunkLevel);
        }
    }
}
