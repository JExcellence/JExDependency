package de.jexcellence.dependency.resolver;

import de.jexcellence.dependency.model.DependencyCoordinate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Fetches the raw POM file of an artifact, usually
 * {@link de.jexcellence.dependency.downloader.DependencyDownloader#fetchPom(DependencyCoordinate)}. Implementations
 * must be safe to call from several threads at once.
 */
@FunctionalInterface
public interface PomSource {

    /**
     * Fetches a POM.
     *
     * @param coordinate artifact whose POM is wanted
     * @return the POM bytes, or {@code null} when no repository has it
     */
    byte @Nullable [] fetch(@NotNull DependencyCoordinate coordinate);
}
