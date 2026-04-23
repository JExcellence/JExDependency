/**
 * Provides the ASM-based remapping pipeline used by JExDependency when shaded.
 * libraries must be relocated at runtime.
 *
 * <p>Remapping is activated automatically when the runtime system property
 * {@code -Djedependency.remap} resolves to {@code true}, {@code 1},
 * {@code yes}, or {@code on}, or when the Paper plugin loader runs in its
 * {@code auto} mode. Developers can force remapping by calling
 * {@link de.jexcellence.dependency.JEDependency#initializeWithRemapping(
 * org.bukkit.plugin.java.JavaPlugin, Class)} or by instantiating
 * {@link de.jexcellence.dependency.remapper.RemappingDependencyManager}
 * directly. In all other cases the framework falls back to
 * {@code de.jexcellence.dependency.manager.DependencyManager} and injects the
 * downloaded artifacts without rewrites.
 *
 * <p>The remapper consumes relocation metadata from JVM properties such as
 * {@code jedependency.relocations}, {@code jedependency.relocations.prefix},
 * and {@code jedependency.relocations.excludes}. These mappings determine how
 * {@link de.jexcellence.dependency.remapper.PackageRemapper} rewrites both
 * classes and service resources before caching the remapped jars under
 * {@code libraries/remapped}. If the ASM tooling ({@code org.ow2.asm:asm} and
 * {@code org.ow2.asm:asm-commons}) or the remapping cache cannot be prepared,
 * the manager logs warnings and gracefully injects the original downloads
 * instead of aborting plugin startup.
 */
package de.jexcellence.dependency.remapper;
