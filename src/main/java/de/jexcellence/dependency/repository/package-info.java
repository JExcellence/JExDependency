/**
 * Built-in Maven repositories, the Central mirror choice and repository credentials.
 *
 * <p>{@link de.jexcellence.dependency.repository.RepositoryType} lists the Maven Central mirror (chosen by
 * {@link de.jexcellence.dependency.repository.CentralMirror}), Maven Central, and community repositories that are
 * only contacted when a plugin or the server turns them on. Retired entries stay for source compatibility and are
 * never contacted. {@link de.jexcellence.dependency.repository.RepositoryUrl} validates configured URLs and
 * {@link de.jexcellence.dependency.repository.RepositoryCredentials} reads private-repository credentials from
 * environment variables.
 *
 * <p>Downloaded jars are stored in the server-wide cache managed by
 * {@link de.jexcellence.dependency.cache.SharedCacheManager}; relocated copies stay in the plugin's
 * {@code libraries/remapped/<set>/} folders.
 */
package de.jexcellence.dependency.repository;
