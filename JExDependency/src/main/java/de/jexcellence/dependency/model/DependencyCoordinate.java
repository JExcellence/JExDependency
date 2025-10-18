package de.jexcellence.dependency.model;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Immutable Maven coordinate used throughout the runtime dependency system.
 *
 * @param groupId    Maven group identifier
 * @param artifactId Maven artifact identifier
 * @param version    version string to resolve
 * @param classifier optional classifier segment (e.g. {@code javadoc})
 */
public record DependencyCoordinate(
        @NotNull String groupId,
        @NotNull String artifactId,
        @NotNull String version,
        @Nullable String classifier
) {

    public DependencyCoordinate {
        Objects.requireNonNull(groupId, "groupId cannot be null");
        Objects.requireNonNull(artifactId, "artifactId cannot be null");
        Objects.requireNonNull(version, "version cannot be null");
    }

    /**
     * Convenience constructor for coordinates without a classifier.
     *
     * @param groupId    Maven group identifier
     * @param artifactId Maven artifact identifier
     * @param version    version string to resolve
     */
    public DependencyCoordinate(
            @NotNull final String groupId,
            @NotNull final String artifactId,
            @NotNull final String version
    ) {
        this(groupId, artifactId, version, null);
    }

    /**
     * Formats the coordinate as a Maven-style string.
     *
     * @return coordinate string in {@code group:artifact:version[:classifier]} format
     */
    public @NotNull String toGavString() {
        final StringBuilder builder = new StringBuilder()
                .append(groupId)
                .append(':')
                .append(artifactId)
                .append(':')
                .append(version);

        if (classifier != null && !classifier.isEmpty()) {
            builder.append(':').append(classifier);
        }

        return builder.toString();
    }

    /**
     * Produces the file name corresponding to this coordinate.
     *
     * @return jar file name including classifier when present
     */
    public @NotNull String toFileName() {
        final StringBuilder builder = new StringBuilder()
                .append(artifactId)
                .append('-')
                .append(version);

        if (classifier != null && !classifier.isEmpty()) {
            builder.append('-').append(classifier);
        }

        return builder.append(".jar").toString();
    }

    /**
     * Produces the repository path fragment (directories plus file name) for this coordinate.
     *
     * @return repository-relative path
     */
    public @NotNull String toRepositoryPath() {
        return groupId.replace('.', '/')
                + '/' + artifactId
                + '/' + version
                + '/' + toFileName();
    }

    /**
     * Parses a Maven coordinate string and returns a {@link DependencyCoordinate}, or {@code null} if the string is not
     * well-formed.
     *
     * @param gavCoordinates coordinate string in {@code group:artifact:version[:classifier]} format
     *
     * @return parsed coordinate or {@code null} when the format is invalid
     */
    public static @Nullable DependencyCoordinate parse(@NotNull final String gavCoordinates) {
        Objects.requireNonNull(gavCoordinates, "GAV coordinates cannot be null");

        final String[] parts = gavCoordinates.split(":");
        if (parts.length < 3) {
            return null;
        }

        final String classifier = parts.length > 3 ? parts[3] : null;
        return new DependencyCoordinate(parts[0], parts[1], parts[2], classifier);
    }
}
