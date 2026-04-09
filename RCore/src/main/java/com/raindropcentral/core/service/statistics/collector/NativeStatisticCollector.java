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

package com.raindropcentral.core.service.statistics.collector;

import com.raindropcentral.core.service.statistics.config.StatisticsDeliveryConfig;
import com.raindropcentral.core.service.statistics.queue.DeliveryPriority;
import com.raindropcentral.core.service.statistics.queue.QueuedStatistic;
import com.raindropcentral.rplatform.logging.CentralLogger;
import com.raindropcentral.rplatform.type.EStatisticType.StatisticDataType;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Statistic;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Collects Minecraft's built-in player statistics.
 * Supports delta tracking and aggregation of native statistics.
 *
 * @author JExcellence
 * @since 1.0.0
 */
public class NativeStatisticCollector {

    private static final Logger LOGGER = CentralLogger.getLoggerByName("RCore");
    private static final String SOURCE_PLUGIN = "minecraft";

    private final StatisticsDeliveryConfig config;
    private final Map<UUID, NativeStatisticSnapshot> lastSnapshots;

    // General statistics to track
    private static final Statistic[] GENERAL_STATS = {
        Statistic.DEATHS, Statistic.PLAYER_KILLS, Statistic.MOB_KILLS,
        Statistic.DAMAGE_DEALT, Statistic.DAMAGE_TAKEN, Statistic.JUMP,
        Statistic.FISH_CAUGHT, Statistic.ANIMALS_BRED, Statistic.LEAVE_GAME,
        Statistic.PLAY_ONE_MINUTE, Statistic.TIME_SINCE_DEATH, Statistic.TIME_SINCE_REST
    };

    // Travel statistics mapping
    private static final Map<Statistic, NativeStatisticSnapshot.TravelMethod> TRAVEL_STATS = Map.of(
        Statistic.WALK_ONE_CM, NativeStatisticSnapshot.TravelMethod.WALK,
        Statistic.SPRINT_ONE_CM, NativeStatisticSnapshot.TravelMethod.SPRINT,
        Statistic.CROUCH_ONE_CM, NativeStatisticSnapshot.TravelMethod.CROUCH,
        Statistic.SWIM_ONE_CM, NativeStatisticSnapshot.TravelMethod.SWIM,
        Statistic.FLY_ONE_CM, NativeStatisticSnapshot.TravelMethod.FLY,
        Statistic.CLIMB_ONE_CM, NativeStatisticSnapshot.TravelMethod.CLIMB,
        Statistic.FALL_ONE_CM, NativeStatisticSnapshot.TravelMethod.FALL,
        Statistic.AVIATE_ONE_CM, NativeStatisticSnapshot.TravelMethod.ELYTRA,
        Statistic.BOAT_ONE_CM, NativeStatisticSnapshot.TravelMethod.BOAT,
        Statistic.MINECART_ONE_CM, NativeStatisticSnapshot.TravelMethod.MINECART
    );

    /**
     * Executes NativeStatisticCollector.
     */
    public NativeStatisticCollector(final @NotNull StatisticsDeliveryConfig config) {
        this.config = config;
        this.lastSnapshots = new ConcurrentHashMap<>();
    }

    /**
     * Collects all native statistics for a player.
     */
    public List<QueuedStatistic> collectForPlayer(final @NotNull Player player) {
        return collectForPlayer(player, false);
    }

    /**
     * Collects delta native statistics for a player.
     */
    public List<QueuedStatistic> collectDeltaForPlayer(final @NotNull Player player) {
        return collectForPlayer(player, true);
    }

