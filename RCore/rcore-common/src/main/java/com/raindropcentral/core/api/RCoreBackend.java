package com.raindropcentral.core.api;

import com.raindropcentral.core.database.entity.player.RPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * Cross-module abstraction implemented by Free and Premium runtime variants to
 * decouple {@link RCoreAdapter} from storage or executor specifics.
 *
 * <p>Implementations must provide a dedicated {@link java.util.concurrent.ExecutorService}
 * to satisfy the threading guarantees promised in this package's overview.</p>
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
public interface RCoreBackend {

    /**
     * Supplies the executor used for every asynchronous stage composed by the
     * adapter, typically tied to the backend's persistence infrastructure.
     *
     * @return executor for adapter and backend asynchronous work
     */
    @NotNull ExecutorService getExecutor();

    /**
     * Asynchronously resolves a player using the provided unique identifier.
     *
     * @param uniqueId player UUID to search for
     * @return future containing the player when present
     * @throws NullPointerException if {@code uniqueId} is {@code null}
     */
    @NotNull CompletableFuture<Optional<RPlayer>> findByUuidAsync(@NotNull UUID uniqueId);

    /**
     * Asynchronously resolves a player by last known name.
     *
     * @param playerName case-insensitive player name
     * @return future containing the player when present
     * @throws NullPointerException if {@code playerName} is {@code null}
     */
    @NotNull CompletableFuture<Optional<RPlayer>> findByNameAsync(@NotNull String playerName);

    /**
     * Persists a new player entity and returns the saved instance.
     *
     * @param player player aggregate to create
     * @return future containing the persisted player entity
     * @throws NullPointerException if {@code player} is {@code null}
     */
    @NotNull CompletableFuture<RPlayer> createAsync(@NotNull RPlayer player);

    /**
     * Persists updates for an existing player and returns the refreshed entity.
     *
     * @param player player aggregate to update
     * @return future containing the updated player entity
     * @throws NullPointerException if {@code player} is {@code null}
     */
    @NotNull CompletableFuture<RPlayer> updateAsync(@NotNull RPlayer player);
}