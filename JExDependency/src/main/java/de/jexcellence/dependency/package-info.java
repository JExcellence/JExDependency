/**
 * Bootstrap utilities for runtime dependency management used by Raindrop Central plugins.
 * <p>
 * The {@link de.jexcellence.dependency.JEDependency} facade exposes three entry points that control
 * how dependencies are downloaded and injected into the plugin class loader:
 * </p>
 * <ul>
 *     <li>{@link de.jexcellence.dependency.JEDependency#initialize(org.bukkit.plugin.java.JavaPlugin, Class)} and
 *     its overload perform a synchronous bootstrap on the calling thread. They resolve dependency
 *     coordinates, download any missing jars, and immediately inject them before returning.</li>
 *     <li>{@link de.jexcellence.dependency.JEDependency#initializeAsync(org.bukkit.plugin.java.JavaPlugin, Class)}
 *     runs the same workflow asynchronously inside a {@link java.util.concurrent.CompletableFuture}. Callers must
 *     await completion before referencing classes supplied by downloaded dependencies.</li>
 *     <li>{@link de.jexcellence.dependency.JEDependency#initializeWithRemapping(org.bukkit.plugin.java.JavaPlugin, Class)}
 *     forces the remapping pipeline. When {@code RemappingDependencyManager} is present the call applies package
 *     relocations even if the {@code -Djedependency.remap} system property would otherwise disable the feature.</li>
 * </ul>
 * <p>
 * Dependency metadata is sourced from YAML descriptors located next to the provided anchor class under
 * {@code dependency/*.yml}. Any additional coordinates supplied to the entry points are merged into that list.
 * The {@code DependencyManager} (or remapping counterpart) then downloads artifacts into the plugin data directory
 * under {@code <data>/libraries}. When remapping is enabled the effective jars are materialized within the
 * {@code <data>/libraries/remapped} folder. System properties such as {@code -Djedependency.relocations},
 * {@code -Djedependency.relocations.prefix}, and {@code -Djedependency.relocations.excludes} customize relocation
 * rules that the remapper applies while producing those remapped jars.
 * </p>
 * <p>
 * The Paper loader integration implemented in
 * {@link de.jexcellence.dependency.loader.PaperPluginLoader} performs the same YAML resolution, download, and
 * remapping logic ahead of plugin start-up. It also sets {@code paper.plugin.loader.active=true} so the plugin-side
 * bootstrap can detect that the loader already primed {@code <data>/libraries}. When {@link de.jexcellence.dependency.JEDependency}
 * subsequently runs, it injects the preloaded jars (preferring the {@code remapped} directory) and continues through
 * the same initialization sequence. Keeping this handshake aligned ensures both the loader and plugin entry points
 * emit consistent logging, honor the same relocation settings, and remain in sync during future changes.
 * </p>
 */
package de.jexcellence.dependency;
