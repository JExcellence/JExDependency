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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.OpenContext;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.context.SlotClickContext;
import me.devnatan.inventoryframework.state.MutableState;
import me.devnatan.inventoryframework.state.State;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;

import com.raindropcentral.rdt.RDT;
import com.raindropcentral.rdt.database.entity.RChunk;
import com.raindropcentral.rdt.database.entity.RDTPlayer;
import com.raindropcentral.rdt.database.entity.RTown;
import com.raindropcentral.rdt.items.ChunkBlock;
import com.raindropcentral.rdt.utils.ChunkBlockState;
import com.raindropcentral.rdt.utils.ChunkType;
import com.raindropcentral.rdt.utils.TownPermissions;
import com.raindropcentral.rplatform.utility.heads.view.Down;
import com.raindropcentral.rplatform.utility.heads.view.Next;
import com.raindropcentral.rplatform.utility.heads.view.Previous;
import com.raindropcentral.rplatform.utility.heads.view.Return;
import com.raindropcentral.rplatform.utility.heads.view.Up;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.BaseView;

/**
 * Chunk-claim navigation view centered on a town nexus chunk.
 *
 * <p>The middle slot starts at the nexus chunk and every visible slot maps to a concrete
 * chunk coordinate. Directional controls shift the visible window in cardinal directions.</p>
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.4
 */
public final class ChunkClaimView extends BaseView {

    private static final int DEFAULT_MIN_Y_OFFSET = -10;
    private static final int DEFAULT_MAX_Y_OFFSET = 10;

    private static final int GRID_CENTER_X = 3;
    private static final int GRID_CENTER_Y = 2;

    private static final int NAVIGATION_LEFT_SLOT = 18;
    private static final int NAVIGATION_RIGHT_SLOT = 26;
    private static final int NAVIGATION_UP_SLOT = 4;
    private static final int NAVIGATION_DOWN_SLOT = 49;
    private static final int BACK_BUTTON_SLOT = 45;
    private static final int CENTER_BUTTON_SLOT = 53;
    private static final int STATUS_SLOT = 8;

    private static final Map<GridPosition, Integer> CHUNK_SLOT_MAPPING = createChunkSlotMapping();
    private static final List<Integer> ALL_CHUNK_SLOTS = createAllChunkSlots();
    private static final Set<Integer> RESERVED_SLOTS = Set.of(
            NAVIGATION_LEFT_SLOT,
            NAVIGATION_RIGHT_SLOT,
            NAVIGATION_UP_SLOT,
            NAVIGATION_DOWN_SLOT,
            BACK_BUTTON_SLOT,
            CENTER_BUTTON_SLOT,
            STATUS_SLOT
    );

    private final State<RDT> rdt = initialState("plugin");
    private final State<UUID> townUuid = initialState("town_uuid");

    private final MutableState<Integer> viewOffsetX = mutableState(0);
    private final MutableState<Integer> viewOffsetZ = mutableState(0);
    private final MutableState<Integer> chunkLimit = mutableState(64);
    private final MutableState<Integer> gridRevision = mutableState(0);

    /**
     * Creates the chunk-claim view with return navigation to town overview.
     */
    public ChunkClaimView() {
        super(TownOverviewView.class);
    }

    /**
     * Returns the i18n key namespace for this view.
     *
     * @return translation key prefix
     */
    @Override
    protected @NotNull String getKey() {
        return "chunk_claim_ui";
    }

    /**
     * Returns the inventory size in rows.
     *
     * @return row count
     */
    @Override
    protected int getSize() {
        return 6;
    }

    /**
     * Resolves title placeholders using town metadata.
     *
     * @param openContext open context
     * @return title placeholders
     */
    @Override
    protected @NotNull Map<String, Object> getTitlePlaceholders(final @NotNull OpenContext openContext) {
        final RTown town = this.resolveTown(openContext, openContext.getPlayer());
        if (town == null) {
            return Map.of("town_name", "Unknown");
        }
        return Map.of("town_name", town.getTownName());
    }

