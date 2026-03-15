package com.raindropcentral.rdt.listeners;

import com.raindropcentral.rdt.RDT;
import com.raindropcentral.rdt.database.entity.RChunk;
import com.raindropcentral.rdt.database.entity.RDTPlayer;
import com.raindropcentral.rdt.database.entity.RTown;
import com.raindropcentral.rdt.database.entity.TownRole;
import com.raindropcentral.rdt.database.repository.RRDTPlayer;
import com.raindropcentral.rdt.database.repository.RRTown;
import com.raindropcentral.rdt.items.ChunkBlock;
import com.raindropcentral.rdt.items.Nexus;
import com.raindropcentral.rdt.utils.ChunkBlockState;
import com.raindropcentral.rdt.utils.ChunkType;
import com.raindropcentral.rdt.utils.TownPermissions;
import com.raindropcentral.rdt.view.town.TownChunkView;
import com.raindropcentral.rdt.view.town.TownInfoView;
import de.jexcellence.jextranslate.i18n.I18n;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Handles placement and break flow for town Nexus and Chunk Block items.
 *
 * <p>This listener enforces Nexus ownership rules, pending-claim Chunk Block placement rules,
 * and open-air side constraints for placed Chunk Blocks.</p>
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.14
 */
@SuppressWarnings("unused")
public final class BlockListener implements Listener {

    private static final int REQUIRED_OPEN_SIDES = 2;
    private static final List<BlockFace> ADJACENT_FACES = List.of(
            BlockFace.NORTH,
            BlockFace.SOUTH,
            BlockFace.EAST,
            BlockFace.WEST,
            BlockFace.UP,
            BlockFace.DOWN
    );

    private final RDT plugin;

    /**
     * Creates the nexus placement listener.
     *
     * @param plugin active RDT runtime
     */
    public BlockListener(final @NotNull RDT plugin) {
        this.plugin = plugin;
    }

    /**
     * Routes block placement events for Nexus placement, Chunk Block placement, and Chunk Block.
     * side-coverage protection.
     *
     * @param event block place event
     */
    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(final @NotNull BlockPlaceEvent event) {
        final ItemStack placedItem = event.getItemInHand();
        this.enforceChunkBlockNeighborConstraint(event);
        if (event.isCancelled()) {
            return;
        }

        if (placedItem != null && ChunkBlock.equals(this.plugin, placedItem)) {
            this.handleChunkBlockPlacement(event, placedItem);
            return;
        }

        if (placedItem != null && Nexus.equals(this.plugin, placedItem)) {
            this.handleNexusPlacement(event, placedItem);
        }
    }

    /**
     * Opens the Town Info view when a player right-clicks a placed nexus block.
     *
     * @param event player interact event
     */
    @EventHandler(ignoreCancelled = true)
    public void onPlayerInteract(final @NotNull PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) {
            return;
        }

        final RRTown townRepository = this.plugin.getTownRepository();
        if (townRepository == null || this.plugin.getViewFrame() == null) {
            return;
        }

        final Block clickedBlock = event.getClickedBlock();
        final RTown town = townRepository.findByNexusLocation(clickedBlock.getLocation().toBlockLocation());
        if (town != null) {
            event.setCancelled(true);
            this.plugin.getViewFrame().open(
                    TownInfoView.class,
                    event.getPlayer(),
                    Map.of(
                            "plugin", this.plugin,
                            "town_uuid", town.getIdentifier()
                    )
            );
            return;
        }

        final RTown chunkBlockTown = this.resolveTownForManagedChunkBlock(clickedBlock);
        if (chunkBlockTown == null) {
            return;
        }

        final RRDTPlayer playerRepository = this.plugin.getPlayerRepository();
        if (playerRepository == null) {
            event.setCancelled(true);
            this.sendMessage(event.getPlayer(), "town_chunk_ui.error.system_unavailable", Map.of());
            return;
        }

        final RDTPlayer townPlayer = playerRepository.findByPlayer(event.getPlayer().getUniqueId());
        if (!chunkBlockTown.hasTownPermission(townPlayer, TownPermissions.UPGRADE_CHUNK)) {
            event.setCancelled(true);
            this.sendMessage(event.getPlayer(), "town_chunk_ui.error.no_permission", Map.of(
                    "permission", TownPermissions.UPGRADE_CHUNK.getPermissionKey()
            ));
            return;
        }

