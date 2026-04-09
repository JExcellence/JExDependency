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

package com.raindropcentral.rds.listeners;

import com.raindropcentral.rds.RDS;
import com.raindropcentral.rds.configs.ConfigSection;
import com.raindropcentral.rds.database.entity.RDSPlayer;
import com.raindropcentral.rds.database.entity.Shop;
import com.raindropcentral.rds.database.entity.TownShopOutpost;
import com.raindropcentral.rds.items.ShopBlock;
import com.raindropcentral.rds.items.TownShopBlock;
import com.raindropcentral.rds.service.shop.ShopOwnershipSupport;
import com.raindropcentral.rds.service.shop.TownShopService;
import com.raindropcentral.rplatform.protection.RProtectionBridge;
import de.jexcellence.jextranslate.i18n.I18n;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;

/**
 * Handles shop chest placement and protection rules.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
@SuppressWarnings("unused")
public class BlockListener implements Listener {

    private static final String TOWN_PLACEMENT_BYPASS_PERMISSION = "raindropshops.admin.bypass.town";
    private static final String OUTPOST_PLACEMENT_BYPASS_PERMISSION = "raindropshops.admin.bypass.outpost";

    private final RDS rds;

    /**
     * Creates the block listener for the plugin instance.
     *
     * @param rds plugin entry point
     */
    public BlockListener(
            final RDS rds
    ) {
        this.rds = rds;
    }

    /**
     * Prevents direct breaking of shop chest blocks.
     *
     * @param event fired when a player breaks a block
     */
    @EventHandler
    public void onBlockBreak(
            final BlockBreakEvent event
    ) {
        if (event == null) {
            return;
        }

        final Shop shop = this.rds.getShopRepository().findByLocation(event.getBlock().getLocation());
        if (shop == null) {
            return;
        }

        event.setCancelled(true);
        new I18n.Builder("block_listener.error.shop_protected", event.getPlayer())
                .includePrefix()
                .build()
                .sendMessage();
    }

    /**
     * Creates new shops and upgrades single-chest shops into double chests.
     *
     * @param event fired when a player places a block
     */
    @EventHandler
    public void onBlockPlace(
            final BlockPlaceEvent event
    ) {
        if (event == null) {
            return;
        }

        final Block placedBlock = event.getBlockPlaced();
        final ItemStack item = event.getItemInHand();
        final boolean shopBlock = ShopBlock.equals(this.rds, item);
        final TownShopBlock.Metadata townShopMetadata = TownShopBlock.getMetadata(this.rds, item);
        final boolean townShopBlock = townShopMetadata != null;
        final Block mergedPartner = this.getMergedChestPartner(placedBlock);

        if (!shopBlock && !townShopBlock) {
            this.preventNormalChestShopMerge(event, mergedPartner);
            return;
        }

        if (townShopBlock) {
            this.handleTownShopPlacement(event, townShopMetadata, placedBlock.getLocation(), mergedPartner);
            return;
        }

        final UUID ownerId = this.resolveOwnerId(item);
        if (ownerId == null) {
            event.setCancelled(true);
            this.sendMessage(event.getPlayer(), "block_listener.error.invalid_owner_uuid");
            return;
        }

        if (!event.getPlayer().getUniqueId().equals(ownerId)) {
            event.setCancelled(true);
            this.sendMessage(event.getPlayer(), "block_listener.error.not_owner");
            return;
        }

        final ConfigSection config = this.rds.getDefaultConfig();
        if (!this.canPlacePlayerShop(event.getPlayer(), config)) {
            event.setCancelled(true);
            return;
        }

        if (mergedPartner != null) {
            this.upgradeShopToDoubleChest(event, ownerId, placedBlock.getLocation(), mergedPartner);
            return;
        }

        this.createSingleChestShop(event, ownerId, placedBlock.getLocation(), config);
    }

    private void preventNormalChestShopMerge(
            final @NotNull BlockPlaceEvent event,
            final @Nullable Block mergedPartner
    ) {
        if (mergedPartner == null) {
            return;
        }

        final Shop mergedShop = this.rds.getShopRepository().findByLocation(mergedPartner.getLocation());
        if (mergedShop == null) {
            return;
        }

        event.setCancelled(true);
        this.sendMessage(event.getPlayer(), "block_listener.error.double_chest_requires_shop_block");
    }

    private void createSingleChestShop(
            final @NotNull BlockPlaceEvent event,
            final @NotNull UUID ownerId,
            final @NotNull Location shopLocation,
            final @NotNull ConfigSection config
    ) {
        final RDSPlayer playerData = this.getOrCreatePlayer(ownerId);

        final int maxShops = this.rds.getMaximumShops(event.getPlayer(), config);
        final int activeOwnedShops = ShopOwnershipSupport.countOwnedPlayerShops(this.rds, ownerId);
        if (maxShops > 0 && activeOwnedShops >= maxShops) {
            event.setCancelled(true);
            new I18n.Builder("block_listener.error.max_shops_reached", event.getPlayer())
                    .withPlaceholders(Map.of(
                            "owned_shops", activeOwnedShops,
                            "max_shops", maxShops
                    ))
                    .includePrefix()
                    .build()
                    .sendMessage();
            return;
        }

        this.placeShopItem(ownerId, playerData, shopLocation);
    }

    private void handleTownShopPlacement(
        final @NotNull BlockPlaceEvent event,
        final @Nullable TownShopBlock.Metadata metadata,
        final @NotNull Location shopLocation,
        final @Nullable Block mergedPartner
    ) {
        final TownShopService townShopService = this.rds.getTownShopService();
        if (townShopService == null || metadata == null) {
            event.setCancelled(true);
            this.sendMessage(event.getPlayer(), "block_listener.error.invalid_town_shop");
            return;
        }

        final TownShopOutpost outpost = townShopService.getOutpost(metadata.chunkUuid());
        if (outpost == null) {
            event.setCancelled(true);
            this.sendMessage(event.getPlayer(), "block_listener.error.invalid_town_shop");
            return;
        }

        if (!outpost.matchesLocation(shopLocation)) {
            event.setCancelled(true);
            this.sendMessage(event.getPlayer(), "block_listener.error.town_shop_wrong_outpost");
            return;
        }

        if (mergedPartner != null) {
            this.upgradeTownShopToDoubleChest(event, shopLocation, mergedPartner, metadata, townShopService);
            return;
        }

        if (!townShopService.canPlaceTownShop(shopLocation, event.getPlayer(), metadata)) {
            event.setCancelled(true);
            this.sendMessage(event.getPlayer(), "block_listener.error.town_shop_unavailable");
            return;
        }

        this.placeTownShop(outpost, shopLocation);
    }

    private boolean canPlacePlayerShop(
            final @NotNull Player player,
            final @NotNull ConfigSection config
    ) {
        if (!config.getProtection().isOnlyPlayerShops()) {
            return true;
        }

        if (hasTownPlacementBypassPermission(player)) {
            return true;
        }

        final RProtectionBridge protectionBridge = RProtectionBridge.getBridge();
        if (protectionBridge == null || !protectionBridge.isAvailable()) {
            this.sendMessage(player, "block_listener.error.protection_unavailable");
            return false;
        }

        if (!protectionBridge.isPlayerInTown(player)) {
            this.sendMessage(player, "block_listener.error.protection_requires_town");
            return false;
        }

        if (!protectionBridge.isPlayerStandingInOwnTown(player)) {
            this.sendMessage(player, "block_listener.error.protection_requires_own_town");
            return false;
        }

        if (this.isRdtProtectionBridge(protectionBridge)
                && !hasOutpostPlacementBypassPermission(player)
                && !this.isPlayerStandingInRdtOutpost(player)) {
            this.sendMessage(player, "block_listener.error.protection_requires_outpost");
            return false;
        }

        return true;
    }

    /**
     * Determines whether a player can bypass own-town placement restrictions.
     *
     * @param player player placing the shop block
     * @return {@code true} when the player has the bypass permission
     */
    static boolean hasTownPlacementBypassPermission(
            final @NotNull Player player
    ) {
        return player.hasPermission(TOWN_PLACEMENT_BYPASS_PERMISSION);
    }

    /**
     * Determines whether a player can bypass RDT outpost placement restrictions.
     *
     * @param player player placing the shop block
     * @return {@code true} when the player has the outpost bypass permission
     */
    static boolean hasOutpostPlacementBypassPermission(
            final @NotNull Player player
    ) {
        return player.hasPermission(OUTPOST_PLACEMENT_BYPASS_PERMISSION);
    }

    private boolean isRdtProtectionBridge(final @NotNull RProtectionBridge protectionBridge) {
        return "RDT".equalsIgnoreCase(protectionBridge.getPluginName());
    }

    private boolean isPlayerStandingInRdtOutpost(final @NotNull Player player) {
        try {
            final Plugin plugin = Bukkit.getPluginManager().getPlugin("RDT");
            if (plugin == null || !plugin.isEnabled()) {
                return false;
            }

            final Object runtime = this.resolveRdtRuntime(plugin);
            if (runtime == null) {
                return false;
            }

            final Object townRuntimeService = this.invokeOptional(runtime, "getTownRuntimeService");
            if (townRuntimeService == null) {
                return false;
            }

            final Object result = this.invokeOptional(townRuntimeService, "isOutpostChunk", player.getLocation());
            return result instanceof Boolean allowed && allowed;
        } catch (final ReflectiveOperationException ignored) {
            return false;
        }
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

    private void upgradeShopToDoubleChest(
            final @NotNull BlockPlaceEvent event,
            final @NotNull UUID ownerId,
            final @NotNull Location secondaryLocation,
            final @NotNull Block mergedPartner
    ) {
        final Shop adjacentShop = this.rds.getShopRepository().findByLocation(mergedPartner.getLocation());
        if (adjacentShop == null) {
            event.setCancelled(true);
            this.sendMessage(event.getPlayer(), "block_listener.error.double_chest_requires_shop_block");
            return;
        }

        if (!adjacentShop.isOwner(ownerId)) {
            event.setCancelled(true);
            this.sendMessage(event.getPlayer(), "block_listener.error.double_chest_owner_mismatch");
            return;
        }

        if (adjacentShop.isDoubleChest()) {
            event.setCancelled(true);
            this.sendMessage(event.getPlayer(), "block_listener.error.shop_already_double_chest");
            return;
        }

        adjacentShop.setSecondaryShopLocation(secondaryLocation);
        this.rds.getShopRepository().update(adjacentShop);
    }

    private void upgradeTownShopToDoubleChest(
        final @NotNull BlockPlaceEvent event,
        final @NotNull Location secondaryLocation,
        final @NotNull Block mergedPartner,
        final @NotNull TownShopBlock.Metadata metadata,
        final @NotNull TownShopService townShopService
    ) {
        final Shop adjacentShop = this.rds.getShopRepository().findByLocation(mergedPartner.getLocation());
        if (adjacentShop == null) {
            event.setCancelled(true);
            this.sendMessage(event.getPlayer(), "block_listener.error.double_chest_requires_shop_block");
            return;
        }

        if (!adjacentShop.isTownShop()) {
            event.setCancelled(true);
            this.sendMessage(event.getPlayer(), "block_listener.error.double_chest_owner_mismatch");
            return;
        }

        if (adjacentShop.isDoubleChest()) {
            event.setCancelled(true);
            this.sendMessage(event.getPlayer(), "block_listener.error.shop_already_double_chest");
            return;
        }

        if (!townShopService.canExtendTownShop(secondaryLocation, event.getPlayer(), adjacentShop, metadata)) {
            event.setCancelled(true);
            this.sendMessage(event.getPlayer(), "block_listener.error.town_shop_unavailable");
            return;
        }

        adjacentShop.setSecondaryShopLocation(secondaryLocation);
        this.rds.getShopRepository().update(adjacentShop);
    }

    private void placeShopItem(
            final @NotNull UUID ownerId,
            final @NotNull RDSPlayer playerData,
            final @NotNull Location shopLocation
    ) {
        final Shop shop = new Shop(ownerId, shopLocation);
        this.rds.getShopRepository().create(shop);
        this.rds.getPlayerRepository().update(playerData);
    }

    private void placeTownShop(
        final @NotNull TownShopOutpost outpost,
        final @NotNull Location shopLocation
    ) {
        final Shop shop = Shop.createTownShop(outpost, shopLocation);
        this.rds.getShopRepository().create(shop);
    }

    private @Nullable Block getMergedChestPartner(
            final @NotNull Block block
    ) {
        if (block.getType() != Material.CHEST) {
            return null;
        }

        final BlockData blockData = block.getBlockData();
        if (!(blockData instanceof Chest chestData) || chestData.getType() == Chest.Type.SINGLE) {
            return null;
        }

        final BlockFace offset = chestData.getType() == Chest.Type.LEFT
                ? this.rotateClockwise(chestData.getFacing())
                : this.rotateCounterClockwise(chestData.getFacing());
        return block.getRelative(offset);
    }

    private @NotNull BlockFace rotateClockwise(
            final @NotNull BlockFace facing
    ) {
        return switch (facing) {
            case NORTH -> BlockFace.EAST;
            case EAST -> BlockFace.SOUTH;
            case SOUTH -> BlockFace.WEST;
            case WEST -> BlockFace.NORTH;
            default -> throw new IllegalArgumentException("Unsupported chest facing: " + facing);
        };
    }

    private @NotNull BlockFace rotateCounterClockwise(
            final @NotNull BlockFace facing
    ) {
        return switch (facing) {
            case NORTH -> BlockFace.WEST;
            case WEST -> BlockFace.SOUTH;
            case SOUTH -> BlockFace.EAST;
            case EAST -> BlockFace.NORTH;
            default -> throw new IllegalArgumentException("Unsupported chest facing: " + facing);
        };
    }

    private @Nullable UUID resolveOwnerId(
            final @Nullable ItemStack item
    ) {
        try {
            return ShopBlock.getOwner(this.rds, item);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private void sendMessage(
            final @NotNull Player player,
            final @NotNull String key
    ) {
        new I18n.Builder(key, player)
                .includePrefix()
                .build()
                .sendMessage();
    }

    private @NotNull RDSPlayer getOrCreatePlayer(final @NotNull UUID ownerId) {
        final RDSPlayer existingPlayer = this.rds.getPlayerRepository().findByPlayer(ownerId);
        if (existingPlayer != null) {
            return existingPlayer;
        }

        final RDSPlayer newPlayer = new RDSPlayer(ownerId);
        this.rds.getPlayerRepository().create(newPlayer);
        return newPlayer;
    }
}
