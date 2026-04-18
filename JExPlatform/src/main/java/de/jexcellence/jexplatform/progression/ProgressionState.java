package de.jexcellence.jexplatform.progression;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

/**
 * Represents the progression state of a node for a specific player.
 *
 * <p>Encapsulates the node, its current status, and any missing prerequisites.
 *
 * @param <T>                   the type of progression node
 * @param node                  the progression node
 * @param status                the current status for the player
 * @param missingPrerequisites  prerequisite identifiers not yet completed (empty if unlocked)
 * @author JExcellence
 * @since 1.0.0
 */
public record ProgressionState<T extends ProgressionNode<T>>(
        @NotNull T node,
        @NotNull ProgressionStatus status,
        @NotNull List<String> missingPrerequisites
) {

    /**
     * Status of a progression node from a player's perspective.
     *
     * @author JExcellence
     * @since 1.0.0
     */
    public enum ProgressionStatus {
        /** Prerequisites not met — cannot be started. */
        LOCKED,
        /** Prerequisites met — can be started. */
        AVAILABLE,
        /** Currently in progress. */
        ACTIVE,
        /** Successfully completed. */
        COMPLETED;

        /**
         * Checks whether this status represents an unlocked state.
         *
         * @return {@code true} if not {@link #LOCKED}
         */
        public boolean isUnlocked() {
            return this != LOCKED;
        }

        /**
         * Checks whether this status allows starting the node.
         *
         * @return {@code true} if {@link #AVAILABLE}
         */
        public boolean canStart() {
            return this == AVAILABLE;
        }

        /**
         * Checks whether this status represents an active node.
         *
         * @return {@code true} if {@link #ACTIVE}
         */
        public boolean isActive() {
            return this == ACTIVE;
        }

        /**
         * Checks whether this status represents a completed node.
         *
         * @return {@code true} if {@link #COMPLETED}
         */
        public boolean isCompleted() {
            return this == COMPLETED;
        }
    }

    /** Compact constructor with null checks and defensive copy. */
    public ProgressionState {
        Objects.requireNonNull(node, "node cannot be null");
        Objects.requireNonNull(status, "status cannot be null");
        Objects.requireNonNull(missingPrerequisites, "missingPrerequisites cannot be null");
        missingPrerequisites = List.copyOf(missingPrerequisites);
    }

    /**
     * Checks whether the node is unlocked (available, active, or completed).
     *
     * @return {@code true} if the node is not locked
     */
    public boolean isUnlocked() {
        return status.isUnlocked();
    }

    /**
     * Checks whether the node is locked (prerequisites not met).
     *
     * @return {@code true} if the node is locked
     */
    public boolean isLocked() {
        return status == ProgressionStatus.LOCKED;
    }

    /**
     * Checks whether the node can be started.
     *
     * @return {@code true} if the node is available
     */
    public boolean canStart() {
        return status.canStart();
    }

    /**
     * Checks whether the node is in progress.
     *
     * @return {@code true} if the node is active
     */
    public boolean isActive() {
        return status.isActive();
    }

    /**
     * Checks whether the node is completed.
     *
     * @return {@code true} if the node is completed
     */
    public boolean isCompleted() {
        return status.isCompleted();
    }
}
