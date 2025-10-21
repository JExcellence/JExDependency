package com.raindropcentral.rplatform.scheduler.impl;

import com.raindropcentral.rplatform.scheduler.ISchedulerAdapter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Bukkit/Paper scheduler implementation that delegates to {@link org.bukkit.scheduler.BukkitScheduler}
 * for all operations while preserving {@link ISchedulerAdapter}'s threading guarantees.
 * <p>
 * Entity and location scoped scheduling is emulated by returning to the main thread because the legacy
 * scheduler lacks Folia's region abstractions.
 * </p>
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.2
 */
public class BukkitISchedulerImpl implements ISchedulerAdapter {

    private static final Logger LOG = Logger.getLogger("RPlatform");
    private final JavaPlugin plugin;

    /**
     * Creates a scheduler adapter bound to the provided plugin for all Bukkit scheduling calls.
     *
     * @param plugin owning plugin, used as the scheduling context
     */
    public BukkitISchedulerImpl(final @NotNull JavaPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    /**
     * {@inheritDoc}
     * <p>
     * Delegates to {@link org.bukkit.scheduler.BukkitScheduler#runTask(org.bukkit.plugin.Plugin, Runnable)}
     * ensuring the task runs on the primary tick thread.
     * </p>
     */
    @Override
    public void runSync(@NotNull Runnable task) {
        Bukkit.getScheduler().runTask(plugin, safe(task));
    }

    /**
     * {@inheritDoc}
     * <p>
     * Uses {@link org.bukkit.scheduler.BukkitScheduler#runTaskAsynchronously(org.bukkit.plugin.Plugin, Runnable)}
     * which executes on Bukkit's managed async pool.
     * </p>
     */
    @Override
    public void runAsync(@NotNull Runnable task) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, safe(task));
    }

    /**
     * {@inheritDoc}
     * <p>
     * Schedules via {@link org.bukkit.scheduler.BukkitScheduler#runTaskLater(org.bukkit.plugin.Plugin, Runnable, long)}
     * so the task runs synchronously after the configured delay.
     * </p>
     */
    @Override
    public void runDelayed(@NotNull Runnable task, long delayTicks) {
        Bukkit.getScheduler().runTaskLater(plugin, safe(task), delayTicks);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Uses {@link org.bukkit.scheduler.BukkitScheduler#runTaskTimer(org.bukkit.plugin.Plugin, Runnable, long, long)}
     * guaranteeing execution on the main thread at the requested cadence.
     * </p>
     */
    @Override
    public void runRepeating(@NotNull Runnable task, long delayTicks, long periodTicks) {
        Bukkit.getScheduler().runTaskTimer(plugin, safe(task), delayTicks, periodTicks);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Since entity scoped schedulers are unavailable, this simply redirects to {@link #runSync(Runnable)}.
     * </p>
     */
    @Override
    public void runAtEntity(@NotNull Entity entity, @NotNull Runnable task) {
        runSync(task);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Location scoped execution is emulated by running on the main thread because Bukkit has no region
     * aware scheduler.
     * </p>
     */
    @Override
    public void runAtLocation(@NotNull Location location, @NotNull Runnable task) {
        runSync(task);
    }

    /**
     * {@inheritDoc}
     * <p>
     * The Bukkit implementation treats global execution as synchronous execution on the tick thread.
     * </p>
     */
    @Override
    public void runGlobal(@NotNull Runnable task) {
        runSync(task);
    }

    /**
     * {@inheritDoc}
     * <p>
     * The future completes from within the scheduler callback to propagate errors using the platform's
     * logging configuration.
     * </p>
     */
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

    /**
     * Wraps a task to ensure all exceptions are logged consistently before rethrowing to the scheduler.
     *
     * @param task runnable to wrap
     * @return defensive runnable that logs failures
     */
    private Runnable safe(Runnable task) {
        Objects.requireNonNull(task, "task");
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