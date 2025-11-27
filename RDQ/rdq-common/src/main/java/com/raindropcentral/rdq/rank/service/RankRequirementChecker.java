package com.raindropcentral.rdq.rank.service;

import com.raindropcentral.rdq.rank.RankRequirement;
import com.raindropcentral.rdq.rank.repository.PlayerRankRepository;
import org.bukkit.Bukkit;
import org.bukkit.Statistic;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

public final class RankRequirementChecker {

    private static final Logger LOGGER = Logger.getLogger(RankRequirementChecker.class.getName());

    private final PlayerRankRepository playerRankRepository;

    public RankRequirementChecker(@NotNull PlayerRankRepository playerRankRepository) {
        this.playerRankRepository = playerRankRepository;
    }

    @NotNull
    public CompletableFuture<Boolean> checkAll(@NotNull UUID playerId, @NotNull List<RankRequirement> requirements) {
        if (requirements.isEmpty()) {
            return CompletableFuture.completedFuture(true);
        }

        var futures = requirements.stream()
            .map(req -> check(playerId, req))
            .toList();

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(v -> futures.stream().allMatch(f -> f.join()));
    }

    @NotNull
    public CompletableFuture<Boolean> check(@NotNull UUID playerId, @NotNull RankRequirement requirement) {
        return switch (requirement) {
            case RankRequirement.StatisticRequirement(var statisticType, var amount) ->
                checkStatistic(playerId, statisticType, amount);

            case RankRequirement.PermissionRequirement(var permission) ->
                checkPermission(playerId, permission);

            case RankRequirement.PreviousRankRequirement(var requiredRankId) ->
                playerRankRepository.hasUnlockedRankAsync(playerId, requiredRankId);

            case RankRequirement.CurrencyRequirement(var currency, var amount) ->
                checkCurrency(playerId, currency, amount);

            case RankRequirement.ItemRequirement(var material, var amount) ->
                checkItem(playerId, material, amount);

            case RankRequirement.LevelRequirement(var level) ->
                checkLevel(playerId, level);

            case RankRequirement.PlaytimeRequirement(var seconds) ->
                checkPlaytime(playerId, seconds);
        };
    }


    private CompletableFuture<Boolean> checkStatistic(@NotNull UUID playerId, @NotNull String statisticType, int amount) {
        return CompletableFuture.supplyAsync(() -> {
            var player = Bukkit.getPlayer(playerId);
            if (player == null) {
                return false;
            }

            try {
                var statistic = Statistic.valueOf(statisticType.toUpperCase());
                return player.getStatistic(statistic) >= amount;
            } catch (IllegalArgumentException e) {
                LOGGER.warning("Unknown statistic type: " + statisticType);
                return false;
            }
        });
    }

    private CompletableFuture<Boolean> checkPermission(@NotNull UUID playerId, @NotNull String permission) {
        return CompletableFuture.supplyAsync(() -> {
            var player = Bukkit.getPlayer(playerId);
            return player != null && player.hasPermission(permission);
        });
    }

    private CompletableFuture<Boolean> checkCurrency(@NotNull UUID playerId, @NotNull String currency, @NotNull java.math.BigDecimal amount) {
        return CompletableFuture.completedFuture(true);
    }

    private CompletableFuture<Boolean> checkItem(@NotNull UUID playerId, @NotNull String material, int amount) {
        return CompletableFuture.supplyAsync(() -> {
            var player = Bukkit.getPlayer(playerId);
            if (player == null) {
                return false;
            }

            try {
                var mat = org.bukkit.Material.valueOf(material.toUpperCase());
                var inventory = player.getInventory();
                int count = 0;
                for (var item : inventory.getContents()) {
                    if (item != null && item.getType() == mat) {
                        count += item.getAmount();
                    }
                }
                return count >= amount;
            } catch (IllegalArgumentException e) {
                LOGGER.warning("Unknown material: " + material);
                return false;
            }
        });
    }

    private CompletableFuture<Boolean> checkLevel(@NotNull UUID playerId, int level) {
        return CompletableFuture.supplyAsync(() -> {
            var player = Bukkit.getPlayer(playerId);
            return player != null && player.getLevel() >= level;
        });
    }

    private CompletableFuture<Boolean> checkPlaytime(@NotNull UUID playerId, long seconds) {
        return CompletableFuture.supplyAsync(() -> {
            var player = Bukkit.getPlayer(playerId);
            if (player == null) {
                return false;
            }
            var ticksPlayed = player.getStatistic(Statistic.PLAY_ONE_MINUTE);
            var secondsPlayed = ticksPlayed / 20;
            return secondsPlayed >= seconds;
        });
    }
}
