/**
 * Provides the runtime download pipeline used by {@code JEDependency}.
 *
 * <p>Coordinates are gathered from the plugin bundle via the {@code dependency/*.yml} files that the
 * {@link de.jexcellence.dependency.loader.YamlDependencyLoader} reads. The loader prioritizes the
 * server-specific path (Paper or Spigot) before falling back to the generic descriptor, ensuring
 * that every declared string is parsed into a {@link de.jexcellence.dependency.model.DependencyCoordinate}
 * by {@link de.jexcellence.dependency.manager.DependencyManager} and the
 * equivalent flow inside {@link de.jexcellence.dependency.remapper.RemappingDependencyManager}. These
 * coordinates directly determine the JAR names and repository paths handed to this package.
 *
 * <p>When downloads begin, {@link de.jexcellence.dependency.downloader.DependencyDownloader} tries any
 * repositories registered at runtime (for example, entries from additional YAML metadata or
 * configuration) before falling back to each built-in {@link de.jexcellence.dependency.repository.RepositoryType}
 * in order. This sequence ensures that custom mirrors or authenticated hosts are preferred while the
 * standard Maven mirrors remain available as a safety net.
 *
 * <p>The downloader performs strict validation: it skips work if a previously downloaded file already
 * exists and is recognised as a JAR, enforces minimum content length, streams responses to a temporary
 * file, and performs an atomic rename into the target {@code libraries/} directory so the runtime only
 * ever observes complete outputs. Both synchronous and virtual-thread-backed asynchronous APIs are
 * exposed, allowing dependency resolution to integrate with legacy blocking startup code or the
 * asynchronous bootstrap that feeds Paper's plugin loader.
 *
 * <p>JVM system properties control where those downloads are injected. {@code JEDependency} selects the
 * remapping pipeline when {@code -Djedependency.remap} is enabled, forwarding the same coordinate list
 * to {@link de.jexcellence.dependency.remapper.RemappingDependencyManager}. Remapping in turn honours
 * {@code -Djedependency.relocations}, {@code -Djedependency.relocations.prefix}, and
 * {@code -Djedependency.relocations.excludes}, which the remapper parses to build relocation maps before
 * rewriting bytecode. Consumers should therefore set these JVM properties at launch to control whether
 * JARs remain in {@code libraries/} or are relocated into {@code libraries/remapped/} with shaded
 * packages.
 *
 * <p>Runtime classpath injection always favours remapped outputs when they are available. During the
 * Paper plugin-loader handshake, {@link de.jexcellence.dependency.JEDependency} searches the plugin's
 * data folder, preferring {@code libraries/remapped/} whenever that directory exists before using
 * {@code libraries/}. The remapper follows the same rule: if remapping fails or no relocations are
 * requested, the original downloads in {@code libraries/} are injected; otherwise, the shaded copies in
 * {@code libraries/remapped/} are cached and reused. These fallbacks guarantee that consumers always
 * receive a working classpath even when remapping is disabled or partially configured.
 */
package de.jexcellence.dependency.downloader;
