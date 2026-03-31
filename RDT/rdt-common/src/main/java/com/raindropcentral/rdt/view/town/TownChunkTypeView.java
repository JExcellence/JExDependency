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

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import me.devnatan.inventoryframework.component.BukkitItemComponentBuilder;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.OpenContext;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.context.SlotClickContext;
import me.devnatan.inventoryframework.state.State;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;

import com.raindropcentral.rdt.RDT;
import com.raindropcentral.rdt.database.entity.RChunk;
import com.raindropcentral.rdt.database.entity.RDTPlayer;
import com.raindropcentral.rdt.database.entity.RTown;
import com.raindropcentral.rdt.database.repository.RRDTPlayer;
import com.raindropcentral.rdt.database.repository.RRTown;
import com.raindropcentral.rdt.utils.ChunkBlockState;
import com.raindropcentral.rdt.utils.ChunkType;
import com.raindropcentral.rdt.utils.TownPermissions;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.APaginatedView;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

/**
 * Paginated selector for changing a chunk's {@link ChunkType}.
 *
 * <p>This view is opened from {@link TownChunkView} and updates the target chunk type on click.</p>
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.4
 */
public final class TownChunkTypeView extends APaginatedView<ChunkType> {

    private final State<RDT> rdt = initialState("plugin");
    private final State<UUID> townUuid = initialState("town_uuid");
    private final State<Integer> chunkX = initialState("chunk_x");
    private final State<Integer> chunkZ = initialState("chunk_z");
    private final State<Integer> blockX = initialState("block_x");
    private final State<Integer> blockY = initialState("block_y");
    private final State<Integer> blockZ = initialState("block_z");
    private final State<String> blockWorld = initialState("block_world");

    /**
     * Creates this paginated chunk-type selection view with back navigation.
     */
    public TownChunkTypeView() {
        super(TownChunkView.class);
    }

    /**
     * Returns the i18n key namespace for this view.
     *
     * @return translation key prefix
     */
    @Override
    protected @NotNull String getKey() {
        return "town_chunk_type_ui";
    }

    /**
     * Resolves placeholders for this view title.
     *
     * @param openContext open context
     * @return title placeholders
     */
    @Override
    protected @NotNull Map<String, Object> getTitlePlaceholders(final @NotNull OpenContext openContext) {
        final RTown town = this.resolveTown(openContext, openContext.getPlayer());
        final @Nullable Integer resolvedChunkX = this.resolveChunkX(openContext);
        final @Nullable Integer resolvedChunkZ = this.resolveChunkZ(openContext);
        return Map.of(
                "town_name", town == null ? "Unknown" : town.getTownName(),
                "chunk_x", resolvedChunkX == null ? "-" : resolvedChunkX,
                "chunk_z", resolvedChunkZ == null ? "-" : resolvedChunkZ
        );
    }

    /**
     * Cancels default inventory movement behavior.
     *
     * @param click click context
     */
    @Override
    public void onClick(final @NotNull SlotClickContext click) {
        click.setCancelled(true);
    }

    /**
     * Verifies viewer context and access before rendering paginated entries.
     *
     * @param render render context
     * @param player viewing player
     */
    @Override
    public void onFirstRender(final @NotNull RenderContext render, final @NotNull Player player) {
        if (!this.verifyViewerAccess(render, player)) {
            player.closeInventory();
            return;
        }
        super.onFirstRender(render, player);
    }

    /**
     * Loads all chunk types for pagination.
     *
     * @param context view context
     * @return future chunk type list
     */
    @Override
    protected @NotNull CompletableFuture<List<ChunkType>> getAsyncPaginationSource(final @NotNull Context context) {
        final List<ChunkType> types = Arrays.stream(ChunkType.values())
                .filter(type -> type != ChunkType.CLAIM_PENDING)
                .filter(type -> type != ChunkType.CHUNK_BLOCK)
                .sorted(Comparator.comparing(ChunkType::name))
                .toList();
        return CompletableFuture.completedFuture(types);
    }

