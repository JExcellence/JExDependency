package de.jexcellence.dependency.downloader;

import de.jexcellence.dependency.exception.DependencyDownloadException;
import de.jexcellence.dependency.model.DependencyCoordinate;
import de.jexcellence.dependency.model.DownloadResult;
import de.jexcellence.dependency.repository.RepositoryType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handles downloading dependency artifacts from a curated list of repositories. Downloads are streamed directly to the.
 * plugin's libraries directory, validated as JAR files and optionally executed asynchronously using virtual threads.
 * The downloader keeps track of custom repositories supplied at runtime and logs HTTP outcomes for troubleshooting.
 */
public class DependencyDownloader {

    private static final String LOGGER_NAME = "JExDependency";
    private static final String USER_AGENT = "JEDependency-Downloader/2.0.0";
    private static final String ACCEPT = "application/java-archive, application/octet-stream, */*;q=0.1";
    private static final int CONNECTION_TIMEOUT_MS = 10_000;
    private static final int READ_TIMEOUT_MS = 30_000;
    private static final int BUFFER_SIZE = 65536;
    private static final int MAX_REDIRECTS = 5;
    private static final long MIN_JAR_SIZE = 1024L;

    private static final int DEFAULT_MAX_RETRIES = 3;
    private static final long INITIAL_BACKOFF_MS = 1_000L;
    private static final long MAX_BACKOFF_MS = 15_000L;
    private static final double BACKOFF_MULTIPLIER = 2.0;
    private static final String CONNECTIVITY_PROBE_URL = "https://repo1.maven.org/maven2/";
    private static final int PROBE_TIMEOUT_MS = 5_000;

    /**
     * HTTP status codes considered transient and eligible for retry. Covers server-side errors and
     * rate limiting responses that are likely to succeed on a subsequent attempt.
     */
    private static final Set<Integer> RETRYABLE_STATUS_CODES = Set.of(
            408, // Request Timeout
            429, // Too Many Requests
            500, // Internal Server Error
            502, // Bad Gateway
            503, // Service Unavailable
            504  // Gateway Timeout
    );

    private final Logger logger;
    private final List<String> customRepositories;
    private final ExecutorService executorService;
    private final AtomicBoolean networkAvailable;

    private int maxRetries;

    /**
     * Creates a new downloader backed by a virtual-thread executor that can be used for asynchronous operations.
     */
    public DependencyDownloader() {
        this.logger = Logger.getLogger(LOGGER_NAME);
        this.customRepositories = new ArrayList<>();
        this.executorService = Executors.newVirtualThreadPerTaskExecutor();
        this.networkAvailable = new AtomicBoolean(true);
        this.maxRetries = DEFAULT_MAX_RETRIES;
    }

    /**
     * Sets the maximum number of retry attempts for transient HTTP errors per repository URL.
     * A value of {@code 0} disables retries entirely. The default is {@value #DEFAULT_MAX_RETRIES}.
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
     * Probes network connectivity by attempting a HEAD request to Maven Central. When the probe
     * fails, subsequent download calls skip remote resolution and immediately return cached files
     * or failures — avoiding long timeout chains against every repository.
     *
     * <p>Call this once before processing the dependency list. The result is cached for the lifetime
     * of this downloader instance.</p>
     *
     * @return {@code true} if the network is reachable, {@code false} otherwise
     */
    public boolean probeConnectivity() {
        try {
            final URL probeUrl = URI.create(CONNECTIVITY_PROBE_URL).toURL();
            final HttpURLConnection connection = (HttpURLConnection) probeUrl.openConnection();
            connection.setConnectTimeout(PROBE_TIMEOUT_MS);
            connection.setReadTimeout(PROBE_TIMEOUT_MS);
            connection.setRequestMethod("HEAD");
            connection.setRequestProperty("User-Agent", USER_AGENT);

            final int responseCode = connection.getResponseCode();
            final boolean reachable = responseCode >= 200 && responseCode < 400;
            networkAvailable.set(reachable);

            if (!reachable) {
                logger.warning("Network probe failed (HTTP " + responseCode + ") — cached dependencies will be used where available");
            } else {
                logger.fine("Network probe successful");
            }

            return reachable;
        } catch (final Exception exception) {
            networkAvailable.set(false);
            logger.warning("Network unreachable — cached dependencies will be used where available. " +
                    "Missing artifacts will fail immediately instead of timing out against every repository.");
            return false;
        }
    }

