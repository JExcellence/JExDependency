/**
 * Houses the components that mirror the dependency discovery and class path preparation used.
 * by the Paper bootstrap entrypoint.
 *
 * <p>{@link de.jexcellence.dependency.loader.PaperPluginLoader} is instantiated by Paper before the
 * plugin is constructed. It reads dependency coordinates using the same
 * {@link de.jexcellence.dependency.loader.YamlDependencyLoader} implementation that the in-plugin
 * managers invoke, downloads artefacts into {@code <data>/libraries/}, and, when remapping is
 * enabled, coordinates {@code libraries/remapped/} population via
 * {@link de.jexcellence.dependency.remapper.RemappingDependencyManager}. After the directory is
 * prepared it injects each resolved JAR into the {@link io.papermc.paper.plugin.loader.PluginClasspathBuilder}
 * in the same order they were discovered so subsequent plugin-side injection observes a stable
 * class loader state.
 *
 * <p>Because both the loader and the plugin managers read the identical YAML sources and persist into
 * the same directory structure, changes to lookup heuristics, relocation toggles, or injection
 * ordering must be applied in tandem. Keeping the shared system properties (such as
 * {@code jedependency.remap}, relocation prefixes, and {@code paper.plugin.loader.active}) aligned
 * guarantees that Paper bootstrap and runtime initialization remain in sync.
 */
package de.jexcellence.dependency.loader;

