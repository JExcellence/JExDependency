package de.jexcellence.dependency.loader;

import de.jexcellence.dependency.model.DependencyCoordinate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Objects;

/**
 * One line of the {@code dependencies:} list in {@code dependencies.yml}:
 * {@code group:artifact:version[:classifier]}, optionally followed by {@code @ <repository URL>} to download it only
 * from that repository and {@code sha256=<hex>} to accept only that exact jar.
 *
 * @param coordinate the artifact
 * @param repository base URL without a trailing slash to download from instead of the normal search order,
 *                   or {@code null}
 * @param sha256     expected SHA-256 of the jar in lower-case hex, or {@code null}
 */
public record DependencyEntry(
        @NotNull DependencyCoordinate coordinate,
        @Nullable String repository,
        @Nullable String sha256
) {

    /**
     * Validates the entry.
     */
    public DependencyEntry {
        Objects.requireNonNull(coordinate, "coordinate");
        if (sha256 != null) {
            sha256 = sha256.toLowerCase(Locale.ROOT);
            if (!sha256.matches("[0-9a-f]{64}")) {
                throw new IllegalArgumentException("sha256 must be 64 hex characters");
            }
        }
    }

    /**
     * Creates an entry without a pinned repository or hash.
     *
     * @param coordinate the artifact
     * @return the entry
     */
    public static @NotNull DependencyEntry of(@NotNull final DependencyCoordinate coordinate) {
        return new DependencyEntry(coordinate, null, null);
    }
}
