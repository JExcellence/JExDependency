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
import com.raindropcentral.rdt.database.entity.RTown;
import com.raindropcentral.rdt.database.repository.RRDTPlayer;
import com.raindropcentral.rplatform.proxy.NetworkLocation;
import com.raindropcentral.rplatform.proxy.PendingArrivalToken;
import com.raindropcentral.rplatform.proxy.ProxyService;
import com.raindropcentral.rplatform.proxy.ProxyTransferRequest;
import de.jexcellence.jextranslate.i18n.I18n;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Coordinates local and proxy-routed town-spawn execution.
 *
 * <p>This service keeps the safe-location search and delayed teleport logic on Paper while
 * supporting authoritative cross-server spawn routing through proxy transfer plus pending-arrival
 * tokens.</p>
 */
public final class TownSpawnService {

    /**
     * Stable module identifier used for pending-arrival token matching.
     */
    public static final String MODULE_ID = "rdt";
    /**
     * Stable action identifier used for town-spawn arrival handling.
     */
    public static final String TOWN_SPAWN_ARRIVAL_ACTION = "town_spawn_arrival";

    private static final int TICKS_PER_SECOND = 20;
    private static final int MAX_SPAWN_SEARCH_RADIUS = 48;
    private static final long DEFAULT_TOKEN_TTL_SECONDS = 180L;
    private static final Set<Material> UNSAFE_STAND_BLOCKS = Set.of(
        Material.LAVA,
        Material.MAGMA_BLOCK,
        Material.FIRE,
        Material.SOUL_FIRE,
        Material.CAMPFIRE,
        Material.SOUL_CAMPFIRE,
        Material.CACTUS,
        Material.POWDER_SNOW
    );

    private final RDT plugin;

    /**
     * Creates one town-spawn routing service.
     *
     * @param plugin active RDT runtime
     */
    public TownSpawnService(final @NotNull RDT plugin) {
        this.plugin = plugin;
    }

    /**
     * Handles command-driven town-spawn requests.
     *
     * @param player requesting player
     */
    public void startTownSpawnFromCommand(final @NotNull Player player) {
        final RDTPlayer townPlayer = this.resolveTownPlayer(player);
        if (townPlayer == null || townPlayer.getTownUUID() == null) {
            this.sendMessage(player, "command.prt.town.not_in_town", Map.of());
            return;
        }

        final UUID townUuid = townPlayer.getTownUUID();
        final RTown town = this.resolveTown(townUuid);
        if (town == null) {
            this.sendMessage(player, "command.prt.town.not_in_town", Map.of());
            return;
        }

        final @Nullable NetworkLocation destination = this.resolveTownSpawnNetworkLocation(town);
        if (destination == null) {
            this.sendMessage(player, "command.prt.spawn.spawn_unavailable", Map.of());
            return;
        }

        if (!this.isLocalDestination(destination)) {
            final boolean proxyTownSpawnEnabled = this.plugin.getDefaultConfig().isProxyTownSpawnEnabled();
            final ProxyService proxyService = this.plugin.getProxyService();
            if (!proxyTownSpawnEnabled) {
                this.sendMessage(
                    player,
                    "command.prt.spawn.destination_unavailable",
                    Map.of("server", destination.serverId())
                );
                return;
            }
            if (!proxyService.isAvailable()) {
                this.sendMessage(player, "command.prt.spawn.proxy_unavailable", Map.of());
                return;
            }

            this.routeThroughProxy(player, town, destination, proxyService);
            return;
        }

        this.scheduleLocalTeleport(player, townUuid, destination);
    }