    private List<QueuedStatistic> collectForPlayer(final @NotNull Player player, final boolean deltaOnly) {
        if (!config.isCollectNativeStatistics()) {
            return List.of();
        }

        List<QueuedStatistic> result = new ArrayList<>();
        UUID playerUuid = player.getUniqueId();
        long timestamp = System.currentTimeMillis();

        NativeStatisticSnapshot lastSnapshot = lastSnapshots.get(playerUuid);
        NativeStatisticSnapshot.Builder newSnapshotBuilder = NativeStatisticSnapshot.builder(playerUuid)
            .timestamp(timestamp);

        try {
            // Collect general statistics
            if (config.isCollectGeneralStatistics()) {
                result.addAll(collectGeneralStatistics(player, lastSnapshot, newSnapshotBuilder, deltaOnly, timestamp));
            }

            // Collect block statistics
            if (config.isCollectBlockStatistics()) {
                result.addAll(collectBlockStatistics(player, lastSnapshot, newSnapshotBuilder, deltaOnly, timestamp));
            }

            // Collect item statistics
            if (config.isCollectItemStatistics()) {
                result.addAll(collectItemStatistics(player, lastSnapshot, newSnapshotBuilder, deltaOnly, timestamp));
            }

            // Collect mob statistics
            if (config.isCollectMobStatistics()) {
                result.addAll(collectMobStatistics(player, lastSnapshot, newSnapshotBuilder, deltaOnly, timestamp));
            }

            // Collect travel statistics
            if (config.isCollectTravelStatistics()) {
                result.addAll(collectTravelStatistics(player, lastSnapshot, newSnapshotBuilder, deltaOnly, timestamp));
            }

            // Store new snapshot
            lastSnapshots.put(playerUuid, newSnapshotBuilder.build());

        } catch (Exception e) {
            LOGGER.warning("Failed to collect native statistics for " + player.getName() + ": " + e.getMessage());
        }

        return result;
    }

