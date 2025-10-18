package de.jexcellence.dependency.model;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ProcessingResult {

    private final List<DependencyCoordinate> successful;
    private final List<DownloadResult> failed;
    private final long processingTimeMillis;

    public ProcessingResult(
            @NotNull final List<DependencyCoordinate> successful,
            @NotNull final List<DownloadResult> failed,
            final long processingTimeMillis
    ) {
        this.successful = new ArrayList<>(successful);
        this.failed = new ArrayList<>(failed);
        this.processingTimeMillis = processingTimeMillis;
    }

    public @NotNull List<DependencyCoordinate> getSuccessful() {
        return Collections.unmodifiableList(successful);
    }

    public @NotNull List<DownloadResult> getFailed() {
        return Collections.unmodifiableList(failed);
    }

    public long getProcessingTimeMillis() {
        return processingTimeMillis;
    }

    public int getTotalCount() {
        return successful.size() + failed.size();
    }

    public int getSuccessCount() {
        return successful.size();
    }

    public int getFailureCount() {
        return failed.size();
    }

    public boolean hasFailures() {
        return !failed.isEmpty();
    }

    public boolean isFullySuccessful() {
        return failed.isEmpty() && !successful.isEmpty();
    }
}
