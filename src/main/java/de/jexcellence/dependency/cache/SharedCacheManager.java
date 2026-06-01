package de.jexcellence.dependency.cache;

import de.jexcellence.dependency.model.DependencyCoordinate;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.Instant;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages a server-wide shared download cache for Maven artifacts. Instead of each plugin downloading
 * its own copy of a dependency, all plugins share a single cache directory keyed by GAV coordinates
 * (groupId/artifactId/version). This saves bandwidth and disk space while still allowing per-plugin
 * relocation (remapped jars remain in the plugin's own directory).
 *
 * <p>The cache root defaults to {@code {server-root}/.jexdependency/cache/} and can be overridden
 * via the system property {@code jedependency.cache.dir}.</p>
 *
 * <h2>Thread safety</h2>
 * <p>Concurrent access from multiple plugins is handled via {@link FileChannel}-based file locking.
 * A double-checked pattern avoids acquiring the lock when the artifact already exists on disk.</p>
 *
 * <h2>Directory layout</h2>
 * <pre>
 * .jexdependency/cache/
 *   jars/
 *     com/google/guava/33.2.1-jre/guava-33.2.1-jre.jar
 *   poms/
 *     com/google/guava/33.2.1-jre/guava-33.2.1-jre.pom
 *   locks/
 *     com.google.guava--33.2.1-jre.lock
 * </pre>
 */
public final class SharedCacheManager {

    private static final Logger LOGGER = Logger.getLogger("JExDependency");

    private static final String CACHE_DIR_PROPERTY = "jedependency.cache.dir";
    private static final String DEFAULT_CACHE_DIR = ".jexdependency/cache";
    private static final String JARS_SUBDIR = "jars";
    private static final String POMS_SUBDIR = "poms";
    private static final String LOCKS_SUBDIR = "locks";

    /**
     * Lock files older than this duration are considered stale (from a crashed JVM) and deleted
     * during initialization.
     */
    private static final Duration STALE_LOCK_THRESHOLD = Duration.ofHours(1);

    private final Path cacheRoot;
    private final Path jarsDir;
    private final Path pomsDir;
    private final Path locksDir;

    /**
     * Lazy singleton holder. The instance is created on first access and reused for the lifetime
     * of the JVM — there is exactly one shared cache per server.
     */
    private static final class Holder {
        static final SharedCacheManager INSTANCE = new SharedCacheManager();
    }

    /**
     * Returns the singleton cache manager instance.
     *
     * @return shared cache manager
     */
    public static @NotNull SharedCacheManager getInstance() {
        return Holder.INSTANCE;
    }

    private SharedCacheManager() {
        final String customDir = System.getProperty(CACHE_DIR_PROPERTY);
        if (customDir != null && !customDir.trim().isEmpty()) {
            this.cacheRoot = Path.of(customDir.trim());
        } else {
            this.cacheRoot = Path.of(System.getProperty("user.dir"), DEFAULT_CACHE_DIR);
        }

        this.jarsDir = cacheRoot.resolve(JARS_SUBDIR);
        this.pomsDir = cacheRoot.resolve(POMS_SUBDIR);
        this.locksDir = cacheRoot.resolve(LOCKS_SUBDIR);

        ensureDirectories();
        cleanStaleLocks();
    }

    // -------------------------------------------------------------------------
    // Path resolution
    // -------------------------------------------------------------------------

    /**
     * Resolves the path where a JAR artifact should be cached, following the standard Maven
     * repository layout: {@code jars/groupId-as-dirs/artifactId/version/artifactId-version.jar}.
     *
     * @param coordinate the artifact coordinate
     * @return absolute path to the cached JAR file (may or may not exist yet)
     */
    public @NotNull Path resolveJarPath(@NotNull final DependencyCoordinate coordinate) {
        return jarsDir.resolve(coordinate.toRepositoryPath());
    }

    /**
     * Resolves the path where a POM file should be cached, using the same GAV directory layout
     * as JARs but with a {@code .pom} extension.
     *
     * @param coordinate the artifact coordinate
     * @return absolute path to the cached POM file (may or may not exist yet)
     */
    public @NotNull Path resolvePomPath(@NotNull final DependencyCoordinate coordinate) {
        final String pomPath = coordinate.groupId().replace('.', '/')
                + '/' + coordinate.artifactId()
                + '/' + coordinate.version()
                + '/' + coordinate.artifactId() + '-' + coordinate.version() + ".pom";
        return pomsDir.resolve(pomPath);
    }

    /**
     * Returns the directory containing cached JAR files. This is the root of the Maven-layout
     * directory tree under the shared cache.
     *
     * @return path to the shared JAR cache directory
     */
    public @NotNull File getJarsCacheDirectory() {
        return jarsDir.toFile();
    }

    /**
     * Returns the directory containing cached POM files.
     *
     * @return path to the shared POM cache directory
     */
    public @NotNull File getPomsCacheDirectory() {
        return pomsDir.toFile();
    }

    /**
     * Returns the root directory of the shared cache.
     *
     * @return cache root path
     */
    public @NotNull Path getCacheRoot() {
        return cacheRoot;
    }

    // -------------------------------------------------------------------------
    // Locked download
    // -------------------------------------------------------------------------

    /**
     * Returns the cached JAR file for the given coordinate if it already exists and is valid.
     * If the file is missing, acquires a file lock, double-checks, and invokes the download
     * supplier to populate the cache. The supplier should download the artifact and return
     * {@code true} on success.
     *
     * <p>This method is safe to call concurrently from multiple plugins — file-channel locking
     * ensures only one download per artifact at a time.</p>
     *
     * @param coordinate     the artifact to resolve
     * @param downloadAction a supplier that downloads the artifact to the path returned by
     *                       {@link #resolveJarPath}; returns {@code true} on success
     * @return the path to the cached JAR, or {@code null} if the download failed
     */
    public Path getOrDownload(
            @NotNull final DependencyCoordinate coordinate,
            @NotNull final Supplier<Boolean> downloadAction
    ) {
        final Path jarPath = resolveJarPath(coordinate);

        // Fast path: already cached
        if (isValidFile(jarPath)) {
            return jarPath;
        }

        // Ensure parent directories exist
        try {
            Files.createDirectories(jarPath.getParent());
        } catch (final IOException exception) {
            LOGGER.log(Level.WARNING, "Failed to create cache directory: {0}", jarPath.getParent());
            return null;
        }

        // Acquire lock and double-check
        final Path lockFile = locksDir.resolve(sanitizeLockName(coordinate));

        try {
            Files.createDirectories(locksDir);
        } catch (final IOException exception) {
            LOGGER.log(Level.FINE, exception, () -> "Failed to create locks directory");
        }

        try (final FileChannel channel = FileChannel.open(lockFile,
                StandardOpenOption.CREATE, StandardOpenOption.WRITE);
             final FileLock ignored = channel.lock()) {

            // Double-check after acquiring lock
            if (isValidFile(jarPath)) {
                return jarPath;
            }

            // Perform the actual download
            final boolean success = Boolean.TRUE.equals(downloadAction.get());

            if (success && isValidFile(jarPath)) {
                return jarPath;
            }

            return null;

        } catch (final IOException exception) {
            LOGGER.log(Level.WARNING, exception, () -> "Failed to acquire cache lock for: " + coordinate.toGavString());
            // Fall through to direct download without locking
            final boolean success = Boolean.TRUE.equals(downloadAction.get());
            return (success && isValidFile(jarPath)) ? jarPath : null;
        } finally {
            safeDelete(lockFile);
        }
    }

    // -------------------------------------------------------------------------
    // Migration support
    // -------------------------------------------------------------------------

    /**
     * Checks a legacy per-plugin cache directory for an existing artifact and copies it to the
     * shared cache if found. This enables transparent migration from the old per-plugin layout
     * without re-downloading.
     *
     * @param coordinate     the artifact to look for
     * @param legacyDirectory the old per-plugin libraries directory
     * @return {@code true} if the artifact was found in the legacy location and migrated
     */
    public boolean migrateFromLegacy(
            @NotNull final DependencyCoordinate coordinate,
            @NotNull final File legacyDirectory
    ) {
        final File legacyFile = new File(legacyDirectory, coordinate.toFileName());
        if (!legacyFile.isFile() || legacyFile.length() <= 0) {
            return false;
        }

        final Path targetPath = resolveJarPath(coordinate);
        if (isValidFile(targetPath)) {
            return true; // Already in shared cache
        }

        try {
            Files.createDirectories(targetPath.getParent());
            Files.copy(legacyFile.toPath(), targetPath);
            LOGGER.log(Level.FINE, () -> "Migrated to shared cache: " + coordinate.toGavString());
            return true;
        } catch (final IOException exception) {
            LOGGER.log(Level.FINE, exception, () -> "Failed to migrate from legacy cache: " + coordinate.toGavString());
            return false;
        }
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private static boolean isValidFile(@NotNull final Path path) {
        return Files.isRegularFile(path) && path.toFile().length() > 0;
    }

    private static @NotNull String sanitizeLockName(@NotNull final DependencyCoordinate coordinate) {
        return coordinate.groupId() + "--" + coordinate.artifactId() + "--" + coordinate.version() + ".lock";
    }

    private void ensureDirectories() {
        try {
            Files.createDirectories(jarsDir);
            Files.createDirectories(pomsDir);
            Files.createDirectories(locksDir);
        } catch (final IOException exception) {
            LOGGER.log(Level.SEVERE, exception, () -> "Failed to create shared cache directories at: " + cacheRoot);
        }
    }

    /**
     * Removes lock files older than {@link #STALE_LOCK_THRESHOLD} to recover from JVM crashes
     * that left locks on disk. OS-level file locks are released on process termination, so these
     * files are safe to delete.
     */
    private void cleanStaleLocks() {
        if (!Files.isDirectory(locksDir)) {
            return;
        }

        try (final var files = Files.list(locksDir)) {
            final Instant threshold = Instant.now().minus(STALE_LOCK_THRESHOLD);

            files.filter(p -> p.toString().endsWith(".lock"))
                    .forEach(lockFile -> {
                        try {
                            final Instant modified = Files.getLastModifiedTime(lockFile).toInstant();
                            if (modified.isBefore(threshold)) {
                                Files.deleteIfExists(lockFile);
                                LOGGER.log(Level.FINE, () -> "Cleaned stale lock: " + lockFile.getFileName());
                            }
                        } catch (final IOException exception) {
                            // Ignore — best effort cleanup
                        }
                    });
        } catch (final IOException exception) {
            LOGGER.log(Level.FINE, exception, () -> "Failed to clean stale locks");
        }
    }

    private static void safeDelete(@NotNull final Path file) {
        try {
            Files.deleteIfExists(file);
        } catch (final IOException exception) {
            // Ignore — lock files are cleaned up on next startup
        }
    }
}
