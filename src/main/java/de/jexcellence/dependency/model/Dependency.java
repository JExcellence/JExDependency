package de.jexcellence.dependency.model;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Immutable model representing a dependency declared in YAML including an optional classifier and repository override.
 *
 * @param groupId    Maven group identifier
 * @param artifactId Maven artifact identifier
 * @param version    version string to resolve
 * @param classifier optional classifier segment (e.g. {@code sources})
 * @param repository optional repository identifier when resolution should bypass defaults
 */
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

    /**
     * Convenience constructor for dependencies without classifier or repository overrides.
     *
     * @param groupId    Maven group identifier
     * @param artifactId Maven artifact identifier
     * @param version    version string to resolve
     */
    public Dependency(
            @NotNull final String groupId,
            @NotNull final String artifactId,
            @NotNull final String version
    ) {
        this(groupId, artifactId, version, null, null);
    }

    /**
     * Builds the file name (including classifier when present) expected at the repository location.
     *
     * @return jar file name
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
     * Builds the repository-relative path for this dependency including the file name.
     *
     * @return repository path fragment
     */
    public @NotNull String toRepositoryPath() {
        return groupId.replace('.', '/')
                + '/' + artifactId
                + '/' + version
                + '/' + toFileName();
    }

    /**
     * Formats the dependency as a Maven coordinate string.
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
     * Parses a Maven coordinate string into a {@link Dependency} instance.
     *
     * @param dependencyString coordinate string in {@code group:artifact:version[:classifier]} format
     *
     * @return parsed dependency
     */
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
