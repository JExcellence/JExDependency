/**
 * Managers that load a plugin's libraries at startup.
 *
 * <p>{@link de.jexcellence.dependency.manager.DependencyManager} reads {@code dependencies.yml} through
 * {@link de.jexcellence.dependency.loader.YamlDependencyLoader}, adds programmatic coordinates, resolves transitive
 * dependencies, downloads into the shared cache with
 * {@link de.jexcellence.dependency.downloader.DependencyDownloader} and injects the jars with
 * {@link de.jexcellence.dependency.injector.ClasspathInjector}. When relocation is requested,
 * {@link de.jexcellence.dependency.remapper.RemappingDependencyManager} takes that role and injects relocated copies
 * from {@code libraries/remapped/<set>/}.
 *
 * <p>On Paper, the plugin loader runs first and records the jars it loaded in
 * {@code libraries/paper-loader-classpath.txt}. When that record is complete, {@code JEDependency} injects exactly
 * those jars and skips the managers.
 */
package de.jexcellence.dependency.manager;
