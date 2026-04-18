package de.jexcellence.jexplatform.progression.exception;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Thrown when a circular dependency is detected in a progression prerequisite chain.
 *
 * <p>Indicates that a node's prerequisite chain forms a cycle, making it impossible
 * to complete the progression (e.g. A → B → C → A).
 *
 * @author JExcellence
 * @since 1.0.0
 */
public class CircularDependencyException extends RuntimeException {

    /** Creates an exception with no detail message. */
    public CircularDependencyException() {
        super();
    }

    /**
     * Creates an exception with the specified detail message.
     *
     * @param message the detail message
     */
    public CircularDependencyException(@NotNull String message) {
        super(message);
    }

    /**
     * Creates an exception with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause   the cause
     */
    public CircularDependencyException(@NotNull String message, @Nullable Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates an exception with the specified cause.
     *
     * @param cause the cause
     */
    public CircularDependencyException(@Nullable Throwable cause) {
        super(cause);
    }
}
