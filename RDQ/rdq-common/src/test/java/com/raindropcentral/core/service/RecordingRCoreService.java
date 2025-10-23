package com.raindropcentral.core.service;

import com.raindropcentral.rcore.database.entity.RPlayer;
import com.raindropcentral.rcore.database.entity.statistic.RAbstractStatistic;
import com.raindropcentral.rcore.database.entity.statistic.RPlayerStatistic;
import org.bukkit.OfflinePlayer;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Recording implementation that exposes deterministic responses for verification in tests.
 */
public class RecordingRCoreService implements RCoreService {

    private final Map<MethodKey, CompletableFuture<?>> responses = new EnumMap<>(MethodKey.class);
    private final Map<MethodKey, Object[]> calls = new EnumMap<>(MethodKey.class);
    private String apiVersion = "stub";

    public enum MethodKey {
        FIND_PLAYER_UUID,
        FIND_PLAYER_OFFLINE,
        FIND_PLAYER_BY_NAME,
        PLAYER_EXISTS_UUID,
        PLAYER_EXISTS_OFFLINE,
        CREATE_PLAYER,
        UPDATE_PLAYER,
        FIND_STATISTICS_UUID,
        FIND_STATISTICS_OFFLINE,
        FIND_STAT_VALUE_UUID,
        FIND_STAT_VALUE_OFFLINE,
        HAS_STAT_UUID,
        HAS_STAT_OFFLINE,
        REMOVE_STAT_UUID,
        REMOVE_STAT_OFFLINE,
        ADD_OR_REPLACE_UUID,
        ADD_OR_REPLACE_OFFLINE,
        STAT_COUNT_UUID,
        STAT_COUNT_OFFLINE
    }

    public void setResponse(MethodKey key, CompletableFuture<?> future) {
        responses.put(key, future);
    }

    public Object[] getLastArgs(MethodKey key) {
        return calls.get(key);
    }

    public void setApiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
    }

    @SuppressWarnings("unchecked")
    private <T> CompletableFuture<T> respond(MethodKey key, Object... args) {
        calls.put(key, args);
        CompletableFuture<?> future = responses.get(key);
        if (future == null) {
            throw new IllegalStateException("No response configured for " + key);
        }
        return (CompletableFuture<T>) future;
    }

    @Override
    public CompletableFuture<Optional<RPlayer>> findPlayerAsync(UUID uniqueId) {
        return respond(MethodKey.FIND_PLAYER_UUID, uniqueId);
    }

    @Override
    public CompletableFuture<Optional<RPlayer>> findPlayerAsync(OfflinePlayer offlinePlayer) {
        return respond(MethodKey.FIND_PLAYER_OFFLINE, offlinePlayer);
    }

    @Override
    public CompletableFuture<Optional<RPlayer>> findPlayerByNameAsync(String playerName) {
        return respond(MethodKey.FIND_PLAYER_BY_NAME, playerName);
    }

    @Override
    public CompletableFuture<Boolean> playerExistsAsync(UUID uniqueId) {
        return respond(MethodKey.PLAYER_EXISTS_UUID, uniqueId);
    }

    @Override
    public CompletableFuture<Boolean> playerExistsAsync(OfflinePlayer offlinePlayer) {
        return respond(MethodKey.PLAYER_EXISTS_OFFLINE, offlinePlayer);
    }

    @Override
    public CompletableFuture<Optional<RPlayer>> createPlayerAsync(UUID uniqueId, String playerName) {
        return respond(MethodKey.CREATE_PLAYER, uniqueId, playerName);
    }

    @Override
    public CompletableFuture<Optional<RPlayer>> updatePlayerAsync(RPlayer player) {
        return respond(MethodKey.UPDATE_PLAYER, player);
    }

    @Override
    public CompletableFuture<Optional<RPlayerStatistic>> findPlayerStatisticsAsync(UUID uniqueId) {
        return respond(MethodKey.FIND_STATISTICS_UUID, uniqueId);
    }

    @Override
    public CompletableFuture<Optional<RPlayerStatistic>> findPlayerStatisticsAsync(OfflinePlayer offlinePlayer) {
        return respond(MethodKey.FIND_STATISTICS_OFFLINE, offlinePlayer);
    }

    @Override
    public CompletableFuture<Optional<Object>> findStatisticValueAsync(UUID uniqueId, String identifier, String plugin) {
        return respond(MethodKey.FIND_STAT_VALUE_UUID, uniqueId, identifier, plugin);
    }

    @Override
    public CompletableFuture<Optional<Object>> findStatisticValueAsync(OfflinePlayer offlinePlayer, String identifier, String plugin) {
        return respond(MethodKey.FIND_STAT_VALUE_OFFLINE, offlinePlayer, identifier, plugin);
    }

    @Override
    public CompletableFuture<Boolean> hasStatisticAsync(UUID uniqueId, String identifier, String plugin) {
        return respond(MethodKey.HAS_STAT_UUID, uniqueId, identifier, plugin);
    }

    @Override
    public CompletableFuture<Boolean> hasStatisticAsync(OfflinePlayer offlinePlayer, String identifier, String plugin) {
        return respond(MethodKey.HAS_STAT_OFFLINE, offlinePlayer, identifier, plugin);
    }

    @Override
    public CompletableFuture<Boolean> removeStatisticAsync(UUID uniqueId, String identifier, String plugin) {
        return respond(MethodKey.REMOVE_STAT_UUID, uniqueId, identifier, plugin);
    }

    @Override
    public CompletableFuture<Boolean> removeStatisticAsync(OfflinePlayer offlinePlayer, String identifier, String plugin) {
        return respond(MethodKey.REMOVE_STAT_OFFLINE, offlinePlayer, identifier, plugin);
    }

    @Override
    public CompletableFuture<Boolean> addOrReplaceStatisticAsync(UUID uniqueId, RAbstractStatistic statistic) {
        return respond(MethodKey.ADD_OR_REPLACE_UUID, uniqueId, statistic);
    }

    @Override
    public CompletableFuture<Boolean> addOrReplaceStatisticAsync(OfflinePlayer offlinePlayer, RAbstractStatistic statistic) {
        return respond(MethodKey.ADD_OR_REPLACE_OFFLINE, offlinePlayer, statistic);
    }

    @Override
    public CompletableFuture<Long> getStatisticCountForPluginAsync(UUID uniqueId, String plugin) {
        return respond(MethodKey.STAT_COUNT_UUID, uniqueId, plugin);
    }

    @Override
    public CompletableFuture<Long> getStatisticCountForPluginAsync(OfflinePlayer offlinePlayer, String plugin) {
        return respond(MethodKey.STAT_COUNT_OFFLINE, offlinePlayer, plugin);
    }

    @Override
    public String getApiVersion() {
        return apiVersion;
    }
}
