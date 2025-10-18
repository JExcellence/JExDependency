/**
 * Supplies runtime classpath injection utilities used by both the Paper plugin loader and the
 * plugin-side dependency bootstrap.
 * <p>
 * {@link de.jexcellence.dependency.injector.ClasspathInjector} is invoked by the loader
 * integration and by {@link de.jexcellence.dependency.JEDependency} once the Bukkit plugin gains
 * control. During the Paper bootstrap phase the
 * {@link de.jexcellence.dependency.loader.PaperPluginLoader} downloads or remaps declared
 * dependencies into {@code &lt;data&gt;/libraries} (preferring {@code libraries/remapped} when
 * relocations succeed) and registers them with Paper via
 * {@link io.papermc.paper.plugin.loader.PluginClasspathBuilder#addLibrary}. When control returns to
 * the plugin's main class, {@code JEDependency} detects the {@code paper.plugin.loader.active}
 * marker and reuses the {@code ClasspathInjector} to inject those same jars into the live
 * {@link ClassLoader}, ensuring feature modules can be resolved during {@code onLoad}/{@code onEnable}
 * execution.
 * </p>
 * <h2>Safeguards for repeated injections</h2>
 * <ul>
 *     <li>
 *         Each injector instance maintains a {@link java.util.Set} of injected {@link java.net.URL}
 *         entries so duplicate calls are ignored with fine-grained logging rather than surfacing
 *         reflective errors.
 *     </li>
 *     <li>
 *         {@link de.jexcellence.dependency.injector.ClasspathInjector#ensureModuleDeencapsulation()}
 *         invokes {@link de.jexcellence.dependency.module.Deencapsulation} exactly once per JVM via
 *         a static guard, preventing redundant {@code ModuleLayer} mutations while still allowing
 *         later calls to succeed.
 *     </li>
 *     <li>
 *         {@link de.jexcellence.dependency.injector.ClasspathInjector#tryInject(ClassLoader, java.io.File)}
 *         wraps {@link de.jexcellence.dependency.injector.ClasspathInjector#inject(ClassLoader, java.io.File)}
 *         to provide warning-level diagnostics without halting the remaining dependency pipeline.
 *     </li>
 *     <li>
 *         File validation rejects missing, unreadable, or non-file paths before any reflection
 *         occurs, guaranteeing that loader-supplied and runtime-supplied libraries share the same
 *         sanity checks.
 *     </li>
 * </ul>
 */
package de.jexcellence.dependency.injector;
