package de.jexcellence.dependency.downloader;

import de.jexcellence.dependency.cache.SharedCacheManager;
import de.jexcellence.dependency.loader.DependencyDescriptor;
import de.jexcellence.dependency.loader.DependencyEntry;
import de.jexcellence.dependency.model.DependencyCoordinate;
import de.jexcellence.dependency.model.DownloadResult;
import de.jexcellence.dependency.repository.RepositoryCredentials;
import de.jexcellence.dependency.repository.RepositoryType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.UnaryOperator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Downloads artifacts into the server-wide cache.
 *
 * <p>Repositories are tried in this order: repositories added with {@link #addRepository(String)}, the Maven
 * Central mirror, Maven Central, repositories declared in {@code dependencies.yml}, and finally the built-in
 * community repositories when they are turned on. A dependency pinned to a repository with
 * {@code coordinate @ url} is downloaded only from that repository.</p>
 *
 * <p>Requests use one HTTP/2 client with connection reuse and at most {@value #MAX_REQUESTS_PER_HOST} parallel
 * requests per host. Each jar is checked against a pinned SHA-256 when the descriptor gives one, otherwise
 * against the {@code .sha256} or {@code .sha1} file the repository publishes.</p>
 */
public class DependencyDownloader {

    private static final String LOGGER_NAME = "JExDependency";
    private static final String USER_AGENT = "JExDependency (+https://github.com/JExcellence/JExDependency)";
    private static final String ACCEPT = "application/java-archive, application/octet-stream, */*;q=0.1";
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(60);
    private static final Duration SMALL_FILE_TIMEOUT = Duration.ofSeconds(15);
    private static final Duration PROBE_TIMEOUT = Duration.ofSeconds(5);
    private static final int MAX_REDIRECTS = 5;
    private static final int MAX_REQUESTS_PER_HOST = 6;

    private static final int DEFAULT_MAX_RETRIES = 3;
    private static final long INITIAL_BACKOFF_MS = 1_000L;
    private static final long MAX_BACKOFF_MS = 15_000L;

    /** HTTP status codes that are worth retrying: timeouts, rate limits and server errors. */
    private static final Set<Integer> RETRYABLE_STATUS_CODES = Set.of(408, 429, 500, 502, 503, 504);

    private final Logger logger = Logger.getLogger(LOGGER_NAME);
    private final ExecutorService executorService;
    private final HttpClient httpClient;
    private final RepositoryCredentials credentials;
    private final boolean builtInRepositories;
    private final AtomicBoolean networkAvailable = new AtomicBoolean(true);
    private final AtomicBoolean communityHintLogged = new AtomicBoolean();
    private final Set<RepositoryType> unreachableRepositories = ConcurrentHashMap.newKeySet();
    private final List<String> customRepositories = new CopyOnWriteArrayList<>();
    private final List<String> declaredRepositories = new CopyOnWriteArrayList<>();
    private final Map<DependencyCoordinate, String> pinnedRepositories = new ConcurrentHashMap<>();
    private final Map<DependencyCoordinate, String> pinnedHashes = new ConcurrentHashMap<>();
    private final Map<String, Semaphore> hostPermits = new ConcurrentHashMap<>();

    private final AtomicInteger cacheHits = new AtomicInteger();
    private final AtomicInteger downloads = new AtomicInteger();
    private final AtomicInteger failures = new AtomicInteger();

    private volatile boolean communityRepositories = RepositoryType.communityEnabledByProperty();
    private volatile int maxRetries = DEFAULT_MAX_RETRIES;

    /**
     * Creates a downloader that uses the built-in repositories and reads repository credentials from the
     * environment.
     */
    public DependencyDownloader() {
        this(true, System::getenv);
    }

    /**
     * Creates a downloader. Tests switch the built-in repositories off so nothing leaves the machine.
     *
     * @param builtInRepositories whether the Central mirror, Central and community repositories are used
     * @param environment         environment lookup for repository credentials
     */
    DependencyDownloader(final boolean builtInRepositories, @NotNull final UnaryOperator<String> environment) {
        this.builtInRepositories = builtInRepositories;
        this.credentials = new RepositoryCredentials(environment);
        this.executorService = DownloadThreads.newExecutor("JExDependency-download");
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(CONNECT_TIMEOUT)
                .followRedirects(HttpClient.Redirect.NEVER)
                .executor(executorService)
                .build();
    }

    /**
     * Sets the maximum number of retry attempts for transient errors per repository URL. {@code 0} disables
     * retries. The default is {@value #DEFAULT_MAX_RETRIES}.
     *
     * @param maxRetries maximum retries per URL, must be non-negative
     * @return this downloader for fluent chaining
     */
    public DependencyDownloader setMaxRetries(final int maxRetries) {
        if (maxRetries < 0) {
            throw new IllegalArgumentException("maxRetries must be non-negative");
        }
        this.maxRetries = maxRetries;
        return this;
    }

    /**
     * Applies a plugin's {@code dependencies.yml}: its repositories, repository pins, pinned hashes and whether
     * community repositories are searched.
     *
     * @param descriptor parsed descriptor
     */
    public void configure(@NotNull final DependencyDescriptor descriptor) {
        for (final String repository : descriptor.repositories()) {
            final String normalized = withSlash(repository);
            if (!declaredRepositories.contains(normalized)) {
                declaredRepositories.add(normalized);
            }
        }
        for (final DependencyEntry entry : descriptor.entries()) {
            if (entry.repository() != null) {
                pinnedRepositories.put(entry.coordinate(), withSlash(entry.repository()));
            }
            if (entry.sha256() != null) {
                pinnedHashes.put(entry.coordinate(), entry.sha256());
            }
        }
        if (descriptor.communityRepositories()) {
            communityRepositories = true;
        }
    }

    /**
     * Turns the built-in community repositories (JitPack, PaperMC, SpigotMC and others) on or off for this
     * downloader. They are off by default unless the server sets {@value RepositoryType#COMMUNITY_PROPERTY}.
     *
     * @param enabled whether community repositories are searched after Central and the declared repositories
     */
    public void setCommunityRepositories(final boolean enabled) {
        this.communityRepositories = enabled;
    }

    /**
     * Probes network connectivity with a HEAD request to the Maven Central mirror, then to Central itself if the
     * mirror cannot be reached. Any HTTP answer counts as reachable: a mirror may answer its root URL with 404
     * and still serve every artifact. When the mirror does not answer but Central does, downloads by this
     * instance skip the mirror instead of waiting for its timeouts on every artifact. When neither host answers,
     * downloads return cached files or fail at once instead of timing out against every repository.
     *
     * @return {@code true} if the network is reachable, {@code false} otherwise
     */
    public boolean probeConnectivity() {
        unreachableRepositories.clear();
        if (!builtInRepositories) {
            networkAvailable.set(true);
            return true;
        }
        final RepositoryType mirror = RepositoryType.MAVEN_CENTRAL_MIRROR;
        if (reachable(mirror)) {
            networkAvailable.set(true);
            return true;
        }
        if (reachable(RepositoryType.MAVEN_CENTRAL)) {
            if (!mirror.getBaseUrl().equals(RepositoryType.MAVEN_CENTRAL.getBaseUrl())) {
                unreachableRepositories.add(mirror);
                logger.log(Level.WARNING, "Maven Central mirror {0} did not answer; downloading from Maven Central "
                        + "directly", mirror.getBaseUrl());
            }
            networkAvailable.set(true);
            return true;
        }

        // Central is unreachable; repositories the plugin declares may still answer.
        final java.util.Set<String> others = new java.util.LinkedHashSet<>(customRepositories);
        others.addAll(declaredRepositories);
        others.addAll(pinnedRepositories.values());
        for (final String other : others) {
            if (reachable(other)) {
                unreachableRepositories.add(mirror);
                unreachableRepositories.add(RepositoryType.MAVEN_CENTRAL);
                logger.log(Level.WARNING, "Maven Central is unreachable; only the plugin''s own repositories are used");
                networkAvailable.set(true);
                return true;
            }
        }

        networkAvailable.set(false);
        logger.warning("Network unreachable: cached dependencies will be used where available. "
                + "Missing artifacts will fail immediately instead of timing out against every repository.");
        return false;
    }

    /**
     * Returns the built-in repositories this downloader uses, in order: the Central mirror and Central, plus the
     * community repositories when they are turned on, without those {@link #probeConnectivity()} found
     * unreachable.
     *
     * @return unmodifiable list of built-in repositories
     */
    public @NotNull List<RepositoryType> activeRepositories() {
        if (!builtInRepositories) {
            return List.of();
        }
        return RepositoryType.defaultOrder(communityRepositories).stream()
                .filter(repository -> !unreachableRepositories.contains(repository))
                .toList();
    }

    /**
     * Returns the repository base URLs tried for an artifact, in order, each ending with a slash.
     *
     * @param coordinate artifact to look up
     * @return the pinned repository alone, or the full search order
     */
    public @NotNull List<String> repositoryUrls(@NotNull final DependencyCoordinate coordinate) {
        final String pinned = pinnedRepositories.get(coordinate);
        if (pinned != null) {
            return List.of(pinned);
        }
        final List<String> urls = new ArrayList<>(customRepositories);
        final List<RepositoryType> builtIn = activeRepositories();
        builtIn.stream().filter(repository -> !repository.isCommunity()).forEach(repository -> add(urls, repository.getBaseUrl()));
        declaredRepositories.forEach(url -> add(urls, url));
        builtIn.stream().filter(RepositoryType::isCommunity).forEach(repository -> add(urls, repository.getBaseUrl()));
        return List.copyOf(urls);
    }

    /**
     * Registers a repository that is tried before all others.
     *
     * @param repositoryUrl base URL of a Maven repository
     */
    public void addRepository(@NotNull final String repositoryUrl) {
        final String normalized = withSlash(repositoryUrl.trim());
        if (!customRepositories.contains(normalized)) {
            customRepositories.add(normalized);
            logger.log(Level.FINE, () -> "Added custom repository: " + normalized);
        }
    }

    /**
     * Downloads a dependency on a virtual thread.
     *
     * @param coordinate      artifact to resolve
     * @param targetDirectory legacy per-plugin directory, checked for jars of older versions
     * @return future with the outcome
     */
    public @NotNull CompletableFuture<DownloadResult> downloadAsync(
            @NotNull final DependencyCoordinate coordinate,
            @NotNull final java.io.File targetDirectory
    ) {
        return CompletableFuture.supplyAsync(() -> download(coordinate, targetDirectory), executorService);
    }

    /**
     * Downloads a dependency into the shared cache, or returns the cached copy. A jar left by an older version in
     * {@code targetDirectory} is moved into the cache instead of being downloaded again.
     *
     * @param coordinate      artifact to resolve
     * @param targetDirectory legacy per-plugin directory, checked for jars of older versions
     * @return outcome with the cached file or an error description
     */
    public @NotNull DownloadResult download(
            @NotNull final DependencyCoordinate coordinate,
            @NotNull final java.io.File targetDirectory
    ) {
        final SharedCacheManager cache = SharedCacheManager.getInstance();
        final Path cachedPath = cache.resolveJarPath(coordinate);

        if (isAcceptable(coordinate, cachedPath)) {
            logger.log(Level.FINE, () -> "Dependency in shared cache: " + coordinate.toGavString());
            cacheHits.incrementAndGet();
            return DownloadResult.success(coordinate, cachedPath.toFile());
        }
        discardIfPresent(cachedPath);

        if (cache.migrateFromLegacy(coordinate, targetDirectory) && isAcceptable(coordinate, cachedPath)) {
            logger.log(Level.FINE, () -> "Migrated from legacy cache: " + coordinate.toGavString());
            cacheHits.incrementAndGet();
            return DownloadResult.success(coordinate, cachedPath.toFile());
        }
        discardIfPresent(cachedPath);

        if (!networkAvailable.get()) {
            final String message = "Network unreachable and no cached artifact available";
            logger.log(Level.WARNING, "{0}: {1}", new Object[]{message, coordinate.toGavString()});
            failures.incrementAndGet();
            return DownloadResult.failure(coordinate, message);
        }

        logger.log(Level.FINE, () -> "Downloading dependency: " + coordinate.toGavString());
        final Path result = cache.getOrDownload(coordinate, () -> downloadToFile(coordinate, cachedPath));
        if (result != null && isAcceptable(coordinate, result)) {
            downloads.incrementAndGet();
            return DownloadResult.success(coordinate, result.toFile());
        }

        final String message = "Failed to download from any repository";
        logger.log(Level.WARNING, "{0}: {1}", new Object[]{message, coordinate.toGavString()});
        failures.incrementAndGet();
        return DownloadResult.failure(coordinate, message);
    }

    /**
     * Fetches the POM file of an artifact from the same repositories its jar comes from.
     *
     * @param coordinate artifact whose POM is wanted; a classifier is ignored
     * @return the POM bytes, or {@code null} when no repository has it
     */
    public byte @Nullable [] fetchPom(@NotNull final DependencyCoordinate coordinate) {
        if (!networkAvailable.get()) {
            return null;
        }
        final String pomPath = coordinate.groupId().replace('.', '/') + '/' + coordinate.artifactId() + '/'
                + coordinate.version() + '/' + coordinate.artifactId() + '-' + coordinate.version() + ".pom";
        for (final String base : repositoryUrls(coordinate)) {
            final URI uri;
            try {
                uri = URI.create(base + pomPath);
            } catch (final IllegalArgumentException exception) {
                continue;
            }
            final SmallFile file = fetchSmallFile(uri, true);
            final byte[] bytes = file.status() == 200 ? file.body() : null;
            if (bytes != null && bytes.length > 0) {
                logger.log(Level.FINE, () -> "Fetched POM " + coordinate.toGavString() + " from " + base);
                return bytes;
            }
        }
        return null;
    }

    /**
     * Returns how many jars this downloader took from the cache, downloaded and failed to get so far.
     *
     * @return counters since the downloader was created
     */
    public @NotNull DownloadStats stats() {
        return new DownloadStats(cacheHits.get(), downloads.get(), failures.get());
    }

    /**
     * Download counters.
     *
     * @param cached     jars reused from the shared cache or moved there from an old per-plugin folder
     * @param downloaded jars fetched from a repository
     * @param failed     jars that could not be loaded
     */
    public record DownloadStats(int cached, int downloaded, int failed) {
    }

    /**
     * Stops the download threads. Call when the downloader is no longer needed.
     */
    public void shutdown() {
        executorService.shutdown();
    }

    // -------------------------------------------------------------------------
    // Download
    // -------------------------------------------------------------------------

    private boolean downloadToFile(@NotNull final DependencyCoordinate coordinate, @NotNull final Path targetFile) {
        final List<String> repositories = repositoryUrls(coordinate);
        for (final String base : repositories) {
            final URI uri;
            try {
                uri = URI.create(base + coordinate.toRepositoryPath());
            } catch (final IllegalArgumentException exception) {
                logger.log(Level.WARNING, "Invalid artifact URL for {0} in {1}", new Object[]{coordinate.toGavString(), base});
                continue;
            }
            logger.log(Level.FINEST, () -> "Trying " + uri);
            if (attemptWithRetry(uri, coordinate, targetFile)) {
                logger.log(Level.FINE, () -> "Downloaded " + coordinate.toGavString() + " from " + base);
                return true;
            }
            if (Thread.currentThread().isInterrupted()) {
                return false;
            }
        }

        if (builtInRepositories && !communityRepositories && !pinnedRepositories.containsKey(coordinate)
                && communityHintLogged.compareAndSet(false, true)) {
            logger.log(Level.WARNING, "{0} was not found on Maven Central. If it comes from another repository, "
                    + "list that repository under ''repositories:'' in dependencies.yml or pin it with "
                    + "''{0} @ <url>''. Server owners can restore the old community repositories with "
                    + "-D{1}=true.", new Object[]{coordinate.toGavString(), RepositoryType.COMMUNITY_PROPERTY});
        }
        return false;
    }

    private boolean attemptWithRetry(
            @NotNull final URI uri,
            @NotNull final DependencyCoordinate coordinate,
            @NotNull final Path targetFile
    ) {
        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            final Attempt result = attemptOnce(uri, coordinate, targetFile);
            if (result.kind == Attempt.Kind.SUCCESS) {
                return true;
            }
            if (result.kind == Attempt.Kind.FAILED || attempt == maxRetries) {
                return false;
            }
            final long delay = result.retryAfterMs > 0
                    ? Math.min(result.retryAfterMs, MAX_BACKOFF_MS)
                    : Math.min(INITIAL_BACKOFF_MS << attempt, MAX_BACKOFF_MS);
            final int next = attempt + 1;
            logger.log(Level.FINE, () -> "Retrying " + uri + " in " + delay + " ms (attempt " + next + "/"
                    + maxRetries + ")");
            if (!sleep(delay)) {
                logger.log(Level.FINE, () -> "Interrupted while waiting to retry " + uri);
                return false;
            }
        }
        return false;
    }

    private @NotNull Attempt attemptOnce(
            @NotNull final URI start,
            @NotNull final DependencyCoordinate coordinate,
            @NotNull final Path targetFile
    ) {
        Path tempFile = null;
        try {
            Files.createDirectories(targetFile.getParent());
            tempFile = Files.createTempFile(targetFile.getParent(), targetFile.getFileName().toString(), ".part");
            URI current = start;
            for (int redirect = 0; redirect <= MAX_REDIRECTS; redirect++) {
                final Path destination = tempFile;
                final HttpResponse<Path> response = send(current, REQUEST_TIMEOUT, info -> info.statusCode() == 200
                        ? HttpResponse.BodySubscribers.ofFile(destination)
                        : HttpResponse.BodySubscribers.replacing(null));
                final int status = response.statusCode();

                if (status == 200) {
                    return accept(current, coordinate, response, tempFile, targetFile)
                            ? Attempt.SUCCESS : Attempt.FAILED;
                }
                if (status >= 300 && status < 400) {
                    current = redirectTarget(current, response);
                    if (current == null) {
                        return Attempt.FAILED;
                    }
                    continue;
                }
                if (status == 404 || status == 410) {
                    return Attempt.FAILED;
                }
                if (RETRYABLE_STATUS_CODES.contains(status)) {
                    logger.log(Level.FINE, "HTTP {0} from {1}", new Object[]{status, current});
                    return Attempt.retry(retryAfterMs(response));
                }
                logger.log(Level.WARNING, "HTTP {0} when downloading {1}", new Object[]{status, current});
                return Attempt.FAILED;
            }
            logger.log(Level.WARNING, "Too many redirects for {0}", start);
            return Attempt.FAILED;
        } catch (final HttpTimeoutException | ConnectException exception) {
            logger.log(Level.FINE, exception, () -> "Network error from " + start);
            return Attempt.retry(0);
        } catch (final IOException | IllegalArgumentException exception) {
            logger.log(Level.FINE, exception, () -> "Download failed from " + start);
            return Attempt.FAILED;
        } catch (final InterruptedException exception) {
            Thread.currentThread().interrupt();
            return Attempt.FAILED;
        } finally {
            if (tempFile != null) {
                deleteQuietly(tempFile);
            }
        }
    }

    private boolean accept(
            @NotNull final URI source,
            @NotNull final DependencyCoordinate coordinate,
            @NotNull final HttpResponse<Path> response,
            @NotNull final Path tempFile,
            @NotNull final Path targetFile
    ) throws IOException {
        final long size = Files.size(tempFile);
        final long expected = response.headers().firstValueAsLong("Content-Length").orElse(-1L);
        if (size == 0 || (expected >= 0 && expected != size)) {
            logger.log(Level.WARNING, "Incomplete download from {0}: {1} of {2} bytes",
                    new Object[]{source, size, expected});
            return false;
        }
        if (!SharedCacheManager.isUsableJar(tempFile)) {
            logger.log(Level.WARNING, "Downloaded file is not a valid jar: {0} (Content-Type={1})",
                    new Object[]{source, response.headers().firstValue("Content-Type").orElse("unknown")});
            return false;
        }
        if (!checksumMatches(source, coordinate, tempFile)) {
            return false;
        }
        move(tempFile, targetFile);
        logger.log(Level.FINE, () -> "Downloaded " + targetFile.getFileName() + " (" + size + " bytes)");
        return true;
    }

    // -------------------------------------------------------------------------
    // Checksums
    // -------------------------------------------------------------------------

    /**
     * Checks a downloaded jar. A pinned SHA-256 must match. Without a pin, the repository's {@code .sha256} file
     * is used, then its {@code .sha1} file; when it publishes neither, the jar is accepted.
     */
    private boolean checksumMatches(
            @NotNull final URI source,
            @NotNull final DependencyCoordinate coordinate,
            @NotNull final Path file
    ) throws IOException {
        final String pinned = pinnedHashes.get(coordinate);
        if (pinned != null) {
            final String actual = hash(file, "SHA-256");
            if (pinned.equals(actual)) {
                return true;
            }
            logger.log(Level.WARNING, "SHA-256 of {0} from {1} does not match dependencies.yml: expected {2}, got {3}",
                    new Object[]{coordinate.toGavString(), source, pinned, actual});
            return false;
        }

        for (final String algorithm : List.of("SHA-256", "SHA-1")) {
            final String extension = algorithm.equals("SHA-256") ? ".sha256" : ".sha1";
            final SmallFile checksumFile = fetchSmallFile(URI.create(source + extension), true);
            if (checksumFile.status() == 404 || checksumFile.status() == 410) {
                continue;
            }
            final String published = checksumFile.status() == 200 ? parseChecksum(checksumFile.body(), algorithm) : null;
            if (published == null) {
                // The checksum exists but could not be read: never accept the jar unverified.
                logger.log(Level.WARNING, "Could not read {0} for {1} (HTTP {2}); trying the next repository",
                        new Object[]{extension, coordinate.toGavString(), checksumFile.status()});
                return false;
            }
            final String actual = hash(file, algorithm);
            if (published.equals(actual)) {
                logger.log(Level.FINE, () -> algorithm + " OK: " + coordinate.toGavString());
                return true;
            }
            logger.log(Level.WARNING, "{0} mismatch for {1} from {2}: expected {3}, got {4}",
                    new Object[]{algorithm, coordinate.toGavString(), source, published, actual});
            return false;
        }
        logger.log(Level.FINE, () -> "No checksum published for " + source + "; accepting the jar");
        return true;
    }

    private static @Nullable String parseChecksum(final byte @Nullable [] bytes, @NotNull final String algorithm) {
        if (bytes == null) {
            return null;
        }
        final String text = new String(bytes, StandardCharsets.US_ASCII).trim();
        if (text.isEmpty()) {
            return null;
        }
        final String value = text.split("\\s+")[0].toLowerCase(Locale.ROOT);
        final int length = algorithm.equals("SHA-256") ? 64 : 40;
        return value.length() == length && value.chars().allMatch(c -> Character.digit(c, 16) >= 0) ? value : null;
    }

    private boolean isAcceptable(@NotNull final DependencyCoordinate coordinate, @NotNull final Path jar) {
        if (!SharedCacheManager.isUsableJar(jar)) {
            return false;
        }
        final String pinned = pinnedHashes.get(coordinate);
        if (pinned == null) {
            return true;
        }
        try {
            if (pinned.equals(hash(jar, "SHA-256"))) {
                return true;
            }
            logger.log(Level.WARNING, "Cached {0} does not match the SHA-256 in dependencies.yml; downloading it again",
                    coordinate.toGavString());
            return false;
        } catch (final IOException exception) {
            return false;
        }
    }

    static @NotNull String hash(@NotNull final Path file, @NotNull final String algorithm) throws IOException {
        final MessageDigest digest;
        try {
            digest = MessageDigest.getInstance(algorithm);
        } catch (final NoSuchAlgorithmException exception) {
            throw new IOException(algorithm + " is not available", exception);
        }
        final byte[] buffer = new byte[64 * 1024];
        try (InputStream input = Files.newInputStream(file)) {
            int read;
            while ((read = input.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
        }
        final StringBuilder hex = new StringBuilder();
        for (final byte value : digest.digest()) {
            hex.append(Character.forDigit((value >> 4) & 0xF, 16)).append(Character.forDigit(value & 0xF, 16));
        }
        return hex.toString();
    }

    // -------------------------------------------------------------------------
    // HTTP
    // -------------------------------------------------------------------------

    /**
     * Result of fetching a small file.
     *
     * @param status HTTP status of the final answer, or {@code -1} when no answer arrived
     * @param body   body of a 200 answer, otherwise {@code null}
     */
    private record SmallFile(int status, byte @Nullable [] body) {
        static final SmallFile NO_ANSWER = new SmallFile(-1, null);
    }

    /**
     * Fetches a small file such as a checksum or POM. Follows redirects, retries once on a transient error.
     */
    private @NotNull SmallFile fetchSmallFile(@NotNull final URI start, final boolean retry) {
        SmallFile last = SmallFile.NO_ANSWER;
        for (int attempt = 0; attempt <= (retry ? 1 : 0); attempt++) {
            URI current = start;
            try {
                for (int redirect = 0; redirect <= MAX_REDIRECTS && current != null; redirect++) {
                    final HttpResponse<byte[]> response = send(current, SMALL_FILE_TIMEOUT, info ->
                            info.statusCode() == 200
                                    ? HttpResponse.BodySubscribers.ofByteArray()
                                    : HttpResponse.BodySubscribers.replacing(null));
                    final int status = response.statusCode();
                    if (status >= 300 && status < 400) {
                        current = redirectTarget(current, response);
                        continue;
                    }
                    last = new SmallFile(status, status == 200 ? response.body() : null);
                    if (status == 200 || !RETRYABLE_STATUS_CODES.contains(status)) {
                        return last;
                    }
                    break;
                }
            } catch (final HttpTimeoutException | ConnectException exception) {
                logger.log(Level.FINEST, exception, () -> "Retryable error fetching " + start);
                last = SmallFile.NO_ANSWER;
            } catch (final IOException | IllegalArgumentException exception) {
                logger.log(Level.FINEST, exception, () -> "Could not fetch " + start);
                return SmallFile.NO_ANSWER;
            } catch (final InterruptedException exception) {
                Thread.currentThread().interrupt();
                return SmallFile.NO_ANSWER;
            }
            if (retry && attempt == 0 && !sleep(INITIAL_BACKOFF_MS)) {
                return last;
            }
        }
        return last;
    }

    private <T> @NotNull HttpResponse<T> send(
            @NotNull final URI uri,
            @NotNull final Duration timeout,
            @NotNull final HttpResponse.BodyHandler<T> handler
    ) throws IOException, InterruptedException {
        final HttpRequest.Builder builder = HttpRequest.newBuilder(uri)
                .timeout(timeout)
                .header("User-Agent", USER_AGENT)
                .header("Accept", ACCEPT)
                .GET();
        final String authorization = credentials.authorizationFor(uri);
        if (authorization != null) {
            builder.header("Authorization", authorization);
        }
        final Semaphore permits = hostPermits.computeIfAbsent(String.valueOf(uri.getHost()),
                host -> new Semaphore(MAX_REQUESTS_PER_HOST));
        permits.acquire();
        try {
            return httpClient.send(builder.build(), handler);
        } finally {
            permits.release();
        }
    }

    /**
     * Resolves a redirect. A redirect from {@code https} to plain {@code http} is refused.
     *
     * @return the next URI, or {@code null} when the redirect is missing or unsafe
     */
    private @Nullable URI redirectTarget(@NotNull final URI current, @NotNull final HttpResponse<?> response) {
        final Optional<String> location = response.headers().firstValue("Location");
        if (location.isEmpty() || location.get().isBlank()) {
            logger.log(Level.WARNING, "Redirect without Location header from {0}", current);
            return null;
        }
        final URI next;
        try {
            next = current.resolve(location.get().trim());
        } catch (final IllegalArgumentException exception) {
            logger.log(Level.WARNING, "Invalid redirect from {0}: {1}", new Object[]{current, location.get()});
            return null;
        }
        if ("https".equalsIgnoreCase(current.getScheme()) && !"https".equalsIgnoreCase(next.getScheme())) {
            logger.log(Level.WARNING, "Refusing redirect from {0} to insecure {1}", new Object[]{current, next});
            return null;
        }
        logger.log(Level.FINEST, () -> "Redirect " + response.statusCode() + " to " + next);
        return next;
    }

    private boolean reachable(@NotNull final RepositoryType repository) {
        return reachable(repository.getBaseUrl());
    }

    private boolean reachable(@NotNull final String baseUrl) {
        final HttpRequest request;
        try {
            request = HttpRequest.newBuilder(URI.create(baseUrl))
                .timeout(PROBE_TIMEOUT)
                .header("User-Agent", USER_AGENT)
                .method("HEAD", HttpRequest.BodyPublishers.noBody())
                .build();
        } catch (final IllegalArgumentException exception) {
            return false;
        }
        try {
            final int status = httpClient.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
            logger.log(Level.FINE, () -> "Network probe reached " + baseUrl + " (HTTP " + status + ")");
            return true;
        } catch (final IOException | IllegalArgumentException exception) {
            logger.log(Level.FINE, exception, () -> "Network probe got no answer from " + baseUrl);
            return false;
        } catch (final InterruptedException exception) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private static long retryAfterMs(@NotNull final HttpResponse<?> response) {
        return response.headers().firstValue("Retry-After").map(value -> {
            try {
                return Long.parseLong(value.trim()) * 1_000L;
            } catch (final NumberFormatException exception) {
                return 0L;
            }
        }).orElse(0L);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static void move(@NotNull final Path source, @NotNull final Path destination) throws IOException {
        try {
            Files.move(source, destination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (final AtomicMoveNotSupportedException exception) {
            Files.move(source, destination, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void discardIfPresent(@NotNull final Path jar) {
        if (Files.exists(jar)) {
            logger.log(Level.FINE, () -> "Discarding unusable cached file " + jar);
            deleteQuietly(jar);
        }
    }

    private void deleteQuietly(@NotNull final Path file) {
        try {
            Files.deleteIfExists(file);
        } catch (final IOException exception) {
            logger.log(Level.FINE, exception, () -> "Failed to delete " + file);
        }
    }

    private static void add(@NotNull final List<String> urls, @NotNull final String url) {
        if (!urls.contains(url)) {
            urls.add(url);
        }
    }

    private static @NotNull String withSlash(@NotNull final String url) {
        return url.endsWith("/") ? url : url + "/";
    }

    /**
     * Sleeps for the given time.
     *
     * @return {@code false} when the thread was interrupted; the interrupt flag is restored
     */
    private static boolean sleep(final long millis) {
        try {
            Thread.sleep(millis);
            return true;
        } catch (final InterruptedException exception) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /** Outcome of one download attempt. */
    private static final class Attempt {
        private enum Kind { SUCCESS, FAILED, RETRY }

        static final Attempt SUCCESS = new Attempt(Kind.SUCCESS, 0);
        static final Attempt FAILED = new Attempt(Kind.FAILED, 0);

        private final Kind kind;
        private final long retryAfterMs;

        private Attempt(final Kind kind, final long retryAfterMs) {
            this.kind = kind;
            this.retryAfterMs = retryAfterMs;
        }

        static Attempt retry(final long retryAfterMs) {
            return new Attempt(Kind.RETRY, retryAfterMs);
        }
    }
}
