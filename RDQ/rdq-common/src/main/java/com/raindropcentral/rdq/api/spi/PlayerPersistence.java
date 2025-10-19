package com.raindropcentral.rdq.api.spi;

import com.raindropcentral.rdq.database.entity.player.RDQPlayer;

import java.util.concurrent.CompletableFuture;

/**
 * Captures the persistence responsibilities for {@link RDQPlayer} aggregates.
 *
 * <p><strong>Threading:</strong> Adapters are created during RDQ bootstrap and invoked on the
 * module's asynchronous executors. Implementations must ensure the returned futures represent work
 * that does not block the main thread.</p>
 *
 * <p><strong>Lifecycle:</strong> Instances remain active until plugin disable and should be capable
 * of handling repeated updates for the same player as snapshots evolve.</p>
 *
 * <p><strong>Integration:</strong> Downstream services rely on consistent persistence semantics to
 * keep quest and bounty progress synchronized across editions.</p>
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
public interface PlayerPersistence {
    /**
     * Persists the provided player snapshot.
     *
     * <p><strong>Error handling:</strong> Complete the future exceptionally when the storage layer
     * rejects the update so RDQ can perform compensating actions such as retries or user prompts.</p>
     *
     * <p><strong>Transactions:</strong> Apply updates within a single transaction when possible so
     * quest state and metadata remain consistent.</p>
     *
     * <p><strong>Extension guidance:</strong> When extending {@link RDQPlayer}, ensure new fields are
     * persisted atomically to avoid partial updates.</p>
     *
     * @param player the player entity to synchronise with the backing data store; includes bounty
     *               statistics and metadata accumulated during gameplay sessions
     * @return a future that completes when the storage engine acknowledges the update; failures
     *         should surface via exceptional completion so RDQ can perform compensating actions
     */
    CompletableFuture<Void> updateAsync(RDQPlayer player);
}