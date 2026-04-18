package de.jexcellence.jexplatform.reward;

import de.jexcellence.jexplatform.logging.JExLogger;
import de.jexcellence.jexplatform.reward.event.RewardFailedEvent;
import de.jexcellence.jexplatform.reward.event.RewardGrantEvent;
import de.jexcellence.jexplatform.reward.event.RewardGrantedEvent;
import de.jexcellence.jexplatform.reward.lifecycle.RewardLifecycleRegistry;
import de.jexcellence.jexplatform.reward.metrics.RewardMetrics;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Service for granting rewards with events, lifecycle hooks, and metrics.
 *
 * @author JExcellence
 * @since 1.0.0
 */
public final class RewardService {

    private final JExLogger logger;
    private final RewardLifecycleRegistry lifecycleRegistry;
    private final RewardMetrics metrics;

    /**
     * Creates a new reward service.
     *
     * @param logger            the platform logger
     * @param lifecycleRegistry the lifecycle hook registry
     * @param metrics           the metrics collector
     */
    public RewardService(@NotNull JExLogger logger,
                         @NotNull RewardLifecycleRegistry lifecycleRegistry,
                         @NotNull RewardMetrics metrics) {
        this.logger = logger;
        this.lifecycleRegistry = lifecycleRegistry;
        this.metrics = metrics;
    }

    /**
     * Grants a reward to the player.
     *
     * @param player the player
     * @param reward the reward to grant
     * @return a future resolving to {@code true} on success
     */
    public @NotNull CompletableFuture<Boolean> grant(@NotNull Player player,
                                                     @NotNull AbstractReward reward) {
        var event = new RewardGrantEvent(player, reward);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return CompletableFuture.completedFuture(false);
        }

        if (!lifecycleRegistry.executeBeforeGrant(player, reward)) {
            return CompletableFuture.completedFuture(false);
        }

        var startTime = System.nanoTime();

        return reward.grant(player)
                .thenApply(success -> {
                    var duration = System.nanoTime() - startTime;
                    metrics.recordGrant(reward.typeId(), duration, success);

                    if (success) {
                        lifecycleRegistry.executeAfterGrant(player, reward);
                        Bukkit.getPluginManager().callEvent(
                                new RewardGrantedEvent(player, reward));
                    }
                    return success;
                })
                .exceptionally(ex -> {
                    var duration = System.nanoTime() - startTime;
                    metrics.recordGrant(reward.typeId(), duration, false);
                    metrics.recordError(reward.typeId());
                    logger.error("Error granting reward: {}", reward.typeId(), ex);
                    lifecycleRegistry.executeOnError(player, reward, ex);
                    Bukkit.getPluginManager().callEvent(
                            new RewardFailedEvent(player, reward, ex));
                    return false;
                });
    }

    /**
     * Grants all rewards to the player.
     *
     * @param player  the player
     * @param rewards the rewards
     * @return a future resolving when all are processed
     */
    public @NotNull CompletableFuture<Void> grantAll(@NotNull Player player,
                                                     @NotNull List<AbstractReward> rewards) {
        var futures = rewards.stream()
                .map(r -> grant(player, r))
                .toArray(CompletableFuture[]::new);
        return CompletableFuture.allOf(futures);
    }
}
