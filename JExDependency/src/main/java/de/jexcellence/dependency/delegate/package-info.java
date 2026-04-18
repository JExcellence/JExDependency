/**
 * Defines the delegation layer that bridges Bukkit's {@link org.bukkit.plugin.java.JavaPlugin}.
 * lifecycle with the modular services shipped in the RaindropCentral distribution.
 *
 * <p>The {@link de.jexcellence.dependency.delegate.PluginDelegate} contract mirrors the
 * {@code onLoad}, {@code onEnable}, and {@code onDisable} callbacks exposed by Bukkit while
 * surfacing the backing plugin instance for dependency management tasks. Implementations are
 * expected to orchestrate dependency bootstrap through
 * {@link de.jexcellence.dependency.JEDependency}, wire service singletons, and shield the primary
 * plugin class from edition-specific logic.
 * </p>
 *
 * <p>{@link de.jexcellence.dependency.delegate.AbstractPluginDelegate} supplies a shared base that
 * exposes frequently used helpers (configuration access, {@link org.bukkit.Server},
 * {@link org.bukkit.plugin.PluginManager}, and {@link java.util.logging.Logger}) and enforces
 * consistent interaction with the Paper plugin loader handshake. Production plugins such as the
 * RCore and RDQ editions extend this class so they can focus on registering services, initializing
 * asynchronous workflows, and forwarding lifecycle notifications to their feature managers.
 * </p>
 * <h2>Choosing an implementation</h2>
 * <ul>
 *     <li>
 *         Extend {@link de.jexcellence.dependency.delegate.AbstractPluginDelegate} when the
 *         plugin needs the full convenience surface—access to the configured data directory,
 *         logging utilities, command lookups, and Paper-aware lifecycle guards. This is the
 *         recommended path for flagship variants and any plugin that consumes the dependency
 *         injector.
 *     </li>
 *     <li>
 *         Implement {@link de.jexcellence.dependency.delegate.PluginDelegate} directly when the
 *         plugin already manages its own environment or needs to wrap a pre-existing lifecycle
 *         (for example, embedding RaindropCentral services into a different bootstrap). Direct
 *         implementations retain full control over threading and resource management while still
 *         aligning with the dependency initialization flow.
 *     </li>
 * </ul>
 */
package de.jexcellence.dependency.delegate;