    /**
     * Registers an additional base repository URL. The URL is normalised to include a trailing slash and will be tried.
     * before the built-in repository list when resolving artifacts.
     *
     * @param repositoryUrl base URL pointing to a Maven-style repository root
     */
    public void addRepository(@NotNull final String repositoryUrl) {
        final String normalizedUrl = repositoryUrl.endsWith("/") ? repositoryUrl : repositoryUrl + "/";
        customRepositories.add(normalizedUrl);
        logger.log(Level.FINE, () -> "Added custom repository: " + normalizedUrl);
    }

    /**
     * Downloads a dependency asynchronously using the downloader's virtual-thread executor. Callers should inspect the.
     * returned {@link DownloadResult} to verify success and retrieve the downloaded file.
     *
     * @param coordinate      artifact coordinates to resolve
     * @param targetDirectory directory where the resulting file should be placed
     *
     * @return future containing the outcome of the download attempt
     */
    public @NotNull CompletableFuture<DownloadResult> downloadAsync(
            @NotNull final DependencyCoordinate coordinate,
            @NotNull final File targetDirectory
    ) {
        return CompletableFuture.supplyAsync(
                () -> download(coordinate, targetDirectory),
                executorService
        );
    }

    /**
     * Downloads a dependency synchronously. The method validates any cached file before reaching out to remote.
     * repositories, tries custom repositories first and returns a {@link DownloadResult} describing the outcome.
     *
     * @param coordinate      artifact coordinates to resolve
     * @param targetDirectory directory where the resulting file should be placed
     *
     * @return outcome containing the file on success or an error description on failure
     */
    public @NotNull DownloadResult download(
            @NotNull final DependencyCoordinate coordinate,
            @NotNull final File targetDirectory
    ) {
        final File targetFile = new File(targetDirectory, coordinate.toFileName());

        if (isValidExistingFile(targetFile)) {
            logger.log(Level.FINE, () -> "Dependency already exists: " + targetFile.getName());
            return DownloadResult.success(coordinate, targetFile);
        }

        if (!networkAvailable.get()) {
            final String errorMessage = "Network unreachable and no cached artifact available";
            logger.log(Level.WARNING, "{0}: {1}", new Object[]{errorMessage, coordinate.toGavString()});
            return DownloadResult.failure(coordinate, errorMessage);
        }

        logger.log(Level.FINE, () -> "Downloading dependency: " + coordinate.toGavString());

        for (final String customRepo : customRepositories) {
            final String downloadUrl = customRepo + coordinate.toRepositoryPath();
            logger.log(Level.FINEST, () -> "Trying custom repository: " + downloadUrl);

            if (attemptDownloadWithRetry(downloadUrl, targetFile)) {
                logger.fine("Downloaded from custom repository");
                return DownloadResult.success(coordinate, targetFile);
            }
        }

        for (final RepositoryType repository : RepositoryType.values()) {
            final String downloadUrl = repository.buildUrl(coordinate);
            logger.log(Level.FINEST, () -> "Trying repository: " + repository.name() + " at " + downloadUrl);

            if (attemptDownloadWithRetry(downloadUrl, targetFile)) {
                logger.log(Level.FINE, () -> "Downloaded from repository: " + repository.name());
                return DownloadResult.success(coordinate, targetFile);
            }
        }

        final String errorMessage = "Failed to download from any repository";
        logger.log(Level.WARNING, "{0}: {1}", new Object[]{errorMessage, coordinate.toGavString()});
        return DownloadResult.failure(coordinate, errorMessage);
    }

