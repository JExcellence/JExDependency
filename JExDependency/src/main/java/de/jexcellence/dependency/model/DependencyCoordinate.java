package de.jexcellence.dependency.model;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

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

    public DependencyCoordinate(
            @NotNull final String groupId,
            @NotNull final String artifactId,
            @NotNull final String version
    ) {
        this(groupId, artifactId, version, null);
    }

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

    public @NotNull String toRepositoryPath() {
        return groupId.replace('.', '/')
                + '/' + artifactId
                + '/' + version
                + '/' + toFileName();
    }

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
