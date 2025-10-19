package com.raindropcentral.rdq.api.spi;

import com.raindropcentral.rdq.database.entity.bounty.RBounty;

import java.util.concurrent.CompletableFuture;

/**
 * Defines the persistence contract for {@link RBounty} entities so RDQ editions can
 * supply storage-specific adapters.
 *
 * <p>Implementations are constructed during the repository wiring phase and are
 * invoked on RDQ's asynchronous executors. Providers must therefore return futures
 * that represent work already scheduled on a non-main thread. Complete the futures
 * exceptionally to signal persistence failures—RDQ will react by surfacing the
 * error to calling services and may retry based on edition policy.</p>
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
public interface BountyPersistence {
    /**
     * Persists a new bounty record using the underlying storage engine.
     *
     * @param bounty the bounty domain object to persist; contains the issuer, target, and
     *               reward metadata prepared by RDQ prior to persistence
     * @return a future that completes with the stored bounty instance, including any generated
     *         identifiers or storage-assigned defaults
     */
    CompletableFuture<RBounty> createAsync(RBounty bounty);

    /**
     * Deletes a bounty by its identifier.
     *
     * <p>Implementations must treat the operation as idempotent: removing a non-existent bounty
     * should complete the future successfully rather than fail the execution.</p>
     *
     * @param bountyId the unique identifier that RDQ assigned to the bounty during creation
     * @return a future that completes when the deletion has been persisted or is otherwise
     *         acknowledged as complete by the backing store
     */
    CompletableFuture<Void> deleteByIdAsync(long bountyId);

    /**
     * Convenience method delegating to {@link #deleteByIdAsync(long)} using the entity reference
     * already loaded within RDQ.
     *
     * @param bounty the bounty entity whose identifier should be deleted
     * @return the future returned from {@link #deleteByIdAsync(long)}
     */
    default CompletableFuture<Void> deleteAsync(RBounty bounty) {
        return deleteByIdAsync(bounty.getId());
    }
}