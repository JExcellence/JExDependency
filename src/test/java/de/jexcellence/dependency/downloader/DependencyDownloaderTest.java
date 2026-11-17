package de.jexcellence.dependency.downloader;

import com.sun.net.httpserver.HttpServer;
import de.jexcellence.dependency.loader.DependencyDescriptor;
import de.jexcellence.dependency.loader.DependencyEntry;
import de.jexcellence.dependency.model.DependencyCoordinate;
import de.jexcellence.dependency.model.DownloadResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Runs the downloader against local HTTP servers; nothing leaves the machine. */
class DependencyDownloaderTest {

    private final Map<String, Response> routes = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> hits = new ConcurrentHashMap<>();
    private HttpServer server;
    private String baseUrl;
    private DependencyDownloader downloader;

    @TempDir
    Path legacyDirectory;

    private record Response(int status, byte[] body, Map<String, String> headers) {
    }

    @BeforeEach
    void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            final String path = exchange.getRequestURI().getPath();
            hits.computeIfAbsent(path, key -> new AtomicInteger()).incrementAndGet();
            final Response response = routes.getOrDefault(path, new Response(404, new byte[0], Map.of()));
            response.headers().forEach((name, value) -> exchange.getResponseHeaders().add(name, value));
            exchange.sendResponseHeaders(response.status(), response.body().length == 0 ? -1 : response.body().length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(response.body());
            }
        });
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort() + "/maven";
        downloader = new DependencyDownloader(false, name -> null).setMaxRetries(2);
    }

    @AfterEach
    void stop() {
        downloader.shutdown();
        server.stop(0);
    }

    private static DependencyCoordinate uniqueCoordinate() {
        return new DependencyCoordinate("test.jexdependency", "demo", UUID.randomUUID().toString());
    }

    private static byte[] jar() throws IOException {
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (JarOutputStream out = new JarOutputStream(bytes)) {
            out.putNextEntry(new JarEntry("demo/Marker.class"));
            out.write(UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8));
            out.closeEntry();
        }
        return bytes.toByteArray();
    }

    private static String hex(final byte[] data, final String algorithm) throws Exception {
        final StringBuilder builder = new StringBuilder();
        for (final byte value : MessageDigest.getInstance(algorithm).digest(data)) {
            builder.append(String.format("%02x", value));
        }
        return builder.toString();
    }

    private String jarPath(final DependencyCoordinate coordinate) {
        return "/maven/" + coordinate.toRepositoryPath();
    }

    private void serve(final String path, final byte[] body) {
        routes.put(path, new Response(200, body, Map.of()));
    }

    private void useRepository() {
        downloader.configure(new DependencyDescriptor(List.of(), List.of(baseUrl), false, List.of()));
    }

    @Test
    @DisplayName("downloads from a declared repository and checks the published SHA-256")
    void downloadsAndVerifiesSha256() throws Exception {
        final DependencyCoordinate coordinate = uniqueCoordinate();
        final byte[] jar = jar();
        serve(jarPath(coordinate), jar);
        serve(jarPath(coordinate) + ".sha256", hex(jar, "SHA-256").getBytes(StandardCharsets.US_ASCII));
        useRepository();

        final DownloadResult result = downloader.download(coordinate, legacyDirectory.toFile());

        assertTrue(result.success(), String.valueOf(result.errorMessage()));
        assertArrayEquals(jar, Files.readAllBytes(result.file().toPath()));
        assertEquals(1, hits.get(jarPath(coordinate) + ".sha256").get());
        assertEquals(null, hits.get(jarPath(coordinate) + ".sha1"));
    }

    @Test
    @DisplayName("rejects a jar whose published SHA-1 does not match")
    void rejectsSha1Mismatch() throws Exception {
        final DependencyCoordinate coordinate = uniqueCoordinate();
        serve(jarPath(coordinate), jar());
        serve(jarPath(coordinate) + ".sha1", "0".repeat(40).getBytes(StandardCharsets.US_ASCII));
        useRepository();

        assertFalse(downloader.download(coordinate, legacyDirectory.toFile()).success());
    }

    @Test
    @DisplayName("a pinned SHA-256 decides, and a pinned repository is the only one contacted")
    void pinnedHashAndRepository() throws Exception {
        final DependencyCoordinate coordinate = uniqueCoordinate();
        final byte[] jar = jar();
        serve(jarPath(coordinate), jar);
        // A wrong published checksum must not matter when the descriptor pins the hash.
        serve(jarPath(coordinate) + ".sha256", "f".repeat(64).getBytes(StandardCharsets.US_ASCII));

        downloader.addRepository("http://127.0.0.1:1/never-contacted");
        downloader.configure(new DependencyDescriptor(
                List.of(new DependencyEntry(coordinate, baseUrl, hex(jar, "SHA-256"))), List.of(), false, List.of()));

        assertEquals(List.of(baseUrl + "/"), downloader.repositoryUrls(coordinate));
        assertTrue(downloader.download(coordinate, legacyDirectory.toFile()).success());
    }

    @Test
    @DisplayName("a pinned SHA-256 that does not match fails the download")
    void pinnedHashMismatch() throws Exception {
        final DependencyCoordinate coordinate = uniqueCoordinate();
        serve(jarPath(coordinate), jar());
        downloader.configure(new DependencyDescriptor(
                List.of(new DependencyEntry(coordinate, baseUrl, "0".repeat(64))), List.of(), false, List.of()));

        assertFalse(downloader.download(coordinate, legacyDirectory.toFile()).success());
    }

    @Test
    @DisplayName("retries a 503 and succeeds on the next attempt")
    void retriesServerErrors() throws Exception {
        final DependencyCoordinate coordinate = uniqueCoordinate();
        final byte[] jar = jar();
        final String path = jarPath(coordinate);
        final AtomicInteger calls = new AtomicInteger();
        server.createContext(path, exchange -> {
            if (!exchange.getRequestURI().getPath().equals(path)) {
                // Contexts match by prefix; checksum requests get a 404.
                exchange.sendResponseHeaders(404, -1);
                exchange.close();
                return;
            }
            final boolean first = calls.getAndIncrement() == 0;
            final byte[] body = first ? new byte[0] : jar;
            exchange.sendResponseHeaders(first ? 503 : 200, body.length == 0 ? -1 : body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        });
        useRepository();

        assertTrue(downloader.download(coordinate, legacyDirectory.toFile()).success());
        assertEquals(2, calls.get());
    }

    @Test
    @DisplayName("refuses an HTML page served under a jar name")
    void refusesHtml() {
        final DependencyCoordinate coordinate = uniqueCoordinate();
        serve(jarPath(coordinate), "<html>rate limited</html>".getBytes(StandardCharsets.UTF_8));
        useRepository();

        assertFalse(downloader.download(coordinate, legacyDirectory.toFile()).success());
    }

    @Test
    @DisplayName("search order: added, Central mirror, Central, declared, then community only when enabled")
    void searchOrder() {
        final DependencyDownloader builtIn = new DependencyDownloader();
        try {
            builtIn.setCommunityRepositories(false);
            builtIn.addRepository("https://first.example.org");
            builtIn.configure(new DependencyDescriptor(List.of(), List.of("https://declared.example.org"), false,
                    List.of()));
            final DependencyCoordinate coordinate = uniqueCoordinate();

            final List<String> urls = builtIn.repositoryUrls(coordinate);
            assertEquals(4, urls.size(), urls.toString());
            assertEquals("https://first.example.org/", urls.get(0));
            assertEquals("https://repo1.maven.org/maven2/", urls.get(2));
            assertEquals("https://declared.example.org/", urls.get(3));

            builtIn.setCommunityRepositories(true);
            final List<String> withCommunity = builtIn.repositoryUrls(coordinate);
            assertEquals(urls, withCommunity.subList(0, 4));
            assertTrue(withCommunity.contains("https://jitpack.io/"), withCommunity.toString());
        } finally {
            builtIn.shutdown();
        }
    }

    @Test
    @DisplayName("fetches POM files from the same repositories as jars")
    void fetchesPom() {
        final DependencyCoordinate coordinate = uniqueCoordinate();
        final byte[] pom = "<project/>".getBytes(StandardCharsets.UTF_8);
        serve("/maven/test/jexdependency/demo/" + coordinate.version() + "/demo-" + coordinate.version() + ".pom", pom);
        useRepository();

        final byte[] fetched = downloader.fetchPom(coordinate);
        assertNotNull(fetched);
        assertArrayEquals(pom, fetched);
    }
}
