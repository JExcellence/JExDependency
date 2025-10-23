package com.raindropcentral.core.service;

import com.raindropcentral.rcore.database.entity.RPlayer;
import com.raindropcentral.rcore.database.entity.statistic.RAbstractStatistic;
import com.raindropcentral.rcore.database.entity.statistic.RPlayerStatistic;
import org.bukkit.OfflinePlayer;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Minimal representation of the upstream RCore service API exposed for tests.
 */
public interface RCoreService {

    CompletableFuture<Optional<RPlayer>> findPlayerAsync(UUID uniqueId);

    CompletableFuture<Optional<RPlayer>> findPlayerAsync(OfflinePlayer offlinePlayer);

    CompletableFuture<Optional<RPlayer>> findPlayerByNameAsync(String playerName);

    CompletableFuture<Boolean> playerExistsAsync(UUID uniqueId);

    CompletableFuture<Boolean> playerExistsAsync(OfflinePlayer offlinePlayer);

    CompletableFuture<Optional<RPlayer>> createPlayerAsync(UUID uniqueId, String playerName);

    CompletableFuture<Optional<RPlayer>> updatePlayerAsync(RPlayer player);

    CompletableFuture<Optional<RPlayerStatistic>> findPlayerStatisticsAsync(UUID uniqueId);

    CompletableFuture<Optional<RPlayerStatistic>> findPlayerStatisticsAsync(OfflinePlayer offlinePlayer);

    CompletableFuture<Optional<Object>> findStatisticValueAsync(UUID uniqueId, String identifier, String plugin);

    CompletableFuture<Optional<Object>> findStatisticValueAsync(OfflinePlayer offlinePlayer, String identifier, String plugin);

    CompletableFuture<Boolean> hasStatisticAsync(UUID uniqueId, String identifier, String plugin);

    CompletableFuture<Boolean> hasStatisticAsync(OfflinePlayer offlinePlayer, String identifier, String plugin);

    CompletableFuture<Boolean> removeStatisticAsync(UUID uniqueId, String identifier, String plugin);

    CompletableFuture<Boolean> removeStatisticAsync(OfflinePlayer offlinePlayer, String identifier, String plugin);

    CompletableFuture<Boolean> addOrReplaceStatisticAsync(UUID uniqueId, RAbstractStatistic statistic);

    CompletableFuture<Boolean> addOrReplaceStatisticAsync(OfflinePlayer offlinePlayer, RAbstractStatistic statistic);

    CompletableFuture<Long> getStatisticCountForPluginAsync(UUID uniqueId, String plugin);

    CompletableFuture<Long> getStatisticCountForPluginAsync(OfflinePlayer offlinePlayer, String plugin);

    String getApiVersion();
}