    /**
     * Renders an individual chunk-type selection entry.
     *
     * @param context context
     * @param builder builder
     * @param index index
     * @param entry chunk type entry
     */
    @Override
    protected void renderEntry(
            final @NotNull Context context,
            final @NotNull BukkitItemComponentBuilder builder,
            final int index,
            final @NotNull ChunkType entry
    ) {
        final Player viewer = context.getPlayer();
        final RTown town = this.resolveTown(context, viewer);
        if (town == null) {
            builder.withItem(
                    UnifiedBuilderFactory.item(Material.BARRIER)
                            .setName(this.i18n("type.unavailable.name", viewer).build().component())
                            .setLore(this.i18n("type.unavailable.lore", viewer).build().children())
                            .build()
            );
            return;
        }

        final RChunk targetChunk = this.resolveTargetChunk(context, town);
        if (targetChunk == null) {
            builder.withItem(
                    UnifiedBuilderFactory.item(Material.BARRIER)
                            .setName(this.i18n("type.unavailable.name", viewer).build().component())
                            .setLore(this.i18n("type.unavailable.lore", viewer).build().children())
                            .build()
            );
            return;
        }

        final ChunkType currentEffectiveType = this.resolveDisplayedChunkType(context, town, targetChunk);
        final boolean selected = currentEffectiveType == entry;
        final String selectedState = this.toPlain(
                viewer,
                selected ? "type.state.selected" : "type.state.not_selected"
        );

        builder.withItem(
                UnifiedBuilderFactory.item(this.resolveDisplayMaterial(context, entry, selected))
                        .setName(this.i18n("type.name", viewer)
                                .withPlaceholders(Map.of(
                                        "chunk_type", entry.name(),
                                        "selected_state", selectedState
                                ))
                                .build()
                                .component())
                        .setLore(this.i18n("type.lore", viewer)
                                .withPlaceholders(Map.of(
                                        "chunk_type", entry.name(),
                                        "selected_state", selectedState,
                                        "chunk_x", targetChunk.getX_loc(),
                                        "chunk_z", targetChunk.getZ_loc()
                                ))
                                .build()
                                .children())
                        .build()
        ).onClick(click -> this.handleTypeSelection(click, entry));
    }

    /**
     * Renders additional fixed controls for this paginated view.
     *
     * @param render render context
     * @param player viewer
     */
    @Override
    protected void onPaginatedRender(final @NotNull RenderContext render, final @NotNull Player player) {
    }

    private boolean verifyViewerAccess(
            final @NotNull Context context,
            final @NotNull Player player
    ) {
        final RTown town = this.resolveTown(context, player);
        if (town == null) {
            this.i18n("error.town_unavailable", player).includePrefix().build().sendMessage();
            return false;
        }

        final RDTPlayer townPlayer = this.resolveTownPlayer(context, player);
        if (!town.hasTownPermission(townPlayer, TownPermissions.UPGRADE_CHUNK)) {
            this.i18n("error.no_permission", player)
                    .includePrefix()
                    .withPlaceholder("permission", TownPermissions.UPGRADE_CHUNK.getPermissionKey())
                    .build()
                    .sendMessage();
            return false;
        }

        final RChunk targetChunk = this.resolveTargetChunk(context, town);
        if (targetChunk == null) {
            this.i18n("error.chunk_unavailable", player).includePrefix().build().sendMessage();
            return false;
        }
        final Location targetBlockLocation = this.resolveBlockLocation(context);
        if (targetBlockLocation == null) {
            this.i18n("error.block_unavailable", player).includePrefix().build().sendMessage();
            return false;
        }

        final boolean sameChunk = targetBlockLocation.getChunk().getX() == targetChunk.getX_loc()
                && targetBlockLocation.getChunk().getZ() == targetChunk.getZ_loc();
        if (!sameChunk) {
            this.i18n("error.block_unavailable", player).includePrefix().build().sendMessage();
            return false;
        }
        if (!this.matchesStoredChunkBlockLocation(targetChunk, targetBlockLocation)) {
            this.i18n("error.block_unavailable", player).includePrefix().build().sendMessage();
            return false;
        }
        return true;
    }

