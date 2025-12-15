package com.raindropcentral.core.service.statistics.delivery;

/**
 * Server-level metrics included with batch payloads.
 * Provides performance and capacity information about the Minecraft server.
 *
 * @param tps1m          TPS average over the last 1 minute
 * @param tps5m          TPS average over the last 5 minutes
 * @param tps15m         TPS average over the last 15 minutes
 * @param heapUsed       heap memory currently used in bytes
 * @param heapMax        maximum heap memory available in bytes
 * @param nonHeapUsed    non-heap memory used in bytes
 * @param cpuUsage       CPU usage percentage (0-100)
 * @param onlinePlayers  current number of online players
 * @param maxPlayers     maximum player capacity
 * @param uptimeMs       server uptime in milliseconds
 * @param worldCount     number of loaded worlds
 * @param loadedChunks   total number of loaded chunks across all worlds
 * @param entityCount    total number of entities across all worlds
 * @param tileEntityCount total number of tile entities across all worlds
 *
 * @author JExcellence
 * @since 1.0.0
 */
public record ServerMetrics(
    double tps1m,
    double tps5m,
    double tps15m,
    long heapUsed,
    long heapMax,
    long nonHeapUsed,
    double cpuUsage,
    int onlinePlayers,
    int maxPlayers,
    long uptimeMs,
    int worldCount,
    int loadedChunks,
    int entityCount,
    int tileEntityCount
) {

    /**
     * Creates a builder for ServerMetrics.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for ServerMetrics.
     */
    public static class Builder {
        private double tps1m = 20.0;
        private double tps5m = 20.0;
        private double tps15m = 20.0;
        private long heapUsed = 0;
        private long heapMax = 0;
        private long nonHeapUsed = 0;
        private double cpuUsage = 0.0;
        private int onlinePlayers = 0;
        private int maxPlayers = 0;
        private long uptimeMs = 0;
        private int worldCount = 0;
        private int loadedChunks = 0;
        private int entityCount = 0;
        private int tileEntityCount = 0;

        public Builder tps1m(double tps1m) { this.tps1m = tps1m; return this; }
        public Builder tps5m(double tps5m) { this.tps5m = tps5m; return this; }
        public Builder tps15m(double tps15m) { this.tps15m = tps15m; return this; }
        public Builder heapUsed(long heapUsed) { this.heapUsed = heapUsed; return this; }
        public Builder heapMax(long heapMax) { this.heapMax = heapMax; return this; }
        public Builder nonHeapUsed(long nonHeapUsed) { this.nonHeapUsed = nonHeapUsed; return this; }
        public Builder cpuUsage(double cpuUsage) { this.cpuUsage = cpuUsage; return this; }
        public Builder onlinePlayers(int onlinePlayers) { this.onlinePlayers = onlinePlayers; return this; }
        public Builder maxPlayers(int maxPlayers) { this.maxPlayers = maxPlayers; return this; }
        public Builder uptimeMs(long uptimeMs) { this.uptimeMs = uptimeMs; return this; }
        public Builder worldCount(int worldCount) { this.worldCount = worldCount; return this; }
        public Builder loadedChunks(int loadedChunks) { this.loadedChunks = loadedChunks; return this; }
        public Builder entityCount(int entityCount) { this.entityCount = entityCount; return this; }
        public Builder tileEntityCount(int tileEntityCount) { this.tileEntityCount = tileEntityCount; return this; }

        public ServerMetrics build() {
            return new ServerMetrics(
                tps1m, tps5m, tps15m, heapUsed, heapMax, nonHeapUsed,
                cpuUsage, onlinePlayers, maxPlayers, uptimeMs,
                worldCount, loadedChunks, entityCount, tileEntityCount
            );
        }
    }
}
