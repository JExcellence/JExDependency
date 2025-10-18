package com.raindropcentral.rplatform.scheduler.impl;

import com.raindropcentral.rplatform.scheduler.ISchedulerAdapter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Scheduler implementation for Spigot/Paper servers using the standard Bukkit scheduler.
 * For "at-entity" and "at-location" semantics on non-Folia servers, we simply run on the main thread.
 */
public class BukkitISchedulerImpl implements ISchedulerAdapter {

    private static final Logger LOG = Logger.getLogger("RPlatform");
    private final JavaPlugin plugin;

    public BukkitISchedulerImpl(final @NotNull JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void runSync(@NotNull Runnable task) {
        Bukkit.getScheduler().runTask(plugin, safe(task));
    }

    @Override
    public void runAsync(@NotNull Runnable task) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, safe(task));
    }

    @Override
    public void runDelayed(@NotNull Runnable task, long delayTicks) {
        Bukkit.getScheduler().runTaskLater(plugin, safe(task), delayTicks);
    }

    @Override
    public void runRepeating(@NotNull Runnable task, long delayTicks, long periodTicks) {
        Bukkit.getScheduler().runTaskTimer(plugin, safe(task), delayTicks, periodTicks);
    }

    @Override
    public void runAtEntity(@NotNull Entity entity, @NotNull Runnable task) {
        // On Bukkit/Paper non-Folia, there is no entity-region concept; run on main.
        runSync(task);
    }

    @Override
    public void runAtLocation(@NotNull Location location, @NotNull Runnable task) {
        // On Bukkit/Paper non-Folia, there is no region scheduler; run on main.
        runSync(task);
    }

    @Override
    public void runGlobal(@NotNull Runnable task) {
        // Global == main thread for Bukkit/Paper.
        runSync(task);
    }

    @Override
    public @NotNull CompletableFuture<Void> runAsyncFuture(@NotNull Runnable task) {
        final CompletableFuture<Void> future = new CompletableFuture<>();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                task.run();
                future.complete(null);
            } catch (Throwable t) {
                LOG.log(Level.SEVERE, "[Scheduler] Async task failed", t);
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    private Runnable safe(Runnable task) {
        return () -> {
            try {
                task.run();
            } catch (Throwable t) {
                LOG.log(Level.SEVERE, "[Scheduler] Task failed", t);
                throw t;
            }
        };
    }
}