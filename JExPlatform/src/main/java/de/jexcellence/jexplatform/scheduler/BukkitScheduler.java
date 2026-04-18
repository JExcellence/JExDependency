package de.jexcellence.jexplatform.scheduler;

import de.jexcellence.jexplatform.logging.JExLogger;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Bukkit/Paper scheduler delegating to {@link org.bukkit.scheduler.BukkitScheduler}.
 *
 * <p>Entity and location scoped methods fall back to the main thread because
 * the legacy scheduler has no region-aware abstractions.
 *
 * @author JExcellence
 * @since 1.0.0
 */
public final class BukkitScheduler implements PlatformScheduler {

    private static final JExLogger LOG = JExLogger.of("Scheduler");
    private final JavaPlugin plugin;

    /**
     * Creates a scheduler bound to the given plugin.
     *
     * @param plugin owning plugin for Bukkit scheduling calls
     */
    BukkitScheduler(@NotNull JavaPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    @Override
    public void runSync(@NotNull Runnable task) {
        Bukkit.getScheduler().runTask(plugin, guard(task));
    }

    @Override
    public void runAsync(@NotNull Runnable task) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, guard(task));
    }

    @Override
    public @NotNull TaskHandle runDelayed(@NotNull Runnable task, long delayTicks) {
        return wrap(Bukkit.getScheduler().runTaskLater(plugin, guard(task), delayTicks));
    }

    @Override
    public @NotNull TaskHandle runRepeating(@NotNull Runnable task, long delayTicks, long periodTicks) {
        return wrap(Bukkit.getScheduler().runTaskTimer(plugin, guard(task), delayTicks, periodTicks));
    }

    @Override
    public @NotNull TaskHandle runRepeatingAsync(@NotNull Runnable task, long delayTicks, long periodTicks) {
        return wrap(Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, guard(task), delayTicks, periodTicks));
    }

    /** Falls back to {@link #runSync(Runnable)} — Bukkit has no entity scheduler. */
    @Override
    public void runAtEntity(@NotNull Entity entity, @NotNull Runnable task) {
        runSync(task);
    }

    /** Falls back to {@link #runSync(Runnable)} — Bukkit has no region scheduler. */
    @Override
    public void runAtLocation(@NotNull Location location, @NotNull Runnable task) {
        runSync(task);
    }

    @Override
    public @NotNull CompletableFuture<Void> runAsyncFuture(@NotNull Runnable task) {
        var future = new CompletableFuture<Void>();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                task.run();
                future.complete(null);
            } catch (Throwable t) {
                LOG.error("Async task failed", t);
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    // ── Internal ────────────────────────────────────────────────────────────────

    private Runnable guard(@NotNull Runnable task) {
        Objects.requireNonNull(task, "task");
        return () -> {
            try {
                task.run();
            } catch (Throwable t) {
                LOG.error("Scheduled task failed", t);
                throw t;
            }
        };
    }

    private static TaskHandle wrap(@NotNull BukkitTask bukkitTask) {
        return new TaskHandle() {

            @Override
            public boolean cancel() {
                if (bukkitTask.isCancelled()) return false;
                bukkitTask.cancel();
                return true;
            }

            @Override
            public boolean isCancelled() {
                return bukkitTask.isCancelled();
            }
        };
    }
}
