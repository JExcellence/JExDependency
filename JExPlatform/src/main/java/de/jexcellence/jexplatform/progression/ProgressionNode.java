package de.jexcellence.jexplatform.progression;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Interface for any entity that participates in a sequential progression system.
 *
 * <p>Implementations include quests, ranks, achievements, story chapters, and any
 * feature requiring sequential progression with prerequisites. Supports linear
 * chains, branching paths, and multi-prerequisite convergence.
 *
 * <p>The self-referential type parameter enables type-safe operations in
 * {@link ProgressionValidator} and {@link CompletionTracker}.
 *
 * @param <T> the concrete type implementing this interface
 * @author JExcellence
 * @since 1.0.0
 */
public interface ProgressionNode<T extends ProgressionNode<T>> {

    /**
     * Returns the unique identifier for this progression node.
     *
     * @return the unique identifier (never null or empty)
     */
    @NotNull String identifier();

    /**
     * Returns identifiers of nodes that must be completed before this node
     * becomes available.
     *
     * <p>All listed nodes must be completed (AND logic). Return an empty
     * list for initial nodes with no prerequisites.
     *
     * @return prerequisite node identifiers (never null, may be empty)
     */
    @NotNull List<String> previousNodeIdentifiers();

    /**
     * Returns identifiers of nodes that depend on this node as a prerequisite.
     *
     * <p>When this node is completed, the progression system checks these
     * dependents for automatic unlocking. Return an empty list for final nodes.
     *
     * @return dependent node identifiers (never null, may be empty)
     */
    @NotNull List<String> nextNodeIdentifiers();

    /**
     * Checks whether this is an initial node (no prerequisites).
     *
     * @return {@code true} if this node has no prerequisites
     */
    default boolean isInitialNode() {
        return previousNodeIdentifiers().isEmpty();
    }

    /**
     * Checks whether this is a final node (no dependents).
     *
     * @return {@code true} if this node has no dependent nodes
     */
    default boolean isFinalNode() {
        return nextNodeIdentifiers().isEmpty();
    }

    /**
     * Checks whether this node has any prerequisites.
     *
     * @return {@code true} if prerequisites exist
     */
    default boolean hasPrerequisites() {
        return !previousNodeIdentifiers().isEmpty();
    }
}
