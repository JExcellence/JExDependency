package de.jexcellence.core.database.repository;

import de.jexcellence.core.database.entity.CentralServer;
import de.jexcellence.core.database.entity.CorePlayer;
import de.jexcellence.core.database.entity.statistic.PlayerStatistic;
import de.jexcellence.jehibernate.repository.base.AbstractCrudRepository;
import jakarta.persistence.EntityManagerFactory;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * Repository for {@link PlayerStatistic} aggregate rows.
 */
public class PlayerStatisticRepository extends AbstractCrudRepository<PlayerStatistic, Long> {

    public PlayerStatisticRepository(
            @NotNull ExecutorService executor,
            @NotNull EntityManagerFactory emf,
            @NotNull Class<PlayerStatistic> entityClass
    ) {
        super(executor, emf, entityClass);
    }

    public @NotNull CompletableFuture<List<PlayerStatistic>> findByPlayerAndServerAsync(
            @NotNull CorePlayer player, @NotNull CentralServer server) {
        return query()
                .and("player", player)
                .and("server", server)
                .listAsync();
    }

    public @NotNull CompletableFuture<List<PlayerStatistic>> findByPlayerAsync(@NotNull CorePlayer player) {
        return query().and("player", player).listAsync();
    }

    public @NotNull CompletableFuture<List<PlayerStatistic>> findByServerAsync(@NotNull CentralServer server) {
        return query().and("server", server).listAsync();
    }

    public @NotNull CompletableFuture<PlayerStatistic> findOrCreateByPlayerAndServerAsync(
            @NotNull CorePlayer player, @NotNull CentralServer server) {
        return findByPlayerAndServerAsync(player, server)
                .thenApply(rows -> {
                    if (!rows.isEmpty()) return rows.get(0);
                    final PlayerStatistic fresh = new PlayerStatistic(player);
                    fresh.setServer(server);
                    return create(fresh);
                });
    }
}
