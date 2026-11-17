package de.jexcellence.dependency.repository;

import de.jexcellence.dependency.model.DependencyCoordinate;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Maven repositories known to JExDependency. Downloads try the repositories returned by {@link #searchOrder()}
 * in declaration order; retired constants stay for source compatibility but are never contacted.
 */
public enum RepositoryType {

    /**
     * Mirror of Maven Central, tried first so end-user downloads do not hit Central itself. Configured through
     * {@link CentralMirror}; defaults to {@value CentralMirror#DEFAULT_MIRROR}. The URL is read once, when this
     * enum is first used, so set the system property before JExDependency starts.
     */
    MAVEN_CENTRAL_MIRROR(CentralMirror.configured(), false),
    /** Maven Central itself, used only for artifacts the mirror cannot serve. */
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

    /** System property that turns the community repositories on for every plugin on the server. */
    public static final String COMMUNITY_PROPERTY = "jedependency.repository.community";

    private static final List<RepositoryType> SEARCH_ORDER = buildSearchOrder();

    private final String baseUrl;
    private final boolean retired;

    RepositoryType(@NotNull final String baseUrl, final boolean retired) {
        this.baseUrl = normalizeUrl(baseUrl);
        this.retired = retired;
    }

    /**
     * Returns every active repository in order, community repositories included. Retired and duplicate entries
     * are left out. Downloads use {@link #defaultOrder(boolean)}, which leaves the community repositories out
     * unless they are turned on.
     *
     * @return unmodifiable list of active repositories
     */
    public static @NotNull List<RepositoryType> searchOrder() {
        return SEARCH_ORDER;
    }

    /**
     * Returns the built-in repositories downloads use: the Central mirror and Central, plus the community
     * repositories when {@code includeCommunity} is set.
     *
     * @param includeCommunity whether community repositories such as JitPack are added after Central
     * @return unmodifiable list in search order
     */
    public static @NotNull List<RepositoryType> defaultOrder(final boolean includeCommunity) {
        if (includeCommunity) {
            return SEARCH_ORDER;
        }
        return SEARCH_ORDER.stream().filter(repository -> !repository.isCommunity()).toList();
    }

    /**
     * Tells whether the server owner turned the community repositories on for every plugin with
     * {@code -D}{@value #COMMUNITY_PROPERTY}{@code =true}.
     *
     * @return {@code true} when the system property is {@code true}
     */
    public static boolean communityEnabledByProperty() {
        return Boolean.parseBoolean(System.getProperty(COMMUNITY_PROPERTY, "false").trim());
    }

    /**
     * Tells whether this is a community repository. Community repositories are only contacted when a plugin opts
     * in with {@code community-repositories: true} in {@code dependencies.yml} or the server sets
     * {@value #COMMUNITY_PROPERTY}; otherwise only the Central mirror and Central are built in.
     *
     * @return {@code false} for {@link #MAVEN_CENTRAL_MIRROR} and {@link #MAVEN_CENTRAL}, {@code true} otherwise
     */
    public boolean isCommunity() {
        return this != MAVEN_CENTRAL_MIRROR && this != MAVEN_CENTRAL;
    }

    /**
     * Active repositories in declaration order, each host contacted once. When the mirror is configured to be
     * Central itself, the separate Central entry is skipped.
     */
    private static @NotNull List<RepositoryType> buildSearchOrder() {
        final Set<String> seenUrls = new HashSet<>();
        final List<RepositoryType> order = new ArrayList<>();
        for (final RepositoryType repository : values()) {
            if (!repository.retired && seenUrls.add(repository.baseUrl)) {
                order.add(repository);
            }
        }
        return List.copyOf(order);
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
