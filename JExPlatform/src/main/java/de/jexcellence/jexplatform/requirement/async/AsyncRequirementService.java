package de.jexcellence.jexplatform.requirement.async;

import de.jexcellence.jexplatform.requirement.AbstractRequirement;
import de.jexcellence.jexplatform.requirement.RequirementService;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Asynchronous wrapper around {@link RequirementService}.
 *
 * <p>Composes operations as {@link CompletableFuture} chains for non-blocking
 * requirement evaluation.
 *
 * @author JExcellence
 * @since 1.0.0
 */
public final class AsyncRequirementService {

    private final RequirementService delegate;

    /**
     * Creates an async requirement service.
     *
     * @param delegate the synchronous service to wrap
     */
    public AsyncRequirementService(@NotNull RequirementService delegate) {
        this.delegate = delegate;
    }

    /**
     * Asynchronously checks whether the requirement is met.
     *
     * @param player      the player
     * @param requirement the requirement
     * @return a future resolving to the result
     */
    public @NotNull CompletableFuture<Boolean> isMetAsync(
            @NotNull Player player,
            @NotNull AbstractRequirement requirement) {
        return CompletableFuture.supplyAsync(() -> delegate.isMet(player, requirement));
    }

    /**
     * Asynchronously calculates progress.
     *
     * @param player      the player
     * @param requirement the requirement
     * @return a future resolving to the progress
     */
    public @NotNull CompletableFuture<Double> calculateProgressAsync(
            @NotNull Player player,
            @NotNull AbstractRequirement requirement) {
        return CompletableFuture.supplyAsync(
                () -> delegate.calculateProgress(player, requirement));
    }

    /**
     * Asynchronously consumes resources.
     *
     * @param player      the player
     * @param requirement the requirement
     * @return a future completing when done
     */
    public @NotNull CompletableFuture<Void> consumeAsync(
            @NotNull Player player,
            @NotNull AbstractRequirement requirement) {
        return CompletableFuture.runAsync(() -> delegate.consume(player, requirement));
    }

    /**
     * Asynchronously checks whether all requirements are met.
     *
     * @param player       the player
     * @param requirements the requirements
     * @return a future resolving to the result
     */
    public @NotNull CompletableFuture<Boolean> areAllMetAsync(
            @NotNull Player player,
            @NotNull List<AbstractRequirement> requirements) {
        var futures = requirements.stream()
                .map(req -> isMetAsync(player, req))
                .toList();

        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new))
                .thenApply(v -> futures.stream()
                        .allMatch(CompletableFuture::join));
    }
}
