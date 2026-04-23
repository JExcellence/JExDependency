package de.jexcellence.core.service;

import de.jexcellence.core.api.event.StatisticChangeEvent;
import de.jexcellence.core.database.entity.CentralServer;
import de.jexcellence.core.database.entity.CorePlayer;
import de.jexcellence.core.database.entity.statistic.AbstractStatistic;
import de.jexcellence.core.database.entity.statistic.BooleanStatistic;
import de.jexcellence.core.database.entity.statistic.DateStatistic;
import de.jexcellence.core.database.entity.statistic.NumberStatistic;
import de.jexcellence.core.database.entity.statistic.PlayerStatistic;
import de.jexcellence.core.database.entity.statistic.StringStatistic;
import de.jexcellence.core.database.repository.PlayerStatisticRepository;
import de.jexcellence.core.database.repository.StatisticRepository;
import de.jexcellence.jexplatform.logging.JExLogger;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Service facade over {@link PlayerStatisticRepository} + {@link StatisticRepository}.
 * Exposes typed put/get helpers for the four statistic subclasses.
 */
public class StatisticService {

    private final PlayerStatisticRepository aggregateRepo;
    private final StatisticRepository rowRepo;
    private final JExLogger logger;

    public StatisticService(
            @NotNull PlayerStatisticRepository aggregateRepo,
            @NotNull StatisticRepository rowRepo,
            @NotNull JExLogger logger
    ) {
        this.aggregateRepo = aggregateRepo;
        this.rowRepo = rowRepo;
        this.logger = logger;
    }

    public @NotNull CompletableFuture<PlayerStatistic> aggregate(@NotNull CorePlayer player, @NotNull CentralServer server) {
        return this.aggregateRepo.findOrCreateByPlayerAndServerAsync(player, server).exceptionally(ex -> {
            this.logger.error("aggregate fetch failed: {}", ex.getMessage());
            return null;
        });
    }

    public @NotNull CompletableFuture<Void> putNumber(@NotNull PlayerStatistic agg, @NotNull String identifier, @NotNull String plugin, double value) {
        return put(agg, new NumberStatistic(identifier, plugin, value));
    }

    public @NotNull CompletableFuture<Void> putBoolean(@NotNull PlayerStatistic agg, @NotNull String identifier, @NotNull String plugin, boolean value) {
        return put(agg, new BooleanStatistic(identifier, plugin, value));
    }

    public @NotNull CompletableFuture<Void> putString(@NotNull PlayerStatistic agg, @NotNull String identifier, @NotNull String plugin, @NotNull String value) {
        return put(agg, new StringStatistic(identifier, plugin, value));
    }

    public @NotNull CompletableFuture<Void> putDate(@NotNull PlayerStatistic agg, @NotNull String identifier, @NotNull String plugin, long epochMillis) {
        return put(agg, new DateStatistic(identifier, plugin, epochMillis));
    }

    public @NotNull Optional<Object> find(@NotNull PlayerStatistic agg, @NotNull String identifier, @NotNull String plugin) {
        for (final AbstractStatistic row : agg.getStatistics()) {
            if (row.matches(identifier, plugin)) return Optional.of(row.getValue());
        }
        return Optional.empty();
    }

    private @NotNull CompletableFuture<Void> put(@NotNull PlayerStatistic agg, @NotNull AbstractStatistic row) {
        row.setPlayerStatistic(agg);
        agg.getStatistics().removeIf(existing -> existing.getIdentifier().equals(row.getIdentifier()));
        agg.getStatistics().add(row);
        return CompletableFuture.runAsync(() -> {
            this.aggregateRepo.update(agg);
            Bukkit.getPluginManager().callEvent(new StatisticChangeEvent(
                    row.getPlugin(),
                    row.getIdentifier(),
                    agg.getPlayer() != null ? agg.getPlayer().getUniqueId() : null,
                    row.getValue()
            ));
        }).exceptionally(ex -> {
            this.logger.error("put statistic failed: {}", ex.getMessage());
            return null;
        });
    }
}
