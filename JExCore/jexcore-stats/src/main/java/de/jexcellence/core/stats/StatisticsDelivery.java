package de.jexcellence.core.stats;

import de.jexcellence.jexplatform.logging.JExLogger;
import org.bukkit.Bukkit;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.UUID;

/**
 * Public facade of the statistics delivery subsystem. Constructed via
 * {@link #install} once during plugin startup; registered as a Bukkit
 * service so other plugins can push entries without linking to the
 * internal classes.
 */
public final class StatisticsDelivery {

    private final StatisticsConfig config;
    private final StatisticsQueue queue;
    private final DeliveryEngine engine;
    private final DeliveryMetrics metrics;

    private StatisticsDelivery(
            @NotNull StatisticsConfig config,
            @NotNull StatisticsQueue queue,
            @NotNull DeliveryEngine engine,
            @NotNull DeliveryMetrics metrics
    ) {
        this.config = config;
        this.queue = queue;
        this.engine = engine;
        this.metrics = metrics;
    }

    /**
     * Boots the delivery subsystem: creates the queue, transport, engine,
     * starts the scheduler, and registers the returned service on
     * {@link org.bukkit.plugin.ServicesManager}.
     *
     * @param plugin owning plugin
     * @param config delivery configuration
     * @param serverUuid stable server UUID for batch provenance
     * @param transport HTTP transport (typically {@link HttpCentralTransport})
     * @param logger platform logger
     * @return the running facade
     * @throws IOException when the spool directory can't be created
     */
    public static @NotNull StatisticsDelivery install(
            @NotNull JavaPlugin plugin,
            @NotNull StatisticsConfig config,
            @NotNull UUID serverUuid,
            @NotNull CentralTransport transport,
            @NotNull JExLogger logger
    ) throws IOException {
        final DeliveryMetrics metrics = new DeliveryMetrics();
        final StatisticsQueue queue = new StatisticsQueue(config.maxQueueSize(), metrics);
        final DeliveryEngine engine = new DeliveryEngine(plugin, config, serverUuid, queue, transport, metrics, logger);
        engine.start();
        final StatisticsDelivery facade = new StatisticsDelivery(config, queue, engine, metrics);
        Bukkit.getServicesManager().register(StatisticsDelivery.class, facade, plugin, ServicePriority.Normal);
        logger.info("StatisticsDelivery online — endpoint={}, queue={}", config.endpoint(), config.maxQueueSize());
        return facade;
    }

    /**
     * Shuts down the engine and flushes what can still be sent. Safe to
     * call multiple times.
     *
     * @param plugin owning plugin used to unregister the Bukkit service
     */
    public void shutdown(@NotNull JavaPlugin plugin) {
        this.engine.stop();
        Bukkit.getServicesManager().unregister(StatisticsDelivery.class, this);
    }

    /**
     * Enqueue one entry. Drops silently if the queue is at capacity and no
     * lower-priority entry can be evicted.
     *
     * @param entry statistic entry
     */
    public void push(@NotNull StatisticEntry entry) {
        this.queue.enqueue(entry);
    }

    /**
     * Synchronous flush — forces the engine to drain all pending entries
     * before returning. Blocks the caller; call from an admin command
     * only.
     */
    public void flushSync() {
        this.engine.flushAll();
    }

    public @NotNull DeliveryMetrics metrics() {
        return this.metrics;
    }

    public int queueSize() {
        return this.queue.size();
    }

    public @NotNull StatisticsConfig config() {
        return this.config;
    }
}