    /**
     * Attempts to consume one pending town-spawn arrival token after player join.
     *
     * @param player joined player
     */
    public void consumePendingArrivalOnJoin(final @NotNull Player player) {
        if (!this.plugin.getDefaultConfig().isProxyTownSpawnEnabled()) {
            return;
        }

        final ProxyService proxyService = this.plugin.getProxyService();
        if (!proxyService.isAvailable()) {
            return;
        }

        final Optional<PendingArrivalToken> tokenOptional = proxyService.pendingArrivals().consumeFirstForPlayer(
            player.getUniqueId(),
            MODULE_ID,
            TOWN_SPAWN_ARRIVAL_ACTION,
            this.plugin.getServerRouteId()
        );
        if (tokenOptional.isEmpty()) {
            return;
        }

        final PendingArrivalToken token = tokenOptional.get();
        if (token.isExpired(System.currentTimeMillis())) {
            this.sendMessage(player, "command.prt.spawn.arrival_expired", Map.of());
            return;
        }

        final UUID townUuidFromToken = parseUuid(token.payload().get("town_uuid"));
        final UUID townUuid = townUuidFromToken == null
            ? this.resolvePlayerTownUuid(player)
            : townUuidFromToken;
        if (townUuid == null) {
            this.sendMessage(player, "command.prt.town.not_in_town", Map.of());
            return;
        }

        this.scheduleLocalTeleport(player, townUuid, token.destination());
    }

    private void routeThroughProxy(
        final @NotNull Player player,
        final @NotNull RTown town,
        final @NotNull NetworkLocation destination,
        final @NotNull ProxyService proxyService
    ) {
        final long tokenTtlSeconds = Math.max(
            DEFAULT_TOKEN_TTL_SECONDS,
            this.plugin.getDefaultConfig().getTownSpawnTeleportDelaySeconds() + DEFAULT_TOKEN_TTL_SECONDS
        );
        final PendingArrivalToken token = proxyService.pendingArrivals().issueToken(
            player.getUniqueId(),
            MODULE_ID,
            TOWN_SPAWN_ARRIVAL_ACTION,
            destination,
            Duration.ofSeconds(tokenTtlSeconds),
            Map.of("town_uuid", town.getIdentifier().toString())
        );

        this.sendMessage(
            player,
            "command.prt.spawn.routing",
            Map.of("server", destination.serverId())
        );

        final ProxyTransferRequest transferRequest = new ProxyTransferRequest(
            player.getUniqueId(),
            this.plugin.getServerRouteId(),
            destination.serverId(),
            token.tokenId(),
            Map.of(
                "module", MODULE_ID,
                "action", TOWN_SPAWN_ARRIVAL_ACTION
            )
        );

        proxyService.requestPlayerTransfer(transferRequest)
            .handle((successful, throwable) -> throwable == null && Boolean.TRUE.equals(successful))
            .thenAccept(successful -> this.runSync(() -> {
                if (!successful) {
                    proxyService.pendingArrivals().consumeToken(token.tokenId());
                    this.sendMessage(
                        player,
                        "command.prt.spawn.destination_unavailable",
                        Map.of("server", destination.serverId())
                    );
                }
            }));
    }

    private void scheduleLocalTeleport(
        final @NotNull Player player,
        final @NotNull UUID expectedTownUuid,
        final @NotNull NetworkLocation fallbackNetworkLocation
    ) {
        final Location localSpawnLocation = this.resolveLocalLocation(fallbackNetworkLocation);
        if (localSpawnLocation == null || localSpawnLocation.getWorld() == null) {
            this.sendMessage(player, "command.prt.spawn.world_unavailable", Map.of());
            return;
        }

        final int delaySeconds = this.plugin.getDefaultConfig().getTownSpawnTeleportDelaySeconds();
        this.sendMessage(
            player,
            "command.prt.spawn.starting",
            Map.of("seconds", delaySeconds)
        );

        final long delayTicks = Math.max(0L, delaySeconds) * TICKS_PER_SECOND;
        final Runnable teleportTask = () -> this.executeTownSpawnTeleport(
            player.getUniqueId(),
            expectedTownUuid,
            fallbackNetworkLocation
        );

        if (this.plugin.getScheduler() != null) {
            this.plugin.getScheduler().runDelayed(teleportTask, delayTicks);
            return;
        }

        this.plugin.getServer().getScheduler().runTaskLater(
            this.plugin.getPlugin(),
            teleportTask,
            delayTicks
        );
    }

