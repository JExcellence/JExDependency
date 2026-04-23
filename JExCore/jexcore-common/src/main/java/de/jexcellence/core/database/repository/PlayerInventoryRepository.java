package de.jexcellence.core.database.repository;

import de.jexcellence.core.database.entity.CentralServer;
import de.jexcellence.core.database.entity.CorePlayer;
import de.jexcellence.core.database.entity.PlayerInventory;
import de.jexcellence.jehibernate.repository.base.AbstractCrudRepository;
import jakarta.persistence.EntityManagerFactory;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * Repository for {@link PlayerInventory} snapshots. Queries are always
 * player+server scoped to prevent cross-server contamination.
 */
public class PlayerInventoryRepository extends AbstractCrudRepository<PlayerInventory, Long> {

    public PlayerInventoryRepository(
            @NotNull ExecutorService executor,
            @NotNull EntityManagerFactory emf,
            @NotNull Class<PlayerInventory> entityClass
    ) {
        super(executor, emf, entityClass);
    }

    public @NotNull CompletableFuture<List<PlayerInventory>> findByPlayerAndServerAsync(
            @NotNull CorePlayer player, @NotNull CentralServer server) {
        return query()
                .and("player", player)
                .and("server", server)
                .listAsync();
    }

    public @NotNull CompletableFuture<Optional<PlayerInventory>> findLatestByPlayerAndServerAsync(
            @NotNull CorePlayer player, @NotNull CentralServer server) {
        return query()
                .and("player", player)
                .and("server", server)
                .orderByDesc("id")
                .firstAsync();
    }

    public @NotNull CompletableFuture<List<PlayerInventory>> findByPlayerAsync(@NotNull CorePlayer player) {
        return query().and("player", player).listAsync();
    }

    public @NotNull CompletableFuture<List<PlayerInventory>> findByServerAsync(@NotNull CentralServer server) {
        return query().and("server", server).listAsync();
    }

    public @NotNull CompletableFuture<Void> deleteByPlayerAndServerAsync(
            @NotNull CorePlayer player, @NotNull CentralServer server) {
        return findByPlayerAndServerAsync(player, server)
                .thenCompose(rows -> {
                    if (rows.isEmpty()) return CompletableFuture.completedFuture(null);
                    final CompletableFuture<?>[] futures = rows.stream()
                            .filter(row -> row.getId() != null)
                            .map(row -> deleteAsync(row.getId()))
                            .toArray(CompletableFuture[]::new);
                    return CompletableFuture.allOf(futures);
                });
    }
}
