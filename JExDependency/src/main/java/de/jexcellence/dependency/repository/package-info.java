/**
 * Declares the built-in Maven repositories consulted by the dependency downloader.
 * <p>
 * {@link de.jexcellence.dependency.repository.RepositoryType} enumerates stable mirrors from Maven
 * Central, community archives, and project-specific Nexus instances. Every entry normalizes its base
 * URL to include a trailing slash, and exposes helpers that derive a complete artifact path from a
 * {@link de.jexcellence.dependency.model.DependencyCoordinate}. The downloader walks this enum after it
 * has exhausted user-supplied repository overrides, ensuring predictable fallback behaviour across
 * different environments.
 * <p>
 * Repository resolution feeds directly into the runtime directory strategy documented in
 * {@link de.jexcellence.dependency.downloader}. Libraries are always stored beneath the plugin data
 * folder in {@code libraries/}, and remapped copies (when requested) are written to
 * {@code libraries/remapped/}. Consumers that bundle their own mirrors should still expect these
 * directory conventions so the Paper plugin loader and {@code JEDependency} can discover the outputs
 * during startup.
 */
package de.jexcellence.dependency.repository;
