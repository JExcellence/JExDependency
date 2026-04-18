package de.jexcellence.dependency.model;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Objects;

/**
 * Result container describing the outcome of downloading a dependency coordinate.
 *
 * @param coordinate   coordinate that was processed
 * @param file         resolved file on disk when the download succeeded
 * @param success      whether the download succeeded
 * @param errorMessage optional error description when the download failed
 */
public record DownloadResult(
        @NotNull DependencyCoordinate coordinate,
        @Nullable File file,
        boolean success,
        @Nullable String errorMessage
) {

    public DownloadResult {
        Objects.requireNonNull(coordinate, "coordinate cannot be null");
    }

    /**
     * Creates a successful download result wrapper.
     *
     * @param coordinate coordinate that was downloaded
     * @param file       downloaded file
     *
     * @return success result
     */
    public static @NotNull DownloadResult success(
            @NotNull final DependencyCoordinate coordinate,
            @NotNull final File file
    ) {
        return new DownloadResult(coordinate, file, true, null);
    }

    /**
     * Creates a failed download result wrapper.
     *
     * @param coordinate   coordinate that failed to download
     * @param errorMessage error description to surface to callers
     *
     * @return failure result
     */
    public static @NotNull DownloadResult failure(
            @NotNull final DependencyCoordinate coordinate,
            @NotNull final String errorMessage
    ) {
        return new DownloadResult(coordinate, null, false, errorMessage);
    }
}
