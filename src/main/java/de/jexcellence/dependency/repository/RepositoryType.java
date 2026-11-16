package de.jexcellence.dependency.repository;

import de.jexcellence.dependency.model.DependencyCoordinate;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

/**
 * Maven repositories known to JExDependency. Downloads try the repositories returned by {@link #searchOrder()}
 * in declaration order; retired constants stay for source compatibility but are never contacted.
 */
public enum RepositoryType {

    /** Maven Central, on the same host that the connectivity probe checks. */
    MAVEN_CENTRAL("https://repo1.maven.org/maven2", false),
    /**
     * Same host as {@link #MAVEN_CENTRAL}.
     *
     * @deprecated duplicate of {@link #MAVEN_CENTRAL}; not contacted any more
     */
    @Deprecated(since = "2.2.0")
    MAVEN_CENTRAL_REPO1("https://repo1.maven.org/maven2", true),
    /**
     * Second host name of Maven Central; same content as {@link #MAVEN_CENTRAL}.
     *
     * @deprecated duplicate of {@link #MAVEN_CENTRAL}; not contacted any more
     */
    @Deprecated(since = "2.2.0")
    APACHE_MAVEN("https://repo.maven.apache.org/maven2", true),
    /**
     * Sonatype OSSRH group, shut down in 2025; requests redirect to a path that does not exist.
     *
     * @deprecated retired service; not contacted any more
     */
    @Deprecated(since = "2.2.0")
    SONATYPE_OSS("https://oss.sonatype.org/content/groups/public/", true),
    /**
     * Sonatype OSSRH snapshots, shut down in 2025.
     *
     * @deprecated retired service; not contacted any more
     */
    @Deprecated(since = "2.2.0")
    SONATYPE_OSS_SNAPSHOTS("https://oss.sonatype.org/content/repositories/snapshots", true),
    /** NeetGames Nexus repository. */
    NEETGAMES_NEXUS("https://nexus.neetgames.com/repository/maven-releases/", false),
    /** JitPack build service turning Git repositories into Maven artifacts. */
    JITPACK("https://jitpack.io", false),
    /** Auxilor public Maven repository. */
    AUXILOR("https://repo.auxilor.io/repository/maven-public/", false),
    /** Tcoded releases repository. */
    TCODED("https://repo.tcoded.com/releases", false),
    /** PaperMC public repository. */
    PAPERMC("https://repo.papermc.io/repository/maven-public/", false),
    /** SpigotMC snapshot repository. */
    SPIGOTMC("https://hub.spigotmc.org/nexus/content/repositories/snapshots/", false),
    /**
     * JCenter, read-only since 2021; requests redirect to Maven Central.
     *
     * @deprecated duplicate of {@link #MAVEN_CENTRAL}; not contacted any more
     */
    @Deprecated(since = "2.2.0")
    JCENTER("https://jcenter.bintray.com/", true),
    /** Gradle plugin portal repository. */
    GRADLE_PLUGINS("https://plugins.gradle.org/m2/", false);

    private static final List<RepositoryType> SEARCH_ORDER = Arrays.stream(values())
            .filter(repository -> !repository.retired)
            .toList();

    private final String baseUrl;
    private final boolean retired;

    RepositoryType(@NotNull final String baseUrl, final boolean retired) {
        this.baseUrl = normalizeUrl(baseUrl);
        this.retired = retired;
    }

    /**
     * Returns the repositories that downloads try, in order. Retired and duplicate entries are left out.
     *
     * @return unmodifiable list of repositories to contact
     */
    public static @NotNull List<RepositoryType> searchOrder() {
        return SEARCH_ORDER;
    }

    /**
     * Tells whether this repository is retired or a duplicate and therefore never contacted.
     *
     * @return {@code true} for retired entries
     */
    public boolean isRetired() {
        return retired;
    }

    /**
     * Returns the base URL for this repository, always ending with a trailing slash.
     *
     * @return normalised repository base URL
     */
    public @NotNull String getBaseUrl() {
        return baseUrl;
    }

    /**
     * Builds the absolute URL to download the specified coordinate from this repository.
     *
     * @param coordinate dependency coordinate to resolve
     *
     * @return absolute download URL
     */
    public @NotNull String buildUrl(@NotNull final DependencyCoordinate coordinate) {
        return baseUrl + coordinate.toRepositoryPath();
    }

    /**
     * Builds the absolute URL to download the specified coordinate components from this repository.
     *
     * @param groupId    Maven group identifier
     * @param artifactId Maven artifact identifier
     * @param version    version string to resolve
     *
     * @return absolute download URL
     */
    public @NotNull String buildPath(
            @NotNull final String groupId,
            @NotNull final String artifactId,
            @NotNull final String version
    ) {
        if (groupId == null || groupId.trim().isEmpty()) {
            throw new IllegalArgumentException("Group ID cannot be null or empty");
        }
        if (artifactId == null || artifactId.trim().isEmpty()) {
            throw new IllegalArgumentException("Artifact ID cannot be null or empty");
        }
        if (version == null || version.trim().isEmpty()) {
            throw new IllegalArgumentException("Version cannot be null or empty");
        }

        final String normalizedGroupId = groupId.replace('.', '/');
        final String jarFileName = artifactId + "-" + version + ".jar";

        return baseUrl + normalizedGroupId + "/" + artifactId + "/" + version + "/" + jarFileName;
    }

    private static @NotNull String normalizeUrl(@NotNull final String url) {
        return url.endsWith("/") ? url : url + "/";
    }
}
