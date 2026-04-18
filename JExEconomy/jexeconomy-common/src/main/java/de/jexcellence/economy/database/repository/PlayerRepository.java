package de.jexcellence.economy.database.repository;

import de.jexcellence.economy.database.entity.EconomyPlayer;
import de.jexcellence.jehibernate.repository.base.AbstractCrudRepository;
import jakarta.persistence.EntityManagerFactory;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * Repository for {@link EconomyPlayer} entities.
 *
 * @author JExcellence
 * @since 3.0.0
 */
public class PlayerRepository extends AbstractCrudRepository<EconomyPlayer, Long> {

    /**
     * Creates a player repository.
     *
     * @param executor    the executor for async operations
     * @param emf         the entity manager factory
     * @param entityClass the entity class
     */
    public PlayerRepository(@NotNull ExecutorService executor,
                            @NotNull EntityManagerFactory emf,
                            @NotNull Class<EconomyPlayer> entityClass) {
        super(executor, emf, entityClass);
    }

    /**
     * Finds a player by UUID.
     *
     * @param uuid the player's UUID
     * @return the player, or empty
     */
    public @NotNull Optional<EconomyPlayer> findByUuid(@NotNull UUID uuid) {
        return query().and("uniqueId", uuid).first();
    }

    /**
     * Finds a player by UUID asynchronously.
     *
     * @param uuid the player's UUID
     * @return future resolving to the player, or empty
     */
    public @NotNull CompletableFuture<Optional<EconomyPlayer>> findByUuidAsync(
            @NotNull UUID uuid) {
        return query().and("uniqueId", uuid).firstAsync();
    }

    /**
     * Finds or creates a player record.
     *
     * @param uuid       the player's UUID
     * @param playerName the player's current name
     * @return the existing or newly created player
     */
    public @NotNull EconomyPlayer findOrCreate(@NotNull UUID uuid,
                                               @NotNull String playerName) {
        var existing = findByUuid(uuid);
        if (existing.isPresent()) {
            var player = existing.get();
            if (!player.getPlayerName().equals(playerName)) {
                player.setPlayerName(playerName);
                return update(player);
            }
            return player;
        }
        return create(new EconomyPlayer(uuid, playerName));
    }

    /**
     * Finds or creates a player record asynchronously.
     *
     * @param uuid       the player's UUID
     * @param playerName the player's current name
     * @return future resolving to the existing or newly created player
     */
    public @NotNull CompletableFuture<EconomyPlayer> findOrCreateAsync(
            @NotNull UUID uuid,
            @NotNull String playerName) {
        return CompletableFuture.supplyAsync(() -> findOrCreate(uuid, playerName));
    }
}
