package de.jexcellence.dependency.exception;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DownloadException extends DependencyException {

    public DownloadException(@NotNull final String message) {
        super(message);
    }

    public DownloadException(@NotNull final String message, @Nullable final Throwable cause) {
        super(message, cause);
    }
}
