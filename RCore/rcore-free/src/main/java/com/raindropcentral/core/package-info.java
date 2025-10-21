/**
 * Free-edition runtime that bridges the shared {@code rcore-common} services with the Bukkit plugin entrypoint.
 *
 * <p><strong>Edition scope:</strong> Classes in this package own the {@link org.bukkit.plugin.java.JavaPlugin}
 * bootstrap ({@link com.raindropcentral.core.RCoreFree}) and the variant backend implementation
 * ({@link com.raindropcentral.core.RCoreFreeImpl}). They adapt the database repositories and services shipped in
 * {@code rcore-common} so the free distribution can expose {@link com.raindropcentral.core.service.RCoreService}
 * through Bukkit's {@link org.bukkit.plugin.ServicesManager}.</p>
 *
 * <p><strong>Initialization vs. common:</strong> Unlike the common module—which only defines the
 * {@link com.raindropcentral.core.api.RCoreBackend} contract and its {@link com.raindropcentral.core.api.RCoreAdapter}
 * facade—this package owns the concrete lifecycle. The free runtime boots {@link de.jexcellence.dependency.JEDependency},
 * initializes {@link com.raindropcentral.rplatform.RPlatform}, and chains asynchronous startup stages inside
 * {@link com.raindropcentral.core.RCoreFreeImpl#onEnable()} to register metrics, wire repositories, and publish the
 * shared service.</p>
 *
 * <p><strong>Shared services:</strong></p>
 * <ul>
 *     <li><em>Inherited</em>: The exposed API surface remains the {@link com.raindropcentral.core.service.RCoreService}
 *     contract provided by {@link com.raindropcentral.core.api.RCoreAdapter}, ensuring callers reuse the same futures,
 *     statistics helpers, and logging instrumentation defined in {@code rcore-common}.</li>
 *     <li><em>Variant overrides</em>: {@link com.raindropcentral.core.RCoreFreeImpl} implements
 *     {@link com.raindropcentral.core.api.RCoreBackend} to supply edition-specific executor configuration,
 *     repository wiring ({@link com.raindropcentral.core.database.repository.RPlayerRepository},
 *     {@link com.raindropcentral.core.database.repository.RStatisticRepository}, etc.), and service registration rules.</li>
 * </ul>
 *
 * <p>Event listeners that hydrate player data during startup live in the {@link com.raindropcentral.core.listener}
 * sub-package.</p>
 */
package com.raindropcentral.core;
