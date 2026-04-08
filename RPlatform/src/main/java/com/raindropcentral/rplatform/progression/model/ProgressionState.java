package com.raindropcentral.rplatform.progression.model;

import com.raindropcentral.rplatform.progression.IProgressionNode;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Represents the progression state of a node for a specific player.
 * <p>
 * This immutable record encapsulates all information about a player's relationship
 * with a progression node, including:
 * <ul>
 *     <li>The node itself</li>
 *     <li>The current status (LOCKED, AVAILABLE, ACTIVE, COMPLETED)</li>
 *     <li>Missing prerequisites (if locked)</li>
 * </ul>
 *
 * <h2>Usage Example:</h2>
 * <pre>{@code
 * ProgressionState<Quest> state = validator.getProgressionState(playerId, questId).join();
 *
 * if (state.isLocked()) {
 *     player.sendMessage("Complete these quests first: " +
 *         String.join(", ", state.missingPrerequisites()));
 * } else if (state.canStart()) {
 *     // Show start button
 * } else if (state.isActive()) {
 *     // Show progress
 * } else if (state.isCompleted()) {
 *     // Show completion badge
 * }
 * }</pre>
 *
 * @param <T> The type of progression node
 * @param node The progression node
 * @param status The current status for the player
 * @param missingPrerequisites List of prerequisite identifiers that are not yet completed (empty if unlocked)
 * @author RaindropCentral
 * @version 1.0.0
 * @since 1.0.0
 */
public record ProgressionState<T extends IProgressionNode<T>>(
    @NotNull T node,
    @NotNull ProgressionStatus status,
    @NotNull List<String> missingPrerequisites
) {
    
    /**
     * Compact constructor with validation.
     *
     * @param node The progression node (must not be null)
     * @param status The current status (must not be null)
     * @param missingPrerequisites List of missing prerequisites (must not be null)
     * @throws NullPointerException if any parameter is null
     */
    public ProgressionState {
        if (node == null) {
            throw new NullPointerException("node cannot be null");
        }
        if (status == null) {
            throw new NullPointerException("status cannot be null");
        }
        if (missingPrerequisites == null) {
            throw new NullPointerException("missingPrerequisites cannot be null");
        }
        // Make defensive copy to ensure immutability
        missingPrerequisites = List.copyOf(missingPrerequisites);
    }
    
    /**
     * Checks if the node is unlocked (available or better).
     * <p>
     * A node is unlocked if its status is AVAILABLE, ACTIVE, or COMPLETED.
     *
     * @return true if the node is unlocked
     */
    public boolean isUnlocked() {
        return status.isUnlocked();
    }
    
    /**
     * Checks if the node is locked (prerequisites not met).
     * <p>
     * A node is locked if its status is LOCKED.
     *
     * @return true if the node is locked
     */
    public boolean isLocked() {
        return status == ProgressionStatus.LOCKED;
    }
    
    /**
     * Checks if the node can be started.
     * <p>
     * A node can be started if its status is AVAILABLE.
     *
     * @return true if the node can be started
     */
    public boolean canStart() {
        return status.canStart();
    }
    
    /**
     * Checks if the node is in progress.
     * <p>
     * A node is in progress if its status is ACTIVE.
     *
     * @return true if the node is active
     */
    public boolean isActive() {
        return status.isActive();
    }
    
    /**
     * Checks if the node is completed.
     * <p>
     * A node is completed if its status is COMPLETED.
     *
     * @return true if the node is completed
     */
    public boolean isCompleted() {
        return status.isCompleted();
    }
    
    /**
     * Gets the node identifier.
     * <p>
     * Convenience method equivalent to {@code node().getIdentifier()}.
     *
     * @return the node identifier
     */
    public @NotNull String getNodeIdentifier() {
        return node.getIdentifier();
    }
    
    /**
     * Checks if there are missing prerequisites.
     * <p>
     * This is typically true when the status is LOCKED, but may also be
     * used for informational purposes in other states.
     *
     * @return true if there are missing prerequisites
     */
    public boolean hasMissingPrerequisites() {
        return !missingPrerequisites.isEmpty();
    }
    
    /**
     * Gets the count of missing prerequisites.
     * <p>
     * Convenience method equivalent to {@code missingPrerequisites().size()}.
     *
     * @return the number of missing prerequisites
     */
    public int getMissingPrerequisiteCount() {
        return missingPrerequisites.size();
    }
}
