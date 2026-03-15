package com.raindropcentral.rplatform.scheduler;

import com.raindropcentral.rplatform.api.PlatformType;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

/**
 * Coordinates cross-platform scheduling so the {@link com.raindropcentral.rplatform.RPlatform} runtime can.
 * offer a single façade over Folia's region aware execution model and the legacy Bukkit scheduler used by
 * Paper and Spigot.
 *
 * <p>Each method describes whether work happens on the primary tick thread or asynchronously so callers can
 * interact with Bukkit APIs safely. Implementations are discovered reflectively via
 * {@link #create(JavaPlugin, PlatformType)} ensuring Folia servers load
 * {@code scheduler.impl.FoliaISchedulerImpl} while other platforms fall back to
 * {@code scheduler.impl.BukkitISchedulerImpl}.
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
public interface ISchedulerAdapter {

    /**
     * Executes the supplied {@code task} on the main server thread.
 *
 * <p>On Folia this delegates to the global region scheduler, while Paper and Spigot enqueue the task on
     * the standard Bukkit synchronous scheduler queue. Use this for Bukkit API interactions that must
     * occur on the tick thread.
     *
     * @param task runnable to execute synchronously; must not be {@code null}
     */
    void runSync(final @NotNull Runnable task);

    /**
     * Schedules the {@code task} away from the primary server thread.
 *
 * <p>Implementations may use the Bukkit asynchronous pool or the JVM's default executor. Because the task
     * runs off-thread it must avoid touching Bukkit state unless it hands work back to {@link #runSync(Runnable)}.
     *
     * @param task runnable to execute asynchronously; must not be {@code null}
     */
    void runAsync(final @NotNull Runnable task);

    /**
     * Enqueues {@code task} to execute after {@code delayTicks} on the scheduler's synchronous thread model.
 *
 * <p>Folia implementations leverage the global region scheduler, while Bukkit/Paper use
     * {@code BukkitScheduler#runTaskLater}. The task therefore executes on the tick thread when the delay
     * expires.
     *
     * @param task runnable to execute after the delay; must not be {@code null}
     * @param delayTicks number of ticks to wait before execution
     */
    void runDelayed(final @NotNull Runnable task, final long delayTicks);

    /**
     * Registers {@code task} to execute repeatedly with the given delay and period on the scheduler's.
     * synchronous execution context.
 *
 * <p>The first invocation happens after {@code delayTicks}; subsequent executions repeat every
     * {@code periodTicks}. Implementations make best efforts to keep cadence consistent with the server
     * tick rate.
     *
     * @param task runnable to execute repeatedly; must not be {@code null}
     * @param delayTicks initial delay before the first run, in ticks
     * @param periodTicks interval between runs after the initial delay, in ticks
     */
    void runRepeating(final @NotNull Runnable task, final long delayTicks, final long periodTicks);

    /**
     * Executes runRepeatingAsync.
     */
    void runRepeatingAsync(final @NotNull Runnable task, final long delayTicks, final long periodTicks);

    /**
     * Executes {@code task} in the context of {@code entity}'s scheduler if the platform supports it.
 *
 * <p>Folia scopes the work to the entity's region, preserving thread safety guarantees for entity state,
     * while Bukkit/Paper fall back to {@link #runSync(Runnable)} because entity scoped schedulers are not
     * available.
     *
     * @param entity entity whose scheduler should host the task; must not be {@code null}
     * @param task runnable to execute for the entity context; must not be {@code null}
     */
    void runAtEntity(final @NotNull Entity entity, final @NotNull Runnable task);

    /**
     * Executes {@code task} for the region that contains {@code location} when a region aware scheduler.
     * exists.
 *
 * <p>Folia keeps the work pinned to the region thread, ensuring world access happens without blocking
     * other regions. On Bukkit/Paper the task executes on the main thread.
     *
     * @param location location to determine the execution region; must not be {@code null}
     * @param task runnable to execute for the region; must not be {@code null}
     */
    void runAtLocation(final @NotNull Location location, final @NotNull Runnable task);

    /**
     * Executes {@code task} on the platform's global scheduler entry point.
 *
 * <p>Folia resolves the global region scheduler while Bukkit/Paper schedules the work on the main thread.
     * Callers should use this for tick thread safe logic that is not tied to a particular location or entity.
     *
     * @param task runnable to execute globally; must not be {@code null}
     */
    void runGlobal(final @NotNull Runnable task);

    /**
     * Executes {@code task} asynchronously and propagates completion via a {@link CompletableFuture}.
 *
 * <p>The returned future completes successfully when the task finishes or exceptionally when the task
     * throws. This is useful for coordinating async work with the rest of the platform.
     *
     * @param task runnable to execute asynchronously; must not be {@code null}
     * @return future that completes when the task finishes
     */
    @NotNull CompletableFuture<Void> runAsyncFuture(final @NotNull Runnable task);

    /**
     * Creates a scheduler adapter for the supplied {@code platformType} using reflection so Folia can load.
     * its region aware implementation without introducing a hard compile time dependency.
     *
     * @param plugin        owning plugin used when interacting with scheduler APIs
     * @param platformType  detected platform type
     * @return platform specific scheduler adapter
     */
    static @NotNull ISchedulerAdapter create(
            final @NotNull JavaPlugin plugin,
            final @NotNull PlatformType platformType
    ) {
        return switch (platformType) {
            case FOLIA -> createFoliaScheduler(plugin);
            case PAPER, SPIGOT -> createBukkitScheduler(plugin);
        };
    }

    /**
     * Attempts to construct the Folia scheduler implementation and falls back to the Bukkit variant if.
     * any reflection step fails.
     *
     * @param plugin plugin instance used when binding to scheduler APIs
     * @return Folia aware scheduler when available, otherwise the Bukkit adapter
     */
    private static @NotNull ISchedulerAdapter createFoliaScheduler(final @NotNull JavaPlugin plugin) {
        try {
            final Class<?> clazz = Class.forName("com.raindropcentral.rplatform.scheduler.impl.FoliaISchedulerImpl");
            return (ISchedulerAdapter) clazz.getConstructor(JavaPlugin.class).newInstance(plugin);
        } catch (final Exception e) {
            return createBukkitScheduler(plugin);
        }
    }

    /**
     * Constructs the Bukkit/Paper scheduler implementation or throws if reflection fails completely.
     *
     * @param plugin plugin instance used when binding to scheduler APIs
     * @return scheduler backed by the Bukkit scheduler
     */
    private static @NotNull ISchedulerAdapter createBukkitScheduler(final @NotNull JavaPlugin plugin) {
        try {
            final Class<?> clazz = Class.forName("com.raindropcentral.rplatform.scheduler.impl.BukkitISchedulerImpl");
            return (ISchedulerAdapter) clazz.getConstructor(JavaPlugin.class).newInstance(plugin);
        } catch (final Exception e) {
            throw new RuntimeException("Failed to create scheduler", e);
        }
    }
}
