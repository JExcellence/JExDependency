package com.raindropcentral.rdq.api.spi;

import com.raindropcentral.rdq.database.entity.bounty.RBounty;

import java.util.concurrent.CompletableFuture;

/**
 * Defines the persistence contract for {@link RBounty} entities so RDQ editions can
 * supply storage-specific adapters.
 *
 * <p><strong>Threading:</strong> Implementations are constructed during the repository wiring phase
 * and invoked from RDQ's asynchronous executors. The returned futures must represent work that has
 * already been scheduled on an IO-safe thread so the Paper main thread never blocks. Avoid wrapping
 * blocking database calls directly inside the default executor thread and instead use dedicated
 * persistence pools.</p>
 *
 * <p><strong>Lifecycle:</strong> Adapters are resolved once during RDQ bootstrap and reused until
 * disable. Each method should treat invocations as short-lived transactional boundaries and avoid
 * caching mutable state. Shut down any per-adapter resources (connection pools or caches) during the
 * plugin disable sequence.</p>
 *
 * <p><strong>Integration:</strong> Services across RDQ and external editions rely on this SPI to
 * coordinate bounty storage. Ensure new fields added to {@link RBounty} are persisted consistently
 * across all editions and synchronise schema migrations with consumer expectations.</p>
 *
 * <p><strong>Validation:</strong> Callers perform preliminary validation, but implementations should
 * double-check invariant enforcement (such as issuer/target availability) before committing data to
 * long-term storage so corrupted entities never escape into the wider ecosystem.</p>
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
public interface BountyPersistence {
    /**
     * Persists a new bounty record using the underlying storage engine.
     *
     * <p><strong>Threading:</strong> Schedule persistence work on the adapter's IO executor before
     * completing the returned future so upstream callers never block RDQ's coordination threads.</p>
     *
     * <p><strong>Error handling:</strong> Complete the returned future exceptionally to surface
     * validation or storage errors—RDQ callers will log and abort the user action. Implementations
     * should not swallow database exceptions.</p>
     *
     * <p><strong>Transactions:</strong> Wrap persistence in a single transaction where supported so
     * the bounty and its relationships are committed atomically.</p>
     *
     * <p><strong>Extension guidance:</strong> When extending the entity schema, ensure the returned
     * {@link RBounty} reflects any generated identifiers or defaults.</p>
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
     * <p><strong>Threading:</strong> Submit the deletion operation to an asynchronous executor so
     * the calling thread remains non-blocking while the underlying storage work completes.</p>
     *
     * <p><strong>Error handling:</strong> Complete the returned future exceptionally when the
     * underlying storage reports non-recoverable failures. Not-found scenarios should resolve
     * normally so callers can treat the operation as idempotent.</p>
     *
     * <p><strong>Transactions:</strong> Execute within a transaction boundary where supported so
     * cascading deletions remain consistent.</p>
     *
     * <p><strong>Extension guidance:</strong> Custom implementations may emit audit logs before the
     * future completes but should avoid blocking the RDQ executors.</p>
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
     * <p><strong>Threading:</strong> This default implementation avoids additional asynchronous
     * wrapping and simply forwards to {@link #deleteByIdAsync(long)}, preserving the original
     * executor semantics of the concrete adapter.</p>
     *
     * <p><strong>Extension guidance:</strong> Override if additional context (such as tenant or shard
     * identifiers) must be derived from the entity before deletion.</p>
     *
     * @implSpec Calls {@link #deleteByIdAsync(long)} with {@link RBounty#getId()} and returns the
     * same future instance.
     *
     * @param bounty the bounty entity whose identifier should be deleted
     * @return the future returned from {@link #deleteByIdAsync(long)}
     */
    default CompletableFuture<Void> deleteAsync(RBounty bounty) {
        return deleteByIdAsync(bounty.getId());
    }
}