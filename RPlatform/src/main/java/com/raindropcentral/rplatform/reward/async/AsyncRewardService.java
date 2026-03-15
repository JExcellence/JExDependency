package com.raindropcentral.rplatform.reward.async;

import com.raindropcentral.rplatform.reward.AbstractReward;
import com.raindropcentral.rplatform.reward.RewardService;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Represents the AsyncRewardService API type.
 */
public final class AsyncRewardService {

    private static final AsyncRewardService INSTANCE = new AsyncRewardService();
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final RewardService rewardService = RewardService.getInstance();

    private AsyncRewardService() {}

    /**
     * Gets instance.
     */
    public static AsyncRewardService getInstance() {
        return INSTANCE;
    }

    /**
     * Executes grantAsync.
     */
    public @NotNull CompletableFuture<Boolean> grantAsync(@NotNull Player player, @NotNull AbstractReward reward) {
        return CompletableFuture.supplyAsync(
            () -> rewardService.grant(player, reward).join(),
            executor
        );
    }

    /**
     * Executes grantAllAsync.
     */
    public @NotNull CompletableFuture<Boolean> grantAllAsync(
        @NotNull Player player,
        @NotNull List<AbstractReward> rewards
    ) {
        return CompletableFuture.supplyAsync(
            () -> rewardService.grantAll(player, rewards).join(),
            executor
        );
    }

    /**
     * Executes grantAllWithResults.
     */
    public @NotNull CompletableFuture<List<RewardResult>> grantAllWithResults(
        @NotNull Player player,
        @NotNull List<AbstractReward> rewards
    ) {
        return CompletableFuture.supplyAsync(() -> {
            return rewards.stream()
                .map(reward -> {
                    try {
                        boolean success = rewardService.grant(player, reward).join();
                        return new RewardResult(reward, success, null);
                    } catch (Exception e) {
                        return new RewardResult(reward, false, e);
                    }
                })
                .toList();
        }, executor);
    }

    /**
     * Represents the RewardResult API type.
     */
    public record RewardResult(
        @NotNull AbstractReward reward,
        boolean success,
        Throwable error
    ) {}

    /**
     * Executes shutdown.
     */
    public void shutdown() {
        executor.shutdown();
    }
}