        final int chunkX = clickedBlock.getChunk().getX();
        final int chunkZ = clickedBlock.getChunk().getZ();
        final RChunk targetChunk = this.findTownChunk(chunkBlockTown, chunkX, chunkZ);
        if (targetChunk == null) {
            event.setCancelled(true);
            this.sendMessage(event.getPlayer(), "town_chunk_ui.error.chunk_unavailable", Map.of());
            return;
        }

        event.setCancelled(true);
        this.plugin.getViewFrame().open(
                TownChunkView.class,
                event.getPlayer(),
                Map.of(
                        "plugin", this.plugin,
                        "town_uuid", chunkBlockTown.getIdentifier(),
                        "chunk_x", targetChunk.getX_loc(),
                        "chunk_z", targetChunk.getZ_loc(),
                        "block_x", clickedBlock.getX(),
                        "block_y", clickedBlock.getY(),
                        "block_z", clickedBlock.getZ(),
                        "block_world", clickedBlock.getWorld().getName()
                )
        );
    }

    private void handleNexusPlacement(
            final @NotNull BlockPlaceEvent event,
            final @NotNull ItemStack placedItem
    ) {
        final Player player = event.getPlayer();
        final UUID townUuid = Nexus.getTownUUID(this.plugin, placedItem);
        final UUID mayorUuid = Nexus.getMayorUUID(this.plugin, placedItem);
        final String townName = Nexus.getTownName(this.plugin, placedItem);

        if (townUuid == null || mayorUuid == null || townName == null || townName.isBlank()) {
            event.setCancelled(true);
            this.sendMessage(player, "nexus_listener.error.invalid_item", Map.of());
            return;
        }

        if (!player.getUniqueId().equals(mayorUuid)) {
            event.setCancelled(true);
            this.sendMessage(player, "nexus_listener.error.only_mayor", Map.of());
            return;
        }

        final RRTown townRepository = this.plugin.getTownRepository();
        final RRDTPlayer playerRepository = this.plugin.getPlayerRepository();
        if (townRepository == null || playerRepository == null) {
            event.setCancelled(true);
            this.sendMessage(player, "nexus_listener.error.system_unavailable", Map.of());
            return;
        }

        final RDTPlayer existingPlayerRecord = playerRepository.findByPlayer(player.getUniqueId());
        final boolean joiningTownNow = existingPlayerRecord == null
                || existingPlayerRecord.getTownUUID() == null
                || !townUuid.equals(existingPlayerRecord.getTownUUID());
        if (
                existingPlayerRecord != null
                        && existingPlayerRecord.getTownUUID() != null
                        && !townUuid.equals(existingPlayerRecord.getTownUUID())
        ) {
            event.setCancelled(true);
            this.sendMessage(player, "nexus_listener.error.already_in_other_town", Map.of());
            return;
        }

        final RTown sameNameTown = townRepository.findByTName(townName);
        if (sameNameTown != null && !townUuid.equals(sameNameTown.getIdentifier())) {
            event.setCancelled(true);
            this.sendMessage(player, "nexus_listener.error.town_name_taken", Map.of("town_name", townName));
            return;
        }

        final Location placedLocation = this.resolvePlacedBlockLocation(event);
        final int placedChunkX = event.getBlockPlaced().getChunk().getX();
        final int placedChunkZ = event.getBlockPlaced().getChunk().getZ();
        RTown town = townRepository.findByTownUUID(townUuid);
        final boolean firstPlacement = town == null;
        final boolean relocatedNexus = !firstPlacement;
        boolean townRequiresUpdate = false;
        if (firstPlacement) {
            town = new RTown(
                    townUuid,
                    player.getUniqueId(),
                    townName,
                    placedLocation,
                    placedLocation
            );
            this.ensureNexusChunk(town, placedChunkX, placedChunkZ);
            townRepository.create(town);
        } else {
            if (!player.getUniqueId().equals(town.getMayor())) {
                event.setCancelled(true);
                this.sendMessage(player, "nexus_listener.error.only_mayor", Map.of());
                return;
            }

            if (town.hasNexusPlaced()) {
                event.setCancelled(true);
                this.sendMessage(player, "nexus_listener.error.already_placed", Map.of());
                return;
            }

            town.setNexusLocation(placedLocation);
            if (town.getTownSpawnLocation() == null) {
                town.setTownSpawnLocation(placedLocation);
            }
            town.setActive(true);
            this.ensureNexusChunk(town, placedChunkX, placedChunkZ);
            townRequiresUpdate = true;
        }

        final RDTPlayer mayorPlayerRecord = this.upsertMayorPlayerRecord(
                playerRepository,
                existingPlayerRecord,
                town,
                player.getUniqueId(),
                townUuid
        );

        if (firstPlacement && !this.hasTownMember(town, player.getUniqueId())) {
            town.addMember(mayorPlayerRecord);
            townRequiresUpdate = true;
        }

        if (joiningTownNow) {
            town.recordPlayerJoined(player.getUniqueId(), RTown.MAYOR_ROLE_ID);
            townRequiresUpdate = true;
        }

        final List<RChunk> inactiveChunks = this.resolveInactiveChunks(town);
        if (townRequiresUpdate) {
            townRepository.update(town);
        }

        if (this.plugin.getBossBarFactory() != null) {
            this.plugin.getBossBarFactory().run(player, placedChunkX, placedChunkZ);
        }

        this.sendMessage(player, "nexus_listener.success.placed", Map.of(
                "town_name", townName,
                "world", this.resolveWorldName(placedLocation),
                "x", placedLocation.getBlockX(),
                "y", placedLocation.getBlockY(),
                "z", placedLocation.getBlockZ()
        ));
        if (relocatedNexus) {
            this.sendInactiveChunkWarnings(player, inactiveChunks);
        }
    }

    private void handleChunkBlockPlacement(
            final @NotNull BlockPlaceEvent event,
            final @NotNull ItemStack placedItem
    ) {
        final Player player = event.getPlayer();
        final UUID townUuid = ChunkBlock.getTownUUID(this.plugin, placedItem);
        final UUID mayorUuid = ChunkBlock.getMayorUUID(this.plugin, placedItem);
        final String townName = ChunkBlock.getTownName(this.plugin, placedItem);
        final Integer targetChunkX = ChunkBlock.getChunkX(this.plugin, placedItem);
        final Integer targetChunkZ = ChunkBlock.getChunkZ(this.plugin, placedItem);
        final ChunkType itemChunkType = ChunkBlock.getChunkType(this.plugin, placedItem);

        if (townUuid == null
                || mayorUuid == null
                || townName == null
                || townName.isBlank()
                || targetChunkX == null
                || targetChunkZ == null
                || ChunkType.equalsType(itemChunkType, ChunkType.CLAIM_PENDING)) {
            event.setCancelled(true);
            this.sendMessage(player, "chunk_block_listener.error.invalid_item", Map.of());
            return;
        }

        final RRTown townRepository = this.plugin.getTownRepository();
        final RRDTPlayer playerRepository = this.plugin.getPlayerRepository();
        if (townRepository == null || playerRepository == null) {
            event.setCancelled(true);
            this.sendMessage(player, "chunk_block_listener.error.system_unavailable", Map.of());
            return;
        }

        final RTown town = townRepository.findByTownUUID(townUuid);
        if (town == null) {
            event.setCancelled(true);
            this.sendMessage(player, "chunk_block_listener.error.town_unavailable", Map.of());
            return;
        }
        if (!town.getTownName().equals(townName)) {
            event.setCancelled(true);
            this.sendMessage(player, "chunk_block_listener.error.invalid_item", Map.of());
            return;
        }
        if (!town.getMayor().equals(mayorUuid)) {
            event.setCancelled(true);
            this.sendMessage(player, "chunk_block_listener.error.invalid_item", Map.of());
            return;
        }

        final RDTPlayer townPlayer = playerRepository.findByPlayer(player.getUniqueId());
        if (!town.hasTownPermission(townPlayer, TownPermissions.PLACE_CHUNK)) {
            event.setCancelled(true);
            this.sendMessage(player, "chunk_block_listener.error.no_permission", Map.of(
                    "permission", TownPermissions.PLACE_CHUNK.getPermissionKey()
            ));
            return;
        }

        final int currentChunkX = event.getBlockPlaced().getChunk().getX();
        final int currentChunkZ = event.getBlockPlaced().getChunk().getZ();
        if (currentChunkX != targetChunkX || currentChunkZ != targetChunkZ) {
            event.setCancelled(true);
            this.sendMessage(player, "chunk_block_listener.error.wrong_chunk", Map.of(
                    "target_chunk_x", targetChunkX,
                    "target_chunk_z", targetChunkZ,
                    "current_chunk_x", currentChunkX,
                    "current_chunk_z", currentChunkZ
            ));
            return;
        }

        final @Nullable RChunk targetChunk = this.findTownChunk(town, targetChunkX, targetChunkZ);
        if (targetChunk == null) {
            event.setCancelled(true);
            this.sendMessage(player, "chunk_block_listener.error.chunk_not_pending", Map.of(
                    "chunk_x", targetChunkX,
                    "chunk_z", targetChunkZ
            ));
            return;
        }

        final boolean pendingPlacement = ChunkType.equalsType(targetChunk.getType(), ChunkType.CLAIM_PENDING);
        if (!pendingPlacement && !ChunkType.equalsType(targetChunk.getType(), itemChunkType)) {
            event.setCancelled(true);
            this.sendMessage(player, "chunk_block_listener.error.chunk_type_mismatch", Map.of(
                    "chunk_x", targetChunkX,
                    "chunk_z", targetChunkZ,
                    "expected_type", targetChunk.getType().name(),
                    "item_type", itemChunkType.name()
            ));
            return;
        }

        final Location nexusLocation = town.getNexusLocation();
        if (nexusLocation == null || nexusLocation.getWorld() == null) {
            event.setCancelled(true);
            this.sendMessage(player, "chunk_block_listener.error.nexus_unavailable", Map.of());
            return;
        }

        final String nexusWorld = nexusLocation.getWorld().getName();
        final String blockWorld = event.getBlockPlaced().getWorld().getName();
        if (!nexusWorld.equals(blockWorld)) {
            event.setCancelled(true);
            this.sendMessage(player, "chunk_block_listener.error.nexus_world_mismatch", Map.of(
                    "nexus_world", nexusWorld,
                    "block_world", blockWorld
            ));
            return;
        }

        final int minimumY = this.plugin.getDefaultConfig().getChunkBlockMinY();
        final int maximumY = this.plugin.getDefaultConfig().getChunkBlockMaxY();
        final int blockY = event.getBlockPlaced().getY();
        final int yDistance = ChunkBlockState.resolveYDistanceFromNexus(nexusLocation, blockY);
        if (yDistance < minimumY || yDistance > maximumY) {
            event.setCancelled(true);
            this.sendMessage(player, "chunk_block_listener.error.y_out_of_range", Map.of(
                    "min_y", minimumY,
                    "max_y", maximumY,
                    "y", blockY,
                    "nexus_y", nexusLocation.getBlockY(),
                    "y_distance", yDistance
            ));
            return;
        }

        final int openSides = this.countAirSides(event.getBlockPlaced());
        if (openSides < REQUIRED_OPEN_SIDES) {
            event.setCancelled(true);
            this.sendMessage(player, "chunk_block_listener.error.open_air_required", Map.of(
                    "open_sides", openSides,
                    "required_sides", REQUIRED_OPEN_SIDES
            ));
            return;
        }

        targetChunk.setType(itemChunkType);
        targetChunk.setChunkBlockLocation(
                event.getBlockPlaced().getWorld().getName(),
                event.getBlockPlaced().getX(),
                event.getBlockPlaced().getY(),
                event.getBlockPlaced().getZ()
        );
        event.getBlockPlaced().setType(
                this.plugin.getDefaultConfig().getChunkTypeIconMaterial(itemChunkType),
                false
        );
        townRepository.update(town);

        this.sendMessage(player, "chunk_block_listener.success.placed", Map.of(
                "town_name", town.getTownName(),
                "chunk_x", targetChunkX,
                "chunk_z", targetChunkZ,
                "chunk_type", itemChunkType.name(),
                "y", blockY,
                "y_distance", yDistance
        ));
    }

    private void enforceChunkBlockNeighborConstraint(final @NotNull BlockPlaceEvent event) {
        final Player player = event.getPlayer();
        for (final BlockFace face : ADJACENT_FACES) {
            final Block adjacentBlock = event.getBlockPlaced().getRelative(face);
            if (!this.isManagedChunkBlock(adjacentBlock)) {
                continue;
            }

            final int openSides = this.countAirSides(adjacentBlock);
            if (openSides >= REQUIRED_OPEN_SIDES) {
                continue;
            }

            event.setCancelled(true);
            this.sendMessage(player, "chunk_block_listener.error.cover_blocked", Map.of(
                    "chunk_x", adjacentBlock.getChunk().getX(),
                    "chunk_z", adjacentBlock.getChunk().getZ(),
                    "open_sides", openSides,
                    "required_sides", REQUIRED_OPEN_SIDES
            ));
            return;
        }
    }

    /**
     * Cancels direct player block breaks of placed town nexus and managed Chunk Blocks.
     *
     * @param event block break event
     */
    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(final @NotNull BlockBreakEvent event) {
        final RRTown townRepository = this.plugin.getTownRepository();
        if (townRepository == null) {
            return;
        }

        final RTown town = townRepository.findByNexusLocation(event.getBlock().getLocation());
        if (town != null) {
            event.setCancelled(true);
            this.sendMessage(event.getPlayer(), "nexus_listener.error.break_blocked", Map.of(
                    "town_name", town.getTownName()
            ));
            return;
        }

        final RTown chunkBlockTown = this.resolveTownForManagedChunkBlock(event.getBlock());
        if (chunkBlockTown == null) {
            return;
        }

        event.setCancelled(true);
        this.sendMessage(event.getPlayer(), "chunk_block_listener.error.break_blocked", Map.of(
                "town_name", chunkBlockTown.getTownName(),
                "chunk_x", event.getBlock().getChunk().getX(),
                "chunk_z", event.getBlock().getChunk().getZ()
        ));
    }

    private void ensureNexusChunk(
            final @NotNull RTown town,
            final int chunkX,
            final int chunkZ
    ) {
        boolean hasTargetChunk = false;

        for (final RChunk chunk : town.getChunks()) {
            final boolean sameChunk = chunk.getX_loc() == chunkX && chunk.getZ_loc() == chunkZ;
            if (sameChunk) {
                chunk.setType(ChunkType.NEXUS);
                chunk.clearChunkBlockLocation();
                hasTargetChunk = true;
                continue;
            }

            if (ChunkType.equalsType(chunk.getType(), ChunkType.NEXUS)) {
                chunk.setType(ChunkType.DEFAULT);
                chunk.clearChunkBlockLocation();
            }
        }

        if (!hasTargetChunk) {
            town.addChunk(new RChunk(town, chunkX, chunkZ, ChunkType.NEXUS));
        }
    }

    private @NotNull Location resolvePlacedBlockLocation(final @NotNull BlockPlaceEvent event) {
        return event.getBlockPlaced().getLocation().toBlockLocation();
    }

    private int countAirSides(final @NotNull Block block) {
        int openSides = 0;
        for (final BlockFace face : ADJACENT_FACES) {
            if (block.getRelative(face).getType().isAir()) {
                openSides++;
            }
        }
        return openSides;
    }

    private boolean isManagedChunkBlock(final @NotNull Block block) {
        return this.resolveTownForManagedChunkBlock(block) != null;
    }

    private @Nullable RTown resolveTownForManagedChunkBlock(final @NotNull Block block) {
        final int chunkX = block.getChunk().getX();
        final int chunkZ = block.getChunk().getZ();
        final RTown town = this.findTownByClaimedChunk(chunkX, chunkZ);
        if (town == null) {
            return null;
        }

        final RChunk claimedChunk = this.findTownChunk(town, chunkX, chunkZ);
        if (claimedChunk == null || !this.isManagedChunkType(claimedChunk.getType())) {
            return null;
        }
        if (!this.matchesStoredChunkBlockLocation(claimedChunk, block)) {
            return null;
        }

        final @NotNull org.bukkit.Material expectedMaterial = this.plugin
                .getDefaultConfig()
                .getChunkTypeIconMaterial(claimedChunk.getType());
        if (block.getType() != expectedMaterial) {
            return null;
        }

        return town;
    }

    private boolean matchesStoredChunkBlockLocation(
            final @NotNull RChunk chunk,
            final @NotNull Block block
    ) {
        final String storedWorld = chunk.getChunkBlockWorld();
        final Integer storedX = chunk.getChunkBlockX();
        final Integer storedY = chunk.getChunkBlockY();
        final Integer storedZ = chunk.getChunkBlockZ();
        if (storedWorld == null || storedX == null || storedY == null || storedZ == null) {
            return false;
        }

        return storedWorld.equals(block.getWorld().getName())
                && storedX == block.getX()
                && storedY == block.getY()
                && storedZ == block.getZ();
    }

    private boolean isManagedChunkType(final @Nullable ChunkType chunkType) {
        if (chunkType == null) {
            return false;
        }
        return !ChunkType.equalsType(chunkType, ChunkType.CLAIM_PENDING);
    }

    private @Nullable RTown findTownByClaimedChunk(final int chunkX, final int chunkZ) {
        final RRTown townRepository = this.plugin.getTownRepository();
        if (townRepository == null) {
            return null;
        }

        for (final RTown town : townRepository.findAllByAttributes(Map.of("active", true))) {
            if (this.findTownChunk(town, chunkX, chunkZ) != null) {
                return town;
            }
        }
        return null;
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

    private @NotNull List<RChunk> resolveInactiveChunks(final @NotNull RTown town) {
        final int minimumY = this.plugin.getDefaultConfig().getChunkBlockMinY();
        final int maximumY = this.plugin.getDefaultConfig().getChunkBlockMaxY();
        final List<RChunk> inactiveChunks = new ArrayList<>();
        for (final RChunk chunk : town.getChunks()) {
            if (ChunkBlockState.isInactive(town, chunk, minimumY, maximumY)) {
                inactiveChunks.add(chunk);
            }
        }
        return inactiveChunks;
    }

    private void sendInactiveChunkWarnings(
            final @NotNull Player player,
            final @NotNull List<RChunk> inactiveChunks
    ) {
        if (inactiveChunks.isEmpty()) {
            return;
        }

        final int minimumY = this.plugin.getDefaultConfig().getChunkBlockMinY();
        final int maximumY = this.plugin.getDefaultConfig().getChunkBlockMaxY();
        for (final RChunk inactiveChunk : inactiveChunks) {
            this.sendMessage(player, "nexus_listener.warning.chunk_inactive", Map.of(
                    "chunk_x", inactiveChunk.getX_loc(),
                    "chunk_z", inactiveChunk.getZ_loc(),
                    "min_y", minimumY,
                    "max_y", maximumY
            ));
        }
    }

    private @NotNull String resolveWorldName(final @NotNull Location location) {
        if (location.getWorld() == null) {
            return "unknown";
        }
        return location.getWorld().getName();
    }

    private void syncPlayerPermissionsFromRole(
            final @NotNull RDTPlayer playerRecord,
            final @NotNull RTown town,
            final @NotNull String roleId
    ) {
        final TownRole role = town.findRoleById(roleId);
        if (role == null) {
            playerRecord.replaceTownPermissions(Set.of());
            return;
        }
        playerRecord.replaceTownPermissions(role.getPermissions());
    }

    private @NotNull RDTPlayer upsertMayorPlayerRecord(
            final @NotNull RRDTPlayer playerRepository,
            final @Nullable RDTPlayer existingPlayerRecord,
            final @NotNull RTown town,
            final @NotNull UUID playerUuid,
            final @NotNull UUID townUuid
    ) {
        if (existingPlayerRecord == null) {
            final RDTPlayer createdRecord = new RDTPlayer(playerUuid, townUuid, RTown.MAYOR_ROLE_ID);
            this.syncPlayerPermissionsFromRole(createdRecord, town, RTown.MAYOR_ROLE_ID);
            playerRepository.create(createdRecord);
            return createdRecord;
        }

        existingPlayerRecord.setTownUUID(townUuid);
        existingPlayerRecord.setTownRoleId(RTown.MAYOR_ROLE_ID);
        this.syncPlayerPermissionsFromRole(existingPlayerRecord, town, RTown.MAYOR_ROLE_ID);
        playerRepository.update(existingPlayerRecord);
        return existingPlayerRecord;
    }

    private boolean hasTownMember(
            final @NotNull RTown town,
            final @NotNull UUID playerUuid
    ) {
        for (final RDTPlayer member : town.getMembers()) {
            if (playerUuid.equals(member.getIdentifier())) {
                return true;
            }
        }
        return false;
    }

    private void sendMessage(
            final @NotNull Player player,
            final @NotNull String key,
            final @NotNull Map<String, Object> placeholders
    ) {
        new I18n.Builder(key, player)
                .withPlaceholders(placeholders)
                .includePrefix()
                .build()
                .sendMessage();
    }
}