    private void executeTownSpawnTeleport(
        final @NotNull UUID playerUuid,
        final @NotNull UUID expectedTownUuid,
        final @NotNull NetworkLocation fallbackNetworkLocation
    ) {
        final Player player = this.plugin.getServer().getPlayer(playerUuid);
        if (player == null || !player.isOnline()) {
            return;
        }

        final RDTPlayer townPlayer = this.resolveTownPlayer(player);
        if (townPlayer == null || townPlayer.getTownUUID() == null || !expectedTownUuid.equals(townPlayer.getTownUUID())) {
            return;
        }

        NetworkLocation resolvedNetworkLocation = fallbackNetworkLocation;
        final RTown latestTownState = this.resolveTown(townPlayer.getTownUUID());
        if (latestTownState != null) {
            final @Nullable NetworkLocation townSpawnNetworkLocation =
                latestTownState.getTownSpawnNetworkLocation(this.plugin.getServerRouteId());
            final @Nullable NetworkLocation nexusNetworkLocation =
                latestTownState.getNexusNetworkLocation(this.plugin.getServerRouteId());
            if (townSpawnNetworkLocation != null) {
                resolvedNetworkLocation = townSpawnNetworkLocation;
            } else if (nexusNetworkLocation != null) {
                resolvedNetworkLocation = nexusNetworkLocation;
            }
        }

        if (!this.isLocalDestination(resolvedNetworkLocation)) {
            this.sendMessage(
                player,
                "command.prt.spawn.destination_unavailable",
                Map.of("server", resolvedNetworkLocation.serverId())
            );
            return;
        }

        final Location localSpawnLocation = this.resolveLocalLocation(resolvedNetworkLocation);
        if (localSpawnLocation == null || localSpawnLocation.getWorld() == null) {
            this.sendMessage(player, "command.prt.spawn.world_unavailable", Map.of());
            return;
        }

        final @Nullable Location safeLocation = this.findSafeTownSpawnLocation(localSpawnLocation);
        if (safeLocation == null) {
            this.sendMessage(player, "command.prt.spawn.no_safe_location", Map.of());
            return;
        }

        player.teleport(safeLocation);
        this.sendMessage(
            player,
            "command.prt.spawn.success",
            Map.of(
                "x", safeLocation.getBlockX(),
                "y", safeLocation.getBlockY(),
                "z", safeLocation.getBlockZ()
            )
        );
    }

    private @Nullable NetworkLocation resolveTownSpawnNetworkLocation(final @NotNull RTown town) {
        final @Nullable NetworkLocation existingTownSpawn = town.getTownSpawnNetworkLocation(this.plugin.getServerRouteId());
        if (existingTownSpawn != null) {
            return existingTownSpawn;
        }

        final @Nullable NetworkLocation nexusLocation = town.getNexusNetworkLocation(this.plugin.getServerRouteId());
        if (nexusLocation == null) {
            return null;
        }

        town.setTownSpawnNetworkLocation(nexusLocation);
        if (this.plugin.getTownRepository() != null) {
            this.plugin.getTownRepository().update(town);
        }
        return nexusLocation;
    }

    private @Nullable Location resolveLocalLocation(final @NotNull NetworkLocation networkLocation) {
        final World world = Bukkit.getWorld(networkLocation.worldName());
        if (world == null) {
            return null;
        }

        return new Location(
            world,
            networkLocation.x(),
            networkLocation.y(),
            networkLocation.z(),
            networkLocation.yaw(),
            networkLocation.pitch()
        );
    }

    private boolean isLocalDestination(final @NotNull NetworkLocation destination) {
        return destination.serverId().equalsIgnoreCase(this.plugin.getServerRouteId());
    }

    private @Nullable Location findSafeTownSpawnLocation(final @NotNull Location spawnLocation) {
        final World world = spawnLocation.getWorld();
        if (world == null) {
            return null;
        }

        final int centerX = spawnLocation.getBlockX();
        final int centerZ = spawnLocation.getBlockZ();
        for (int radius = 0; radius <= MAX_SPAWN_SEARCH_RADIUS; radius++) {
            final @Nullable Location location = this.findSafeLocationInRadiusRing(world, centerX, centerZ, radius);
            if (location != null) {
                return location;
            }
        }
        return null;
    }

