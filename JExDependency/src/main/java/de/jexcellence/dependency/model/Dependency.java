package de.jexcellence.dependency.model;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public record Dependency(
        @NotNull String groupId,
        @NotNull String artifactId,
        @NotNull String version,
        @Nullable String classifier,
        @Nullable String repository
) {

    public Dependency {
        Objects.requireNonNull(groupId, "groupId cannot be null");
        Objects.requireNonNull(artifactId, "artifactId cannot be null");
        Objects.requireNonNull(version, "version cannot be null");
    }

    public Dependency(
            @NotNull final String groupId,
            @NotNull final String artifactId,
            @NotNull final String version
    ) {
        this(groupId, artifactId, version, null, null);
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

    public static @NotNull Dependency parse(@NotNull final String dependencyString) {
        Objects.requireNonNull(dependencyString, "dependencyString cannot be null");

        final String[] parts = dependencyString.split(":");
        if (parts.length < 3) {
            throw new IllegalArgumentException("Invalid dependency format: " + dependencyString);
        }

        final String classifier = parts.length > 3 ? parts[3] : null;
        return new Dependency(parts[0], parts[1], parts[2], classifier, null);
    }
}
