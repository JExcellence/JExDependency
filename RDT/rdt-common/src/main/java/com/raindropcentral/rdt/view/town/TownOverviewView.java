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

package com.raindropcentral.rdt.view.town;

import java.util.Map;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;

import com.raindropcentral.rdt.RDT;
import com.raindropcentral.rdt.database.entity.RChunk;
import com.raindropcentral.rdt.database.entity.RDTPlayer;
import com.raindropcentral.rdt.database.entity.RTown;
import com.raindropcentral.rdt.utils.ChunkType;
import com.raindropcentral.rdt.utils.TownPermissions;
import com.raindropcentral.rdt.database.repository.RRDTPlayer;
import com.raindropcentral.rdt.database.repository.RRTown;
import com.raindropcentral.rdt.items.Nexus;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.BaseView;

import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.context.SlotClickContext;
import me.devnatan.inventoryframework.state.State;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

/**
 * Town overview UI for the selected or current player town.
 *
 * <p>This view renders an info button showing whether a nexus is placed and, when available,
 * the exact nexus location stored on the town entity.</p>
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.9
 */
public class TownOverviewView extends BaseView {

    private final State<RDT> rdt = initialState("plugin");
    private final State<UUID> townUuid = initialState("town_uuid");

    /**
     * Returns the i18n key namespace for this view.
     *
     * @return town overview key
     */
    @Override
    protected String getKey() {
        return "town_overview_ui";
    }

    /**
     * Returns the inventory size in rows.
     *
     * @return inventory row count
     */
    @Override
    protected int getSize() {
        return 1;
    }

    /**
     * Cancels default inventory movement behavior in this view.
     *
     * @param click click context
     */
    @Override
    public void onClick(final @NotNull SlotClickContext click) {
        click.setCancelled(true);
    }

    /**
     * Renders town info controls.
     *
     * @param render render context
     * @param player viewing player
     */
    @Override
    public void onFirstRender(final @NotNull RenderContext render, final @NotNull Player player) {
        final RDT plugin = this.resolvePlugin(render);
        final RTown town = this.resolveTown(render, player);
        render.slot(1, 1).withItem(this.buildTownInfoItem(player, plugin, town));
        render.slot(1, 2)
                .withItem(this.buildPickupNexusItem(player, town))
                .onClick(this::handlePickupNexusClick);
        render.slot(1, 3)
                .withItem(this.buildRolesItem(player, town))
                .onClick(this::handleRolesClick);
        render.slot(1, 4)
                .withItem(this.buildPendingJoinItem(player, town))
                .onClick(this::handlePendingJoinClick);
        render.slot(1, 5)
                .withItem(this.buildChunkClaimsItem(player, town))
                .onClick(this::handleChunkClaimsClick);
        render.slot(1, 6)
                .withItem(this.buildTownSpawnItem(player, town))
                .onClick(this::handleSetTownSpawnClick);
        render.slot(1, 7)
                .withItem(this.buildTownBankItem(player, town))
                .onClick(this::handleTownBankClick);
        render.slot(1, 8)
                .withItem(this.buildTownLevelUpItem(player, plugin, town))
                .onClick(this::handleTownLevelUpClick);
    }

    private @Nullable RTown resolveTown(
            final @NotNull Context context,
            final @NotNull Player player
    ) {
        final RDT plugin = this.resolvePlugin(context);
        if (plugin == null) {
            return null;
        }

        final RRTown townRepository = plugin.getTownRepository();
        if (townRepository == null) {
            return null;
        }

        UUID targetTownUuid = this.resolveTownUuid(context, player, plugin);
        if (targetTownUuid == null) {
            return null;
        }

        return townRepository.findByTownUUID(targetTownUuid);
    }

    private @Nullable UUID resolveTownUuid(
            final @NotNull Context context,
            final @NotNull Player player,
            final @NotNull RDT plugin
    ) {
        try {
            final UUID explicitTownUuid = this.townUuid.get(context);
            if (explicitTownUuid != null) {
                return explicitTownUuid;
            }
        } catch (Exception ignored) {
        }

        final RRDTPlayer playerRepository = plugin.getPlayerRepository();
        if (playerRepository == null) {
            return null;
        }

        final RDTPlayer rdtPlayer = playerRepository.findByPlayer(player.getUniqueId());
        if (rdtPlayer == null) {
            return null;
        }
        return rdtPlayer.getTownUUID();
    }

