package de.jexcellence.dependency.exception;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Exception thrown when a downloaded dependency cannot be injected into the runtime classpath.
 */
public class InjectionException extends DependencyException {

    /**
     * Creates a new injection exception with a descriptive message.
     *
     * @param message detail message explaining the failure
     */
    public InjectionException(@NotNull final String message) {
        super(message);
    }

    /**
     * Creates a new injection exception with a descriptive message and root cause.
     *
     * @param message detail message explaining the failure
     * @param cause   underlying cause such as a reflective access failure
     */
    public InjectionException(@NotNull final String message, @Nullable final Throwable cause) {
        super(message, cause);
    }
}
