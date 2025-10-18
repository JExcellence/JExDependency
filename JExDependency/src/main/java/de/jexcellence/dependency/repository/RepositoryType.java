package de.jexcellence.dependency.repository;

import de.jexcellence.dependency.model.DependencyCoordinate;
import org.jetbrains.annotations.NotNull;

public enum RepositoryType {

    MAVEN_CENTRAL("https://central.maven.org/maven2"),
    MAVEN_CENTRAL_REPO1("https://repo1.maven.org/maven2"),
    APACHE_MAVEN("https://repo.maven.apache.org/maven2"),
    SONATYPE_OSS("https://oss.sonatype.org/content/groups/public/"),
    SONATYPE_OSS_SNAPSHOTS("https://oss.sonatype.org/content/repositories/snapshots"),
    NEETGAMES_NEXUS("https://nexus.neetgames.com/repository/maven-releases/"),
    JITPACK("https://jitpack.io"),
    AUXILOR("https://repo.auxilor.io/repository/maven-public/"),
    TCODED("https://repo.tcoded.com/releases"),
    PAPERMC("https://repo.papermc.io/repository/maven-public/"),
    SPIGOTMC("https://hub.spigotmc.org/nexus/content/repositories/snapshots/"),
    JCENTER("https://jcenter.bintray.com/"),
    GRADLE_PLUGINS("https://plugins.gradle.org/m2/");

    private final String baseUrl;

    RepositoryType(@NotNull final String baseUrl) {
        this.baseUrl = normalizeUrl(baseUrl);
    }

    public @NotNull String getBaseUrl() {
        return baseUrl;
    }

    public @NotNull String buildUrl(@NotNull final DependencyCoordinate coordinate) {
        return baseUrl + coordinate.toRepositoryPath();
    }

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