    private @Nullable RDTPlayer resolveTownPlayer(
            final @NotNull Context context,
            final @NotNull Player player
    ) {
        final RDT plugin = this.resolvePlugin(context);
        if (plugin == null || plugin.getPlayerRepository() == null) {
            return null;
        }
        return plugin.getPlayerRepository().findByPlayer(player.getUniqueId());
    }

    private @Nullable RDT resolvePlugin(final @NotNull Context context) {
        try {
            return this.rdt.get(context);
        } catch (Exception ignored) {
            return null;
        }
    }

    private @NotNull ItemStack buildTownInfoItem(
            final @NotNull Player player,
            final @Nullable RDT plugin,
            final @Nullable RTown town
    ) {
        if (town == null) {
            return UnifiedBuilderFactory.item(Material.BARRIER)
                    .setName(this.i18n("info.unavailable.name", player).build().component())
                    .setLore(this.i18n("info.unavailable.lore", player).build().children())
                    .build();
        }

        final @Nullable Location nexusLocation = town.getNexusLocation();
        final boolean placed = nexusLocation != null;
        final String worldName = nexusLocation == null || nexusLocation.getWorld() == null
                ? "unknown"
                : nexusLocation.getWorld().getName();
        final String nexusServer = town.getNexusServerId() == null ? "-" : town.getNexusServerId();
        final @Nullable Integer nextLevel = plugin == null
                ? null
                : plugin.getDefaultConfig().getNextTownLevel(town.getTownLevel());

        return UnifiedBuilderFactory.item(placed ? Material.REINFORCED_DEEPSLATE : Material.GRAY_CONCRETE)
                .setName(this.i18n("info.name", player)
                        .withPlaceholder("town_name", town.getTownName())
                        .build()
                        .component())
                .setLore(this.i18n("info.lore", player)
                        .withPlaceholders(Map.of(
                                "placed_state", this.resolvePlacedStateLabel(player, placed),
                                "world", worldName,
                                "x", nexusLocation == null ? "-" : nexusLocation.getBlockX(),
                                "y", nexusLocation == null ? "-" : nexusLocation.getBlockY(),
                                "z", nexusLocation == null ? "-" : nexusLocation.getBlockZ(),
                                "nexus_server", nexusServer,
                                "town_level", town.getTownLevel(),
                                "next_level", nextLevel == null ? "-" : nextLevel
                        ))
                        .build()
                .children())
                .build();
    }

    private @NotNull String resolvePlacedStateLabel(
            final @NotNull Player player,
            final boolean placed
    ) {
        final String key = placed ? "info.state.placed" : "info.state.unplaced";
        return PlainTextComponentSerializer.plainText().serialize(
                this.i18n(key, player).build().component()
        );
    }

    private @NotNull ItemStack buildPickupNexusItem(
            final @NotNull Player player,
            final @Nullable RTown town
    ) {
        if (town == null) {
            return UnifiedBuilderFactory.item(Material.BARRIER)
                    .setName(this.i18n("pickup.unavailable.name", player).build().component())
                    .setLore(this.i18n("pickup.unavailable.lore", player).build().children())
                    .build();
        }

        if (!town.hasNexusPlaced()) {
            return UnifiedBuilderFactory.item(Material.GRAY_CONCRETE)
                    .setName(this.i18n("pickup.not_placed.name", player).build().component())
                    .setLore(this.i18n("pickup.not_placed.lore", player).build().children())
                    .build();
        }

        return UnifiedBuilderFactory.item(Material.HOPPER)
                .setName(this.i18n("pickup.name", player)
                        .withPlaceholder("town_name", town.getTownName())
                        .build()
                        .component())
                .setLore(this.i18n("pickup.lore", player).build().children())
                .build();
    }

    private @NotNull ItemStack buildRolesItem(
            final @NotNull Player player,
            final @Nullable RTown town
    ) {
        if (town == null) {
            return UnifiedBuilderFactory.item(Material.BARRIER)
                    .setName(this.i18n("roles.unavailable.name", player).build().component())
                    .setLore(this.i18n("roles.unavailable.lore", player).build().children())
                    .build();
        }

        return UnifiedBuilderFactory.item(Material.WRITABLE_BOOK)
                .setName(this.i18n("roles.name", player)
                        .withPlaceholder("town_name", town.getTownName())
                        .build()
                        .component())
                .setLore(this.i18n("roles.lore", player).build().children())
                .build();
    }

