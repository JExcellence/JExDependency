package com.raindropcentral.rdq.reward;

import com.raindropcentral.rdq.RDQ;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class RewardCollector {

    private final RDQ rdq;

    public RewardCollector(@NotNull RDQ rdq) {
        this.rdq = rdq;
    }

    public @NotNull CompletableFuture<CollectionResult> collect(@NotNull Player player, @NotNull List<Reward> rewards) {
        return CompletableFuture.supplyAsync(() -> {
            double totalValue = rewards.stream().mapToDouble(Reward::getEstimatedValue).sum();
            
            var futures = rewards.stream()
                .map(reward -> reward.grant(player))
                .toArray(CompletableFuture[]::new);
            
            return CompletableFuture.allOf(futures)
                .thenApply(v -> {
                    updatePlayerStats(player, totalValue);
                    return new CollectionResult(true, rewards.size(), totalValue);
                });
        }).thenCompose(f -> f);
    }

    private void updatePlayerStats(@NotNull Player player, double totalValue) {
        rdq.getPlayerRepository().findByUuidAsync(player.getUniqueId())
            .thenAccept(rdqPlayer -> rdqPlayer.ifPresent(p -> {
                p.getBountyStatistics().recordBountyClaim(totalValue);
                rdq.getPlayerRepository().update(p);
            }));
    }

    public record CollectionResult(boolean success, int rewardsGranted, double totalValue) {}
}
