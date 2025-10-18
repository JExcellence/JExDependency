package com.raindropcentral.core.api;

import com.raindropcentral.core.database.entity.player.RPlayer;
import com.raindropcentral.core.database.entity.statistic.RAbstractStatistic;
import com.raindropcentral.core.database.entity.statistic.RPlayerStatistic;
import com.raindropcentral.core.service.RCoreService;
import com.raindropcentral.rplatform.logging.CentralLogger;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.logging.Logger;

public class RCoreAdapter implements RCoreService {

    private static final Logger LOGGER = CentralLogger.getLogger(RCoreAdapter.class);

    private final RCoreBackend backend;
    private final Executor executor;

    public RCoreAdapter(final @NotNull RCoreBackend backend) {
        this.backend = Objects.requireNonNull(backend, "backend cannot be null");
        this.executor = backend.getExecutor();
        LOGGER.info("RCoreAdapter initialized successfully");
    }

    @Override
    public CompletableFuture<Optional<RPlayer>> findPlayerAsync(final @NotNull UUID uniqueId) {
        Objects.requireNonNull(uniqueId, "uniqueId cannot be null");
        return backend.findByUuidAsync(uniqueId)
                .whenComplete((res, ex) -> {
                    if (ex != null) {
                        LOGGER.warning("Failed to retrieve player with UUID: %s - %s"
                                .formatted(uniqueId, ex.getMessage()));
                    }
                });
    }

    @Override
    public CompletableFuture<Optional<RPlayer>> findPlayerAsync(final @NotNull OfflinePlayer offlinePlayer) {
        Objects.requireNonNull(offlinePlayer, "offlinePlayer cannot be null");
        return findPlayerAsync(offlinePlayer.getUniqueId());
    }

    @Override
    public CompletableFuture<Optional<RPlayer>> findPlayerByNameAsync(final @NotNull String playerName) {
        Objects.requireNonNull(playerName, "playerName cannot be null");
        return backend.findByNameAsync(playerName)
                .whenComplete((res, ex) -> {
                    if (ex != null) {
                        LOGGER.warning("Failed to retrieve player with name: %s - %s"
                                .formatted(playerName, ex.getMessage()));
                    }
                });
    }

    @Override
    public CompletableFuture<Boolean> playerExistsAsync(final @NotNull UUID uniqueId) {
        return findPlayerAsync(uniqueId)
                .thenApplyAsync(Optional::isPresent, executor);
    }

    @Override
    public CompletableFuture<Boolean> playerExistsAsync(final @NotNull OfflinePlayer offlinePlayer) {
        Objects.requireNonNull(offlinePlayer, "offlinePlayer cannot be null");
        return playerExistsAsync(offlinePlayer.getUniqueId());
    }

    @Override
    public CompletableFuture<Optional<RPlayer>> createPlayerAsync(
            final @NotNull UUID uniqueId,
            final @NotNull String playerName
    ) {
        Objects.requireNonNull(uniqueId, "uniqueId cannot be null");
        Objects.requireNonNull(playerName, "playerName cannot be null");

        return playerExistsAsync(uniqueId)
                .thenComposeAsync(exists -> {
                    if (exists) {
                        LOGGER.warning("Attempted to create player that already exists: %s".formatted(uniqueId));
                        return CompletableFuture.completedFuture(Optional.<RPlayer>empty());
                    }

                    final RPlayer newPlayer = new RPlayer(uniqueId, playerName);
                    final RPlayerStatistic statistics = new RPlayerStatistic(newPlayer);
                    newPlayer.setPlayerStatistic(statistics);

                    return backend.createAsync(newPlayer)
                            .thenApply(Optional::of);
                }, executor)
                .whenComplete((res, ex) -> {
                    if (ex != null) {
                        LOGGER.severe("Failed to create player: %s - %s"
                                .formatted(uniqueId, ex.getMessage()));
                    }
                });
    }

    @Override
    public CompletableFuture<Optional<RPlayer>> updatePlayerAsync(final @NotNull RPlayer player) {
        Objects.requireNonNull(player, "player cannot be null");

        return backend.updateAsync(player)
                .thenApply(Optional::of)
                .whenComplete((res, ex) -> {
                    if (ex != null) {
                        LOGGER.severe("Failed to update player: %s - %s"
                                .formatted(player.getUniqueId(), ex.getMessage()));
                    }
                });
    }

    @Override
    public CompletableFuture<Optional<RPlayerStatistic>> findPlayerStatisticsAsync(final @NotNull UUID uniqueId) {
        Objects.requireNonNull(uniqueId, "uniqueId cannot be null");
        return findPlayerAsync(uniqueId)
                .thenApplyAsync(playerOpt -> playerOpt
                        .map(RPlayer::getPlayerStatistic)
                        .filter(Objects::nonNull), executor);
    }

    @Override
    public CompletableFuture<Optional<RPlayerStatistic>> findPlayerStatisticsAsync(
            final @NotNull OfflinePlayer offlinePlayer
    ) {
        Objects.requireNonNull(offlinePlayer, "offlinePlayer cannot be null");
        return findPlayerStatisticsAsync(offlinePlayer.getUniqueId());
    }

    @Override
    public CompletableFuture<Optional<Object>> findStatisticValueAsync(
            final @NotNull UUID uniqueId,
            final @NotNull String identifier,
            final @NotNull String plugin
    ) {
        Objects.requireNonNull(identifier, "identifier cannot be null");
        Objects.requireNonNull(plugin, "plugin cannot be null");

        return findPlayerStatisticsAsync(uniqueId)
                .thenApplyAsync(statsOpt -> statsOpt
                        .flatMap(stats -> stats.getStatisticValue(identifier, plugin)), executor);
    }