    private @NotNull ItemStack buildPendingJoinItem(
            final @NotNull Player player,
            final @Nullable RTown town
    ) {
        if (town == null) {
            return UnifiedBuilderFactory.item(Material.BARRIER)
                    .setName(this.i18n("pending_join.unavailable.name", player).build().component())
                    .setLore(this.i18n("pending_join.unavailable.lore", player).build().children())
                    .build();
        }

        return UnifiedBuilderFactory.item(Material.CLOCK)
                .setName(this.i18n("pending_join.name", player)
                        .withPlaceholder("town_name", town.getTownName())
                        .build()
                        .component())
                .setLore(this.i18n("pending_join.lore", player)
                        .withPlaceholders(Map.of(
                                "pending_count", town.getPendingJoinRequests().size(),
                                "permission", TownPermissions.TOWN_INVITE.getPermissionKey()
                        ))
                        .build()
                        .children())
                .build();
    }

    private @NotNull ItemStack buildChunkClaimsItem(
            final @NotNull Player player,
            final @Nullable RTown town
    ) {
        if (town == null) {
            return UnifiedBuilderFactory.item(Material.BARRIER)
                    .setName(this.i18n("chunks.unavailable.name", player).build().component())
                    .setLore(this.i18n("chunks.unavailable.lore", player).build().children())
                    .build();
        }

        final Location nexusLocation = town.getNexusLocation();
        final String chunkX = nexusLocation == null || nexusLocation.getWorld() == null
                ? "-"
                : String.valueOf(nexusLocation.getChunk().getX());
        final String chunkZ = nexusLocation == null || nexusLocation.getWorld() == null
                ? "-"
                : String.valueOf(nexusLocation.getChunk().getZ());
        final int claimedChunks = town.getChunks().size();

        return UnifiedBuilderFactory.item(Material.MAP)
                .setName(this.i18n("chunks.name", player)
                        .withPlaceholder("town_name", town.getTownName())
                        .build()
                        .component())
                .setLore(this.i18n("chunks.lore", player)
                        .withPlaceholders(Map.of(
                                "claimed_chunks", claimedChunks,
                                "chunk_x", chunkX,
                                "chunk_z", chunkZ
                        ))
                        .build()
                        .children())
                .build();
    }

    private @NotNull ItemStack buildTownSpawnItem(
            final @NotNull Player player,
            final @Nullable RTown town
    ) {
        if (town == null) {
            return UnifiedBuilderFactory.item(Material.BARRIER)
                    .setName(this.i18n("spawn.unavailable.name", player).build().component())
                    .setLore(this.i18n("spawn.unavailable.lore", player).build().children())
                    .build();
        }

        final @Nullable Location townSpawnLocation = town.getTownSpawnLocation();
        final String worldName = townSpawnLocation == null || townSpawnLocation.getWorld() == null
                ? "-"
                : townSpawnLocation.getWorld().getName();
        final String spawnServer = town.getTownSpawnServerId() == null ? "-" : town.getTownSpawnServerId();

        return UnifiedBuilderFactory.item(Material.LODESTONE)
                .setName(this.i18n("spawn.name", player)
                        .withPlaceholder("town_name", town.getTownName())
                        .build()
                        .component())
                .setLore(this.i18n("spawn.lore", player)
                        .withPlaceholders(Map.of(
                                "spawn_world", worldName,
                                "spawn_x", townSpawnLocation == null ? "-" : townSpawnLocation.getBlockX(),
                                "spawn_y", townSpawnLocation == null ? "-" : townSpawnLocation.getBlockY(),
                                "spawn_z", townSpawnLocation == null ? "-" : townSpawnLocation.getBlockZ(),
                                "spawn_server", spawnServer,
                                "permission", TownPermissions.PLACE_NEXUS.getPermissionKey()
                        ))
                        .build()
                        .children())
                .build();
    }

    private @NotNull ItemStack buildTownBankItem(
            final @NotNull Player player,
            final @Nullable RTown town
    ) {
        if (town == null) {
            return UnifiedBuilderFactory.item(Material.BARRIER)
                    .setName(this.i18n("bank.unavailable.name", player).build().component())
                    .setLore(this.i18n("bank.unavailable.lore", player).build().children())
                    .build();
        }

        return UnifiedBuilderFactory.item(Material.CHEST)
                .setName(this.i18n("bank.name", player)
                        .withPlaceholder("town_name", town.getTownName())
                        .build()
                        .component())
                .setLore(this.i18n("bank.lore", player)
                        .withPlaceholders(Map.of(
                                "currency_count", town.getBankCurrencyCount(),
                                "deposit_permission", TownPermissions.TOWN_DEPOSIT.getPermissionKey(),
                                "withdraw_permission", TownPermissions.TOWN_WITHDRAW.getPermissionKey()
                        ))
                        .build()
                        .children())
                .build();
    }

