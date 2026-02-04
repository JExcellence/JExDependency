package com.raindropcentral.rplatform.reward.async;

import com.raindropcentral.rplatform.reward.AbstractReward;
import com.raindropcentral.rplatform.reward.RewardService;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class AsyncRewardService {

    private static final AsyncRewardService INSTANCE = new AsyncRewardService();
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final RewardService rewardService = RewardService.getInstance();

    private AsyncRewardService() {}

    public static AsyncRewardService getInstance() {
        return INSTANCE;
    }

    public @NotNull CompletableFuture<Boolean> grantAsync(@NotNull Player player, @NotNull AbstractReward reward) {
        return CompletableFuture.supplyAsync(
            () -> rewardService.grant(player, reward).join(),
            executor
        );
    }

    public @NotNull CompletableFuture<Boolean> grantAllAsync(
        @NotNull Player player,
        @NotNull List<AbstractReward> rewards
    ) {
        return CompletableFuture.supplyAsync(
            () -> rewardService.grantAll(player, rewards).join(),
            executor
        );
    }

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

    public record RewardResult(
        @NotNull AbstractReward reward,
        boolean success,
        Throwable error
    ) {}

    public void shutdown() {
        executor.shutdown();
    }
}
