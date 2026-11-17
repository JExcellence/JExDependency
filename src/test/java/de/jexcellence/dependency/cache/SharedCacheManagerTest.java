package de.jexcellence.dependency.cache;

import de.jexcellence.dependency.model.DependencyCoordinate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SharedCacheManagerTest {

    private static final DependencyCoordinate COORDINATE =
            Objects.requireNonNull(DependencyCoordinate.parse("com.example:demo-lib:1.2.3"));

    @TempDir
    Path baseRoot;

    @Test
    @DisplayName("writes the layout marker into a new cache and uses the base root")
    void writesMarkerForNewCache() throws IOException {
        final SharedCacheManager cache = new SharedCacheManager(baseRoot);

        assertEquals(baseRoot, cache.getCacheRoot());
        final String marker = Files.readString(baseRoot.resolve(SharedCacheManager.FORMAT_FILE));
        assertTrue(marker.contains("format=" + SharedCacheManager.CACHE_FORMAT), marker);
    }

    @Test
    @DisplayName("keeps using the base root when the marker matches")
    void reusesMatchingLayout() {
        new SharedCacheManager(baseRoot);
        final SharedCacheManager second = new SharedCacheManager(baseRoot);

        assertEquals(baseRoot, second.getCacheRoot());
    }

    @Test
    @DisplayName("isolates itself when the cache was written by an unknown layout version")
    void isolatesUnknownLayout() throws IOException {
        Files.writeString(baseRoot.resolve(SharedCacheManager.FORMAT_FILE), "format=99\n");

        final SharedCacheManager cache = new SharedCacheManager(baseRoot);

        assertEquals(baseRoot.resolve("format-" + SharedCacheManager.CACHE_FORMAT), cache.getCacheRoot());
        assertTrue(cache.resolveJarPath(COORDINATE).startsWith(cache.getCacheRoot()));
        assertEquals("format=99\n", Files.readString(baseRoot.resolve(SharedCacheManager.FORMAT_FILE)));
    }

    @Test
    @DisplayName("rewrites an unreadable marker instead of failing")
    void rewritesCorruptMarker() throws IOException {
        Files.writeString(baseRoot.resolve(SharedCacheManager.FORMAT_FILE), "format=not-a-number\n");

        final SharedCacheManager cache = new SharedCacheManager(baseRoot);

        assertEquals(baseRoot, cache.getCacheRoot());
        assertTrue(Files.readString(baseRoot.resolve(SharedCacheManager.FORMAT_FILE))
                .contains("format=" + SharedCacheManager.CACHE_FORMAT));
    }

    @Test
    @DisplayName("downloads once when several threads of one JVM request the same artifact")
    void concurrentRequestsDownloadOnce() throws Exception {
        final SharedCacheManager cache = new SharedCacheManager(baseRoot);
        final Path jarPath = cache.resolveJarPath(COORDINATE);
        final AtomicInteger downloads = new AtomicInteger();
        final CountDownLatch start = new CountDownLatch(1);
        final int threads = 8;

        final ExecutorService executor = Executors.newFixedThreadPool(threads);
        try {
            final List<Future<Path>> results = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                results.add(executor.submit(() -> {
                    start.await();
                    return cache.getOrDownload(COORDINATE, () -> {
                        downloads.incrementAndGet();
                        sleepQuietly();
                        return writeJar(jarPath);
                    });
                }));
            }
            start.countDown();

            for (final Future<Path> result : results) {
                assertEquals(jarPath, result.get(30, TimeUnit.SECONDS));
            }
        } finally {
            executor.shutdownNow();
        }

        assertEquals(1, downloads.get());
    }

    @Test
    @DisplayName("returns null when the download action fails")
    void failedDownloadReturnsNull() {
        final SharedCacheManager cache = new SharedCacheManager(baseRoot);

        assertNull(cache.getOrDownload(COORDINATE, () -> false));
    }


    private static boolean writeJar(final Path jarPath) {
        try {
            Files.createDirectories(jarPath.getParent());
            Files.write(jarPath, new byte[]{0x50, 0x4B, 0x03, 0x04});
            return true;
        } catch (final IOException exception) {
            return false;
        }
    }

    private static void sleepQuietly() {
        try {
            Thread.sleep(200);
        } catch (final InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    @Test
    @DisplayName("isUsableJar accepts a real jar and rejects empty, HTML and missing files")
    void usableJarCheck() throws IOException {
        final Path jar = writeRealJar(baseRoot.resolve("ok.jar"));
        final Path empty = Files.createFile(baseRoot.resolve("empty.jar"));
        final Path html = Files.writeString(baseRoot.resolve("page.jar"), "<html>rate limited</html>");

        assertTrue(SharedCacheManager.isUsableJar(jar));
        assertFalse(SharedCacheManager.isUsableJar(empty));
        assertFalse(SharedCacheManager.isUsableJar(html));
        assertFalse(SharedCacheManager.isUsableJar(baseRoot.resolve("missing.jar")));
    }

    @Test
    @DisplayName("legacy migration skips a broken jar and copies a real one")
    void legacyMigrationChecksJar() throws IOException {
        final SharedCacheManager cache = new SharedCacheManager(baseRoot.resolve("cache"));
        final Path legacy = Files.createDirectories(baseRoot.resolve("legacy"));
        final Path legacyJar = legacy.resolve(COORDINATE.toFileName());

        Files.writeString(legacyJar, "<html>not a jar</html>");
        assertFalse(cache.migrateFromLegacy(COORDINATE, legacy.toFile()));
        assertFalse(Files.exists(cache.resolveJarPath(COORDINATE)));

        writeRealJar(legacyJar);
        assertTrue(cache.migrateFromLegacy(COORDINATE, legacy.toFile()));
        assertTrue(SharedCacheManager.isUsableJar(cache.resolveJarPath(COORDINATE)));
    }

    private static Path writeRealJar(final Path path) throws IOException {
        try (JarOutputStream out = new JarOutputStream(Files.newOutputStream(path))) {
            out.putNextEntry(new JarEntry("demo/Marker.class"));
            out.write(new byte[]{1, 2, 3});
            out.closeEntry();
        }
        return path;
    }
}