    private @NotNull ItemStack buildTownLevelUpItem(
            final @NotNull Player player,
            final @Nullable RDT plugin,
            final @Nullable RTown town
    ) {
        if (town == null || plugin == null) {
            return UnifiedBuilderFactory.item(Material.BARRIER)
                    .setName(this.i18n("level_up.unavailable.name", player).build().component())
                    .setLore(this.i18n("level_up.unavailable.lore", player).build().children())
                    .build();
        }

        final @Nullable Integer nextLevel = TownLevelUpSupport.resolveNextLevel(plugin, town);
        if (nextLevel == null) {
            return UnifiedBuilderFactory.item(Material.LIME_STAINED_GLASS_PANE)
                    .setName(this.i18n("level_up.max_level.name", player).build().component())
                    .setLore(this.i18n("level_up.max_level.lore", player)
                            .withPlaceholder("current_level", town.getTownLevel())
                            .build()
                            .children())
                    .build();
        }

        return UnifiedBuilderFactory.item(Material.ENCHANTING_TABLE)
                .setName(this.i18n("level_up.name", player)
                        .withPlaceholder("town_name", town.getTownName())
                        .build()
                        .component())
                .setLore(this.i18n("level_up.lore", player)
                        .withPlaceholders(Map.of(
                                "current_level", town.getTownLevel(),
                                "next_level", nextLevel,
                                "permission", TownPermissions.TOWN_LEVEL_UP.getPermissionKey()
                        ))
                        .build()
                        .children())
                .build();
    }

    private void handlePickupNexusClick(final @NotNull SlotClickContext click) {
        click.setCancelled(true);

        final Player player = click.getPlayer();
        final RDT plugin = this.resolvePlugin(click);
        if (plugin == null || plugin.getTownRepository() == null) {
            this.i18n("pickup.error.system_unavailable", player)
                    .includePrefix()
                    .build()
                    .sendMessage();
            return;
        }

        final RTown town = this.resolveTown(click, player);
        if (town == null) {
            this.i18n("pickup.error.town_unavailable", player)
                    .includePrefix()
                    .build()
                    .sendMessage();
            return;
        }

        final RDTPlayer townPlayer = this.resolveTownPlayer(click, player);
        if (!town.hasTownPermission(townPlayer, TownPermissions.PICKUP_NEXUS)) {
            this.i18n("pickup.error.no_permission", player)
                    .includePrefix()
                    .withPlaceholder("permission", TownPermissions.PICKUP_NEXUS.getPermissionKey())
                    .build()
                    .sendMessage();
            return;
        }

        final Location nexusLocation = town.getNexusLocation();
        if (nexusLocation == null) {
            this.i18n("pickup.error.not_placed", player)
                    .includePrefix()
                    .build()
                    .sendMessage();
            return;
        }

        if (nexusLocation.getWorld() == null) {
            this.i18n("pickup.error.invalid_location", player)
                    .includePrefix()
                    .build()
                    .sendMessage();
            return;
        }

        if (nexusLocation.getBlock().getType() == Material.REINFORCED_DEEPSLATE) {
            nexusLocation.getBlock().setType(Material.AIR, false);
        }

        final ItemStack nexusItem = Nexus.getNexusItem(
                plugin,
                town.getIdentifier(),
                town.getTownName(),
                town.getMayor()
        );
        final Map<Integer, ItemStack> leftovers = player.getInventory().addItem(nexusItem);
        if (leftovers.isEmpty()) {
            this.i18n("pickup.success.given", player)
                    .includePrefix()
                    .withPlaceholder("town_name", town.getTownName())
                    .build()
                    .sendMessage();
        } else {
            leftovers.values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
            this.i18n("pickup.success.dropped", player)
                    .includePrefix()
                    .withPlaceholder("town_name", town.getTownName())
                    .build()
                    .sendMessage();
        }

        town.setNexusLocation(null);
        this.clearNexusChunkMarkers(town);
        plugin.getTownRepository().update(town);
        player.closeInventory();
    }

