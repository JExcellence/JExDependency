package de.jexcellence.jexplatform.reward.async;

import de.jexcellence.jexplatform.reward.AbstractReward;
import de.jexcellence.jexplatform.reward.RewardService;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Asynchronous wrapper around {@link RewardService}.
 *
 * <p>Composes operations as {@link CompletableFuture} chains for non-blocking
 * reward granting.
 *
 * @author JExcellence
 * @since 1.0.0
 */
public final class AsyncRewardService {

    private final RewardService delegate;

    /**
     * Creates an async reward service.
     *
     * @param delegate the synchronous service to wrap
     */
    public AsyncRewardService(@NotNull RewardService delegate) {
        this.delegate = delegate;
    }

    /**
     * Asynchronously grants a reward to the player.
     *
     * @param player the player
     * @param reward the reward
     * @return a future resolving to the result
     */
    public @NotNull CompletableFuture<Boolean> grantAsync(
            @NotNull Player player,
            @NotNull AbstractReward reward) {
        return CompletableFuture.supplyAsync(() -> delegate.grant(player, reward).join());
    }

    /**
     * Asynchronously grants all rewards to the player.
     *
     * @param player  the player
     * @param rewards the rewards
     * @return a future resolving when all are processed
     */
    public @NotNull CompletableFuture<Void> grantAllAsync(
            @NotNull Player player,
            @NotNull List<AbstractReward> rewards) {
        var futures = rewards.stream()
                .map(r -> grantAsync(player, r))
                .toList();

        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
    }
}
