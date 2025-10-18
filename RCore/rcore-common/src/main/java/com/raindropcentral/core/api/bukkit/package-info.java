/**
 * Bukkit-specific helpers for publishing the shared {@link com.raindropcentral.core.service.RCoreService}
 * adapter.
 *
 * <p><strong>Initialization flow:</strong></p>
 * <ol>
 *     <li>Construct or obtain a fully initialized {@link com.raindropcentral.core.api.RCoreBackend}
 *     once persistence, executors, and platform integrations are ready.</li>
 *     <li>Call {@link RCoreBukkitServiceRegistrar#register(org.bukkit.plugin.java.JavaPlugin,
 *     com.raindropcentral.core.api.RCoreBackend, org.bukkit.plugin.ServicePriority)} during plugin
 *     enablement. The registrar wraps the backend in a {@link com.raindropcentral.core.api.RCoreAdapter}
 *     and registers it with Bukkit's {@link org.bukkit.plugin.ServicesManager}.</li>
 *     <li>Retain the returned {@link com.raindropcentral.core.service.RCoreService} reference for
 *     downstream modules that need direct access.</li>
 *     <li>Invoke {@link RCoreBukkitServiceRegistrar#unregister(org.bukkit.plugin.java.JavaPlugin)}
 *     during shutdown so RDQ's {@code com.raindropcentral.rdq.service.RCoreBridge} and RPlatform
 *     discovery routines do not hold stale providers.</li>
 * </ol>
 *
 * <p><strong>Consumer expectations:</strong></p>
 * <ul>
 *     <li>RDQ locates the service through {@code com.raindropcentral.rdq.service.RCoreBridge}, which
 *     polls the {@link org.bukkit.plugin.ServicesManager} and executes adapter methods reflectively.</li>
 *     <li>RPlatform features coordinate lookup retries via
 *     {@code com.raindropcentral.rplatform.service.ServiceRegistry}; consistent logging from
 *     {@link RCoreBukkitServiceRegistrar} helps operators diagnose priority conflicts and service
 *     availability.</li>
 * </ul>
 */
package com.raindropcentral.core.api.bukkit;