    private void clearNexusChunkMarkers(final @NotNull RTown town) {
        for (final RChunk chunk : town.getChunks()) {
            if (ChunkType.equalsType(chunk.getType(), ChunkType.NEXUS)) {
                chunk.setType(ChunkType.DEFAULT);
            }
        }
    }

    private void handleRolesClick(final @NotNull SlotClickContext click) {
        click.setCancelled(true);

        final Player player = click.getPlayer();
        final RDT plugin = this.resolvePlugin(click);
        if (plugin == null) {
            this.i18n("roles.error.system_unavailable", player)
                    .includePrefix()
                    .build()
                    .sendMessage();
            return;
        }

        final RTown town = this.resolveTown(click, player);
        if (town == null) {
            this.i18n("roles.error.town_unavailable", player)
                    .includePrefix()
                    .build()
                    .sendMessage();
            return;
        }

        final RDTPlayer townPlayer = this.resolveTownPlayer(click, player);
        if (!town.hasTownPermission(townPlayer, TownPermissions.VIEW_ROLES)) {
            this.i18n("roles.error.no_permission", player)
                    .includePrefix()
                    .withPlaceholder("permission", TownPermissions.VIEW_ROLES.getPermissionKey())
                    .build()
                    .sendMessage();
            return;
        }

        click.openForPlayer(
                RolesOverviewView.class,
                Map.of(
                        "plugin", plugin,
                        "town_uuid", town.getIdentifier()
                )
        );
    }

    private void handlePendingJoinClick(final @NotNull SlotClickContext click) {
        click.setCancelled(true);

        final Player player = click.getPlayer();
        final RDT plugin = this.resolvePlugin(click);
        if (plugin == null) {
            this.i18n("pending_join.error.system_unavailable", player)
                    .includePrefix()
                    .build()
                    .sendMessage();
            return;
        }

        final RTown town = this.resolveTown(click, player);
        if (town == null) {
            this.i18n("pending_join.error.town_unavailable", player)
                    .includePrefix()
                    .build()
                    .sendMessage();
            return;
        }

        final RDTPlayer townPlayer = this.resolveTownPlayer(click, player);
        if (!town.hasTownPermission(townPlayer, TownPermissions.TOWN_INVITE)) {
            this.i18n("pending_join.error.no_permission", player)
                    .includePrefix()
                    .withPlaceholder("permission", TownPermissions.TOWN_INVITE.getPermissionKey())
                    .build()
                    .sendMessage();
            return;
        }

        click.openForPlayer(
                TownPendingJoinView.class,
                Map.of(
                        "plugin", plugin,
                        "town_uuid", town.getIdentifier()
                )
        );
    }

    private void handleChunkClaimsClick(final @NotNull SlotClickContext click) {
        click.setCancelled(true);

        final Player player = click.getPlayer();
        final RDT plugin = this.resolvePlugin(click);
        if (plugin == null) {
            this.i18n("chunks.error.system_unavailable", player)
                    .includePrefix()
                    .build()
                    .sendMessage();
            return;
        }

        final RTown town = this.resolveTown(click, player);
        if (town == null) {
            this.i18n("chunks.error.town_unavailable", player)
                    .includePrefix()
                    .build()
                    .sendMessage();
            return;
        }

        final RDTPlayer townPlayer = this.resolveTownPlayer(click, player);
        if (!town.hasTownPermission(townPlayer, TownPermissions.CLAIM_CHUNK)) {
            this.i18n("chunks.error.no_permission", player)
                    .includePrefix()
                    .withPlaceholder("permission", TownPermissions.CLAIM_CHUNK.getPermissionKey())
                    .build()
                    .sendMessage();
            return;
        }

        if (!town.hasNexusPlaced()) {
            this.i18n("chunks.error.nexus_not_placed", player)
                    .includePrefix()
                    .build()
                    .sendMessage();
            return;
        }

        click.openForPlayer(
                ChunkClaimView.class,
                Map.of(
                        "plugin", plugin,
                        "town_uuid", town.getIdentifier()
                )
        );
    }

