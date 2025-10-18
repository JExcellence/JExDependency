package de.jexcellence.dependency.downloader;

import de.jexcellence.dependency.model.DependencyCoordinate;
import de.jexcellence.dependency.model.DownloadResult;
import de.jexcellence.dependency.repository.RepositoryType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DependencyDownloader {

    private static final String USER_AGENT = "JEDependency-Downloader/2.0.0";
    private static final String ACCEPT = "application/java-archive, application/octet-stream, */*;q=0.1";
    private static final int CONNECTION_TIMEOUT_MS = 10_000;
    private static final int READ_TIMEOUT_MS = 30_000;
    private static final int BUFFER_SIZE = 8192;
    private static final int MAX_REDIRECTS = 5;
    private static final long MIN_JAR_SIZE = 1024L;

    private final Logger logger;
    private final List<String> customRepositories;
    private final ExecutorService executorService;

    public DependencyDownloader() {
        this.logger = Logger.getLogger(getClass().getName());
        this.customRepositories = new ArrayList<>();
        this.executorService = Executors.newVirtualThreadPerTaskExecutor();
    }

    public void addRepository(@NotNull final String repositoryUrl) {
        final String normalizedUrl = repositoryUrl.endsWith("/") ? repositoryUrl : repositoryUrl + "/";
        customRepositories.add(normalizedUrl);
        logger.fine("Added custom repository: " + normalizedUrl);
    }

    public @NotNull CompletableFuture<DownloadResult> downloadAsync(
            @NotNull final DependencyCoordinate coordinate,
            @NotNull final File targetDirectory
    ) {
        return CompletableFuture.supplyAsync(
                () -> download(coordinate, targetDirectory),
                executorService
        );
    }

    public @NotNull DownloadResult download(
            @NotNull final DependencyCoordinate coordinate,
            @NotNull final File targetDirectory
    ) {
        final File targetFile = new File(targetDirectory, coordinate.toFileName());

        if (isValidExistingFile(targetFile)) {
            logger.fine("Dependency already exists: " + targetFile.getName());
            return DownloadResult.success(coordinate, targetFile);
        }

        logger.fine("Downloading dependency: " + coordinate.toGavString());

        for (final String customRepo : customRepositories) {
            final String downloadUrl = customRepo + coordinate.toRepositoryPath();
            logger.finest("Trying custom repository: " + downloadUrl);

            if (attemptDownload(downloadUrl, targetFile)) {
                logger.fine("Downloaded from custom repository");
                return DownloadResult.success(coordinate, targetFile);
            }
        }

        for (final RepositoryType repository : RepositoryType.values()) {
            final String downloadUrl = repository.buildUrl(coordinate);
            logger.finest("Trying repository: " + repository.name() + " at " + downloadUrl);

            if (attemptDownload(downloadUrl, targetFile)) {
                logger.fine("Downloaded from repository: " + repository.name());
                return DownloadResult.success(coordinate, targetFile);
            }
        }

        final String errorMessage = "Failed to download from any repository";
        logger.warning(errorMessage + ": " + coordinate.toGavString());
        return DownloadResult.failure(coordinate, errorMessage);
    }

    private boolean isValidExistingFile(@NotNull final File file) {
        return file.isFile() && file.length() > 0L && isValidJarFile(file);
    }

    private boolean attemptDownload(@NotNull final String downloadUrl, @NotNull final File targetFile) {
        try {
            final URI uri = URI.create(downloadUrl);
            URL url = uri.toURL();
            int redirectCount = 0;

            while (redirectCount <= MAX_REDIRECTS) {
                final HttpURLConnection connection = createConnection(url);
                connection.setInstanceFollowRedirects(false);

                final int responseCode = connection.getResponseCode();

                if (responseCode >= 200 && responseCode < 300) {
                    return handleSuccessfulResponse(connection, url, targetFile);
                }

                if (responseCode >= 300 && responseCode < 400) {
                    final String location = connection.getHeaderField("Location");
                    if (location == null || location.isEmpty()) {
                        logger.warning("Redirect without Location header from: " + url);
                        return false;
                    }

                    url = URI.create(location).toURL();
                    logger.finest("Redirect " + responseCode + " to " + url);
                    redirectCount++;
                    continue;
                }

                if (responseCode != 404)
                    logger.warning("HTTP " + responseCode + " when downloading " + url);
                return false;
            }

            logger.warning("Too many redirects (" + MAX_REDIRECTS + ") for " + downloadUrl);
            return false;

        } catch (final Exception exception) {
            logger.log(Level.FINE, "Download failed from URL: " + downloadUrl, exception);
            return false;
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

        logger.fine(String.format(Locale.ROOT,
                "Successfully downloaded %s (%d bytes, Content-Type=%s) to %s",
                url, bytesWritten, contentType, targetFile.getAbsolutePath()));

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
            logger.warning("Downloaded 0 bytes from " + url);
            return false;
        }

        if (expectedLength > 0 && bytesWritten != expectedLength) {
            logger.warning(String.format(Locale.ROOT,
                    "Content-Length mismatch for %s: expected %d, got %d",
                    url, expectedLength, bytesWritten));
            return false;
        }

        if (!isValidJarFile(file)) {
            logger.warning("Downloaded file is not a valid JAR: " + file.getName() +
                    " (Content-Type=" + contentType + ", bytes=" + bytesWritten + ")");
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
            logger.log(Level.FINE, "Failed to delete file: " + file, exception);
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

    public void shutdown() {
        executorService.shutdown();
    }
}