    /**
     * Collects statistics for all online players.
     */
    public List<QueuedStatistic> collectAllOnlinePlayers() {
        List<QueuedStatistic> result = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            result.addAll(collectDeltaForPlayer(player));
        }
        return result;
    }

    /**
     * Collects general statistics (deaths, kills, jumps, etc.).
     */
    public List<QueuedStatistic> collectGeneralStatistics(final @NotNull Player player) {
        return collectGeneralStatistics(player, null,
            NativeStatisticSnapshot.builder(player.getUniqueId()), false, System.currentTimeMillis());
    }

    private List<QueuedStatistic> collectGeneralStatistics(
        final @NotNull Player player,
        final NativeStatisticSnapshot lastSnapshot,
        final NativeStatisticSnapshot.Builder newSnapshotBuilder,
        final boolean deltaOnly,
        final long timestamp
    ) {
        List<QueuedStatistic> result = new ArrayList<>();
        UUID playerUuid = player.getUniqueId();

        for (Statistic stat : GENERAL_STATS) {
            try {
                int value = player.getStatistic(stat);
                newSnapshotBuilder.generalStat(stat, value);

                int lastValue = lastSnapshot != null ?
                    lastSnapshot.generalStats().getOrDefault(stat, 0) : 0;
                int delta = value - lastValue;

                if (!deltaOnly || delta != 0) {
                    String key = "mc_" + stat.name().toLowerCase();
                    result.add(createStatistic(playerUuid, key, deltaOnly ? delta : value, deltaOnly, timestamp));
                }
            } catch (Exception ignored) {
                // Some statistics may not be available
            }
        }

        return result;
    }

    /**
     * Cached set of block materials to avoid triggering DataFixer on main thread.
     * Initialized lazily on first use.
     */
    private static Set<Material> blockMaterials = null;

    /**
     * Cached set of item materials to avoid triggering DataFixer on main thread.
     * Initialized lazily on first use.
     */
    private static Set<Material> itemMaterials = null;

    /**
     * Initializes the block materials cache asynchronously to avoid blocking the main thread.
     * This prevents the DataFixer from being initialized on the main thread during player disconnect.
     */
    private static synchronized void initializeBlockMaterialsCache() {
        if (blockMaterials != null) {
            return;
        }

        // Initialize with empty set first to prevent multiple initializations
        blockMaterials = new HashSet<>();

        // Populate asynchronously to avoid blocking
        CompletableFuture.runAsync(() -> {
            Set<Material> blocks = new HashSet<>();
            for (Material material : Material.values()) {
                try {
                    if (material.isBlock()) {
                        blocks.add(material);
                    }
                } catch (Exception e) {
                    // Ignore materials that can't be checked
                }
            }
            blockMaterials = blocks;
        });
    }

    /**
     * Initializes the item materials cache asynchronously to avoid blocking the main thread.
     * This prevents the DataFixer from being initialized on the main thread during player disconnect.
     */
    private static synchronized void initializeItemMaterialsCache() {
        if (itemMaterials != null) {
            return;
        }

        // Initialize with empty set first to prevent multiple initializations
        itemMaterials = new HashSet<>();

        // Populate asynchronously to avoid blocking
        CompletableFuture.runAsync(() -> {
            Set<Material> items = new HashSet<>();
            for (Material material : Material.values()) {
                try {
                    if (material.isItem()) {
                        items.add(material);
                    }
                } catch (Exception e) {
                    // Ignore materials that can't be checked
                }
            }
            itemMaterials = items;
        });
    }

    /**
     * Collects block statistics (broken, placed).
     */
    public List<QueuedStatistic> collectBlockStatistics(final @NotNull Player player) {
        return collectBlockStatistics(player, null,
            NativeStatisticSnapshot.builder(player.getUniqueId()), false, System.currentTimeMillis());
    }

    private List<QueuedStatistic> collectBlockStatistics(
        final @NotNull Player player,
        final NativeStatisticSnapshot lastSnapshot,
        final NativeStatisticSnapshot.Builder newSnapshotBuilder,
        final boolean deltaOnly,
        final long timestamp
    ) {
        // Initialize cache if needed
        if (blockMaterials == null) {
            initializeBlockMaterialsCache();
            // Return empty list if cache not ready yet
            return new ArrayList<>();
        }

        List<QueuedStatistic> result = new ArrayList<>();
        UUID playerUuid = player.getUniqueId();

        int totalBroken = 0;
        int totalPlaced = 0;

        for (Material material : blockMaterials) {
            try {
                // Blocks broken
                int broken = player.getStatistic(Statistic.MINE_BLOCK, material);
                if (broken > 0) {
                    newSnapshotBuilder.blockBroken(material, broken);
                    totalBroken += broken;

                    int lastBroken = lastSnapshot != null ?
                        lastSnapshot.blocksBroken().getOrDefault(material, 0) : 0;
                    int delta = broken - lastBroken;

                    if (!deltaOnly || delta > 0) {
                        String key = "mc_blocks_broken_" + material.name().toLowerCase();
                        result.add(createStatistic(playerUuid, key, deltaOnly ? delta : broken, deltaOnly, timestamp));
                    }
                }

                // Blocks placed
                int placed = player.getStatistic(Statistic.USE_ITEM, material);
                if (placed > 0) {
                    newSnapshotBuilder.blockPlaced(material, placed);
                    totalPlaced += placed;
                }
            } catch (Exception ignored) {}
        }

        // Add aggregates
        if (totalBroken > 0) {
            int lastTotal = lastSnapshot != null ?
                lastSnapshot.blocksBroken().values().stream().mapToInt(Integer::intValue).sum() : 0;
            int delta = totalBroken - lastTotal;
            if (!deltaOnly || delta > 0) {
                result.add(createStatistic(playerUuid, "mc_total_blocks_broken", deltaOnly ? delta : totalBroken, deltaOnly, timestamp));
            }
        }

        return result;
    }

    /**
     * Collects item statistics (crafted, used, picked up, dropped).
     */
    public List<QueuedStatistic> collectItemStatistics(final @NotNull Player player) {
        return collectItemStatistics(player, null,
            NativeStatisticSnapshot.builder(player.getUniqueId()), false, System.currentTimeMillis());
    }

    private List<QueuedStatistic> collectItemStatistics(
        final @NotNull Player player,
        final NativeStatisticSnapshot lastSnapshot,
        final NativeStatisticSnapshot.Builder newSnapshotBuilder,
        final boolean deltaOnly,
        final long timestamp
    ) {
        // Initialize cache if needed
        if (itemMaterials == null) {
            initializeItemMaterialsCache();
            // Return empty list if cache not ready yet
            return new ArrayList<>();
        }

        List<QueuedStatistic> result = new ArrayList<>();
        UUID playerUuid = player.getUniqueId();

        int totalCrafted = 0;

        for (Material material : itemMaterials) {
            try {
                int crafted = player.getStatistic(Statistic.CRAFT_ITEM, material);
                if (crafted > 0) {
                    newSnapshotBuilder.itemCrafted(material, crafted);
                    totalCrafted += crafted;

                    int lastCrafted = lastSnapshot != null ?
                        lastSnapshot.itemsCrafted().getOrDefault(material, 0) : 0;
                    int delta = crafted - lastCrafted;

                    if (!deltaOnly || delta > 0) {
                        String key = "mc_items_crafted_" + material.name().toLowerCase();
                        result.add(createStatistic(playerUuid, key, deltaOnly ? delta : crafted, deltaOnly, timestamp));
                    }
                }
            } catch (Exception ignored) {}
        }

        // Add aggregate
        if (totalCrafted > 0) {
            int lastTotal = lastSnapshot != null ?
                lastSnapshot.itemsCrafted().values().stream().mapToInt(Integer::intValue).sum() : 0;
            int delta = totalCrafted - lastTotal;
            if (!deltaOnly || delta > 0) {
                result.add(createStatistic(playerUuid, "mc_total_items_crafted", deltaOnly ? delta : totalCrafted, deltaOnly, timestamp));
            }
        }

        return result;
    }

    /**
     * Collects mob kill statistics.
     */
    public List<QueuedStatistic> collectMobStatistics(final @NotNull Player player) {
        return collectMobStatistics(player, null,
            NativeStatisticSnapshot.builder(player.getUniqueId()), false, System.currentTimeMillis());
    }

    private List<QueuedStatistic> collectMobStatistics(
        final @NotNull Player player,
        final NativeStatisticSnapshot lastSnapshot,
        final NativeStatisticSnapshot.Builder newSnapshotBuilder,
        final boolean deltaOnly,
        final long timestamp
    ) {
        List<QueuedStatistic> result = new ArrayList<>();
        UUID playerUuid = player.getUniqueId();

        int totalKills = 0;

        for (EntityType entityType : EntityType.values()) {
            if (!entityType.isAlive()) continue;

            try {
                int kills = player.getStatistic(Statistic.KILL_ENTITY, entityType);
                if (kills > 0) {
                    newSnapshotBuilder.mobKill(entityType, kills);
                    totalKills += kills;

                    int lastKills = lastSnapshot != null ?
                        lastSnapshot.mobKills().getOrDefault(entityType, 0) : 0;
                    int delta = kills - lastKills;

                    if (!deltaOnly || delta > 0) {
                        String key = "mc_kills_" + entityType.name().toLowerCase();
                        result.add(createStatistic(playerUuid, key, deltaOnly ? delta : kills, deltaOnly, timestamp));
                    }
                }
            } catch (Exception ignored) {}
        }

        // Add aggregate
        if (totalKills > 0) {
            int lastTotal = lastSnapshot != null ?
                lastSnapshot.mobKills().values().stream().mapToInt(Integer::intValue).sum() : 0;
            int delta = totalKills - lastTotal;
            if (!deltaOnly || delta > 0) {
                result.add(createStatistic(playerUuid, "mc_total_mob_kills", deltaOnly ? delta : totalKills, deltaOnly, timestamp));
            }
        }

        return result;
    }

    /**
     * Collects travel statistics.
     */
    public List<QueuedStatistic> collectTravelStatistics(final @NotNull Player player) {
        return collectTravelStatistics(player, null,
            NativeStatisticSnapshot.builder(player.getUniqueId()), false, System.currentTimeMillis());
    }

    private List<QueuedStatistic> collectTravelStatistics(
        final @NotNull Player player,
        final NativeStatisticSnapshot lastSnapshot,
        final NativeStatisticSnapshot.Builder newSnapshotBuilder,
        final boolean deltaOnly,
        final long timestamp
    ) {
        List<QueuedStatistic> result = new ArrayList<>();
        UUID playerUuid = player.getUniqueId();

        int totalDistance = 0;

        for (var entry : TRAVEL_STATS.entrySet()) {
            try {
                int distance = player.getStatistic(entry.getKey());
                NativeStatisticSnapshot.TravelMethod method = entry.getValue();
                newSnapshotBuilder.travelStat(method, distance);
                totalDistance += distance;

                int lastDistance = lastSnapshot != null ?
                    lastSnapshot.travelStats().getOrDefault(method, 0) : 0;
                int delta = distance - lastDistance;

                if (!deltaOnly || delta > 0) {
                    String key = "mc_distance_" + method.name().toLowerCase();
                    // Convert from cm to blocks
                    int valueInBlocks = (deltaOnly ? delta : distance) / 100;
                    result.add(createStatistic(playerUuid, key, valueInBlocks, deltaOnly, timestamp));
                }
            } catch (Exception ignored) {}
        }

        // Add aggregate
        if (totalDistance > 0) {
            int lastTotal = lastSnapshot != null ?
                lastSnapshot.travelStats().values().stream().mapToInt(Integer::intValue).sum() : 0;
            int delta = totalDistance - lastTotal;
            if (!deltaOnly || delta > 0) {
                int valueInBlocks = (deltaOnly ? delta : totalDistance) / 100;
                result.add(createStatistic(playerUuid, "mc_total_distance", valueInBlocks, deltaOnly, timestamp));
            }
        }

        return result;
    }

    /**
     * Aggregates total blocks broken for a player.
     */
    public QueuedStatistic aggregateTotalBlocksBroken(final @NotNull Player player) {
        int total = 0;
        for (Material material : Material.values()) {
            if (!material.isBlock()) continue;
            try {
                total += player.getStatistic(Statistic.MINE_BLOCK, material);
            } catch (Exception ignored) {}
        }
        return createStatistic(player.getUniqueId(), "mc_total_blocks_broken", total, false, System.currentTimeMillis());
    }

    /**
     * Aggregates total distance traveled for a player.
     */
    public QueuedStatistic aggregateTotalDistanceTraveled(final @NotNull Player player) {
        int total = 0;
        for (Statistic stat : TRAVEL_STATS.keySet()) {
            try {
                total += player.getStatistic(stat);
            } catch (Exception ignored) {}
        }
        return createStatistic(player.getUniqueId(), "mc_total_distance", total / 100, false, System.currentTimeMillis());
    }

    /**
     * Aggregates total mob kills for a player.
     */
    public QueuedStatistic aggregateTotalMobKills(final @NotNull Player player) {
        int total = 0;
        for (EntityType entityType : EntityType.values()) {
            if (!entityType.isAlive()) continue;
            try {
                total += player.getStatistic(Statistic.KILL_ENTITY, entityType);
            } catch (Exception ignored) {}
        }
        return createStatistic(player.getUniqueId(), "mc_total_mob_kills", total, false, System.currentTimeMillis());
    }

    /**
     * Clears snapshot data for a player.
     */
    public void clearPlayerSnapshot(final @NotNull UUID playerUuid) {
        lastSnapshots.remove(playerUuid);
    }

    private QueuedStatistic createStatistic(
        final UUID playerUuid,
        final String key,
        final int value,
        final boolean isDelta,
        final long timestamp
    ) {
        return QueuedStatistic.builder()
            .playerUuid(playerUuid)
            .statisticKey(key)
            .value(value)
            .dataType(StatisticDataType.NUMBER)
            .collectionTimestamp(timestamp)
            .priority(DeliveryPriority.NORMAL)
            .isDelta(isDelta)
            .sourcePlugin(SOURCE_PLUGIN)
            .build();
    }
}