    private void handleTypeSelection(
            final @NotNull SlotClickContext click,
            final @NotNull ChunkType selectedType
    ) {
        click.setCancelled(true);

        final Player player = click.getPlayer();
        final RDT plugin = this.resolvePlugin(click);
        if (plugin == null || plugin.getTownRepository() == null) {
            this.i18n("error.system_unavailable", player).includePrefix().build().sendMessage();
            return;
        }

        final RTown town = this.resolveTown(click, player);
        if (town == null) {
            this.i18n("error.town_unavailable", player).includePrefix().build().sendMessage();
            return;
        }

        final RDTPlayer townPlayer = this.resolveTownPlayer(click, player);
        if (!town.hasTownPermission(townPlayer, TownPermissions.UPGRADE_CHUNK)) {
            this.i18n("error.no_permission", player)
                    .includePrefix()
                    .withPlaceholder("permission", TownPermissions.UPGRADE_CHUNK.getPermissionKey())
                    .build()
                    .sendMessage();
            return;
        }

        final RChunk targetChunk = this.resolveTargetChunk(click, town);
        if (targetChunk == null) {
            this.i18n("error.chunk_unavailable", player).includePrefix().build().sendMessage();
            return;
        }

        final Location targetBlockLocation = this.resolveBlockLocation(click);
        if (targetBlockLocation == null) {
            this.i18n("error.block_unavailable", player).includePrefix().build().sendMessage();
            return;
        }
        final boolean sameChunk = targetBlockLocation.getChunk().getX() == targetChunk.getX_loc()
                && targetBlockLocation.getChunk().getZ() == targetChunk.getZ_loc();
        if (!sameChunk) {
            this.i18n("error.block_unavailable", player).includePrefix().build().sendMessage();
            return;
        }
        if (!this.matchesStoredChunkBlockLocation(targetChunk, targetBlockLocation)) {
            this.i18n("error.block_unavailable", player).includePrefix().build().sendMessage();
            return;
        }

        final ChunkType previousType = this.resolveDisplayedChunkType(click, town, targetChunk);
        if (previousType == selectedType) {
            this.i18n("message.already_selected", player)
                    .includePrefix()
                    .withPlaceholders(Map.of(
                            "chunk_type", selectedType.name(),
                            "chunk_x", targetChunk.getX_loc(),
                            "chunk_z", targetChunk.getZ_loc()
                    ))
                    .build()
                    .sendMessage();
        } else {
            targetChunk.setType(selectedType);
            targetBlockLocation.getBlock().setType(
                    plugin.getDefaultConfig().getChunkTypeIconMaterial(selectedType),
                    false
            );
            plugin.getTownRepository().update(town);

            this.i18n("message.updated", player)
                    .includePrefix()
                    .withPlaceholders(Map.of(
                            "chunk_x", targetChunk.getX_loc(),
                            "chunk_z", targetChunk.getZ_loc(),
                            "old_type", previousType.name(),
                            "new_type", selectedType.name()
                    ))
                    .build()
                    .sendMessage();
        }

        click.openForPlayer(
                TownChunkTypeView.class,
                Map.of(
                        "plugin", plugin,
                        "town_uuid", town.getIdentifier(),
                        "chunk_x", targetChunk.getX_loc(),
                        "chunk_z", targetChunk.getZ_loc(),
                        "block_x", targetBlockLocation.getBlockX(),
                        "block_y", targetBlockLocation.getBlockY(),
                        "block_z", targetBlockLocation.getBlockZ(),
                        "block_world", targetBlockLocation.getWorld().getName()
                )
        );
    }

