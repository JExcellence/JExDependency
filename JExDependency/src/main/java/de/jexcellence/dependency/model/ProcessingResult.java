package de.jexcellence.dependency.model;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Summary of a dependency processing run, capturing successes, failures and total execution time.
 */
public class ProcessingResult {

    private final List<DependencyCoordinate> successful;
    private final List<DownloadResult> failed;
    private final long processingTimeMillis;

    /**
     * Creates a processing result snapshot.
     *
     * @param successful           coordinates successfully downloaded and injected
     * @param failed               failed download attempts with associated error details
     * @param processingTimeMillis total time spent downloading and injecting in milliseconds
     */
    public ProcessingResult(
            @NotNull final List<DependencyCoordinate> successful,
            @NotNull final List<DownloadResult> failed,
            final long processingTimeMillis
    ) {
        this.successful = new ArrayList<>(successful);
        this.failed = new ArrayList<>(failed);
        this.processingTimeMillis = processingTimeMillis;
    }

    /**
     * Returns an immutable view of successful coordinates.
     *
     * @return immutable list of successful coordinates
     */
    public @NotNull List<DependencyCoordinate> getSuccessful() {
        return Collections.unmodifiableList(successful);
    }

    /**
     * Returns an immutable view of failed downloads.
     *
     * @return immutable list of failures
     */
    public @NotNull List<DownloadResult> getFailed() {
        return Collections.unmodifiableList(failed);
    }

    /**
     * Returns the time spent processing dependencies.
     *
     * @return duration in milliseconds
     */
    public long getProcessingTimeMillis() {
        return processingTimeMillis;
    }

    /**
     * Returns the total number of dependency coordinates processed.
     *
     * @return total dependency count
     */
    public int getTotalCount() {
        return successful.size() + failed.size();
    }

    /**
     * Returns the number of successful downloads.
     *
     * @return success count
     */
    public int getSuccessCount() {
        return successful.size();
    }

    /**
     * Returns the number of failed downloads.
     *
     * @return failure count
     */
    public int getFailureCount() {
        return failed.size();
    }

    /**
     * Indicates whether any dependency failed to download or inject.
     *
     * @return {@code true} when at least one failure occurred
     */
    public boolean hasFailures() {
        return !failed.isEmpty();
    }

    /**
     * Indicates whether all processed dependencies succeeded and at least one dependency was present.
     *
     * @return {@code true} when every dependency succeeded
     */
    public boolean isFullySuccessful() {
        return failed.isEmpty() && !successful.isEmpty();
    }
}