    private @Nullable Location findSafeLocationInRadiusRing(
        final @NotNull World world,
        final int centerX,
        final int centerZ,
        final int radius
    ) {
        if (radius == 0) {
            return this.findSafeLocationInColumn(world, centerX, centerZ);
        }

        final int minX = centerX - radius;
        final int maxX = centerX + radius;
        final int minZ = centerZ - radius;
        final int maxZ = centerZ + radius;

        for (int x = minX; x <= maxX; x++) {
            final @Nullable Location location = this.findSafeLocationInColumn(world, x, minZ);
            if (location != null) {
                return location;
            }
        }
        for (int z = minZ + 1; z <= maxZ; z++) {
            final @Nullable Location location = this.findSafeLocationInColumn(world, maxX, z);
            if (location != null) {
                return location;
            }
        }
        for (int x = maxX - 1; x >= minX; x--) {
            final @Nullable Location location = this.findSafeLocationInColumn(world, x, maxZ);
            if (location != null) {
                return location;
            }
        }
        for (int z = maxZ - 1; z > minZ; z--) {
            final @Nullable Location location = this.findSafeLocationInColumn(world, minX, z);
            if (location != null) {
                return location;
            }
        }
        return null;
    }

    private @Nullable Location findSafeLocationInColumn(
        final @NotNull World world,
        final int x,
        final int z
    ) {
        final int minY = world.getMinHeight();
        final int maxStandY = world.getMaxHeight() - 3;
        final int startY = Math.min(maxStandY, world.getHighestBlockYAt(x, z));

        for (int standY = startY; standY >= minY; standY--) {
            final Material standBlock = world.getBlockAt(x, standY, z).getType();
            final Material feetBlock = world.getBlockAt(x, standY + 1, z).getType();
            final Material headBlock = world.getBlockAt(x, standY + 2, z).getType();

            if (!this.isSafeStandBlock(standBlock)) {
                continue;
            }
            if (!feetBlock.isAir() || !headBlock.isAir()) {
                continue;
            }

            return new Location(
                world,
                x + 0.5D,
                standY + 1.0D,
                z + 0.5D
            );
        }

        return null;
    }

    private boolean isSafeStandBlock(final @NotNull Material material) {
        if (!material.isSolid()) {
            return false;
        }
        return !UNSAFE_STAND_BLOCKS.contains(material);
    }

    private @Nullable RDTPlayer resolveTownPlayer(final @NotNull Player player) {
        final RRDTPlayer playerRepository = this.plugin.getPlayerRepository();
        if (playerRepository == null) {
            return null;
        }
        return playerRepository.findByPlayer(player.getUniqueId());
    }

    private @Nullable UUID resolvePlayerTownUuid(final @NotNull Player player) {
        final RDTPlayer townPlayer = this.resolveTownPlayer(player);
        if (townPlayer == null) {
            return null;
        }
        return townPlayer.getTownUUID();
    }

    private @Nullable RTown resolveTown(final @NotNull UUID townUuid) {
        if (this.plugin.getTownRepository() == null) {
            return null;
        }
        return this.plugin.getTownRepository().findByTownUUID(townUuid);
    }

    private void sendMessage(
        final @NotNull Player player,
        final @NotNull String key,
        final @NotNull Map<String, Object> placeholders
    ) {
        final I18n.Builder messageBuilder = new I18n.Builder(key, player).includePrefix();
        if (!placeholders.isEmpty()) {
            messageBuilder.withPlaceholders(placeholders);
        }
        messageBuilder.build().sendMessage();
    }

    private void runSync(final @NotNull Runnable task) {
        if (this.plugin.getScheduler() != null) {
            this.plugin.getScheduler().runSync(task);
            return;
        }
        Bukkit.getScheduler().runTask(this.plugin.getPlugin(), task);
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
}