    private @NotNull Material resolveDisplayMaterial(
            final @NotNull Context context,
            final @NotNull ChunkType type,
            final boolean selected
    ) {
        if (selected) {
            return Material.LIME_DYE;
        }

        final RDT plugin = this.resolvePlugin(context);
        if (plugin == null) {
            return Material.BARRIER;
        }
        return plugin.getDefaultConfig().getChunkTypeIconMaterial(type);
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

        final UUID resolvedTownUuid = this.resolveTownUuid(context, player, plugin);
        if (resolvedTownUuid == null) {
            return null;
        }
        return townRepository.findByTownUUID(resolvedTownUuid);
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

        final RRDTPlayer playerRepository = plugin.getPlayerRepository();
        if (playerRepository == null) {
            return null;
        }

        final RDTPlayer townPlayer = playerRepository.findByPlayer(player.getUniqueId());
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

    private @Nullable RChunk resolveTargetChunk(
            final @NotNull Context context,
            final @NotNull RTown town
    ) {
        final Integer resolvedChunkX = this.resolveChunkX(context);
        final Integer resolvedChunkZ = this.resolveChunkZ(context);
        if (resolvedChunkX == null || resolvedChunkZ == null) {
            return null;
        }

        for (final RChunk chunk : town.getChunks()) {
            if (chunk.getX_loc() == resolvedChunkX && chunk.getZ_loc() == resolvedChunkZ) {
                return chunk;
            }
        }
        return null;
    }

    private @Nullable Integer resolveChunkX(final @NotNull Context context) {
        try {
            return this.chunkX.get(context);
        } catch (final Exception ignored) {
            return null;
        }
    }

    private @Nullable Integer resolveChunkZ(final @NotNull Context context) {
        try {
            return this.chunkZ.get(context);
        } catch (final Exception ignored) {
            return null;
        }
    }

    private @Nullable Integer resolveBlockX(final @NotNull Context context) {
        try {
            return this.blockX.get(context);
        } catch (final Exception ignored) {
            return null;
        }
    }

    private @Nullable Integer resolveBlockY(final @NotNull Context context) {
        try {
            return this.blockY.get(context);
        } catch (final Exception ignored) {
            return null;
        }
    }

    private @Nullable Integer resolveBlockZ(final @NotNull Context context) {
        try {
            return this.blockZ.get(context);
        } catch (final Exception ignored) {
            return null;
        }
    }

    private @Nullable String resolveBlockWorld(final @NotNull Context context) {
        try {
            return this.blockWorld.get(context);
        } catch (final Exception ignored) {
            return null;
        }
    }

    private @Nullable Location resolveBlockLocation(final @NotNull Context context) {
        final Integer x = this.resolveBlockX(context);
        final Integer y = this.resolveBlockY(context);
        final Integer z = this.resolveBlockZ(context);
        final String worldName = this.resolveBlockWorld(context);
        if (x == null || y == null || z == null || worldName == null || worldName.isBlank()) {
            return null;
        }

        final World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return null;
        }
        return new Location(world, x, y, z).toBlockLocation();
    }

    private @NotNull ChunkType resolveDisplayedChunkType(
            final @NotNull Context context,
            final @NotNull RTown town,
            final @NotNull RChunk chunk
    ) {
        final RDT plugin = this.resolvePlugin(context);
        if (plugin == null) {
            return chunk.getType();
        }
        return ChunkBlockState.resolveEffectiveType(
                town,
                chunk,
                plugin.getDefaultConfig().getChunkBlockMinY(),
                plugin.getDefaultConfig().getChunkBlockMaxY()
        );
    }

    private boolean matchesStoredChunkBlockLocation(
            final @NotNull RChunk chunk,
            final @NotNull Location blockLocation
    ) {
        final String storedWorld = chunk.getChunkBlockWorld();
        final Integer storedX = chunk.getChunkBlockX();
        final Integer storedY = chunk.getChunkBlockY();
        final Integer storedZ = chunk.getChunkBlockZ();
        if (storedWorld == null || storedX == null || storedY == null || storedZ == null) {
            return false;
        }

        return storedWorld.equals(blockLocation.getWorld().getName())
                && storedX == blockLocation.getBlockX()
                && storedY == blockLocation.getBlockY()
                && storedZ == blockLocation.getBlockZ();
    }

    private @Nullable RDT resolvePlugin(final @NotNull Context context) {
        try {
            return this.rdt.get(context);
        } catch (final Exception ignored) {
            return null;
        }
    }

    private @NotNull String toPlain(
            final @NotNull Player player,
            final @NotNull String key
    ) {
        return PlainTextComponentSerializer.plainText().serialize(
                this.i18n(key, player).build().component()
        );
    }
}