    /**
     * Cancels default item movement behavior in this inventory.
     *
     * @param click click context
     */
    @Override
    public void onClick(final @NotNull SlotClickContext click) {
        click.setCancelled(true);
    }

    /**
     * Renders the complete chunk-claim interface.
     *
     * @param render render context
     * @param player viewing player
     */
    @Override
    public void onFirstRender(final @NotNull RenderContext render, final @NotNull Player player) {
        final RTown town = this.resolveTown(render, player);
        if (town == null) {
            this.i18n("error.town_unavailable", player).includePrefix().build().sendMessage();
            player.closeInventory();
            return;
        }

        final Location nexusLocation = town.getNexusLocation();
        if (nexusLocation == null || nexusLocation.getWorld() == null) {
            this.i18n("error.nexus_not_placed", player).includePrefix().build().sendMessage();
            player.closeInventory();
            return;
        }

        final RDTPlayer viewerTownRecord = this.resolveTownPlayer(render, player);
        if (!town.hasTownPermission(viewerTownRecord, TownPermissions.CLAIM_CHUNK)) {
            this.i18n("error.no_permission", player)
                    .includePrefix()
                    .withPlaceholder("permission", TownPermissions.CLAIM_CHUNK.getPermissionKey())
                    .build()
                    .sendMessage();
            player.closeInventory();
            return;
        }

        this.viewOffsetX.set(0, render);
        this.viewOffsetZ.set(0, render);
        this.gridRevision.set(0, render);
        this.chunkLimit.set(this.resolveChunkLimit(render), render);

        this.renderBackground(render, player);
        this.renderDirectionalButtons(render, player);
        this.renderBackButton(render, player);
        this.renderCenterButton(render, player, town);
        this.renderStatusCard(render, player, town);
        this.renderChunkWindow(render, player, town);
    }

    private void renderBackground(
            final @NotNull RenderContext render,
            final @NotNull Player player
    ) {
        for (int slot = 0; slot < 54; slot++) {
            if (RESERVED_SLOTS.contains(slot) || ALL_CHUNK_SLOTS.contains(slot)) {
                continue;
            }
            render.slot(slot)
                    .renderWith(() -> UnifiedBuilderFactory.item(Material.GRAY_STAINED_GLASS_PANE)
                            .setName(this.i18n("background.name", player).build().component())
                            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                            .build());
        }
    }

    private void renderDirectionalButtons(
            final @NotNull RenderContext render,
            final @NotNull Player player
    ) {
        render.slot(NAVIGATION_LEFT_SLOT)
                .renderWith(() -> this.createNavigationItem(new Previous().getHead(player), player, "nav.left"))
                .onClick(click -> this.shiftGrid(click, -1, 0));

        render.slot(NAVIGATION_RIGHT_SLOT)
                .renderWith(() -> this.createNavigationItem(new Next().getHead(player), player, "nav.right"))
                .onClick(click -> this.shiftGrid(click, 1, 0));

        render.slot(NAVIGATION_UP_SLOT)
                .renderWith(() -> this.createNavigationItem(new Up().getHead(player), player, "nav.up"))
                .onClick(click -> this.shiftGrid(click, 0, -1));

        render.slot(NAVIGATION_DOWN_SLOT)
                .renderWith(() -> this.createNavigationItem(new Down().getHead(player), player, "nav.down"))
                .onClick(click -> this.shiftGrid(click, 0, 1));
    }