    @Override
    public CompletableFuture<Optional<Object>> findStatisticValueAsync(
            final @NotNull OfflinePlayer offlinePlayer,
            final @NotNull String identifier,
            final @NotNull String plugin
    ) {
        Objects.requireNonNull(offlinePlayer, "offlinePlayer cannot be null");
        return findStatisticValueAsync(offlinePlayer.getUniqueId(), identifier, plugin);
    }

    @Override
    public CompletableFuture<Boolean> hasStatisticAsync(
            final @NotNull UUID uniqueId,
            final @NotNull String identifier,
            final @NotNull String plugin
    ) {
        Objects.requireNonNull(identifier, "identifier cannot be null");
        Objects.requireNonNull(plugin, "plugin cannot be null");

        return findPlayerStatisticsAsync(uniqueId)
                .thenApplyAsync(statsOpt -> statsOpt
                        .map(stats -> stats.hasStatistic(identifier, plugin))
                        .orElse(false), executor);
    }

    @Override
    public CompletableFuture<Boolean> hasStatisticAsync(
            final @NotNull OfflinePlayer offlinePlayer,
            final @NotNull String identifier,
            final @NotNull String plugin
    ) {
        Objects.requireNonNull(offlinePlayer, "offlinePlayer cannot be null");
        return hasStatisticAsync(offlinePlayer.getUniqueId(), identifier, plugin);
    }

    @Override
    public CompletableFuture<Boolean> removeStatisticAsync(
            final @NotNull UUID uniqueId,
            final @NotNull String identifier,
            final @NotNull String plugin
    ) {
        Objects.requireNonNull(uniqueId, "uniqueId cannot be null");
        Objects.requireNonNull(identifier, "identifier cannot be null");
        Objects.requireNonNull(plugin, "plugin cannot be null");

        return findPlayerAsync(uniqueId)
                .thenComposeAsync(playerOpt -> {
                    if (playerOpt.isEmpty()) {
                        return CompletableFuture.completedFuture(false);
                    }

                    final RPlayer player = playerOpt.get();
                    final RPlayerStatistic stats = player.getPlayerStatistic();

                    if (stats == null || !stats.removeStatistic(identifier, plugin)) {
                        return CompletableFuture.completedFuture(false);
                    }

                    return backend.updateAsync(player).thenApply(p -> true);
                }, executor)
                .whenComplete((res, ex) -> {
                    if (ex != null) {
                        LOGGER.severe("Failed to persist statistic removal for player: %s - %s"
                                .formatted(uniqueId, ex.getMessage()));
                    }
                });
    }

    @Override
    public CompletableFuture<Boolean> removeStatisticAsync(
            final @NotNull OfflinePlayer offlinePlayer,
            final @NotNull String identifier,
            final @NotNull String plugin
    ) {
        Objects.requireNonNull(offlinePlayer, "offlinePlayer cannot be null");
        return removeStatisticAsync(offlinePlayer.getUniqueId(), identifier, plugin);
    }

    @Override
    public CompletableFuture<Boolean> addOrReplaceStatisticAsync(
            final @NotNull UUID uniqueId,
            final @NotNull RAbstractStatistic statistic
    ) {
        Objects.requireNonNull(uniqueId, "uniqueId cannot be null");
        Objects.requireNonNull(statistic, "statistic cannot be null");

        return findPlayerAsync(uniqueId)
                .thenComposeAsync(playerOpt -> {
                    if (playerOpt.isEmpty()) {
                        LOGGER.warning("Attempted to addOrReplaceStatistic for non-existent player: %s"
                                .formatted(uniqueId));
                        return CompletableFuture.completedFuture(false);
                    }

                    final RPlayer player = playerOpt.get();
                    RPlayerStatistic stats = player.getPlayerStatistic();

                    if (stats == null) {
                        stats = new RPlayerStatistic(player);
                        player.setPlayerStatistic(stats);
                    }

                    stats.addOrReplaceStatistic(statistic);

                    return backend.updateAsync(player).thenApply(p -> true);
                }, executor)
                .whenComplete((res, ex) -> {
                    if (ex != null) {
                        LOGGER.severe("Failed to persist addOrReplaceStatistic for player: %s - %s"
                                .formatted(uniqueId, ex.getMessage()));
                    }
                });
    }

    @Override
    public CompletableFuture<Boolean> addOrReplaceStatisticAsync(
            final @NotNull OfflinePlayer offlinePlayer,
            final @NotNull RAbstractStatistic statistic
    ) {
        Objects.requireNonNull(offlinePlayer, "offlinePlayer cannot be null");
        return addOrReplaceStatisticAsync(offlinePlayer.getUniqueId(), statistic);
    }

    @Override
    public CompletableFuture<Long> getStatisticCountForPluginAsync(
            final @NotNull UUID uniqueId,
            final @NotNull String plugin
    ) {
        Objects.requireNonNull(uniqueId, "uniqueId cannot be null");
        Objects.requireNonNull(plugin, "plugin cannot be null");

        return findPlayerStatisticsAsync(uniqueId)
                .thenApplyAsync(statsOpt -> statsOpt
                        .map(stats -> stats.getStatisticCountForPlugin(plugin))
                        .orElse(0L), executor);
    }

    @Override
    public CompletableFuture<Long> getStatisticCountForPluginAsync(
            final @NotNull OfflinePlayer offlinePlayer,
            final @NotNull String plugin
    ) {
        Objects.requireNonNull(offlinePlayer, "offlinePlayer cannot be null");
        return getStatisticCountForPluginAsync(offlinePlayer.getUniqueId(), plugin);
    }

    @Override
    public @NotNull String getApiVersion() {
        return "2.0.0";
    }
}