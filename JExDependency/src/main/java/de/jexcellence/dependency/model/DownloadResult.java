package de.jexcellence.dependency.model;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Objects;

public record DownloadResult(
        @NotNull DependencyCoordinate coordinate,
        @Nullable File file,
        boolean success,
        @Nullable String errorMessage
) {

    public DownloadResult {
        Objects.requireNonNull(coordinate, "coordinate cannot be null");
    }

    public static @NotNull DownloadResult success(
            @NotNull final DependencyCoordinate coordinate,
            @NotNull final File file
    ) {
        return new DownloadResult(coordinate, file, true, null);
    }

    public static @NotNull DownloadResult failure(
            @NotNull final DependencyCoordinate coordinate,
            @NotNull final String errorMessage
    ) {
        return new DownloadResult(coordinate, null, false, errorMessage);
    }
}
