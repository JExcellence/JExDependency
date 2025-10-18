/**
 * Provides the runtime managers that coordinate dependency bootstrap inside plugins.
 * <p>
 * The {@link de.jexcellence.dependency.manager.DependencyManager} path is responsible for
 * orchestrating the synchronous download pipeline: YAML coordinates gathered through
 * {@link de.jexcellence.dependency.loader.YamlDependencyLoader} are merged with any
 * programmatic overrides, {@link de.jexcellence.dependency.downloader.DependencyDownloader}
 * retrieves the JARs into the plugin data directory's {@code libraries/} folder, and
 * {@link de.jexcellence.dependency.injector.ClasspathInjector} wires the results into the
 * plugin's class loader. When remapping is requested, the
 * {@link de.jexcellence.dependency.remapper.RemappingDependencyManager} augments that flow by
 * writing remapped outputs into {@code libraries/remapped/} before the injector runs so the
 * effective class path matches the Paper loader's expectations.
 * <p>
 * Both manager implementations rely on the same target directories and module deencapsulation
 * logic, and they observe the {@code paper.plugin.loader.active} flag established by the Paper
 * loader. Maintaining those shared invariants keeps the plugin bootstrap in lock-step with the
 * Paper-side preload sequence: the loader hydrates {@code libraries/} (and optionally the
 * {@code remapped/} subtree) before plugin code calls {@code initialize*}, at which point the
 * manager injects the JARs using the already populated directories.
 */
package de.jexcellence.dependency.manager;

