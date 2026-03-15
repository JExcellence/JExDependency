package com.raindropcentral.rdt.view.town;

import java.util.Map;
import java.util.UUID;

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
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;

import com.raindropcentral.rdt.RDT;
import com.raindropcentral.rdt.database.entity.RChunk;
import com.raindropcentral.rdt.database.entity.RDTPlayer;
import com.raindropcentral.rdt.database.entity.RTown;
import com.raindropcentral.rdt.database.repository.RRDTPlayer;
import com.raindropcentral.rdt.database.repository.RRTown;
import com.raindropcentral.rdt.items.ChunkBlock;
import com.raindropcentral.rdt.utils.ChunkBlockState;
import com.raindropcentral.rdt.utils.ChunkType;
import com.raindropcentral.rdt.utils.TownPermissions;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.BaseView;

/**
 * Landing view for a placed Chunk Block that allows opening chunk-type upgrades.
 *
 * <p>This view is opened from right-clicking a managed Chunk Block and requires
 * {@link TownPermissions#UPGRADE_CHUNK}.</p>
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.4
 */
public final class TownChunkView extends BaseView {

    private final State<RDT> rdt = initialState("plugin");
    private final State<UUID> townUuid = initialState("town_uuid");
    private final State<Integer> chunkX = initialState("chunk_x");
    private final State<Integer> chunkZ = initialState("chunk_z");
    private final State<Integer> blockX = initialState("block_x");
    private final State<Integer> blockY = initialState("block_y");
    private final State<Integer> blockZ = initialState("block_z");
    private final State<String> blockWorld = initialState("block_world");

    /**
     * Returns the i18n key namespace for this view.
     *
     * @return translation key prefix
     */
    @Override
    protected @NotNull String getKey() {
        return "town_chunk_ui";
    }

