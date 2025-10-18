/**
 * Adapter facade that exposes the stable {@link com.raindropcentral.core.service.RCoreService}
 * contract to Bukkit consumers.
 *
 * <p><strong>Stability guarantees:</strong></p>
 * <ul>
 *     <li>Public types in this package adhere to the semantic window advertised by
 *     {@link com.raindropcentral.core.service.RCoreService#getApiVersion()}.
 *     Minor releases must remain binary compatible with RDQ's reflective bridge
 *     and RPlatform's {@code com.raindropcentral.rplatform.service.ServiceRegistry}
 *     lookups.</li>
 *     <li>Breaking changes require new major API versions accompanied by side-by-side
 *     adapters so legacy RDQ deployments continue resolving the original service.</li>
 *     <li>New capabilities are introduced by additive methods on {@link RCoreAdapter}
 *     delegating to {@link RCoreBackend} implementations, allowing downstream
 *     consumers to receive defaults without recompilation.</li>
 * </ul>
 *
 * <p><strong>Cross-module adapters:</strong></p>
 * <ul>
 *     <li>{@link RCoreAdapter} wraps the active {@link RCoreBackend} supplied by the
 *     runtime (Free or Premium) and exposes the canonical asynchronous surface
 *     shared across the ecosystem.</li>
 *     <li>RDQ resolves the adapter reflectively through
 *     {@code com.raindropcentral.rdq.service.RCoreBridge}, keeping quests and
 *     gameplay modules functional even when the upstream jar is relocated.</li>
 *     <li>RPlatform components load the same adapter via
 *     {@code com.raindropcentral.rplatform.service.ServiceRegistry}, providing
 *     retry semantics and classloader isolation for third-party servers.</li>
 * </ul>
 */
package com.raindropcentral.core.api;