    private @NotNull ItemStack createNavigationItem(
            final @NotNull ItemStack baseItem,
            final @NotNull Player player,
            final @NotNull String key
    ) {
        return UnifiedBuilderFactory.item(baseItem)
                .setName(this.i18n(key + ".name", player).build().component())
                .setLore(this.i18n(key + ".lore", player).build().children())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    private void renderBackButton(
            final @NotNull RenderContext render,
            final @NotNull Player player
    ) {
        render.slot(BACK_BUTTON_SLOT)
                .renderWith(() -> UnifiedBuilderFactory.item(new Return().getHead(player))
                        .setName(this.i18n("back.name", player).build().component())
                        .setLore(this.i18n("back.lore", player).build().children())
                        .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                        .build())
                .onClick(SlotClickContext::back);
    }

    private void renderCenterButton(
            final @NotNull RenderContext render,
            final @NotNull Player player,
            final @NotNull RTown town
    ) {
        render.slot(CENTER_BUTTON_SLOT)
                .renderWith(() -> {
                    final ChunkCoordinate center = this.resolveCenterCoordinate(render, town);
                    return UnifiedBuilderFactory.item(Material.COMPASS)
                            .setName(this.i18n("center.name", player).build().component())
                            .setLore(this.i18n("center.lore", player)
                                    .withPlaceholders(Map.of(
                                            "chunk_x", center.x(),
                                            "chunk_z", center.z()
                                    ))
                                    .build()
                                    .children())
                            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                            .build();
                })
                .updateOnStateChange(this.viewOffsetX, this.viewOffsetZ)
                .onClick(click -> {
                    this.viewOffsetX.set(0, click);
                    this.viewOffsetZ.set(0, click);
                });
    }

    private void renderStatusCard(
            final @NotNull RenderContext render,
            final @NotNull Player player,
            final @NotNull RTown town
    ) {
        render.slot(STATUS_SLOT)
                .renderWith(() -> {
                    final int claimedChunks = town.getChunks().size();
                    final int maxChunks = Math.max(1, this.chunkLimit.get(render));
                    final ChunkCoordinate center = this.resolveCenterCoordinate(render, town);
                    return UnifiedBuilderFactory.item(Material.NETHER_STAR)
                            .setName(this.i18n("status.name", player).build().component())
                            .setLore(this.i18n("status.lore", player)
                                    .withPlaceholders(Map.of(
                                            "claimed_chunks", claimedChunks,
                                            "max_chunks", maxChunks,
                                            "center_x", center.x(),
                                            "center_z", center.z()
                                    ))
                                    .build()
                                    .children())
                            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                            .build();
                })
                .updateOnStateChange(this.viewOffsetX, this.viewOffsetZ, this.chunkLimit, this.gridRevision);
    }

    private void renderChunkWindow(
            final @NotNull RenderContext render,
            final @NotNull Player player,
            final @NotNull RTown town
    ) {
        final List<RTown> activeTownsSnapshot = this.resolveActiveTownsSnapshot(render);

        for (final Integer slot : ALL_CHUNK_SLOTS) {
            final @Nullable GridPosition gridPosition = this.findGridPositionForSlot(slot);
            if (gridPosition == null) {
                continue;
            }

            render.slot(slot)
                    .renderWith(() -> this.createChunkSlotItem(
                            render,
                            player,
                            town,
                            gridPosition,
                            activeTownsSnapshot
                    ))
                    .updateOnStateChange(this.viewOffsetX, this.viewOffsetZ, this.gridRevision)
                    .onClick(click -> this.handleChunkClick(click, town, gridPosition));
        }
    }

    private @NotNull ItemStack createChunkSlotItem(
            final @NotNull Context context,
            final @NotNull Player player,
            final @NotNull RTown town,
            final @NotNull GridPosition gridPosition,
            final @NotNull List<RTown> activeTownsSnapshot
    ) {
        final @Nullable ChunkCoordinate coordinate = this.resolveCoordinateForGridPosition(context, town, gridPosition);
        if (coordinate == null) {
            return UnifiedBuilderFactory.item(Material.BARRIER)
                    .setName(this.i18n("chunk.invalid.name", player).build().component())
                    .setLore(this.i18n("chunk.invalid.lore", player).build().children())
                    .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                    .build();
        }

        if (this.isNexusChunk(town, coordinate.x(), coordinate.z())) {
            return UnifiedBuilderFactory.item(Material.REINFORCED_DEEPSLATE)
                    .setName(this.i18n("chunk.nexus.name", player)
                            .withPlaceholders(Map.of(
                                    "chunk_x", coordinate.x(),
                                    "chunk_z", coordinate.z()
                            ))
                            .build()
                            .component())
                    .setLore(this.i18n("chunk.nexus.lore", player)
                            .withPlaceholders(Map.of(
                                    "chunk_x", coordinate.x(),
                                    "chunk_z", coordinate.z()
                            ))
                            .build()
                            .children())
                    .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                    .build();
        }

        final @Nullable RChunk ownedChunk = this.findTownChunk(town, coordinate.x(), coordinate.z());
        if (ownedChunk != null) {
            if (ChunkType.equalsType(ownedChunk.getType(), ChunkType.CLAIM_PENDING)) {
                return UnifiedBuilderFactory.item(Material.ORANGE_STAINED_GLASS_PANE)
                        .setName(this.i18n("chunk.pending.name", player)
                                .withPlaceholders(Map.of(
                                        "chunk_x", coordinate.x(),
                                        "chunk_z", coordinate.z()
                                ))
                                .build()
                                .component())
                        .setLore(this.i18n("chunk.pending.lore", player)
                                .withPlaceholders(Map.of(
                                        "chunk_x", coordinate.x(),
                                        "chunk_z", coordinate.z()
                                ))
                                .build()
                                .children())
                        .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                        .build();
            }
            if (this.isInactiveChunk(context, town, ownedChunk)) {
                final int minY = this.resolveChunkYOffsetMin(context);
                final int maxY = this.resolveChunkYOffsetMax(context);
                return UnifiedBuilderFactory.item(Material.RED_STAINED_GLASS_PANE)
                        .setName(this.i18n("chunk.inactive.name", player)
                                .withPlaceholders(Map.of(
                                        "chunk_x", coordinate.x(),
                                        "chunk_z", coordinate.z()
                                ))
                                .build()
                                .component())
                        .setLore(this.i18n("chunk.inactive.lore", player)
                                .withPlaceholders(Map.of(
                                        "chunk_x", coordinate.x(),
                                        "chunk_z", coordinate.z(),
                                        "min_y", minY,
                                        "max_y", maxY
                                ))
                                .build()
                                .children())
                        .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                        .build();
            }

            return UnifiedBuilderFactory.item(Material.LIME_STAINED_GLASS_PANE)
                    .setName(this.i18n("chunk.owned.name", player)
                            .withPlaceholders(Map.of(
                                    "chunk_x", coordinate.x(),
                                    "chunk_z", coordinate.z()
                            ))
                            .build()
                            .component())
                    .setLore(this.i18n("chunk.owned.lore", player)
                            .withPlaceholders(Map.of(
                                    "chunk_x", coordinate.x(),
                                    "chunk_z", coordinate.z()
                            ))
                            .build()
                            .children())
                    .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                    .build();
        }

        final @Nullable RTown foreignOwner = this.resolveChunkOwner(
                town,
                coordinate.x(),
                coordinate.z(),
                activeTownsSnapshot
        );
        if (foreignOwner != null) {
            return UnifiedBuilderFactory.item(Material.MAGENTA_STAINED_GLASS_PANE)
                    .setName(this.i18n("chunk.foreign.name", player)
                            .withPlaceholders(Map.of(
                                    "chunk_x", coordinate.x(),
                                    "chunk_z", coordinate.z(),
                                    "owner_town", foreignOwner.getTownName()
                            ))
                            .build()
                            .component())
                    .setLore(this.i18n("chunk.foreign.lore", player)
                            .withPlaceholders(Map.of(
                                    "chunk_x", coordinate.x(),
                                    "chunk_z", coordinate.z(),
                                    "owner_town", foreignOwner.getTownName()
                            ))
                            .build()
                            .children())
                    .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                    .build();
        }

        final boolean claimable = this.isAdjacentToTownChunk(town, coordinate.x(), coordinate.z());
        final Material material = claimable ? Material.YELLOW_STAINED_GLASS_PANE : Material.GRAY_STAINED_GLASS_PANE;
        final String stateKey = claimable ? "chunk.claimable" : "chunk.unclaimable";

        return UnifiedBuilderFactory.item(material)
                .setName(this.i18n(stateKey + ".name", player)
                        .withPlaceholders(Map.of(
                                "chunk_x", coordinate.x(),
                                "chunk_z", coordinate.z()
                        ))
                        .build()
                        .component())
                .setLore(this.i18n(stateKey + ".lore", player)
                        .withPlaceholders(Map.of(
                                "chunk_x", coordinate.x(),
                                "chunk_z", coordinate.z()
                        ))
                        .build()
                        .children())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    private void handleChunkClick(
            final @NotNull SlotClickContext click,
            final @NotNull RTown town,
            final @NotNull GridPosition gridPosition
    ) {
        click.setCancelled(true);
        final Player player = click.getPlayer();
        final RDT plugin = this.resolvePlugin(click);
        if (plugin == null || plugin.getTownRepository() == null) {
            this.i18n("error.system_unavailable", player).includePrefix().build().sendMessage();
            return;
        }

        final RDTPlayer townPlayer = this.resolveTownPlayer(click, player);
        if (!town.hasTownPermission(townPlayer, TownPermissions.CLAIM_CHUNK)) {
            this.i18n("error.no_permission", player)
                    .includePrefix()
                    .withPlaceholder("permission", TownPermissions.CLAIM_CHUNK.getPermissionKey())
                    .build()
                    .sendMessage();
            return;
        }

        final @Nullable ChunkCoordinate coordinate = this.resolveCoordinateForGridPosition(click, town, gridPosition);
        if (coordinate == null) {
            this.i18n("error.invalid_target_chunk", player).includePrefix().build().sendMessage();
            return;
        }

        final int chunkX = coordinate.x();
        final int chunkZ = coordinate.z();
        if (this.isNexusChunk(town, chunkX, chunkZ)) {
            this.i18n("message.nexus_locked", player)
                    .includePrefix()
                    .withPlaceholders(Map.of("chunk_x", chunkX, "chunk_z", chunkZ))
                    .build()
                    .sendMessage();
            return;
        }

        final @Nullable RChunk ownedChunk = this.findTownChunk(town, chunkX, chunkZ);
        if (ownedChunk != null) {
            town.getChunks().remove(ownedChunk);
            plugin.getTownRepository().update(town);
            this.i18n("message.unclaimed", player)
                    .includePrefix()
                    .withPlaceholders(Map.of(
                            "chunk_x", chunkX,
                            "chunk_z", chunkZ,
                            "claimed_chunks", town.getChunks().size(),
                            "max_chunks", this.chunkLimit.get(click)
                    ))
                    .build()
                    .sendMessage();
            this.bumpGridRevision(click);
            return;
        }

        final @Nullable RTown owner = this.resolveChunkOwner(plugin, town, chunkX, chunkZ);
        if (owner != null) {
            this.i18n("message.already_owned", player)
                    .includePrefix()
                    .withPlaceholders(Map.of(
                            "chunk_x", chunkX,
                            "chunk_z", chunkZ,
                            "owner_town", owner.getTownName()
                    ))
                    .build()
                    .sendMessage();
            return;
        }

        if (!this.isAdjacentToTownChunk(town, chunkX, chunkZ)) {
            this.i18n("message.not_adjacent", player)
                    .includePrefix()
                    .withPlaceholders(Map.of("chunk_x", chunkX, "chunk_z", chunkZ))
                    .build()
                    .sendMessage();
            return;
        }

        final int maxChunks = Math.max(1, this.chunkLimit.get(click));
        if (town.getChunks().size() >= maxChunks) {
            this.i18n("message.limit_reached", player)
                    .includePrefix()
                    .withPlaceholders(Map.of(
                            "claimed_chunks", town.getChunks().size(),
                            "max_chunks", maxChunks
                    ))
                    .build()
                    .sendMessage();
            return;
        }

        town.addChunk(new RChunk(town, chunkX, chunkZ, ChunkType.CLAIM_PENDING));
        plugin.getTownRepository().update(town);

        final ItemStack chunkBlockItem = ChunkBlock.getChunkBlockItem(
                plugin,
                town.getIdentifier(),
                town.getTownName(),
                town.getMayor(),
                chunkX,
                chunkZ
        );
        final Map<Integer, ItemStack> leftovers = player.getInventory().addItem(chunkBlockItem);
        final int minY = plugin.getDefaultConfig().getChunkBlockMinY();
        final int maxY = plugin.getDefaultConfig().getChunkBlockMaxY();
        if (leftovers.isEmpty()) {
            this.i18n("message.claim_pending", player)
                    .includePrefix()
                    .withPlaceholders(Map.of(
                            "chunk_x", chunkX,
                            "chunk_z", chunkZ,
                            "claimed_chunks", town.getChunks().size(),
                            "max_chunks", maxChunks,
                            "min_y", minY,
                            "max_y", maxY
                    ))
                    .build()
                    .sendMessage();
        } else {
            leftovers.values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
            this.i18n("message.claim_pending_dropped", player)
                    .includePrefix()
                    .withPlaceholders(Map.of(
                            "chunk_x", chunkX,
                            "chunk_z", chunkZ,
                            "claimed_chunks", town.getChunks().size(),
                            "max_chunks", maxChunks,
                            "min_y", minY,
                            "max_y", maxY
                    ))
                    .build()
                    .sendMessage();
        }
        this.bumpGridRevision(click);
    }

    private void shiftGrid(
            final @NotNull Context context,
            final int deltaX,
            final int deltaZ
    ) {
        this.viewOffsetX.set(this.viewOffsetX.get(context) + deltaX, context);
        this.viewOffsetZ.set(this.viewOffsetZ.get(context) + deltaZ, context);
    }

    private void bumpGridRevision(final @NotNull Context context) {
        this.gridRevision.set(this.gridRevision.get(context) + 1, context);
    }

    private int resolveChunkLimit(final @NotNull Context context) {
        final RDT plugin = this.resolvePlugin(context);
        if (plugin == null) {
            return 64;
        }
        return plugin.getDefaultConfig().getGlobalMaxChunkLimit();
    }

    private int resolveChunkYOffsetMin(final @NotNull Context context) {
        final RDT plugin = this.resolvePlugin(context);
        if (plugin == null) {
            return DEFAULT_MIN_Y_OFFSET;
        }
        return plugin.getDefaultConfig().getChunkBlockMinY();
    }

    private int resolveChunkYOffsetMax(final @NotNull Context context) {
        final RDT plugin = this.resolvePlugin(context);
        if (plugin == null) {
            return DEFAULT_MAX_Y_OFFSET;
        }
        return plugin.getDefaultConfig().getChunkBlockMaxY();
    }

    private boolean isInactiveChunk(
            final @NotNull Context context,
            final @NotNull RTown town,
            final @NotNull RChunk chunk
    ) {
        return ChunkBlockState.isInactive(
                town,
                chunk,
                this.resolveChunkYOffsetMin(context),
                this.resolveChunkYOffsetMax(context)
        );
    }

    private @Nullable ChunkCoordinate resolveCoordinateForGridPosition(
            final @NotNull Context context,
            final @NotNull RTown town,
            final @NotNull GridPosition gridPosition
    ) {
        final Location nexusLocation = town.getNexusLocation();
        if (nexusLocation == null || nexusLocation.getWorld() == null) {
            return null;
        }

        final int nexusChunkX = nexusLocation.getChunk().getX();
        final int nexusChunkZ = nexusLocation.getChunk().getZ();
        final int offsetX = this.viewOffsetX.get(context);
        final int offsetZ = this.viewOffsetZ.get(context);

        final int chunkX = nexusChunkX + (gridPosition.x() - GRID_CENTER_X) + offsetX;
        final int chunkZ = nexusChunkZ + (gridPosition.y() - GRID_CENTER_Y) + offsetZ;
        return new ChunkCoordinate(chunkX, chunkZ);
    }

    private @NotNull ChunkCoordinate resolveCenterCoordinate(
            final @NotNull Context context,
            final @NotNull RTown town
    ) {
        final @Nullable ChunkCoordinate coordinate = this.resolveCoordinateForGridPosition(
                context,
                town,
                new GridPosition(GRID_CENTER_X, GRID_CENTER_Y)
        );
        if (coordinate == null) {
            return new ChunkCoordinate(0, 0);
        }
        return coordinate;
    }

    private boolean isNexusChunk(
            final @NotNull RTown town,
            final int chunkX,
            final int chunkZ
    ) {
        final Location nexusLocation = town.getNexusLocation();
        if (nexusLocation == null || nexusLocation.getWorld() == null) {
            return false;
        }
        return nexusLocation.getChunk().getX() == chunkX && nexusLocation.getChunk().getZ() == chunkZ;
    }

    private @Nullable RChunk findTownChunk(
            final @NotNull RTown town,
            final int chunkX,
            final int chunkZ
    ) {
        for (final RChunk chunk : town.getChunks()) {
            if (chunk.getX_loc() == chunkX && chunk.getZ_loc() == chunkZ) {
                return chunk;
            }
        }
        return null;
    }

    private boolean isAdjacentToTownChunk(
            final @NotNull RTown town,
            final int chunkX,
            final int chunkZ
    ) {
        for (final RChunk chunk : town.getChunks()) {
            final int distance = Math.abs(chunk.getX_loc() - chunkX) + Math.abs(chunk.getZ_loc() - chunkZ);
            if (distance == 1) {
                return true;
            }
        }

        final Location nexusLocation = town.getNexusLocation();
        if (nexusLocation == null || nexusLocation.getWorld() == null) {
            return false;
        }
        final int nexusDistance = Math.abs(nexusLocation.getChunk().getX() - chunkX)
                + Math.abs(nexusLocation.getChunk().getZ() - chunkZ);
        return nexusDistance == 1;
    }

    private @Nullable RTown resolveChunkOwner(
            final @NotNull RDT plugin,
            final @NotNull RTown currentTown,
            final int chunkX,
            final int chunkZ
    ) {
        if (plugin.getTownRepository() == null) {
            return null;
        }

        final List<RTown> activeTowns = plugin.getTownRepository().findAllByAttributes(Map.of("active", true));
        return this.resolveChunkOwner(currentTown, chunkX, chunkZ, activeTowns);
    }

    private @Nullable RTown resolveChunkOwner(
            final @NotNull RTown currentTown,
            final int chunkX,
            final int chunkZ,
            final @NotNull List<RTown> activeTowns
    ) {
        for (final RTown town : activeTowns) {
            if (town.getIdentifier().equals(currentTown.getIdentifier())) {
                continue;
            }

            if (this.findTownChunk(town, chunkX, chunkZ) != null) {
                return town;
            }

            if (this.isNexusChunk(town, chunkX, chunkZ)) {
                return town;
            }
        }

        return null;
    }

    private @NotNull List<RTown> resolveActiveTownsSnapshot(final @NotNull Context context) {
        final RDT plugin = this.resolvePlugin(context);
        if (plugin == null || plugin.getTownRepository() == null) {
            return List.of();
        }
        return plugin.getTownRepository().findAllByAttributes(Map.of("active", true));
    }

    private @Nullable GridPosition findGridPositionForSlot(final int slot) {
        for (final Map.Entry<GridPosition, Integer> entry : CHUNK_SLOT_MAPPING.entrySet()) {
            if (entry.getValue() == slot) {
                return entry.getKey();
            }
        }
        return null;
    }

    private @Nullable RTown resolveTown(
            final @NotNull Context context,
            final @NotNull Player player
    ) {
        final RDT plugin = this.resolvePlugin(context);
        if (plugin == null || plugin.getTownRepository() == null) {
            return null;
        }

        final UUID resolvedTownUuid = this.resolveTownUuid(context, player, plugin);
        if (resolvedTownUuid == null) {
            return null;
        }

        return plugin.getTownRepository().findByTownUUID(resolvedTownUuid);
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
        } catch (final Exception ignored) {
        }

        if (plugin.getPlayerRepository() == null) {
            return null;
        }

        final RDTPlayer townPlayer = plugin.getPlayerRepository().findByPlayer(player.getUniqueId());
        if (townPlayer == null) {
            return null;
        }
        return townPlayer.getTownUUID();
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
        } catch (final Exception ignored) {
            return null;
        }
    }