    private void handleSetTownSpawnClick(final @NotNull SlotClickContext click) {
        click.setCancelled(true);

        final Player player = click.getPlayer();
        final RDT plugin = this.resolvePlugin(click);
        if (plugin == null || plugin.getTownRepository() == null) {
            this.i18n("spawn.error.system_unavailable", player)
                    .includePrefix()
                    .build()
                    .sendMessage();
            return;
        }

        final RTown town = this.resolveTown(click, player);
        if (town == null) {
            this.i18n("spawn.error.town_unavailable", player)
                    .includePrefix()
                    .build()
                    .sendMessage();
            return;
        }

        final RDTPlayer townPlayer = this.resolveTownPlayer(click, player);
        if (!town.hasTownPermission(townPlayer, TownPermissions.PLACE_NEXUS)) {
            this.i18n("spawn.error.no_permission", player)
                    .includePrefix()
                    .withPlaceholder("permission", TownPermissions.PLACE_NEXUS.getPermissionKey())
                    .build()
                    .sendMessage();
            return;
        }

        final Location playerLocation = player.getLocation().toBlockLocation();
        if (playerLocation.getWorld() == null) {
            this.i18n("spawn.error.invalid_location", player)
                    .includePrefix()
                    .build()
                    .sendMessage();
            return;
        }

        town.setTownSpawnLocation(playerLocation);
        town.setTownSpawnServerId(plugin.getServerRouteId());
        plugin.getTownRepository().update(town);
        this.i18n("spawn.success.updated", player)
                .includePrefix()
                .withPlaceholders(Map.of(
                        "world", playerLocation.getWorld().getName(),
                        "x", playerLocation.getBlockX(),
                        "y", playerLocation.getBlockY(),
                        "z", playerLocation.getBlockZ()
                ))
                .build()
                .sendMessage();
    }

    private void handleTownBankClick(final @NotNull SlotClickContext click) {
        click.setCancelled(true);

        final Player player = click.getPlayer();
        final RDT plugin = this.resolvePlugin(click);
        if (plugin == null) {
            this.i18n("bank.error.system_unavailable", player)
                    .includePrefix()
                    .build()
                    .sendMessage();
            return;
        }

        final RTown town = this.resolveTown(click, player);
        if (town == null) {
            this.i18n("bank.error.town_unavailable", player)
                    .includePrefix()
                    .build()
                    .sendMessage();
            return;
        }

        final RDTPlayer townPlayer = this.resolveTownPlayer(click, player);
        final boolean canDeposit = town.hasTownPermission(townPlayer, TownPermissions.TOWN_DEPOSIT);
        final boolean canWithdraw = town.hasTownPermission(townPlayer, TownPermissions.TOWN_WITHDRAW);
        if (!canDeposit && !canWithdraw) {
            this.i18n("bank.error.no_permission", player)
                    .includePrefix()
                    .withPlaceholders(Map.of(
                            "deposit_permission", TownPermissions.TOWN_DEPOSIT.getPermissionKey(),
                            "withdraw_permission", TownPermissions.TOWN_WITHDRAW.getPermissionKey()
                    ))
                    .build()
                    .sendMessage();
            return;
        }

        click.openForPlayer(
                TownBankView.class,
                Map.of(
                        "plugin", plugin,
                        "town_uuid", town.getIdentifier()
                )
        );
    }

    private void handleTownLevelUpClick(final @NotNull SlotClickContext click) {
        click.setCancelled(true);

        final Player player = click.getPlayer();
        final RDT plugin = this.resolvePlugin(click);
        if (plugin == null) {
            this.i18n("level_up.error.system_unavailable", player)
                    .includePrefix()
                    .build()
                    .sendMessage();
            return;
        }

        final RTown town = this.resolveTown(click, player);
        if (town == null) {
            this.i18n("level_up.error.town_unavailable", player)
                    .includePrefix()
                    .build()
                    .sendMessage();
            return;
        }

        final RDTPlayer townPlayer = this.resolveTownPlayer(click, player);
        if (!town.hasTownPermission(townPlayer, TownPermissions.TOWN_LEVEL_UP)) {
            this.i18n("level_up.error.no_permission", player)
                    .includePrefix()
                    .withPlaceholder("permission", TownPermissions.TOWN_LEVEL_UP.getPermissionKey())
                    .build()
                    .sendMessage();
            return;
        }

        final @Nullable Integer nextLevel = TownLevelUpSupport.resolveNextLevel(plugin, town);
        if (nextLevel == null) {
            this.i18n("level_up.error.max_level", player)
                    .includePrefix()
                    .withPlaceholder("current_level", town.getTownLevel())
                    .build()
                    .sendMessage();
            return;
        }

        click.openForPlayer(
                TownLevelUpView.class,
                Map.of(
                        "plugin", plugin,
                        "town_uuid", town.getIdentifier()
                )
        );
    }
}