    /**
     * Attempts to download from a single URL with exponential backoff on transient HTTP errors.
     * Non-retryable failures (404, invalid content) return immediately without retry.
     *
     * @param downloadUrl full artifact URL to download from
     * @param targetFile  local file to write the artifact to
     * @return {@code true} if the download succeeded and the file passed all validation
     */
    private boolean attemptDownloadWithRetry(@NotNull final String downloadUrl, @NotNull final File targetFile) {
        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            final int result = attemptDownloadClassified(downloadUrl, targetFile);

            if (result == DOWNLOAD_SUCCESS) {
                return true;
            }
            if (result == DOWNLOAD_NOT_RETRYABLE) {
                return false;
            }

            // result == DOWNLOAD_RETRYABLE — back off and try again
            if (attempt < maxRetries) {
                final long backoffMs = calculateBackoff(attempt);
                final int currentAttempt = attempt + 1;
                logger.log(Level.FINE, () -> String.format(
                        "Retrying %s in %dms (attempt %d/%d)",
                        downloadUrl, backoffMs, currentAttempt, maxRetries));
                sleep(backoffMs);
            }
        }

        return false;
    }

    private static final int DOWNLOAD_SUCCESS = 0;
    private static final int DOWNLOAD_NOT_RETRYABLE = 1;
    private static final int DOWNLOAD_RETRYABLE = 2;

    private boolean isValidExistingFile(@NotNull final File file) {
        return file.isFile() && file.length() > 0L && isValidJarFile(file);
    }

    /**
     * Attempts a single download from the given URL and classifies the outcome for the retry logic.
     *
     * @return {@link #DOWNLOAD_SUCCESS}, {@link #DOWNLOAD_NOT_RETRYABLE}, or {@link #DOWNLOAD_RETRYABLE}
     */
    private int attemptDownloadClassified(@NotNull final String downloadUrl, @NotNull final File targetFile) {
        try {
            final URI uri = URI.create(downloadUrl);
            URL url = uri.toURL();
            int redirectCount = 0;

            while (redirectCount <= MAX_REDIRECTS) {
                final HttpURLConnection connection = createConnection(url);
                connection.setInstanceFollowRedirects(false);

                final int responseCode = connection.getResponseCode();

                if (responseCode >= 200 && responseCode < 300) {
                    return handleSuccessfulResponse(connection, url, targetFile)
                            ? DOWNLOAD_SUCCESS
                            : DOWNLOAD_NOT_RETRYABLE;
                }

                if (responseCode >= 300 && responseCode < 400) {
                    final String location = connection.getHeaderField("Location");
                    if (location == null || location.isEmpty()) {
                        logger.log(Level.WARNING, "Redirect without Location header from: {0}", url);
                        return DOWNLOAD_NOT_RETRYABLE;
                    }

                    url = URI.create(location).toURL();
                    final URL redirectUrl = url;
                    logger.log(Level.FINEST, () -> "Redirect " + responseCode + " to " + redirectUrl);
                    redirectCount++;
                    continue;
                }

                if (responseCode == 404) {
                    return DOWNLOAD_NOT_RETRYABLE;
                }

                final URL currentUrl = url;
                logger.log(Level.WARNING, "HTTP {0} when downloading {1}", new Object[]{responseCode, currentUrl});
                return RETRYABLE_STATUS_CODES.contains(responseCode)
                        ? DOWNLOAD_RETRYABLE
                        : DOWNLOAD_NOT_RETRYABLE;
            }

            logger.log(Level.WARNING, "Too many redirects ({0}) for {1}", new Object[]{MAX_REDIRECTS, downloadUrl});
            return DOWNLOAD_NOT_RETRYABLE;

        } catch (final java.net.SocketTimeoutException exception) {
            logger.log(Level.FINE, exception, () -> "Timeout downloading from URL: " + downloadUrl);
            return DOWNLOAD_RETRYABLE;
        } catch (final java.net.ConnectException exception) {
            logger.log(Level.FINE, exception, () -> "Connection refused from URL: " + downloadUrl);
            return DOWNLOAD_RETRYABLE;
        } catch (final Exception exception) {
            logger.log(Level.FINE, exception, () -> "Download failed from URL: " + downloadUrl);
            return DOWNLOAD_NOT_RETRYABLE;
        }
    }

    private boolean handleSuccessfulResponse(
            @NotNull final HttpURLConnection connection,
            @NotNull final URL url,
            @NotNull final File targetFile
    ) throws IOException {
        final long contentLength = parseContentLength(connection.getHeaderField("Content-Length"));
        final String contentType = safeLowerCase(connection.getHeaderField("Content-Type"));

        final File tempFile = new File(targetFile.getParentFile(), targetFile.getName() + ".part");
        Files.createDirectories(targetFile.getParentFile().toPath());

        final long bytesWritten;
        try (final InputStream inputStream = connection.getInputStream();
             final FileOutputStream outputStream = new FileOutputStream(tempFile)) {
            bytesWritten = transferData(inputStream, outputStream);
        }

        if (!validateDownload(tempFile, bytesWritten, contentLength, contentType, url)) {
            safeDelete(tempFile);
            return false;
        }

        moveToFinalLocation(tempFile, targetFile);

        logger.log(Level.FINE, () -> "Downloaded: " + targetFile.getName() + " (" + bytesWritten + " bytes)");

        if (!verifyChecksum(url, targetFile)) {
            logger.log(Level.WARNING, "Checksum mismatch for {0} — deleting corrupt artifact", targetFile.getName());
            safeDelete(targetFile);
            return false;
        }

        return true;
    }

    private boolean validateDownload(
            @NotNull final File file,
            final long bytesWritten,
            final long expectedLength,
            @Nullable final String contentType,
            @NotNull final URL url
    ) {
        if (bytesWritten <= 0) {
            logger.log(Level.WARNING, "Downloaded 0 bytes from {0}", url);
            return false;
        }

        if (expectedLength > 0 && bytesWritten != expectedLength) {
            logger.log(Level.WARNING, "Content-Length mismatch for {0}: expected {1}, got {2}",
                    new Object[]{url, expectedLength, bytesWritten});
            return false;
        }

        if (!isValidJarFile(file)) {
            logger.log(Level.WARNING, "Downloaded file is not a valid JAR: {0} (Content-Type={1}, bytes={2})",
                    new Object[]{file.getName(), contentType, bytesWritten});
            return false;
        }

        return true;
    }

    private void moveToFinalLocation(@NotNull final File source, @NotNull final File destination) throws IOException {
        try {
            Files.move(source.toPath(), destination.toPath(),
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (final AtomicMoveNotSupportedException exception) {
            Files.move(source.toPath(), destination.toPath(),
                    StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private @NotNull HttpURLConnection createConnection(@NotNull final URL url) throws IOException {
        final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setConnectTimeout(CONNECTION_TIMEOUT_MS);
        connection.setReadTimeout(READ_TIMEOUT_MS);
        connection.setRequestProperty("User-Agent", USER_AGENT);
        connection.setRequestProperty("Accept", ACCEPT);
        return connection;
    }

    private long transferData(
            @NotNull final InputStream inputStream,
            @NotNull final FileOutputStream outputStream
    ) throws IOException {
        final byte[] buffer = new byte[BUFFER_SIZE];
        long totalBytes = 0L;
        int bytesRead;

        while ((bytesRead = inputStream.read(buffer)) != -1) {
            if (bytesRead > 0) {
                outputStream.write(buffer, 0, bytesRead);
                totalBytes += bytesRead;
            }
        }

        return totalBytes;
    }

    private boolean isValidJarFile(@NotNull final File file) {
        if (!file.isFile() || file.length() < MIN_JAR_SIZE) {
            return false;
        }

        try (final JarFile jarFile = new JarFile(file, true)) {
            return jarFile.entries().hasMoreElements();
        } catch (final Exception exception) {
            return false;
        }
    }

    private void safeDelete(@NotNull final File file) {
        try {
            Files.deleteIfExists(file.toPath());
        } catch (final IOException exception) {
            logger.log(Level.FINE, exception, () -> "Failed to delete file: " + file);
        }
    }

    private long parseContentLength(@Nullable final String header) {
        if (header == null) {
            return -1L;
        }

        try {
            return Long.parseLong(header.trim());
        } catch (final NumberFormatException exception) {
            return -1L;
        }
    }

    private @Nullable String safeLowerCase(@Nullable final String value) {
        return value == null ? null : value.toLowerCase(Locale.ROOT);
    }

    /**
     * Verifies the downloaded JAR against the {@code .sha1} checksum published alongside it in the same
     * repository. When the checksum file is unavailable (HTTP 404, timeout, parse error) the verification
     * is silently skipped and the artifact is considered valid. A mismatch causes the method to return
     * {@code false}, signalling the caller to delete the artifact and retry the download.
     *
     * @param artifactUrl the URL that was used to download the JAR (without {@code .sha1} suffix)
     * @param jarFile     the successfully downloaded JAR file to verify
     * @return {@code true} if the checksum matches or could not be verified, {@code false} on mismatch
     */
    private boolean verifyChecksum(@NotNull final URL artifactUrl, @NotNull final File jarFile) {
        try {
            return performChecksumVerification(artifactUrl, jarFile);
        } catch (final IOException exception) {
            logger.log(Level.FINEST, exception, () -> "SHA-1 check skipped for " + jarFile.getName());
            return true; // Cannot verify — accept the artifact
        }
    }

    /**
     * Performs the actual checksum verification logic. Returns {@code true} when the checksum matches
     * or when the {@code .sha1} file is not available. Returns {@code false} only on a confirmed mismatch.
     *
     * @param artifactUrl the URL that was used to download the JAR
     * @param jarFile     the JAR file to verify
     * @return {@code true} if valid or unverifiable, {@code false} on confirmed mismatch
     * @throws IOException if network or file operations fail
     */
    private boolean performChecksumVerification(@NotNull final URL artifactUrl, @NotNull final File jarFile) throws IOException {
        final URL sha1Url = URI.create(artifactUrl.toString() + ".sha1").toURL();
        final HttpURLConnection conn = createConnection(sha1Url);
        conn.setConnectTimeout(5_000);
        conn.setReadTimeout(5_000);

        if (conn.getResponseCode() != 200) {
            return true; // .sha1 not published by this repository — accept the artifact
        }

        final String expected;
        try (final InputStream is = conn.getInputStream()) {
            // SHA-1 files may contain only the hex string or "hex filename"; take the first token
            expected = new String(is.readAllBytes(), StandardCharsets.UTF_8).trim().split("\\s+")[0];
        }

        if (expected.isEmpty() || expected.length() != 40) {
            return true; // Not a valid SHA-1 hex string — cannot verify, accept
        }

        final String actual;
        try {
            actual = sha1Hex(jarFile);
        } catch (final DependencyDownloadException exception) {
            logger.log(Level.FINEST, exception, () -> "SHA-1 computation failed for " + jarFile.getName());
            return true; // Cannot compute hash \u2014 accept the artifact
        }

        if (expected.equalsIgnoreCase(actual)) {
            logger.log(Level.FINE, () -> "Checksum OK: " + jarFile.getName());
            return true;
        }

        logger.log(Level.WARNING, "Checksum mismatch for {0} \u2014 expected {1}, got {2}",
                new Object[]{jarFile.getName(), expected, actual});
        return false;
    }

    private long calculateBackoff(final int attempt) {
        final long backoff = (long) (INITIAL_BACKOFF_MS * Math.pow(BACKOFF_MULTIPLIER, attempt));
        return Math.min(backoff, MAX_BACKOFF_MS);
    }

    private static void sleep(final long millis) {
        try {
            Thread.sleep(millis);
        } catch (final InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    private static String sha1Hex(@NotNull final File file) throws IOException, DependencyDownloadException {
        final MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-1");
        } catch (final NoSuchAlgorithmException exception) {
            throw new DependencyDownloadException("SHA-1 algorithm not available", exception);
        }
        
        final byte[] buf = new byte[BUFFER_SIZE];
        try (final FileInputStream fis = new FileInputStream(file)) {
            int n;
            while ((n = fis.read(buf)) != -1) {
                md.update(buf, 0, n);
            }
        }
        final byte[] digest = md.digest();
        final StringBuilder sb = new StringBuilder(40);
        for (final byte b : digest) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * Shuts down the virtual-thread executor backing asynchronous downloads. Should be invoked when the downloader is.
     * no longer needed to allow the JVM to exit cleanly.
     */
    public void shutdown() {
        executorService.shutdown();
    }
}
