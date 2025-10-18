package de.jexcellence.dependency.exception;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DependencyException extends RuntimeException {

    public DependencyException(@NotNull final String message) {
        super(message);
    }

    public DependencyException(@NotNull final String message, @Nullable final Throwable cause) {
        super(message, cause);
    }
}