    /**
     * Returns the inventory row count.
     *
     * @return row count
     */
    @Override
    protected int getSize() {
        return 1;
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
     * Renders chunk info and chunk type upgrade actions.
     *
     * @param render render context
     * @param player viewing player
     */
    @Override
    public void onFirstRender(final @NotNull RenderContext render, final @NotNull Player player) {
        final RDT plugin = this.resolvePlugin(render);
        if (plugin == null) {
            this.i18n("error.system_unavailable", player).includePrefix().build().sendMessage();
            player.closeInventory();
            return;
        }

        final RTown town = this.resolveTown(render, player);
        if (town == null) {
            this.i18n("error.town_unavailable", player).includePrefix().build().sendMessage();
            player.closeInventory();
            return;
        }

        final RDTPlayer townPlayer = this.resolveTownPlayer(render, player);
        if (!town.hasTownPermission(townPlayer, TownPermissions.UPGRADE_CHUNK)) {
            this.i18n("error.no_permission", player)
                    .includePrefix()
                    .withPlaceholder("permission", TownPermissions.UPGRADE_CHUNK.getPermissionKey())
                    .build()
                    .sendMessage();
            player.closeInventory();
            return;
        }

        final RChunk chunk = this.resolveTargetChunk(render, town);
        if (chunk == null) {
            this.i18n("error.chunk_unavailable", player).includePrefix().build().sendMessage();
            player.closeInventory();
            return;
        }

        final ChunkType displayChunkType = this.resolveDisplayedChunkType(plugin, town, chunk);
        render.slot(1, 4).withItem(this.buildChunkInfoItem(player, plugin, town, chunk, displayChunkType));
        render.slot(1, 5)
                .withItem(this.buildPickupItem(player, chunk, displayChunkType))
                .onClick(this::handlePickupClick);
        render.slot(1, 6)
                .withItem(this.buildSelectTypeItem(player, chunk, displayChunkType))
                .onClick(this::handleSelectTypeClick);
        render.slot(1, 7)
                .withItem(this.buildProtectionsItem(player, chunk))
                .onClick(this::handleOpenProtectionsClick);
    }

    private void handleSelectTypeClick(final @NotNull SlotClickContext click) {
        click.setCancelled(true);
        final Player player = click.getPlayer();
        final RDT plugin = this.resolvePlugin(click);
        if (plugin == null) {
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

        final Integer resolvedChunkX = this.resolveChunkX(click);
        final Integer resolvedChunkZ = this.resolveChunkZ(click);
        if (resolvedChunkX == null || resolvedChunkZ == null) {
            this.i18n("error.chunk_unavailable", player).includePrefix().build().sendMessage();
            return;
        }

        click.openForPlayer(
                TownChunkTypeView.class,
                Map.of(
                        "plugin", plugin,
                        "town_uuid", town.getIdentifier(),
                        "chunk_x", resolvedChunkX,
                        "chunk_z", resolvedChunkZ,
                        "block_x", this.resolveBlockX(click),
                        "block_y", this.resolveBlockY(click),
                        "block_z", this.resolveBlockZ(click),
                        "block_world", this.resolveBlockWorld(click)
                )
        );
    }

    private @NotNull ItemStack buildChunkInfoItem(
            final @NotNull Player player,
            final @NotNull RDT plugin,
            final @NotNull RTown town,
            final @NotNull RChunk chunk,
            final @NotNull ChunkType displayChunkType
    ) {
        final Material material = plugin.getDefaultConfig().getChunkTypeIconMaterial(displayChunkType);

        return UnifiedBuilderFactory.item(material)
                .setName(this.i18n("chunk.name", player)
                        .withPlaceholders(Map.of(
                                "town_name", town.getTownName(),
                                "chunk_x", chunk.getX_loc(),
                                "chunk_z", chunk.getZ_loc()
                        ))
                        .build()
                        .component())
                .setLore(this.i18n("chunk.lore", player)
                        .withPlaceholders(Map.of(
                                "town_name", town.getTownName(),
                                "chunk_x", chunk.getX_loc(),
                                "chunk_z", chunk.getZ_loc(),
                                "chunk_type", displayChunkType.name()
                        ))
                        .build()
                        .children())
                .build();
    }

    private @NotNull ItemStack buildPickupItem(
            final @NotNull Player player,
            final @NotNull RChunk chunk,
            final @NotNull ChunkType displayChunkType
    ) {
        return UnifiedBuilderFactory.item(Material.HOPPER)
                .setName(this.i18n("pickup.name", player)
                        .withPlaceholders(Map.of(
                                "chunk_x", chunk.getX_loc(),
                                "chunk_z", chunk.getZ_loc(),
                                "chunk_type", displayChunkType.name()
                        ))
                        .build()
                        .component())
                .setLore(this.i18n("pickup.lore", player)
                        .withPlaceholders(Map.of(
                                "chunk_x", chunk.getX_loc(),
                                "chunk_z", chunk.getZ_loc(),
                                "chunk_type", displayChunkType.name(),
                                "permission", TownPermissions.PICKUP_CHUNK.getPermissionKey()
                        ))
                        .build()
                        .children())
                .build();
    }

    private @NotNull ItemStack buildSelectTypeItem(
            final @NotNull Player player,
            final @NotNull RChunk chunk,
            final @NotNull ChunkType displayChunkType
    ) {
        return UnifiedBuilderFactory.item(Material.WRITABLE_BOOK)
                .setName(this.i18n("select_type.name", player)
                        .withPlaceholders(Map.of(
                                "chunk_x", chunk.getX_loc(),
                                "chunk_z", chunk.getZ_loc()
                        ))
                        .build()
                        .component())
                .setLore(this.i18n("select_type.lore", player)
                        .withPlaceholders(Map.of(
                                "chunk_x", chunk.getX_loc(),
                                "chunk_z", chunk.getZ_loc(),
                                "chunk_type", displayChunkType.name()
                        ))
                        .build()
                        .children())
                .build();
    }

    private @NotNull ItemStack buildProtectionsItem(
            final @NotNull Player player,
            final @NotNull RChunk chunk
    ) {
        return UnifiedBuilderFactory.item(Material.SHIELD)
                .setName(this.i18n("protections.name", player)
                        .withPlaceholders(Map.of(
                                "chunk_x", chunk.getX_loc(),
                                "chunk_z", chunk.getZ_loc()
                        ))
                        .build()
                        .component())
                .setLore(this.i18n("protections.lore", player)
                        .withPlaceholders(Map.of(
                                "chunk_x", chunk.getX_loc(),
                                "chunk_z", chunk.getZ_loc(),
                                "permission", TownPermissions.TOWN_PROTECTIONS.getPermissionKey()
                        ))
                        .build()
                        .children())
                .build();
    }

    private void handlePickupClick(final @NotNull SlotClickContext click) {
        click.setCancelled(true);
        final Player player = click.getPlayer();
        final RDT plugin = this.resolvePlugin(click);
        if (plugin == null) {
            this.i18n("error.system_unavailable", player).includePrefix().build().sendMessage();
            return;
        }

        final RTown town = this.resolveTown(click, player);
        if (town == null) {
            this.i18n("error.town_unavailable", player).includePrefix().build().sendMessage();
            return;
        }

        final RDTPlayer townPlayer = this.resolveTownPlayer(click, player);
        if (!town.hasTownPermission(townPlayer, TownPermissions.PICKUP_CHUNK)) {
            this.i18n("pickup.error.no_permission", player)
                    .includePrefix()
                    .withPlaceholder("permission", TownPermissions.PICKUP_CHUNK.getPermissionKey())
                    .build()
                    .sendMessage();
            return;
        }

        final RChunk chunk = this.resolveTargetChunk(click, town);
        if (chunk == null) {
            this.i18n("error.chunk_unavailable", player).includePrefix().build().sendMessage();
            return;
        }

        final Location blockLocation = this.resolveBlockLocation(click);
        if (blockLocation == null) {
            this.i18n("pickup.error.block_unavailable", player).includePrefix().build().sendMessage();
            return;
        }
        final boolean sameChunk = blockLocation.getChunk().getX() == chunk.getX_loc()
                && blockLocation.getChunk().getZ() == chunk.getZ_loc();
        if (!sameChunk) {
            this.i18n("pickup.error.block_unavailable", player).includePrefix().build().sendMessage();
            return;
        }
        if (!this.matchesStoredChunkBlockLocation(chunk, blockLocation)) {
            this.i18n("pickup.error.block_unavailable", player).includePrefix().build().sendMessage();
            return;
        }

        final Material expectedMaterial = plugin.getDefaultConfig().getChunkTypeIconMaterial(chunk.getType());
        if (blockLocation.getBlock().getType() != expectedMaterial) {
            this.i18n("pickup.error.block_unavailable", player).includePrefix().build().sendMessage();
            return;
        }

        final RRTown townRepository = plugin.getTownRepository();
        if (townRepository == null) {
            this.i18n("error.system_unavailable", player).includePrefix().build().sendMessage();
            return;
        }
        final ChunkType displayChunkType = this.resolveDisplayedChunkType(plugin, town, chunk);

        blockLocation.getBlock().setType(Material.AIR, false);
        chunk.clearChunkBlockLocation();
        townRepository.update(town);

        final ItemStack pickupItem = ChunkBlock.getChunkBlockItem(
                plugin,
                town.getIdentifier(),
                town.getTownName(),
                town.getMayor(),
                chunk.getX_loc(),
                chunk.getZ_loc(),
                chunk.getType()
        );

        final Map<Integer, ItemStack> leftovers = player.getInventory().addItem(pickupItem);
        if (leftovers.isEmpty()) {
            this.i18n("pickup.success.given", player)
                    .includePrefix()
                    .withPlaceholders(Map.of(
                            "chunk_x", chunk.getX_loc(),
                            "chunk_z", chunk.getZ_loc(),
                            "chunk_type", displayChunkType.name()
                    ))
                    .build()
                    .sendMessage();
        } else {
            leftovers.values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
            this.i18n("pickup.success.dropped", player)
                    .includePrefix()
                    .withPlaceholders(Map.of(
                            "chunk_x", chunk.getX_loc(),
                            "chunk_z", chunk.getZ_loc(),
                            "chunk_type", displayChunkType.name()
                    ))
                    .build()
                    .sendMessage();
        }

        click.openForPlayer(
                TownChunkView.class,
                Map.of(
                        "plugin", plugin,
                        "town_uuid", town.getIdentifier(),
                        "chunk_x", chunk.getX_loc(),
                        "chunk_z", chunk.getZ_loc(),
                        "block_x", blockLocation.getBlockX(),
                        "block_y", blockLocation.getBlockY(),
                        "block_z", blockLocation.getBlockZ(),
                        "block_world", blockLocation.getWorld().getName()
                )
        );
    }

    private void handleOpenProtectionsClick(final @NotNull SlotClickContext click) {
        click.setCancelled(true);
        final Player player = click.getPlayer();
        final RDT plugin = this.resolvePlugin(click);
        if (plugin == null) {
            this.i18n("error.system_unavailable", player).includePrefix().build().sendMessage();
            return;
        }

        final RTown town = this.resolveTown(click, player);
        if (town == null) {
            this.i18n("error.town_unavailable", player).includePrefix().build().sendMessage();
            return;
        }

        final RDTPlayer townPlayer = this.resolveTownPlayer(click, player);
        if (!town.hasTownPermission(townPlayer, TownPermissions.TOWN_PROTECTIONS)) {
            this.i18n("protections.error.no_permission", player)
                    .includePrefix()
                    .withPlaceholder("permission", TownPermissions.TOWN_PROTECTIONS.getPermissionKey())
                    .build()
                    .sendMessage();
            return;
        }

        final Integer resolvedChunkX = this.resolveChunkX(click);
        final Integer resolvedChunkZ = this.resolveChunkZ(click);
        if (resolvedChunkX == null || resolvedChunkZ == null) {
            this.i18n("error.chunk_unavailable", player).includePrefix().build().sendMessage();
            return;
        }

        final Map<String, Object> openData = new java.util.HashMap<>();
        openData.put("plugin", plugin);
        openData.put("town_uuid", town.getIdentifier());
        openData.put("protection_scope", "chunk");
        openData.put("chunk_x", resolvedChunkX);
        openData.put("chunk_z", resolvedChunkZ);

        final Integer resolvedBlockX = this.resolveBlockX(click);
        final Integer resolvedBlockY = this.resolveBlockY(click);
        final Integer resolvedBlockZ = this.resolveBlockZ(click);
        final String resolvedBlockWorld = this.resolveBlockWorld(click);
        if (resolvedBlockX != null) {
            openData.put("block_x", resolvedBlockX);
        }
        if (resolvedBlockY != null) {
            openData.put("block_y", resolvedBlockY);
        }
        if (resolvedBlockZ != null) {
            openData.put("block_z", resolvedBlockZ);
        }
        if (resolvedBlockWorld != null && !resolvedBlockWorld.isBlank()) {
            openData.put("block_world", resolvedBlockWorld);
        }

        click.openForPlayer(TownProtectionsView.class, openData);
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
            final @NotNull RDT plugin,
            final @NotNull RTown town,
            final @NotNull RChunk chunk
    ) {
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
}
