package de.jexcellence.dependency.model;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * What one {@code JEDependency} startup loaded.
 *
 * @param loaded        libraries downloaded or taken from the cache and put on the classpath in this call
 * @param failed        libraries that could not be loaded, with the reason
 * @param preloadedJars jars the Paper plugin loader had already put on the classpath before the plugin started
 * @param millis        time the call took
 */
public record LoadResult(
        @NotNull List<DependencyCoordinate> loaded,
        @NotNull List<DownloadResult> failed,
        int preloadedJars,
        long millis
) {

    /**
     * Copies the lists so the result cannot change after creation.
     */
    public LoadResult {
        loaded = List.copyOf(loaded);
        failed = List.copyOf(failed);
    }

    /**
     * Creates a result from a {@link ProcessingResult}.
     *
     * @param result        processing outcome
     * @param preloadedJars jars the Paper loader had already loaded
     * @param millis        time the call took
     * @return the result
     */
    public static @NotNull LoadResult of(@NotNull final ProcessingResult result, final int preloadedJars,
                                         final long millis) {
        return new LoadResult(result.getSuccessful(), result.getFailed(), preloadedJars, millis);
    }

    /**
     * Tells whether every library could be loaded.
     *
     * @return {@code true} when nothing failed
     */
    public boolean isSuccessful() {
        return failed.isEmpty();
    }
}
