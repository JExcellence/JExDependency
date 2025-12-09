package com.raindropcentral.core.service;

import com.raindropcentral.core.database.entity.player.RPlayer;
import com.raindropcentral.core.database.repository.RPlayerRepository;
import de.jexcellence.hibernate.repository.InjectRepository;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Service class for managing player entities with comprehensive CRUD operations.
 * <p>
 * This service provides high-level operations for player management, abstracting repository
 * access and providing business logic for player-related operations. All operations are
 * asynchronous and return {@link CompletableFuture} to avoid blocking the main thread.
 * </p>
 * <p>
 * The service automatically injects the {@link RPlayerRepository} via the
 * {@link de.jexcellence.hibernate.repository.RepositoryManager} when instantiated through
 * {@code createInstance()}.
 * </p>
 *
 * @author JExcellence
 * @since 2.0.0
 * @version 2.0.0
 */
@InjectRepository
public class RPlayerService {

    @InjectRepository
    private RPlayerRepository playerRepository;

    /**
     * Constructs a new RPlayerService.
     * <p>
     * The repository will be automatically injected by the RepositoryManager when this service
     * is created via {@code RepositoryManager.getInstance().createInstance(RPlayerService.class)}.
     * </p>
     */
    public RPlayerService() {
        // Repository injected by RepositoryManager
    }

    /**
     * Finds a player by their UUID.
     *
     * @param uuid the player's unique identifier
     * @return future containing an optional with the player if found
     */
    public CompletableFuture<Optional<RPlayer>> findByUuid(final @NotNull UUID uuid) {
        return playerRepository.findByUuidAsync(uuid);
    }

    /**
     * Finds a player by their username.
     *
     * @param playerName the player's username
     * @return future containing an optional with the player if found
     */
    public CompletableFuture<Optional<RPlayer>> findByName(final @NotNull String playerName) {
        return playerRepository.findByNameAsync(playerName);
    }

    /**
     * Checks if a player exists by their UUID.
     *
     * @param uuid the player's unique identifier
     * @return future containing true if the player exists
     */
    public CompletableFuture<Boolean> existsByUuid(final @NotNull UUID uuid) {
        return playerRepository.existsByUuidAsync(uuid);
    }

    /**
     * Creates or updates a player entity.
     * <p>
     * If a player with the same UUID already exists, it will be updated.
     * Otherwise, a new player entity will be created.
     * </p>
     *
     * @param player the player entity to save
     * @return future containing the saved player entity
     */
    public CompletableFuture<RPlayer> createOrUpdate(final @NotNull RPlayer player) {
        return playerRepository.createOrUpdateAsync(player);
    }

    /**
     * Creates or updates a player from a Bukkit player instance.
     * <p>
     * This is a convenience method that handles the common case of saving a player
     * when they join the server. It will create a new RPlayer if one doesn't exist,
     * or update the existing one with current information.
     * </p>
     *
     * @param bukkitPlayer the Bukkit player instance
     * @return future containing the saved player entity
     */
    public CompletableFuture<RPlayer> createOrUpdateFromBukkit(final @NotNull Player bukkitPlayer) {
        return findByUuid(bukkitPlayer.getUniqueId())
                .thenCompose(existingPlayer -> {
                    final RPlayer rPlayer = existingPlayer.orElseGet(() -> new RPlayer(bukkitPlayer));
                    rPlayer.updatePlayerName(bukkitPlayer.getName());
                    return createOrUpdate(rPlayer);
                });
    }

    /**
     * Updates the last seen timestamp for a player.
     * <p>
     * This is typically called when a player leaves the server.
     * </p>
     *
     * @param uuid the player's unique identifier
     * @return future that completes when the update is finished
     */
    public CompletableFuture<Object> updateLastSeen(final @NotNull UUID uuid) {
        return findByUuid(uuid)
                .thenCompose(playerOpt -> playerOpt
                        .map(player -> {
                            player.updateLastSeen();
                            return createOrUpdate(player).thenApply(p -> null);
                        })
                        .orElseGet(() -> CompletableFuture.completedFuture(null))
                );
    }

    /**
     * Deletes a player by their UUID.
     *
     * @param uuid the player's unique identifier
     * @return future that completes when the deletion is finished
     */
    public CompletableFuture<Boolean> deleteByUuid(final @NotNull UUID uuid) {
        return findByUuid(uuid)
                .thenCompose(playerOpt -> playerOpt
                        .map(player -> {
                            if (player.getId() == null) {
                                return CompletableFuture.completedFuture(false);
                            }
                            return playerRepository.deleteAsync(player.getId());
                        })
                        .orElseGet(() -> CompletableFuture.completedFuture(false))
                );
    }

    /**
     * Gets the injected repository instance.
     * <p>
     * This is primarily for testing purposes or advanced use cases.
     * </p>
     *
     * @return the player repository
     */
    public RPlayerRepository getRepository() {
        return playerRepository;
    }
}
