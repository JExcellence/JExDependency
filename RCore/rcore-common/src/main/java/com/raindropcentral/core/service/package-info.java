/**
 * Core service contracts that expose RCore persistence and statistics capabilities to external modules.
 *
 * <p><strong>Service execution context</strong></p>
 * <ul>
 *     <li>{@link com.raindropcentral.core.api.RCoreBackend} exposes the shared
 *     {@link java.util.concurrent.ExecutorService} via {@link com.raindropcentral.core.api.RCoreBackend#getExecutor()}.
 *     Runtime variants such as {@link com.raindropcentral.core.RCoreFreeImpl} provision
 *     virtual-thread executors (see {@link com.raindropcentral.core.RCoreFreeImpl#createExecutorService()}) to keep
 *     database and statistics workflows off the Bukkit main thread.</li>
 *     <li>{@link com.raindropcentral.core.api.RCoreAdapter} captures the backend executor during construction and
 *     chains every {@link java.util.concurrent.CompletableFuture} continuation on that executor so callers inherit the
 *     thread guarantees documented in {@link com.raindropcentral.core.api.RCoreAdapter#RCoreAdapter(com.raindropcentral.core.api.RCoreBackend)}.</li>
 *     <li>Consumers must never reschedule callbacks onto the common pool; instead, propagate the backend executor when
 *     composing futures so cross-module flows (RDQ, RPlatform, third-party plugins) remain thread-safe.</li>
 * </ul>
 *
 * <p><strong>Lifecycle hooks for consumers</strong></p>
 * <ul>
 *     <li>{@link com.raindropcentral.core.RCoreFreeImpl#onLoad()} registers the
 *     {@link com.raindropcentral.core.service.RCoreService} provider with Bukkit's {@code ServicesManager}.
 *     {@link com.raindropcentral.core.RCoreFreeImpl#onDisable()} unregisters the provider and shuts down the executor via
 *     {@link com.raindropcentral.core.RCoreFreeImpl#shutdownExecutor()} to prevent leaked tasks.</li>
 *     <li>Consumers should resolve the service through Bukkit or the {@link com.raindropcentral.rplatform.service.ServiceRegistry}
 *     utility used by RDQ to handle late-binding scenarios. Observe enable/disable ordering: wait for the
 *     asynchronous enable sequence started in {@link com.raindropcentral.core.RCoreFreeImpl#onEnable()} to complete before
 *     issuing writes, and release references when the plugin disables.</li>
 *     <li>Maintain {@link com.raindropcentral.core.service.RCoreService} method signatures and {@link java.util.Optional}-based
 *     contracts; RDQ invokes them reflectively and expects identical asynchronous semantics.</li>
 * </ul>
 *
 * <p><strong>Statistics exposure &amp; RDQ compatibility</strong></p>
 * <ul>
 *     <li>{@link com.raindropcentral.core.service.RCoreService} surfaces statistics via operations such as
 *     {@link com.raindropcentral.core.service.RCoreService#findPlayerStatisticsAsync(java.util.UUID)},
 *     {@link com.raindropcentral.core.service.RCoreService#findStatisticValueAsync(java.util.UUID, String, String)},
 *     and {@link com.raindropcentral.core.service.RCoreService#getStatisticCountForPluginAsync(java.util.UUID, String)};
 *     implementations rely on {@link com.raindropcentral.core.service.RPlayerStatisticService} helpers to keep entity
 *     mutations consistent.</li>
 *     <li>{@link com.raindropcentral.rdq.service.RCoreBridge} reflects over this package to integrate RDQ quest and reward
 *     logic. Keep the service {@linkplain org.bukkit.Server#getServicesManager() registration} under the FQCN
 *     {@code com.raindropcentral.core.service.RCoreService} and avoid renaming exported types to preserve binary
 *     compatibility.</li>
 *     <li>When extending the API, expose new metrics or statistics through additive methods so reflective consumers can
 *     feature-detect capabilities without breaking older RDQ builds.</li>
 * </ul>
 */
package com.raindropcentral.core.service;
