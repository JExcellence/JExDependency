package de.jexcellence.dependency.exception;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Base unchecked exception type for failures encountered while managing runtime dependencies.
 */
public class DependencyException extends RuntimeException {

    /**
     * Creates a new dependency exception with a descriptive message.
     *
     * @param message detail message explaining the failure
     */
    public DependencyException(@NotNull final String message) {
        super(message);
    }

    /**
     * Creates a new dependency exception with a descriptive message and root cause.
     *
     * @param message detail message explaining the failure
     * @param cause   underlying cause
     */
    public DependencyException(@NotNull final String message, @Nullable final Throwable cause) {
        super(message, cause);
    }
}
