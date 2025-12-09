package com.raindropcentral.core.database.repository;

import com.raindropcentral.core.database.entity.central.RCentralServer;
import com.raindropcentral.core.database.entity.inventory.RPlayerInventory;
import com.raindropcentral.core.database.entity.player.RPlayer;
import de.jexcellence.hibernate.repository.GenericCachedRepository;
import jakarta.persistence.EntityManagerFactory;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;

/**
 * Repository for managing RPlayerInventory entities.
 * <p>
 * Handles persistence and retrieval of player inventory snapshots with server-scoped filtering.
 * All inventory operations should filter by both player and server to ensure proper isolation
 * between different servers.
 * </p>
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.0
 */
public class RPlayerInventoryRepository extends GenericCachedRepository<RPlayerInventory, Long, Long> {

    /**
     * Constructs a new RPlayerInventoryRepository.
     *
     * @param executor             executor service for async operations
     * @param entityManagerFactory entity manager factory for database access
     * @throws NullPointerException if any parameter is null
     */
    public RPlayerInventoryRepository(
            final @NotNull ExecutorService executor,
            final @NotNull EntityManagerFactory entityManagerFactory,
            @NotNull Class<RPlayerInventory> entityClass,
            @NotNull Function<RPlayerInventory, Long> keyExtractor
    ) {
        super(executor, entityManagerFactory, entityClass, keyExtractor);
    }

    /**
     * Finds all inventory snapshots for a specific player and server.
     * <p>
     * Returns only inventories that match both the player and server criteria,
     * ensuring proper isolation between different servers.
     * </p>
     *
     * @param player the player to find inventories for
     * @param server the server to filter by
     * @return CompletableFuture containing list of matching inventory snapshots
     * @throws NullPointerException if any parameter is null
     */
    public CompletableFuture<List<RPlayerInventory>> findByPlayerAndServer(
            final @NotNull RPlayer player,
            final @NotNull RCentralServer server
    ) {
        return findListByAttributesAsync(Map.of(
                "rPlayer", player,
                "rCentralServer", server
        ));
    }

    /**
     * Finds the most recent inventory snapshot for a player on a specific server.
     * <p>
     * Useful for restoring a player's inventory when they join a server.
     * </p>
     *
     * @param player the player to find inventory for
     * @param server the server to filter by
     * @return CompletableFuture containing the most recent inventory, or null if none exists
     * @throws NullPointerException if any parameter is null
     */
    public CompletableFuture<RPlayerInventory> findLatestByPlayerAndServer(
            final @NotNull RPlayer player,
            final @NotNull RCentralServer server
    ) {
        return findByAttributesAsync(Map.of(
                "rPlayer", player,
                "rCentralServer", server
        ));
    }

    /**
     * Finds all inventory snapshots for a specific player across all servers.
     * <p>
     * Useful for administrative purposes or cross-server inventory management.
     * </p>
     *
     * @param player the player to find inventories for
     * @return CompletableFuture containing list of all inventory snapshots for the player
     * @throws NullPointerException if player is null
     */
    public CompletableFuture<List<RPlayerInventory>> findByPlayer(final @NotNull RPlayer player) {
        return findListByAttributesAsync(Map.of("rPlayer", player));
    }

    /**
     * Finds all inventory snapshots for a specific server.
     * <p>
     * Useful for server-wide inventory management or backups.
     * </p>
     *
     * @param server the server to find inventories for
     * @return CompletableFuture containing list of all inventory snapshots for the server
     * @throws NullPointerException if server is null
     */
    public CompletableFuture<List<RPlayerInventory>> findByServer(final @NotNull RCentralServer server) {
        return findListByAttributesAsync(Map.of("rCentralServer", server));
    }

    /**
     * Deletes all inventory snapshots for a specific player and server.
     * <p>
     * Useful for clearing a player's inventory data on a specific server.
     * </p>
     *
     * @param player the player whose inventories should be deleted
     * @param server the server to filter by
     * @return CompletableFuture that completes when deletion is finished
     * @throws NullPointerException if any parameter is null
     */
    public CompletableFuture<Void> deleteByPlayerAndServer(
            final @NotNull RPlayer player,
            final @NotNull RCentralServer server
    ) {
        return findByPlayerAndServer(player, server)
                .thenCompose(inventories -> {
                    final CompletableFuture<?>[] deleteFutures = inventories.stream()
                            .filter(Objects::nonNull)
                            .map(rPlayerInventory -> {
                                if (rPlayerInventory.getId() == null)
                                    return CompletableFuture.completedFuture(null);
                                return this.deleteAsync(rPlayerInventory.getId());
                            })
                            .toArray(CompletableFuture[]::new);
                    return CompletableFuture.allOf(deleteFutures);
                });
    }
}
