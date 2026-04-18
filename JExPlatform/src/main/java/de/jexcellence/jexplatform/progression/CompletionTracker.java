package de.jexcellence.jexplatform.progression;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Interface for tracking completion status of progression nodes.
 *
 * <p>Implementations handle persistence and caching of completion data,
 * providing asynchronous operations for checking completion status, tracking
 * active nodes, and managing completion state.
 *
 * @param <T> the type of progression node being tracked
 * @author JExcellence
 * @since 1.0.0
 */
public interface CompletionTracker<T extends ProgressionNode<T>> {

    /**
     * Checks whether a player has completed a specific node.
     *
     * @param playerId       the player UUID
     * @param nodeIdentifier the node identifier to check
     * @return a future resolving to {@code true} if completed
     */
    @NotNull CompletableFuture<Boolean> hasCompleted(
            @NotNull UUID playerId, @NotNull String nodeIdentifier);

    /**
     * Checks whether a player has an active (in-progress) node.
     *
     * @param playerId       the player UUID
     * @param nodeIdentifier the node identifier to check
     * @return a future resolving to {@code true} if active
     */
    @NotNull CompletableFuture<Boolean> isActive(
            @NotNull UUID playerId, @NotNull String nodeIdentifier);

    /**
     * Returns all completed node identifiers for a player.
     *
     * @param playerId the player UUID
     * @return a future resolving to the list of completed node identifiers
     */
    @NotNull CompletableFuture<List<String>> getCompletedNodes(@NotNull UUID playerId);

    /**
     * Marks a node as completed for a player.
     *
     * @param playerId       the player UUID
     * @param nodeIdentifier the node identifier to mark as completed
     * @return a future completing when the operation finishes
     */
    @NotNull CompletableFuture<Void> markCompleted(
            @NotNull UUID playerId, @NotNull String nodeIdentifier);

    /**
     * Invalidates cached completion data for a player.
     *
     * @param playerId the player UUID
     */
    void invalidateCache(@NotNull UUID playerId);
}
