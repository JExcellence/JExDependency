/**
 * Declares the built-in Maven repositories consulted by the dependency downloader.
 *
 * <p>{@link de.jexcellence.dependency.repository.RepositoryType} lists Maven Central and the community
 * repositories that JExDependency knows. Every entry normalizes its base URL to include a trailing slash
 * and derives artifact paths from a {@link de.jexcellence.dependency.model.DependencyCoordinate}. The
 * downloader tries repositories registered at runtime first, then
 * {@link de.jexcellence.dependency.repository.RepositoryType#searchOrder()}; retired entries are kept for
 * source compatibility and never contacted.
 *
 * <p>Downloaded jars are stored in the server-wide cache managed by
 * {@link de.jexcellence.dependency.cache.SharedCacheManager}; relocated copies stay in the plugin's
 * {@code libraries/remapped/<set>/} folders.
 */
package de.jexcellence.dependency.repository;
