/**
 * Houses the components that mirror the dependency discovery and class path preparation used.
 * by the Paper bootstrap entrypoint.
 *
 * <p>{@link de.jexcellence.dependency.loader.PaperPluginLoader} is instantiated by Paper before the
 * plugin is constructed. It reads dependency coordinates using the same
 * {@link de.jexcellence.dependency.loader.YamlDependencyLoader} implementation that the in-plugin
 * managers invoke, downloads artefacts into the server-wide cache, and, when remapping is enabled,
 * writes relocated copies of this plugin's jars to {@code libraries/remapped/<set>/} via
 * {@link de.jexcellence.dependency.remapper.RemappingDependencyManager}. Only the plugin's own jars are
 * added to the {@link io.papermc.paper.plugin.loader.PluginClasspathBuilder}, in declaration order, and
 * their paths are recorded by {@link de.jexcellence.dependency.loader.LoaderClasspathFile} in
 * {@code <data>/libraries/paper-loader-classpath.txt} for the plugin-side injection.
 *
 * <p>Because both the loader and the plugin managers read the identical YAML sources and persist into
 * the same directory structure, changes to lookup heuristics, relocation toggles, or injection
 * ordering must be applied in tandem. Keeping the shared system properties (such as
 * {@code jedependency.remap}, relocation prefixes, and {@code paper.plugin.loader.active}) aligned
 * guarantees that Paper bootstrap and runtime initialization remain in sync.
 */
package de.jexcellence.dependency.loader;

