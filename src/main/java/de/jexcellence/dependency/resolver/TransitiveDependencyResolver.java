package de.jexcellence.dependency.resolver;

import de.jexcellence.dependency.model.DependencyCoordinate;
import de.jexcellence.dependency.repository.RepositoryType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Resolves the full transitive dependency closure of a set of root Maven coordinates by
 * downloading and parsing their {@code .pom} files.
 *
 * <h2>Algorithm overview</h2>
 * <ol>
 *   <li>For each root coordinate, download its POM from Maven repositories.</li>
 *   <li>Walk the {@code <parent>} chain (up to {@value #MAX_PARENT_DEPTH} levels) merging
 *       {@code <properties>} and {@code <dependencyManagement>} entries — child values take
 *       precedence over parent values (standard Maven semantics).</li>
 *   <li>For each direct {@code <dependency>} that is not {@code test}, {@code provided},
 *       {@code system}, or {@code optional}, resolve its version from the merged
 *       {@code <dependencyManagement>} map if not stated inline, then substitute any
 *       {@code ${property}} references.</li>
 *   <li>Recurse into each resolved transitive dependency (up to {@value #MAX_TRANSITIVE_DEPTH}
 *       levels deep) tracking visited coordinates to prevent cycles.</li>
 * </ol>
 *
 * <h2>Caching</h2>
 * Downloaded {@code .pom} files are written to a {@code poms/} sub-directory inside the
 * plugin's libraries directory and are re-used on subsequent server starts, so only new or
 * updated artifacts require network access.
 *
 * <h2>Known limitations</h2>
 * <ul>
 *   <li><b>BOM imports</b> ({@code <scope>import</scope>} in {@code <dependencyManagement>})
 *       are skipped.  Libraries that version all their sub-modules through a BOM (e.g. the
 *       Hibernate ORM platform) still work because the individual artifact POMs list concrete
 *       version numbers after property substitution through the parent chain.</li>
 *   <li><b>Version ranges</b> (e.g. {@code [1.0,2.0)}) are not evaluated; any dependency
 *       whose version cannot be reduced to a plain string is logged and skipped.</li>
 *   <li><b>Maven profiles</b> are ignored.</li>
 * </ul>
 */
public class TransitiveDependencyResolver {

    private static final Logger LOGGER = Logger.getLogger("JExDependency");

    /** Maximum number of parent POM levels to follow when collecting inherited properties. */
    private static final int MAX_PARENT_DEPTH     = 8;
    /** Maximum recursion depth when walking the dependency graph. */
    private static final int MAX_TRANSITIVE_DEPTH = 12;

    private static final int CONNECT_TIMEOUT_MS = 10_000;
    private static final int READ_TIMEOUT_MS    = 20_000;

    private final PomParser pomParser;
    private final File pomCacheDir;

    /**
     * In-process POM cache keyed by {@code groupId:artifactId:version}.
     * Uses {@link LinkedHashMap} so that insertion order is preserved (useful for logging).
     */
    private final Map<String, ParsedPom> pomCache = new LinkedHashMap<>();

    /**
     * Creates a resolver that stores downloaded POM files under a {@code poms/} directory
     * inside the supplied libraries directory.
     *
     * @param librariesDirectory the plugin's libraries cache directory
     */
    public TransitiveDependencyResolver(@NotNull final File librariesDirectory) {
        this.pomParser  = new PomParser();
        this.pomCacheDir = new File(librariesDirectory, "poms");
        this.pomCacheDir.mkdirs();
    }

    /**
     * Resolves the full transitive dependency closure for all given root coordinates.
     *
     * <p>The returned set is ordered breadth-first.  Root coordinates are NOT included in the
     * result — callers are expected to download those separately through the normal flow.
     *
     * @param roots root coordinates to resolve from
     * @return ordered set of transitive coordinates to additionally download and inject;
     *         never {@code null}, may be empty
     */
    public @NotNull Set<DependencyCoordinate> resolve(@NotNull final List<DependencyCoordinate> roots) {
        // Seed the visited set with all root GAVs so we don't re-download them
        final Set<String> visited = new HashSet<>();
        for (final DependencyCoordinate root : roots) {
            visited.add(gavKey(root));
        }

        final Set<DependencyCoordinate> result = new LinkedHashSet<>();

        for (final DependencyCoordinate root : roots) {
            resolveRecursive(root, visited, result, 0, Collections.emptySet());
        }

        return result;
    }

    // -------------------------------------------------------------------------
    // Core recursive resolution
    // -------------------------------------------------------------------------

    private void resolveRecursive(
            @NotNull final DependencyCoordinate coordinate,
            @NotNull final Set<String> visited,
            @NotNull final Set<DependencyCoordinate> result,
            final int depth,
            @NotNull final Set<String> activeExclusions
    ) {
        if (depth >= MAX_TRANSITIVE_DEPTH) {
            LOGGER.log(Level.FINE, () -> "Max transitive depth reached at: " + coordinate.toGavString());
            return;
        }

        final ParsedPom pom = resolvedPomWithParentChain(coordinate);
        if (pom == null) {
            LOGGER.log(Level.WARNING, "Could not resolve POM for: {0} \u2014 skipping its transitives", coordinate.toGavString());
            return;
        }

        // Build an effective version map from the (already merged) dependencyManagement list
        final Map<String, String> managedVersions = buildManagedVersionMap(pom);

        for (final PomDependency dep : pom.dependencies()) {
            final DependencyCoordinate resolved = resolveTransitiveDependency(dep, pom, managedVersions, coordinate, visited, activeExclusions);
            if (resolved == null) {
                continue;
            }

            result.add(resolved);

            // Propagate exclusions declared on this dependency down the recursion
            final Set<String> newExclusions;
            if (dep.exclusions().isEmpty()) {
                newExclusions = activeExclusions;
            } else {
                newExclusions = new HashSet<>(activeExclusions);
                newExclusions.addAll(dep.exclusions());
            }

            resolveRecursive(resolved, visited, result, depth + 1, newExclusions);
        }
    }

    /**
     * Attempts to resolve a single {@link PomDependency} to a concrete {@link DependencyCoordinate}, applying
     * runtime-scope filtering, property substitution, dependencyManagement version fallback, ancestor exclusion
     * checks, and de-duplication. Returns {@code null} when the dependency should be skipped.
     */
    private @Nullable DependencyCoordinate resolveTransitiveDependency(
            @NotNull final PomDependency dep,
            @NotNull final ParsedPom pom,
            @NotNull final Map<String, String> managedVersions,
            @NotNull final DependencyCoordinate coordinate,
            @NotNull final Set<String> visited,
            @NotNull final Set<String> activeExclusions
    ) {
        if (!dep.isRuntimeIncluded()) {
            return null;
        }

        // Resolve property references in coordinates
        final String resolvedGroup = resolveProperties(dep.groupId(), pom);
        final String resolvedArtifact = resolveProperties(dep.artifactId(), pom);
        if (resolvedGroup == null || resolvedArtifact == null) {
            return null;
        }

        // Check active exclusions from ancestor dependency declarations
        final String depKey = resolvedGroup + ":" + resolvedArtifact;
        if (activeExclusions.contains(depKey)
                || activeExclusions.contains(resolvedGroup + ":*")
                || activeExclusions.contains("*:" + resolvedArtifact)) {
            LOGGER.log(Level.FINE, () -> "Excluded by ancestor rule: " + depKey);
            return null;
        }

        // Resolve version: inline -> dependencyManagement -> give up
        String resolvedVersion = resolveProperties(dep.version(), pom);
        if (resolvedVersion == null) {
            resolvedVersion = managedVersions.get(depKey);
        }
        if (resolvedVersion == null) {
            final String finalKey = depKey;
            LOGGER.log(Level.FINE, () -> "Version unresolvable for " + finalKey + " in " + coordinate.toGavString() + " \u2014 skipping");
            return null;
        }

        // Skip version ranges - we don't evaluate them
        if (resolvedVersion.startsWith("[") || resolvedVersion.startsWith("(")) {
            final String finalVersion = resolvedVersion;
            LOGGER.log(Level.FINE, () -> "Skipping version range " + finalVersion + " for " + depKey);
            return null;
        }

        // Deduplication: nearest-wins (first version seen wins)
        final String fullKey = resolvedGroup + ":" + resolvedArtifact + ":" + resolvedVersion;
        if (visited.contains(fullKey)) {
            return null;
        }
        // Also de-duplicate by G:A alone so we don't pull in two different versions
        final String gaKey = resolvedGroup + ":" + resolvedArtifact;
        if (visited.stream().anyMatch(v -> v.startsWith(gaKey + ":"))) {
            return null;
        }

        visited.add(fullKey);

        return new DependencyCoordinate(resolvedGroup, resolvedArtifact, resolvedVersion);
    }

    // -------------------------------------------------------------------------
    // POM download + parent chain merger
    // -------------------------------------------------------------------------

    /**
     * Downloads and parses the POM for {@code coordinate}, then walks the {@code <parent>}
     * chain merging properties and dependencyManagement entries.  Returns a single
     * {@link ParsedPom} whose {@code properties} and {@code dependencyManagement} reflect
     * the full ancestry (child values override parent values).
     */
    private @Nullable ParsedPom resolvedPomWithParentChain(@NotNull final DependencyCoordinate coordinate) {
        final ParsedPom ownPom = downloadAndParsePom(coordinate);
        if (ownPom == null) return null;

        // Merge properties and dependencyManagement from parent chain
        final Map<String, String>   mergedProps  = new HashMap<>(ownPom.properties());
        final List<PomDependency>   mergedMgmt   = new ArrayList<>(ownPom.dependencyManagement());

        DependencyCoordinate parentCoord = ownPom.parent();
        int parentDepth = 0;

        while (parentCoord != null && parentDepth < MAX_PARENT_DEPTH) {
            final ParsedPom parentPom = downloadAndParsePom(parentCoord);
            if (parentPom == null) break;

            // Parent properties only fill gaps — they don't override child
            for (final Map.Entry<String, String> entry : parentPom.properties().entrySet()) {
                mergedProps.putIfAbsent(entry.getKey(), entry.getValue());
            }

            // Parent dependencyManagement entries only fill gaps
            for (final PomDependency managed : parentPom.dependencyManagement()) {
                final String key = managed.groupId() + ":" + managed.artifactId();
                final boolean alreadyPresent = mergedMgmt.stream()
                        .anyMatch(m -> (m.groupId() + ":" + m.artifactId()).equals(key));
                if (!alreadyPresent) {
                    mergedMgmt.add(managed);
                }
            }

            parentCoord = parentPom.parent();
            parentDepth++;
        }

        return new ParsedPom(
                ownPom.groupId(), ownPom.artifactId(), ownPom.version(),
                ownPom.parent(),
                mergedProps,
                mergedMgmt,
                ownPom.dependencies()
        );
    }

    /**
     * Returns the cached or freshly downloaded-and-parsed POM for the given coordinate.
     * On a cache miss, tries all known repositories in order and persists the raw POM bytes
     * to disk so subsequent restarts skip the network request.
     */
    private @Nullable ParsedPom downloadAndParsePom(@NotNull final DependencyCoordinate coordinate) {
        final String cacheKey = gavKey(coordinate);

        // 1. In-memory cache
        if (pomCache.containsKey(cacheKey)) {
            return pomCache.get(cacheKey);
        }

        // 2. Disk cache
        final File diskFile = new File(pomCacheDir,
                coordinate.artifactId() + "-" + coordinate.version() + ".pom");
        if (diskFile.isFile() && diskFile.length() > 0) {
            try (final InputStream is = new FileInputStream(diskFile)) {
                final ParsedPom pom = pomParser.parse(is);
                if (pom != null) {
                    pomCache.put(cacheKey, pom);
                    return pom;
                }
            } catch (final Exception exception) {
                LOGGER.log(Level.FINE, exception, () -> "Failed to read cached POM: " + diskFile);
            }
        }

        // 3. Remote download
        final String pomPath = coordinate.groupId().replace('.', '/') + '/'
                + coordinate.artifactId() + '/'
                + coordinate.version() + '/'
                + coordinate.artifactId() + '-' + coordinate.version() + ".pom";

        for (final RepositoryType repo : RepositoryType.values()) {
            final byte[] bytes = fetchBytes(repo.getBaseUrl() + pomPath);
            if (bytes == null || bytes.length == 0) continue;

            // Persist to disk cache
            try {
                Files.createDirectories(pomCacheDir.toPath());
                Files.write(diskFile.toPath(), bytes);
            } catch (final Exception exception) {
                LOGGER.log(Level.FINE, "Failed to persist POM to disk cache", exception);
            }

            // Parse
            try (final InputStream is = new ByteArrayInputStream(bytes)) {
                final ParsedPom pom = pomParser.parse(is);
                if (pom != null) {
                    pomCache.put(cacheKey, pom);
                    LOGGER.log(Level.FINE, () -> "Resolved POM: " + coordinate.toGavString() + " from " + repo.name());
                    return pom;
                }
            } catch (final Exception exception) {
                LOGGER.log(Level.FINE, exception, () -> "Failed to parse POM from " + repo.name());
            }
        }

        LOGGER.log(Level.WARNING, "Could not download POM: {0}", coordinate.toGavString());
        return null;
    }

    /**
     * Performs a simple HTTP GET and returns the full response body, or {@code null} on
     * error or non-200 response.
     */
    private @Nullable byte[] fetchBytes(@NotNull final String urlString) {
        try {
            final URL url = URI.create(urlString).toURL();
            final HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
            conn.setReadTimeout(READ_TIMEOUT_MS);
            conn.setRequestProperty("User-Agent", "JExDependency-PomResolver/2.0.0");
            conn.setInstanceFollowRedirects(true);

            if (conn.getResponseCode() != 200) return null;

            try (final InputStream is = conn.getInputStream()) {
                return is.readAllBytes();
            }
        } catch (final Exception exception) {
            return null; // 404, timeout, etc. — caller tries next repo
        }
    }

    // -------------------------------------------------------------------------
    // Property resolution
    // -------------------------------------------------------------------------

    /**
     * Builds a {@code "groupId:artifactId" → resolvedVersion} map from the POM's (merged)
     * dependencyManagement list.  Only entries whose versions can be fully resolved are included.
     */
    private @NotNull Map<String, String> buildManagedVersionMap(@NotNull final ParsedPom pom) {
        final Map<String, String> map = new HashMap<>();
        for (final PomDependency managed : pom.dependencyManagement()) {
            final String version = resolveProperties(managed.version(), pom);
            if (version != null) {
                map.put(managed.groupId() + ":" + managed.artifactId(), version);
            }
        }
        return map;
    }

    /**
     * Substitutes all {@code ${property}} references in {@code value} using the properties
     * from {@code pom}.  Returns {@code null} when the value is {@code null} or contains
     * unresolvable placeholders after substitution (so callers can decide whether to skip
     * the dependency).
     *
     * <p>The method loops until no further substitution is possible (handles chained
     * references such as {@code ${foo}} where {@code foo=bar} and {@code bar} is itself a
     * property) with a circuit-breaker to prevent infinite loops on circular definitions.
     */
    private @Nullable String resolveProperties(@Nullable final String value, @NotNull final ParsedPom pom) {
        if (value == null) return null;
        if (!value.contains("${")) return value;

        String current = value;
        int guard = 8; // max substitution rounds

        while (current.contains("${") && guard-- > 0) {
            final String substituted = performSingleSubstitutionPass(current, pom);
            if (substituted.equals(current)) {
                break; // No changes made
            }
            current = substituted;
        }

        // If unresolved placeholders remain, the version is unusable
        return current.contains("${") ? null : current;
    }

    /**
     * Performs a single pass of property substitution, replacing all ${...} placeholders.
     * Extracted to reduce cognitive complexity of resolveProperties.
     *
     * @param value the string to process
     * @param pom   the POM containing property definitions
     * @return the string with one round of substitutions applied
     */
    private @NotNull String performSingleSubstitutionPass(@NotNull final String value, @NotNull final ParsedPom pom) {
        final StringBuilder sb = new StringBuilder(value.length());
        int i = 0;

        while (i < value.length()) {
            final int start = value.indexOf("${", i);
            if (start == -1) {
                sb.append(value, i, value.length());
                break;
            }

            sb.append(value, i, start);
            final int end = value.indexOf('}', start + 2);

            if (end == -1) {
                // Unclosed placeholder — leave as-is
                sb.append(value, start, value.length());
                break;
            }

            final String placeholder = value.substring(start + 2, end);
            final String resolved = lookupProperty(placeholder, pom);

            if (resolved != null) {
                sb.append(resolved);
            } else {
                sb.append(value, start, end + 1); // keep placeholder intact
            }

            i = end + 1;
        }

        return sb.toString();
    }

    /**
     * Resolves a single property name against well-known Maven built-ins first, then against
     * the POM's merged property map.
     */
    private @Nullable String lookupProperty(@NotNull final String name, @NotNull final ParsedPom pom) {
        return switch (name) {
            case "project.version",          "version"          -> pom.version();
            case "project.groupId",          "groupId"          -> pom.groupId();
            case "project.artifactId",       "artifactId"       -> pom.artifactId();
            case "project.parent.version"                       ->
                    pom.parent() != null ? pom.parent().version() : null;
            case "project.parent.groupId"                       ->
                    pom.parent() != null ? pom.parent().groupId() : null;
            default                                             -> pom.properties().get(name);
        };
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static @NotNull String gavKey(@NotNull final DependencyCoordinate coord) {
        return coord.groupId() + ":" + coord.artifactId() + ":" + coord.version();
    }
}
