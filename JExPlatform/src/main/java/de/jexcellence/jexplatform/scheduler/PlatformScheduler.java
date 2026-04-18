package de.jexcellence.jexplatform.scheduler;

import de.jexcellence.jexplatform.server.ServerType;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

/**
 * Platform-agnostic scheduler abstracting Bukkit's global scheduler and
 * Folia's region-aware execution model behind a single API.
 *
 * <p>Use the {@link #create(JavaPlugin, ServerType)} factory to obtain the
 * correct implementation for the running server:
 *
 * <pre>{@code
 * var scheduler = PlatformScheduler.create(plugin, serverType);
 * scheduler.runAsync(() -> heavyWork());
 * scheduler.runAtEntity(player, () -> player.sendMessage("Done"));
 * }</pre>
 *
 * @author JExcellence
 * @since 1.0.0
 */
public sealed interface PlatformScheduler permits BukkitScheduler, FoliaScheduler {

    /**
     * Runs a task on the main server thread (or the global region on Folia).
     *
     * @param task runnable to execute synchronously
     */
    void runSync(@NotNull Runnable task);

    /**
     * Runs a task off the main server thread.
     *
     * @param task runnable to execute asynchronously
     */
    void runAsync(@NotNull Runnable task);

    /**
     * Runs a task after a delay on the main thread.
     *
     * @param task       runnable to execute after the delay
     * @param delayTicks ticks to wait before execution
     * @return cancellable handle for the scheduled task
     */
    @NotNull TaskHandle runDelayed(@NotNull Runnable task, long delayTicks);

    /**
     * Runs a repeating task on the main thread.
     *
     * @param task        runnable to execute repeatedly
     * @param delayTicks  initial delay before the first run
     * @param periodTicks interval between subsequent runs
     * @return cancellable handle for the scheduled task
     */
    @NotNull TaskHandle runRepeating(@NotNull Runnable task, long delayTicks, long periodTicks);

    /**
     * Runs a repeating task off the main thread.
     *
     * @param task        runnable to execute repeatedly
     * @param delayTicks  initial delay before the first run
     * @param periodTicks interval between subsequent runs
     * @return cancellable handle for the scheduled task
     */
    @NotNull TaskHandle runRepeatingAsync(@NotNull Runnable task, long delayTicks, long periodTicks);

    /**
     * Runs a task scoped to an entity's region (Folia) or on the main thread (Bukkit).
     *
     * @param entity the entity whose region should host the task
     * @param task   runnable to execute in the entity's context
     */
    void runAtEntity(@NotNull Entity entity, @NotNull Runnable task);

    /**
     * Runs a task scoped to a location's region (Folia) or on the main thread (Bukkit).
     *
     * @param location the location determining the execution region
     * @param task     runnable to execute in that region
     */
    void runAtLocation(@NotNull Location location, @NotNull Runnable task);

    /**
     * Runs a task asynchronously, returning a future for coordination.
     *
     * <p>The future completes normally when the task finishes or exceptionally
     * when the task throws.
     *
     * @param task runnable to execute asynchronously
     * @return future that completes when the task finishes
     */
    @NotNull CompletableFuture<Void> runAsyncFuture(@NotNull Runnable task);

    /**
     * Creates the appropriate scheduler for the detected server type.
     *
     * @param plugin     owning plugin for scheduler context
     * @param serverType detected server type
     * @return platform-specific scheduler implementation
     */
    static @NotNull PlatformScheduler create(@NotNull JavaPlugin plugin, @NotNull ServerType serverType) {
        return switch (serverType) {
            case ServerType.Folia ignored -> new FoliaScheduler(plugin);
            default -> new BukkitScheduler(plugin);
        };
    }
}
