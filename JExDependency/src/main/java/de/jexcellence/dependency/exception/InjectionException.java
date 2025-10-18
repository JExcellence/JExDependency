package de.jexcellence.dependency.exception;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class InjectionException extends DependencyException {

    public InjectionException(@NotNull final String message) {
        super(message);
    }

    public InjectionException(@NotNull final String message, @Nullable final Throwable cause) {
        super(message, cause);
    }
}
