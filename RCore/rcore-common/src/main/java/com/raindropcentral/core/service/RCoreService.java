package com.raindropcentral.core.service;

import com.raindropcentral.core.database.entity.player.RPlayer;
import com.raindropcentral.core.database.entity.statistic.RAbstractStatistic;
import com.raindropcentral.core.database.entity.statistic.RPlayerStatistic;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface RCoreService {

    CompletableFuture<Optional<RPlayer>> findPlayerAsync(@NotNull UUID uniqueId);
    CompletableFuture<Optional<RPlayer>> findPlayerAsync(@NotNull OfflinePlayer offlinePlayer);
    CompletableFuture<Optional<RPlayer>> findPlayerByNameAsync(@NotNull String playerName);
    CompletableFuture<Boolean> playerExistsAsync(@NotNull UUID uniqueId);
    CompletableFuture<Boolean> playerExistsAsync(@NotNull OfflinePlayer offlinePlayer);

    CompletableFuture<Optional<RPlayer>> createPlayerAsync(@NotNull UUID uniqueId, @NotNull String playerName);
    CompletableFuture<Optional<RPlayer>> updatePlayerAsync(@NotNull RPlayer player);

    CompletableFuture<Optional<RPlayerStatistic>> findPlayerStatisticsAsync(@NotNull UUID uniqueId);
    CompletableFuture<Optional<RPlayerStatistic>> findPlayerStatisticsAsync(@NotNull OfflinePlayer offlinePlayer);

    CompletableFuture<Optional<Object>> findStatisticValueAsync(
        @NotNull UUID uniqueId,
        @NotNull String identifier,
        @NotNull String plugin
    );

    CompletableFuture<Optional<Object>> findStatisticValueAsync(
        @NotNull OfflinePlayer offlinePlayer,
        @NotNull String identifier,
        @NotNull String plugin
    );

    CompletableFuture<Boolean> hasStatisticAsync(
        @NotNull UUID uniqueId,
        @NotNull String identifier,
        @NotNull String plugin
    );

    CompletableFuture<Boolean> hasStatisticAsync(
        @NotNull OfflinePlayer offlinePlayer,
        @NotNull String identifier,
        @NotNull String plugin
    );

    CompletableFuture<Boolean> removeStatisticAsync(
        @NotNull UUID uniqueId,
        @NotNull String identifier,
        @NotNull String plugin
    );

    CompletableFuture<Boolean> removeStatisticAsync(
        @NotNull OfflinePlayer offlinePlayer,
        @NotNull String identifier,
        @NotNull String plugin
    );

    CompletableFuture<Boolean> addOrReplaceStatisticAsync(
        @NotNull UUID uniqueId,
        @NotNull RAbstractStatistic statistic
    );

    CompletableFuture<Boolean> addOrReplaceStatisticAsync(
        @NotNull OfflinePlayer offlinePlayer,
        @NotNull RAbstractStatistic statistic
    );

    CompletableFuture<Long> getStatisticCountForPluginAsync(@NotNull UUID uniqueId, @NotNull String plugin);
    CompletableFuture<Long> getStatisticCountForPluginAsync(@NotNull OfflinePlayer offlinePlayer, @NotNull String plugin);

    @NotNull String getApiVersion();
}