package de.jexcellence.dependency.exception;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Exception thrown when a dependency cannot be downloaded from any configured repository.
 */
public class DownloadException extends DependencyException {

    /**
     * Creates a new download exception with a descriptive message.
     *
     * @param message detail message explaining the failure
     */
    public DownloadException(@NotNull final String message) {
        super(message);
    }

    /**
     * Creates a new download exception with a descriptive message and root cause.
     *
     * @param message detail message explaining the failure
     * @param cause   underlying cause such as an {@link java.io.IOException}
     */
    public DownloadException(@NotNull final String message, @Nullable final Throwable cause) {
        super(message, cause);
    }
}
