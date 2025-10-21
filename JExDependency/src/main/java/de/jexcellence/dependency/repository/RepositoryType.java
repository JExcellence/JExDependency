package de.jexcellence.dependency.repository;

import de.jexcellence.dependency.model.DependencyCoordinate;
import org.jetbrains.annotations.NotNull;

/**
 * Enumeration of Maven repositories queried when downloading dependencies. Each constant describes a commonly used
 * repository within the Raindrop ecosystem.
 */
public enum RepositoryType {

    /** Primary Maven Central endpoint historically served via central.maven.org. */
    MAVEN_CENTRAL("https://central.maven.org/maven2"),
    /** Mirror of Maven Central served from repo1.maven.org. */
    MAVEN_CENTRAL_REPO1("https://repo1.maven.org/maven2"),
    /** Official Apache Maven repository endpoint. */
    APACHE_MAVEN("https://repo.maven.apache.org/maven2"),
    /** Sonatype OSS releases group containing a broad set of community artifacts. */
    SONATYPE_OSS("https://oss.sonatype.org/content/groups/public/"),
    /** Sonatype OSS snapshot repository for bleeding-edge builds. */
    SONATYPE_OSS_SNAPSHOTS("https://oss.sonatype.org/content/repositories/snapshots"),
    /** NeetGames Nexus repository hosting proprietary releases. */
    NEETGAMES_NEXUS("https://nexus.neetgames.com/repository/maven-releases/"),
    /** JitPack build service turning Git repositories into Maven artifacts. */
    JITPACK("https://jitpack.io"),
    /** Auxilor public Maven repository for plugin dependencies. */
    AUXILOR("https://repo.auxilor.io/repository/maven-public/"),
    /** Tcoded releases repository. */
    TCODED("https://repo.tcoded.com/releases"),
    /** PaperMC public repository containing Paper and related libraries. */
    PAPERMC("https://repo.papermc.io/repository/maven-public/"),
    /** SpigotMC snapshot repository for Bukkit/Paper ecosystem dependencies. */
    SPIGOTMC("https://hub.spigotmc.org/nexus/content/repositories/snapshots/"),
    /** Legacy Bintray JCenter repository. */
    JCENTER("https://jcenter.bintray.com/"),
    /** Gradle plugin portal repository. */
    GRADLE_PLUGINS("https://plugins.gradle.org/m2/");

    private final String baseUrl;

    RepositoryType(@NotNull final String baseUrl) {
        this.baseUrl = normalizeUrl(baseUrl);
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
