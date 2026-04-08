package com.raindropcentral.rplatform.progression.model;

/**
 * Enumeration of possible progression states for a node.
 * <p>
 * These states represent the lifecycle of a progression node from the perspective
 * of a specific player:
 * <ul>
 *     <li>{@link #LOCKED} - Prerequisites not met, cannot be started</li>
 *     <li>{@link #AVAILABLE} - Prerequisites met, can be started</li>
 *     <li>{@link #ACTIVE} - Currently in progress</li>
 *     <li>{@link #COMPLETED} - Finished successfully</li>
 * </ul>
 *
 * <h2>State Transitions:</h2>
 * <pre>
 * LOCKED → AVAILABLE → ACTIVE → COMPLETED
 *    ↑         ↓
 *    └─────────┘ (if prerequisites change)
 * </pre>
 *
 * <h2>Usage Example:</h2>
 * <pre>{@code
 * ProgressionStatus status = progressionState.status();
 * switch (status) {
 *     case LOCKED -> showLockedUI();
 *     case AVAILABLE -> showStartButton();
 *     case ACTIVE -> showProgressBar();
 *     case COMPLETED -> showCompletionBadge();
 * }
 * }</pre>
 *
 * @author RaindropCentral
 * @version 1.0.0
 * @since 1.0.0
 */
public enum ProgressionStatus {
    /**
     * Node is locked - prerequisites not met.
     * <p>
     * The player cannot start this node until all prerequisite nodes are completed.
     * UI should display this node as locked with information about missing prerequisites.
     */
    LOCKED,
    
    /**
     * Node is available - can be started.
     * <p>
     * All prerequisites are met and the player can start this node.
     * UI should display this node as available with a start action.
     */
    AVAILABLE,
    
    /**
     * Node is active - currently in progress.
     * <p>
     * The player has started this node and is working on completing it.
     * UI should display progress information and allow viewing details.
     */
    ACTIVE,
    
    /**
     * Node is completed - finished.
     * <p>
     * The player has successfully completed this node.
     * UI should display completion status and any rewards earned.
     */
    COMPLETED;
    
    /**
     * Checks if this status represents an unlocked state.
     * <p>
     * A node is considered unlocked if it's AVAILABLE, ACTIVE, or COMPLETED.
     * Only LOCKED nodes are considered locked.
     *
     * @return true if this status is not LOCKED
     */
    public boolean isUnlocked() {
        return this != LOCKED;
    }
    
    /**
     * Checks if this status allows starting the node.
     * <p>
     * Only AVAILABLE nodes can be started.
     *
     * @return true if this status is AVAILABLE
     */
    public boolean canStart() {
        return this == AVAILABLE;
    }
    
    /**
     * Checks if this status represents an active node.
     * <p>
     * Only ACTIVE nodes are considered in progress.
     *
     * @return true if this status is ACTIVE
     */
    public boolean isActive() {
        return this == ACTIVE;
    }
    
    /**
     * Checks if this status represents a completed node.
     * <p>
     * Only COMPLETED nodes are considered finished.
     *
     * @return true if this status is COMPLETED
     */
    public boolean isCompleted() {
        return this == COMPLETED;
    }
}
