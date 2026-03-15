package com.raindropcentral.rplatform.requirement.async;

import com.raindropcentral.rplatform.requirement.AbstractRequirement;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

/**
 * Base class for requirements that need async operations (database, API calls, etc.).
 *
 * <p>Provides both sync and async methods - sync methods delegate to async for consistency.
 */
public abstract class AsyncRequirement extends AbstractRequirement {

    protected AsyncRequirement(@NotNull String typeId) {
        super(typeId);
    }

    protected AsyncRequirement(@NotNull String typeId, boolean consumeOnComplete) {
        super(typeId, consumeOnComplete);
    }

    // ==================== Async Methods (Override These) ====================

    /**
     * Async check if requirement is met.
     */
    @NotNull
    public abstract CompletableFuture<Boolean> isMetAsync(@NotNull Player player);

    /**
     * Async progress calculation.
     */
    @NotNull
    public abstract CompletableFuture<Double> calculateProgressAsync(@NotNull Player player);

    /**
     * Async consumption.
     */
    @NotNull
    public abstract CompletableFuture<Void> consumeAsync(@NotNull Player player);

    // ==================== Sync Methods (Delegate to Async) ====================

    /**
     * Returns whether met.
     */
    @Override
    public final boolean isMet(@NotNull Player player) {
        return isMetAsync(player).join();
    }

    /**
     * Executes calculateProgress.
     */
    @Override
    public final double calculateProgress(@NotNull Player player) {
        return calculateProgressAsync(player).join();
    }

    /**
     * Executes consume.
     */
    @Override
    public final void consume(@NotNull Player player) {
        consumeAsync(player).join();
    }
}
