package com.raindropcentral.rdq.api.spi;

import com.raindropcentral.rdq.database.entity.player.RDQPlayer;

import java.util.concurrent.CompletableFuture;

/**
 * Captures the persistence responsibilities for {@link RDQPlayer} aggregates.
 *
 * <p>Adapters are created during RDQ bootstrap and receive updates from gameplay
 * services whenever player statistics or quest progress changes. Implementations
 * must offload IO work away from the Paper main thread and complete returned
 * futures to signal persistence success or failure.</p>
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
public interface PlayerPersistence {
    /**
     * Persists the provided player snapshot.
     *
     * @param player the player entity to synchronise with the backing data store; includes bounty
     *               statistics and metadata accumulated during gameplay sessions
     * @return a future that completes when the storage engine acknowledges the update; failures
     *         should surface via exceptional completion so RDQ can perform compensating actions
     */
    CompletableFuture<Void> updateAsync(RDQPlayer player);
}