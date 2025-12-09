package com.raindropcentral.rdq2.shared;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.logging.Logger;

public final class AsyncExecutor {

    private static final Logger LOGGER = Logger.getLogger(AsyncExecutor.class.getName());

    private final ExecutorService executor;
    private volatile boolean shutdown = false;

    public AsyncExecutor() {
        this.executor = createExecutor();
        LOGGER.info("AsyncExecutor initialized with " + getExecutorType());
    }

    private static ExecutorService createExecutor() {
        try {
            return Executors.newVirtualThreadPerTaskExecutor();
        } catch (Throwable ignored) {
            var processors = Runtime.getRuntime().availableProcessors();
            return Executors.newFixedThreadPool(Math.max(2, processors));
        }
    }

    private String getExecutorType() {
        var name = executor.getClass().getSimpleName();
        return name.contains("Virtual") ? "Virtual Threads" : "Thread Pool";
    }

    public <T> CompletableFuture<T> supplyAsync(@NotNull Supplier<T> supplier) {
        if (shutdown) {
            return CompletableFuture.failedFuture(new IllegalStateException("Executor is shut down"));
        }
        return CompletableFuture.supplyAsync(supplier, executor);
    }

    public CompletableFuture<Void> runAsync(@NotNull Runnable runnable) {
        if (shutdown) {
            return CompletableFuture.failedFuture(new IllegalStateException("Executor is shut down"));
        }
        return CompletableFuture.runAsync(runnable, executor);
    }

    public CompletableFuture<Void> runSync(@NotNull JavaPlugin plugin, @NotNull Runnable runnable) {
        if (shutdown) {
            return CompletableFuture.failedFuture(new IllegalStateException("Executor is shut down"));
        }

        var future = new CompletableFuture<Void>();

        if (Bukkit.isPrimaryThread()) {
            try {
                runnable.run();
                future.complete(null);
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        } else {
            Bukkit.getScheduler().runTask(plugin, () -> {
                try {
                    runnable.run();
                    future.complete(null);
                } catch (Throwable t) {
                    future.completeExceptionally(t);
                }
            });
        }

        return future;
    }

    public <T> CompletableFuture<T> supplySync(@NotNull JavaPlugin plugin, @NotNull Supplier<T> supplier) {
        if (shutdown) {
            return CompletableFuture.failedFuture(new IllegalStateException("Executor is shut down"));
        }

        var future = new CompletableFuture<T>();

        if (Bukkit.isPrimaryThread()) {
            try {
                future.complete(supplier.get());
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        } else {
            Bukkit.getScheduler().runTask(plugin, () -> {
                try {
                    future.complete(supplier.get());
                } catch (Throwable t) {
                    future.completeExceptionally(t);
                }
            });
        }

        return future;
    }

    public void shutdown() {
        if (shutdown) {
            return;
        }
        shutdown = true;

        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    LOGGER.warning("Executor did not terminate cleanly");
                }
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        LOGGER.info("AsyncExecutor shut down");
    }

    public boolean isShutdown() {
        return shutdown;
    }

    @NotNull
    public ExecutorService getExecutor() {
        return executor;
    }
}