    private static @NotNull Map<GridPosition, Integer> createChunkSlotMapping() {
        final Map<GridPosition, Integer> mapping = new HashMap<>();

        mapping.put(new GridPosition(0, 0), 1);
        mapping.put(new GridPosition(1, 0), 2);
        mapping.put(new GridPosition(2, 0), 3);
        mapping.put(new GridPosition(3, 0), 5);
        mapping.put(new GridPosition(4, 0), 6);
        mapping.put(new GridPosition(5, 0), 7);

        mapping.put(new GridPosition(0, 1), 10);
        mapping.put(new GridPosition(1, 1), 11);
        mapping.put(new GridPosition(2, 1), 12);
        mapping.put(new GridPosition(3, 1), 13);
        mapping.put(new GridPosition(4, 1), 14);
        mapping.put(new GridPosition(5, 1), 15);
        mapping.put(new GridPosition(6, 1), 16);

        mapping.put(new GridPosition(0, 2), 19);
        mapping.put(new GridPosition(1, 2), 20);
        mapping.put(new GridPosition(2, 2), 21);
        mapping.put(new GridPosition(3, 2), 22);
        mapping.put(new GridPosition(4, 2), 23);
        mapping.put(new GridPosition(5, 2), 24);
        mapping.put(new GridPosition(6, 2), 25);

        mapping.put(new GridPosition(0, 3), 28);
        mapping.put(new GridPosition(1, 3), 29);
        mapping.put(new GridPosition(2, 3), 30);
        mapping.put(new GridPosition(3, 3), 31);
        mapping.put(new GridPosition(4, 3), 32);
        mapping.put(new GridPosition(5, 3), 33);
        mapping.put(new GridPosition(6, 3), 34);

        mapping.put(new GridPosition(0, 4), 37);
        mapping.put(new GridPosition(1, 4), 38);
        mapping.put(new GridPosition(2, 4), 39);
        mapping.put(new GridPosition(3, 4), 40);
        mapping.put(new GridPosition(4, 4), 41);
        mapping.put(new GridPosition(5, 4), 42);
        mapping.put(new GridPosition(6, 4), 43);

        return mapping;
    }

    private static @NotNull List<Integer> createAllChunkSlots() {
        final List<Integer> slots = new ArrayList<>();
        slots.addAll(List.of(1, 2, 3, 5, 6, 7));
        slots.addAll(List.of(10, 11, 12, 13, 14, 15, 16));
        slots.addAll(List.of(19, 20, 21, 22, 23, 24, 25));
        slots.addAll(List.of(28, 29, 30, 31, 32, 33, 34));
        slots.addAll(List.of(37, 38, 39, 40, 41, 42, 43));
        return slots;
    }

    private record GridPosition(int x, int y) {
    }

    private record ChunkCoordinate(int x, int z) {
    }
}
