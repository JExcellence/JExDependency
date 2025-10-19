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
 * <p><strong>Validation:</strong> Callers enforce high-level invariants, but adapters are the final
 * line of defence before data is committed. Double-check tenant boundaries, profile identifiers, and
 * schema expectations before acknowledging an update so corrupted snapshots never propagate.</p>
 *
 * <p><strong>Usage:</strong> Editions typically resolve the adapter from
 * {@link PersistenceRegistry} and invoke it after composing updated {@link RDQPlayer} snapshots.
 * Implementations should remain stateless or confine caches to thread-safe structures to support
 * reuse across asynchronous executors.</p>
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
public interface PlayerPersistence {
    /**
     * Persists the provided player snapshot.
     *
     * <p><strong>Threading:</strong> Schedule storage work on an IO-friendly executor before
     * completing the returned future so RDQ's coordination threads remain responsive.</p>
     *
     * <p><strong>Error handling:</strong> Complete the future exceptionally when the storage layer
     * rejects the update so RDQ can perform compensating actions such as retries or user prompts.</p>
     *
     * <p><strong>Transactions:</strong> Apply updates within a single transaction when possible so
     * quest state and metadata remain consistent.</p>
     *
     * <p><strong>Validation:</strong> Confirm that the incoming {@link RDQPlayer} snapshot contains
     * the identifiers, edition markers, and serialised quest data required by the backing store
     * before attempting persistence.</p>
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