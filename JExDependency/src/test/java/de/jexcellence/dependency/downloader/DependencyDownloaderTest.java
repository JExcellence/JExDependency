package de.jexcellence.dependency.downloader;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import de.jexcellence.dependency.model.DependencyCoordinate;
import de.jexcellence.dependency.model.DownloadResult;
import de.jexcellence.dependency.repository.RepositoryType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DependencyDownloaderTest {

    private static final DependencyCoordinate VALID_COORDINATE = new DependencyCoordinate("test.valid", "demo", "1.0.0");
    private static final DependencyCoordinate ASYNC_COORDINATE = new DependencyCoordinate("test.async", "demo", "1.0.0");
    private static final DependencyCoordinate INVALID_COORDINATE = new DependencyCoordinate("test.invalid", "demo", "1.0.0");
    private static final DependencyCoordinate MISMATCH_COORDINATE = new DependencyCoordinate("test.mismatch", "demo", "1.0.0");
    private static final DependencyCoordinate REDIRECT_COORDINATE = new DependencyCoordinate("test.redirect", "demo", "1.0.0");

    private static final String CUSTOM_REPO_PREFIX = "/repo/custom/";
    private static final String VALID_PATH = CUSTOM_REPO_PREFIX + VALID_COORDINATE.toRepositoryPath();
    private static final String ASYNC_PATH = CUSTOM_REPO_PREFIX + ASYNC_COORDINATE.toRepositoryPath();
    private static final String INVALID_PATH = CUSTOM_REPO_PREFIX + INVALID_COORDINATE.toRepositoryPath();
    private static final String MISMATCH_PATH = CUSTOM_REPO_PREFIX + MISMATCH_COORDINATE.toRepositoryPath();
    private static final String REDIRECT_START_PATH = CUSTOM_REPO_PREFIX + REDIRECT_COORDINATE.toRepositoryPath();
    private static final String REDIRECT_STEP_PREFIX = "/redirect/step/";

    private static final Map<String, AtomicInteger> REQUEST_COUNTS = new ConcurrentHashMap<>();

    private static HttpServer server;
    private static String repositoryBaseUrl;
    private static byte[] validJarBytes;
    private static byte[] invalidJarBytes;
    private static byte[] truncatedJarBytes;
    private static Map<RepositoryType, String> originalRepositoryBases;
    private static Field repositoryBaseField;

    private DependencyDownloader downloader;
    private Path tempDirectory;

    @BeforeAll
    static void beforeAll() throws Exception {
        validJarBytes = createJarBytes();
        invalidJarBytes = createInvalidBytes(validJarBytes.length + 256);
        truncatedJarBytes = Arrays.copyOf(validJarBytes, validJarBytes.length - 256);

        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/", exchange -> {
            try {
                handleRequest(exchange);
            } finally {
                exchange.close();
            }
        });
        server.start();

        repositoryBaseUrl = "http://" + server.getAddress().getHostString() + ":" + server.getAddress().getPort();

        redirectRepositoriesToLocalServer();
    }

    @AfterAll
    static void afterAll() throws Exception {
        restoreRepositoryBases();
        if (server != null) {
            server.stop(0);
        }
    }

    @BeforeEach
    void setUp() throws IOException {
        REQUEST_COUNTS.clear();
        downloader = new DependencyDownloader();
        downloader.addRepository(repositoryBaseUrl + CUSTOM_REPO_PREFIX);
        tempDirectory = Files.createTempDirectory("dependency-downloader-test");
    }

    @AfterEach
    void tearDown() throws Exception {
        if (downloader != null) {
            downloader.shutdown();
        }
        if (tempDirectory != null) {
            deleteDirectory(tempDirectory);
        }
    }

    @Test
    void downloadReturnsSuccessWhenCustomRepositoryProvidesJar() throws IOException {
        final DownloadResult result = downloader.download(VALID_COORDINATE, tempDirectory.toFile());

        assertTrue(result.success());
        assertNotNull(result.file());
        final byte[] downloadedBytes = Files.readAllBytes(result.file().toPath());
        assertArrayEquals(validJarBytes, downloadedBytes);
        assertEquals(1, getRequestCount(VALID_PATH));
    }

    @Test
    void downloadAsyncReturnsSuccessFromCustomRepository() throws Exception {
        final DownloadResult result = downloader.downloadAsync(ASYNC_COORDINATE, tempDirectory.toFile()).get(5, TimeUnit.SECONDS);

        assertTrue(result.success());
        assertNotNull(result.file());
        final byte[] downloadedBytes = Files.readAllBytes(result.file().toPath());
        assertArrayEquals(validJarBytes, downloadedBytes);
        assertEquals(1, getRequestCount(ASYNC_PATH));
    }

    @Test
    void downloadReturnsFailureForInvalidJarResponse() {
        final DownloadResult result = downloader.download(INVALID_COORDINATE, tempDirectory.toFile());

        assertFalse(result.success());
        assertEquals("Failed to download from any repository", result.errorMessage());
        assertTrue(Files.notExists(tempDirectory.resolve(INVALID_COORDINATE.toFileName())));
        assertEquals(1, getRequestCount(INVALID_PATH));
    }

    @Test
    void downloadReturnsFailureWhenContentLengthMismatch() {
        final DownloadResult result = downloader.download(MISMATCH_COORDINATE, tempDirectory.toFile());

        assertFalse(result.success());
        assertEquals("Failed to download from any repository", result.errorMessage());
        assertTrue(Files.notExists(tempDirectory.resolve(MISMATCH_COORDINATE.toFileName())));
        assertEquals(1, getRequestCount(MISMATCH_PATH));
    }

    @Test
    void downloadReturnsFailureWhenRedirectLimitExceeded() {
        final DownloadResult result = downloader.download(REDIRECT_COORDINATE, tempDirectory.toFile());

        assertFalse(result.success());
        assertEquals("Failed to download from any repository", result.errorMessage());
        assertTrue(Files.notExists(tempDirectory.resolve(REDIRECT_COORDINATE.toFileName())));
        assertTrue(getRequestCount(REDIRECT_START_PATH) > 0);
    }

    @Test
    void downloadSkipsNetworkWhenCachedFileIsValid() throws IOException {
        final DownloadResult initial = downloader.download(VALID_COORDINATE, tempDirectory.toFile());
        assertTrue(initial.success());
        assertEquals(1, getRequestCount(VALID_PATH));

        final DownloadResult cached = downloader.download(VALID_COORDINATE, tempDirectory.toFile());
        assertTrue(cached.success());
        assertEquals(1, getRequestCount(VALID_PATH));
    }

    @Test
    void shutdownStopsExecutor() throws Exception {
        final Field executorField = DependencyDownloader.class.getDeclaredField("executorService");
        executorField.setAccessible(true);
        final ExecutorService executor = (ExecutorService) executorField.get(downloader);

        downloader.shutdown();

        assertTrue(executor.isShutdown());
        assertTrue(executor.awaitTermination(1, TimeUnit.SECONDS));
    }

    private static void handleRequest(final HttpExchange exchange) throws IOException {
        final String path = exchange.getRequestURI().getPath();
        REQUEST_COUNTS.computeIfAbsent(path, ignored -> new AtomicInteger()).incrementAndGet();

        if (Objects.equals(path, VALID_PATH) || Objects.equals(path, ASYNC_PATH)) {
            respondWithJar(exchange, validJarBytes);
            return;
        }

        if (Objects.equals(path, INVALID_PATH)) {
            respondWithBytes(exchange, invalidJarBytes, invalidJarBytes.length);
            return;
        }

        if (Objects.equals(path, MISMATCH_PATH)) {
            respondWithBytes(exchange, truncatedJarBytes, truncatedJarBytes.length + 512);
            return;
        }

        if (Objects.equals(path, REDIRECT_START_PATH)) {
            respondWithRedirect(exchange, repositoryBaseUrl + REDIRECT_STEP_PREFIX + "1");
            return;
        }

        if (path.startsWith(REDIRECT_STEP_PREFIX)) {
            final int currentStep = Integer.parseInt(path.substring(REDIRECT_STEP_PREFIX.length()));
            respondWithRedirect(exchange, repositoryBaseUrl + REDIRECT_STEP_PREFIX + (currentStep + 1));
            return;
        }

        exchange.sendResponseHeaders(404, -1);
    }

    private static void respondWithJar(final HttpExchange exchange, final byte[] bytes) throws IOException {
        exchange.getResponseHeaders().add("Content-Type", "application/java-archive");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(bytes);
        }
    }

    private static void respondWithBytes(final HttpExchange exchange, final byte[] bytes, final int reportedLength) throws IOException {
        exchange.getResponseHeaders().add("Content-Type", "application/java-archive");
        exchange.sendResponseHeaders(200, reportedLength);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(bytes);
        }
    }

    private static void respondWithRedirect(final HttpExchange exchange, final String location) throws IOException {
        exchange.getResponseHeaders().add("Location", location);
        exchange.sendResponseHeaders(302, -1);
    }

    private static byte[] createJarBytes() throws IOException {
        final Manifest manifest = new Manifest();
        final Attributes attributes = manifest.getMainAttributes();
        attributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");

        try (var outputStream = new java.io.ByteArrayOutputStream();
             var jarOutputStream = new JarOutputStream(outputStream, manifest)) {
            final JarEntry entry = new JarEntry("test.txt");
            entry.setTime(0L);
            jarOutputStream.putNextEntry(entry);
            final byte[] content = new byte[4096];
            final java.util.Random random = new java.util.Random(42L);
            random.nextBytes(content);
            jarOutputStream.write(content);
            jarOutputStream.closeEntry();
            jarOutputStream.finish();
            return outputStream.toByteArray();
        }
    }

    private static byte[] createInvalidBytes(final int size) {
        final byte[] bytes = new byte[size];
        Arrays.fill(bytes, (byte) 2);
        return bytes;
    }

    private static void deleteDirectory(final Path root) throws IOException {
        if (!Files.exists(root)) {
            return;
        }

        try (var walk = Files.walk(root)) {
            walk.sorted((first, second) -> second.compareTo(first))
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (final IOException ignored) {
                            // Ignored for test cleanup
                        }
                    });
        }
    }

    private static int getRequestCount(final String path) {
        final AtomicInteger counter = REQUEST_COUNTS.get(path);
        return counter == null ? 0 : counter.get();
    }

    private static void redirectRepositoriesToLocalServer() throws Exception {
        repositoryBaseField = RepositoryType.class.getDeclaredField("baseUrl");
        repositoryBaseField.setAccessible(true);
        originalRepositoryBases = new EnumMap<>(RepositoryType.class);

        for (final RepositoryType type : RepositoryType.values()) {
            originalRepositoryBases.put(type, type.getBaseUrl());
            final String redirectedBase = repositoryBaseUrl + "/unused/" + type.name().toLowerCase(Locale.ROOT) + "/";
            repositoryBaseField.set(type, redirectedBase);
        }
    }

    private static void restoreRepositoryBases() throws Exception {
        if (repositoryBaseField != null && originalRepositoryBases != null) {
            for (final Map.Entry<RepositoryType, String> entry : originalRepositoryBases.entrySet()) {
                repositoryBaseField.set(entry.getKey(), entry.getValue());
            }
        }
    }
}
