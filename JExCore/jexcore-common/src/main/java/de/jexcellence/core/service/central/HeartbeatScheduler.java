package de.jexcellence.core.service.central;

import de.jexcellence.core.database.entity.CentralServer;
import de.jexcellence.core.service.CentralServerService;
import de.jexcellence.jexplatform.logging.JExLogger;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;

/**
 * Asynchronous recurring task that refreshes the {@link CentralServer}
 * heartbeat timestamp and player counts. Runs once per interval in ticks
 * (default 1200 = 60 seconds).
 *
 * <p>The scheduler re-reads the row from the database on every tick
 * rather than caching a detached reference. That avoids Hibernate
 * optimistic-lock conflicts when the row is also mutated elsewhere (for
 * example during graceful shutdown).
 */
public class HeartbeatScheduler {

    private static final long DEFAULT_INTERVAL_TICKS = 1200L;

    private final JavaPlugin plugin;
    private final CentralServerService servers;
    private final JExLogger logger;
    private final Long rowId;
    private final long intervalTicks;

    private BukkitTask task;

    public HeartbeatScheduler(
            @NotNull JavaPlugin plugin,
            @NotNull CentralServerService servers,
            @NotNull JExLogger logger,
            @NotNull CentralServer row
    ) {
        this(plugin, servers, logger, row, DEFAULT_INTERVAL_TICKS);
    }

    public HeartbeatScheduler(
            @NotNull JavaPlugin plugin,
            @NotNull CentralServerService servers,
            @NotNull JExLogger logger,
            @NotNull CentralServer row,
            long intervalTicks
    ) {
        this.plugin = plugin;
        this.servers = servers;
        this.logger = logger;
        this.rowId = row.getId();
        this.intervalTicks = intervalTicks;
    }

    public void start() {
        if (this.task != null) return;
        this.task = Bukkit.getScheduler().runTaskTimerAsynchronously(
                this.plugin, this::beat, this.intervalTicks, this.intervalTicks);
    }

    public void stop() {
        if (this.task != null) {
            this.task.cancel();
            this.task = null;
        }
    }

    private void beat() {
        if (this.rowId == null) return;
        try {
            this.servers.repository().findByIdAsync(this.rowId)
                    .thenAccept(opt -> opt.ifPresent(fresh -> {
                        fresh.setLastHeartbeat(LocalDateTime.now());
                        fresh.setCurrentPlayers(Bukkit.getOnlinePlayers().size());
                        fresh.setMaxPlayers(Bukkit.getMaxPlayers());
                        fresh.setTps(Bukkit.getServer().getTPS()[0]);
                        fresh.setFailedHeartbeatCount(0);
                        this.servers.repository().update(fresh);
                        this.logger.debug("heartbeat: {} players, tps={}",
                                fresh.getCurrentPlayers(), fresh.getTps());
                    }))
                    .exceptionally(ex -> {
                        this.logger.error("heartbeat failed: {}", ex.getMessage());
                        return null;
                    });
        } catch (final RuntimeException ex) {
            this.logger.error("heartbeat scheduling failed: {}", ex.getMessage());
        }
    }
}
