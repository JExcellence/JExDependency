package de.jexcellence.dependency.cache;

import de.jexcellence.dependency.model.DependencyCoordinate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.Properties;
import java.util.function.BooleanSupplier;
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
 * A double-checked pattern avoids acquiring the lock when the artifact already exists on disk. File
 * locks belong to the whole JVM, so two plugins in the same server that want the same artifact wait
 * for each other instead of failing with {@link OverlappingFileLockException}.</p>
 *
 * <h2>Layout versioning</h2>
 * <p>The cache root contains {@value #FORMAT_FILE} with the layout version ({@value #CACHE_FORMAT}).
 * Plugins often shade different JExDependency versions; when a library finds a layout version it does
 * not know, it keeps its files in {@code format-<version>/} below the root instead of reading or
 * writing a layout it does not understand. A future layout change must raise {@link #CACHE_FORMAT}.</p>
 *
 * <h2>Directory layout</h2>
 * <pre>
 * .jexdependency/cache/
 *   cache-format.properties
 *   jars/
 *     com/google/guava/guava/33.2.1-jre/guava-33.2.1-jre.jar
 *   poms/
 *     com/google/guava/guava/33.2.1-jre/guava-33.2.1-jre.pom
 *   locks/
 *     com.google.guava--guava--33.2.1-jre.lock
 *   format-&lt;n&gt;/      only when a newer layout owns the root; same structure inside
 * </pre>
 */
public final class SharedCacheManager {

    /** Layout version written to and expected in {@value #FORMAT_FILE}. */
    static final int CACHE_FORMAT = 1;

    /** Marker file in the cache root that records the layout version. */
    static final String FORMAT_FILE = "cache-format.properties";

    private static final Logger LOGGER = Logger.getLogger("JExDependency");

    private static final String CACHE_DIR_PROPERTY = "jedependency.cache.dir";
    private static final String DEFAULT_CACHE_DIR = ".jexdependency/cache";
    private static final String FORMAT_KEY = "format";
    private static final String JARS_SUBDIR = "jars";
    private static final String POMS_SUBDIR = "poms";
    private static final String LOCKS_SUBDIR = "locks";

    /**
     * How long a thread waits for another thread in this JVM that holds the same artifact lock. Longer than the
     * worst case of one download walking every repository with retries; on timeout the waiter gives up instead of
     * downloading without the lock, because both would write the same temporary file.
     */
    private static final Duration IN_PROCESS_LOCK_WAIT = Duration.ofMinutes(10);
    private static final long LOCK_POLL_MILLIS = 100L;

    private final Path cacheRoot;
    private final Path jarsDir;
    private final Path pomsDir;
    private final Path locksDir;

    /**
     * Lazy singleton holder. The instance is created on first access and reused for the lifetime
     * of the JVM: there is exactly one shared cache per server.
     */
    private static final class Holder {
        static final SharedCacheManager INSTANCE = new SharedCacheManager(defaultBaseRoot());
    }

    /**
     * Returns the singleton cache manager instance.
     *
     * <p>The singleton is intentional: there must be exactly one shared on-disk
     * cache (and one set of file locks) per JVM so that multiple JExcellence
     * plugins coordinate downloads instead of racing each other. The
     * initialization-on-demand Holder idiom gives lazy, thread-safe creation.
     *
     * @return shared cache manager
     */
    @SuppressWarnings("java:S6548") // Deliberate, required JVM-wide shared cache, see Javadoc.
    public static @NotNull SharedCacheManager getInstance() {
        return Holder.INSTANCE;
    }

    /**
     * Creates a cache manager below the given base directory. Package-private for tests; production
     * code uses {@link #getInstance()}.
     *
     * @param baseRoot directory that holds (or will hold) the shared cache
     */
    SharedCacheManager(@NotNull final Path baseRoot) {
        this.cacheRoot = selectLayoutRoot(baseRoot);
        this.jarsDir = cacheRoot.resolve(JARS_SUBDIR);
        this.pomsDir = cacheRoot.resolve(POMS_SUBDIR);
        this.locksDir = cacheRoot.resolve(LOCKS_SUBDIR);

        ensureDirectories();
    }

    private static @NotNull Path defaultBaseRoot() {
        final String customDir = System.getProperty(CACHE_DIR_PROPERTY);
        if (customDir != null && !customDir.trim().isEmpty()) {
            return Path.of(customDir.trim());
        }
        return Path.of(System.getProperty("user.dir"), DEFAULT_CACHE_DIR);
    }

    // -------------------------------------------------------------------------
    // Layout versioning
    // -------------------------------------------------------------------------

    /**
     * Returns the directory this library version may use: the base root when its layout version
     * matches (or is not recorded yet), otherwise an isolated {@code format-<n>} directory.
     */
    private static @NotNull Path selectLayoutRoot(@NotNull final Path baseRoot) {
        final Path formatFile = baseRoot.resolve(FORMAT_FILE);
        final Integer recordedFormat = readFormat(formatFile);

        if (recordedFormat == null) {
            writeFormat(baseRoot, formatFile);
            return baseRoot;
        }
        if (recordedFormat == CACHE_FORMAT) {
            return baseRoot;
        }

        final Path isolatedRoot = baseRoot.resolve("format-" + CACHE_FORMAT);
        LOGGER.log(Level.INFO, () -> "Shared cache at " + baseRoot + " uses layout " + recordedFormat
                + "; this JExDependency version uses layout " + CACHE_FORMAT + " and caches in " + isolatedRoot);
        return isolatedRoot;
    }

    private static @Nullable Integer readFormat(@NotNull final Path formatFile) {
        if (!Files.isRegularFile(formatFile)) {
            return null;
        }

        final Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(formatFile)) {
            properties.load(input);
            final String value = properties.getProperty(FORMAT_KEY);
            return value == null ? null : Integer.valueOf(value.trim());
        } catch (final IOException | NumberFormatException exception) {
            LOGGER.log(Level.WARNING, exception, () -> "Unreadable cache layout marker, rewriting: " + formatFile);
            return null;
        }
    }

    private static void writeFormat(@NotNull final Path baseRoot, @NotNull final Path formatFile) {
        final Properties properties = new Properties();
        properties.setProperty(FORMAT_KEY, Integer.toString(CACHE_FORMAT));

        try {
            Files.createDirectories(baseRoot);
            final Path tempFile = Files.createTempFile(baseRoot, FORMAT_FILE, ".tmp");
            try {
                try (OutputStream output = Files.newOutputStream(tempFile)) {
                    properties.store(output, "JExDependency shared cache layout. Do not edit.");
                }
                moveReplacing(tempFile, formatFile);
            } finally {
                Files.deleteIfExists(tempFile);
            }
        } catch (final IOException exception) {
            LOGGER.log(Level.WARNING, exception, () -> "Failed to write cache layout marker: " + formatFile);
        }
    }

    private static void moveReplacing(@NotNull final Path source, @NotNull final Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (final AtomicMoveNotSupportedException exception) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
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
     * Returns the root directory of the shared cache that this library version uses.
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
     * <p>This method is safe to call concurrently from multiple plugins and threads: file-channel
     * locking ensures only one download per artifact at a time.</p>
     *
     * @param coordinate     the artifact to resolve
     * @param downloadAction a supplier that downloads the artifact to the path returned by
     *                       {@link #resolveJarPath}; returns {@code true} on success
     * @return the path to the cached JAR, or {@code null} if the download failed, the cache directories could not
     *         be created, or the thread was interrupted or gave up while waiting for the lock
     */
    public @Nullable Path getOrDownload(
            @NotNull final DependencyCoordinate coordinate,
            @NotNull final BooleanSupplier downloadAction
    ) {
        final Path jarPath = resolveJarPath(coordinate);

        if (isValidFile(jarPath)) {
            return jarPath;
        }

        try {
            Files.createDirectories(jarPath.getParent());
            Files.createDirectories(locksDir);
        } catch (final IOException exception) {
            LOGGER.log(Level.WARNING, exception, () -> "Failed to create cache directories for: " + jarPath);
            return null;
        }

        // Lock files are never deleted: removing one while another process waits on it would let
        // a third process lock a fresh file and download concurrently. They are empty and one per artifact.
        final Path lockFile = locksDir.resolve(lockName(coordinate));

        try (FileChannel channel = FileChannel.open(lockFile, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
             FileLock lock = acquireLock(channel)) {

            if (isValidFile(jarPath)) {
                return jarPath;
            }
            if (lock == null) {
                LOGGER.log(Level.WARNING, () -> "Gave up waiting for another download of " + coordinate.toGavString());
                return null;
            }
            return runDownload(jarPath, downloadAction);

        } catch (final IOException exception) {
            LOGGER.log(Level.WARNING, exception,
                    () -> "Cache lock unavailable, downloading without it: " + coordinate.toGavString());
            return isValidFile(jarPath) ? jarPath : runDownload(jarPath, downloadAction);
        } catch (final InterruptedException exception) {
            Thread.currentThread().interrupt();
            LOGGER.log(Level.WARNING, () -> "Interrupted while waiting for cache lock: " + coordinate.toGavString());
            return null;
        }
    }

    /**
     * Blocks until the file lock is held. Another process holding it makes {@link FileChannel#lock()}
     * wait; another thread of this JVM holding it raises {@link OverlappingFileLockException}, so that
     * case is polled until {@link #IN_PROCESS_LOCK_WAIT} expires.
     *
     * @return the held lock, or {@code null} when another thread of this JVM kept it past the wait limit
     */
    private static @Nullable FileLock acquireLock(@NotNull final FileChannel channel)
            throws IOException, InterruptedException {
        final long deadline = System.nanoTime() + IN_PROCESS_LOCK_WAIT.toNanos();
        while (true) {
            try {
                return channel.lock();
            } catch (final OverlappingFileLockException exception) {
                if (System.nanoTime() >= deadline) {
                    return null;
                }
                Thread.sleep(LOCK_POLL_MILLIS);
            }
        }
    }

    private static @Nullable Path runDownload(@NotNull final Path jarPath, @NotNull final BooleanSupplier action) {
        return action.getAsBoolean() && isValidFile(jarPath) ? jarPath : null;
    }

    // -------------------------------------------------------------------------
    // Migration support
    // -------------------------------------------------------------------------

    /**
     * Copies a JAR from a plugin's legacy per-plugin library directory into the shared cache.
     *
     * @param coordinate      the artifact coordinate
     * @param legacyDirectory the plugin's old library directory
     * @return {@code true} if the artifact is now in the shared cache
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
            return true;
        }

        try {
            Files.createDirectories(targetPath.getParent());
            final Path tempFile = Files.createTempFile(targetPath.getParent(), targetPath.getFileName().toString(), ".part");
            try {
                Files.copy(legacyFile.toPath(), tempFile, StandardCopyOption.REPLACE_EXISTING);
                moveReplacing(tempFile, targetPath);
            } finally {
                Files.deleteIfExists(tempFile);
            }
            LOGGER.log(Level.FINE, () -> "Migrated to shared cache: " + coordinate.toGavString());
            return true;
        } catch (final IOException exception) {
            LOGGER.log(Level.FINE, exception, () -> "Failed to migrate from legacy cache: " + coordinate.toGavString());
            return isValidFile(targetPath);
        }
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private static boolean isValidFile(@NotNull final Path path) {
        return Files.isRegularFile(path) && path.toFile().length() > 0;
    }

    private static @NotNull String lockName(@NotNull final DependencyCoordinate coordinate) {
        final String classifier = coordinate.classifier();
        final String classifierPart = classifier == null || classifier.isEmpty() ? "" : "--" + classifier;
        return coordinate.groupId() + "--" + coordinate.artifactId() + "--" + coordinate.version() + classifierPart + ".lock";
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
}
