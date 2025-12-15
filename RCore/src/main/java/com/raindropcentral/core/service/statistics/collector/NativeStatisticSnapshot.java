package com.raindropcentral.core.service.statistics.collector;

import org.bukkit.Material;
import org.bukkit.Statistic;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.NotNull;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

/**
 * Snapshot of a player's Minecraft native statistics at a point in time.
 * Used for delta calculation between collection intervals.
 *
 * @param playerUuid    the player UUID
 * @param timestamp     when this snapshot was taken
 * @param generalStats  general statistics (deaths, jumps, etc.)
 * @param blocksBroken  blocks broken by material type
 * @param blocksPlaced  blocks placed by material type
 * @param itemsCrafted  items crafted by material type
 * @param itemsUsed     items used by material type
 * @param itemsPickedUp items picked up by material type
 * @param itemsDropped  items dropped by material type
 * @param mobKills      mob kills by entity type
 * @param travelStats   travel distances by method
 *
 * @author JExcellence
 * @since 1.0.0
 */
public record NativeStatisticSnapshot(
    @NotNull UUID playerUuid,
    long timestamp,
    @NotNull Map<Statistic, Integer> generalStats,
    @NotNull Map<Material, Integer> blocksBroken,
    @NotNull Map<Material, Integer> blocksPlaced,
    @NotNull Map<Material, Integer> itemsCrafted,
    @NotNull Map<Material, Integer> itemsUsed,
    @NotNull Map<Material, Integer> itemsPickedUp,
    @NotNull Map<Material, Integer> itemsDropped,
    @NotNull Map<EntityType, Integer> mobKills,
    @NotNull Map<TravelMethod, Integer> travelStats
) {

    /**
     * Creates an empty snapshot for a player.
     *
     * @param playerUuid the player UUID
     * @return an empty snapshot
     */
    public static NativeStatisticSnapshot empty(final @NotNull UUID playerUuid) {
        return new NativeStatisticSnapshot(
            playerUuid,
            System.currentTimeMillis(),
            new EnumMap<>(Statistic.class),
            new EnumMap<>(Material.class),
            new EnumMap<>(Material.class),
            new EnumMap<>(Material.class),
            new EnumMap<>(Material.class),
            new EnumMap<>(Material.class),
            new EnumMap<>(Material.class),
            new EnumMap<>(EntityType.class),
            new EnumMap<>(TravelMethod.class)
        );
    }

    /**
     * Creates a builder for NativeStatisticSnapshot.
     *
     * @param playerUuid the player UUID
     * @return a new builder
     */
    public static Builder builder(final @NotNull UUID playerUuid) {
        return new Builder(playerUuid);
    }

    /**
     * Travel methods tracked by Minecraft.
     */
    public enum TravelMethod {
        WALK,
        SPRINT,
        CROUCH,
        SWIM,
        FLY,
        CLIMB,
        FALL,
        ELYTRA,
        BOAT,
        MINECART,
        PIG,
        HORSE,
        STRIDER
    }

    /**
     * Builder for NativeStatisticSnapshot.
     */
    public static class Builder {
        private final UUID playerUuid;
        private long timestamp = System.currentTimeMillis();
        private final Map<Statistic, Integer> generalStats = new EnumMap<>(Statistic.class);
        private final Map<Material, Integer> blocksBroken = new EnumMap<>(Material.class);
        private final Map<Material, Integer> blocksPlaced = new EnumMap<>(Material.class);
        private final Map<Material, Integer> itemsCrafted = new EnumMap<>(Material.class);
        private final Map<Material, Integer> itemsUsed = new EnumMap<>(Material.class);
        private final Map<Material, Integer> itemsPickedUp = new EnumMap<>(Material.class);
        private final Map<Material, Integer> itemsDropped = new EnumMap<>(Material.class);
        private final Map<EntityType, Integer> mobKills = new EnumMap<>(EntityType.class);
        private final Map<TravelMethod, Integer> travelStats = new EnumMap<>(TravelMethod.class);

        public Builder(final @NotNull UUID playerUuid) {
            this.playerUuid = playerUuid;
        }

        public Builder timestamp(long timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder generalStat(Statistic stat, int value) {
            this.generalStats.put(stat, value);
            return this;
        }

        public Builder blockBroken(Material material, int count) {
            this.blocksBroken.put(material, count);
            return this;
        }

        public Builder blockPlaced(Material material, int count) {
            this.blocksPlaced.put(material, count);
            return this;
        }

        public Builder itemCrafted(Material material, int count) {
            this.itemsCrafted.put(material, count);
            return this;
        }

        public Builder itemUsed(Material material, int count) {
            this.itemsUsed.put(material, count);
            return this;
        }

        public Builder itemPickedUp(Material material, int count) {
            this.itemsPickedUp.put(material, count);
            return this;
        }

        public Builder itemDropped(Material material, int count) {
            this.itemsDropped.put(material, count);
            return this;
        }

        public Builder mobKill(EntityType entityType, int count) {
            this.mobKills.put(entityType, count);
            return this;
        }

        public Builder travelStat(TravelMethod method, int distance) {
            this.travelStats.put(method, distance);
            return this;
        }

        public NativeStatisticSnapshot build() {
            return new NativeStatisticSnapshot(
                playerUuid, timestamp,
                Map.copyOf(generalStats),
                Map.copyOf(blocksBroken),
                Map.copyOf(blocksPlaced),
                Map.copyOf(itemsCrafted),
                Map.copyOf(itemsUsed),
                Map.copyOf(itemsPickedUp),
                Map.copyOf(itemsDropped),
                Map.copyOf(mobKills),
                Map.copyOf(travelStats)
            );
        }
    }
}
