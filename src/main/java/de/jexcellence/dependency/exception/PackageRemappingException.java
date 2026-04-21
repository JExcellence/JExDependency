package de.jexcellence.dependency.exception;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Exception thrown when package remapping of a downloaded artifact fails in a way that cannot be recovered by
 * falling back to a verbatim copy.
 */
public class PackageRemappingException extends DependencyException {

    /**
     * Creates a new package-remapping exception with a descriptive message.
     *
     * @param message detail message explaining the failure
     */
    public PackageRemappingException(@NotNull final String message) {
        super(message);
    }

    /**
     * Creates a new package-remapping exception with a descriptive message and root cause.
     *
     * @param message detail message explaining the failure
     * @param cause   underlying cause such as an {@link java.io.IOException}
     */
    public PackageRemappingException(@NotNull final String message, @Nullable final Throwable cause) {
        super(message, cause);
    }
}
