package de.jexcellence.jexplatform.requirement.async;

import de.jexcellence.jexplatform.requirement.AbstractRequirement;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

/**
 * Abstract requirement that supports asynchronous evaluation.
 *
 * <p>Sync methods delegate to the async counterparts via {@code .join()}.
 * Implementations should override the {@code *Async} methods.
 *
 * @author JExcellence
 * @since 1.0.0
 */
public abstract class AsyncRequirement extends AbstractRequirement {

    /**
     * Creates an async requirement.
     *
     * @param typeId the type identifier
     */
    protected AsyncRequirement(@NotNull String typeId) {
        super(typeId);
    }

    /**
     * Creates an async requirement with consumption flag.
     *
     * @param typeId            the type identifier
     * @param consumeOnComplete whether to consume on completion
     */
    protected AsyncRequirement(@NotNull String typeId, boolean consumeOnComplete) {
        super(typeId, consumeOnComplete);
    }

    /**
     * Asynchronously checks whether the player meets this requirement.
     *
     * @param player the player
     * @return a future resolving to the check result
     */
    public abstract @NotNull CompletableFuture<Boolean> isMetAsync(@NotNull Player player);

    /**
     * Asynchronously calculates progress.
     *
     * @param player the player
     * @return a future resolving to the progress value
     */
    public abstract @NotNull CompletableFuture<Double> calculateProgressAsync(
            @NotNull Player player);

    /**
     * Asynchronously consumes resources.
     *
     * @param player the player
     * @return a future completing when consumption is done
     */
    public abstract @NotNull CompletableFuture<Void> consumeAsync(@NotNull Player player);

    @Override
    public boolean isMet(@NotNull Player player) {
        return isMetAsync(player).join();
    }

    @Override
    public double calculateProgress(@NotNull Player player) {
        return calculateProgressAsync(player).join();
    }

    @Override
    public void consume(@NotNull Player player) {
        consumeAsync(player).join();
    }
}
