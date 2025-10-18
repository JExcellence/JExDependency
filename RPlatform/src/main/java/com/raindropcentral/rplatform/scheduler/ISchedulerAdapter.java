package com.raindropcentral.rplatform.scheduler;

import com.raindropcentral.rplatform.api.PlatformType;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public interface ISchedulerAdapter {

    void runSync(final @NotNull Runnable task);

    void runAsync(final @NotNull Runnable task);

    void runDelayed(final @NotNull Runnable task, final long delayTicks);

    void runRepeating(final @NotNull Runnable task, final long delayTicks, final long periodTicks);

    void runAtEntity(final @NotNull Entity entity, final @NotNull Runnable task);

    void runAtLocation(final @NotNull Location location, final @NotNull Runnable task);

    void runGlobal(final @NotNull Runnable task);

    @NotNull CompletableFuture<Void> runAsyncFuture(final @NotNull Runnable task);

    static @NotNull ISchedulerAdapter create(
            final @NotNull JavaPlugin plugin,
            final @NotNull PlatformType platformType
    ) {
        return switch (platformType) {
            case FOLIA -> createFoliaScheduler(plugin);
            case PAPER, SPIGOT -> createBukkitScheduler(plugin);
        };
    }

    private static @NotNull ISchedulerAdapter createFoliaScheduler(final @NotNull JavaPlugin plugin) {
        try {
            final Class<?> clazz = Class.forName("com.raindropcentral.rplatform.scheduler.impl.FoliaISchedulerImpl");
            return (ISchedulerAdapter) clazz.getConstructor(JavaPlugin.class).newInstance(plugin);
        } catch (final Exception e) {
            return createBukkitScheduler(plugin);
        }
    }

    private static @NotNull ISchedulerAdapter createBukkitScheduler(final @NotNull JavaPlugin plugin) {
        try {
            final Class<?> clazz = Class.forName("com.raindropcentral.rplatform.scheduler.impl.BukkitISchedulerImpl");
            return (ISchedulerAdapter) clazz.getConstructor(JavaPlugin.class).newInstance(plugin);
        } catch (final Exception e) {
            throw new RuntimeException("Failed to create scheduler", e);
        }
    }
}
