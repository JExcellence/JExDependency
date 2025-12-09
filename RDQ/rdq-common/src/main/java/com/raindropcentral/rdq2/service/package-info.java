/**
 * RDQ service layer responsible for presenting edition-specific gameplay features as stable APIs.
 *
 * <h2>Lifecycle</h2>
 * <p>Services are assembled once the staged enable pipeline reaches the repository wiring phase.
 * {@link com.raindropcentral.rdq2.service.bounty.BountyService} implementations are then published through
 * both Bukkit's {@code ServicesManager} and
 * {@link com.raindropcentral.rdq2.service.bounty.BountyServiceProvider}. When RDQ shuts down the provider is
 * reset and services are unregistered, guaranteeing that stale handles are not reused by dependants.</p>
 *
 * <h2>Threading model</h2>
 * <ul>
 *     <li>All public entry points return {@link java.util.concurrent.CompletableFuture}s. Work is
 *     executed on RDQ's virtual-thread executor with an automatic fallback to a fixed-size pool.</li>
 *     <li>Callers must keep Paper/Bukkit main-thread interactions inside
 *     {@link org.bukkit.scheduler.BukkitScheduler#runTask(org.bukkit.plugin.Plugin, Runnable)} or an
 *     equivalent {@code runSync} bridge supplied by the platform bootstrap.</li>
 *     <li>{@link com.raindropcentral.rdq2.service.RCoreBridge} coordinates with the companion RCore
 *     plugin. It inherits the same asynchronous guarantees and surfaces timeout configuration to
 *     align with RDQ's retry expectations.</li>
 * </ul>
 *
 * <h2>External consumption</h2>
 * <ul>
 *     <li>Dependants should resolve {@link com.raindropcentral.rdq2.service.bounty.BountyService} after RDQ
 *     reports as enabled—ideally by subscribing to {@code ServiceRegisterEvent}.</li>
 *     <li>Edition differences are abstracted behind the service contract. Premium deployments expose
 *     extended limits, whereas free editions return constrained values for queries like
 *     {@link com.raindropcentral.rdq2.service.bounty.BountyService#getMaxBountiesPerPlayer()}.</li>
 *     <li>Retry-friendly semantics: failures propagate as exceptionally-completed futures, allowing
 *     consumers to offer UI retries or queue compensation work.</li>
 * </ul>
 *
 * <h2>Testing expectations</h2>
 * <ul>
 *     <li>MockBukkit-based tests validate reward issuance, cancellation, and timeout propagation.</li>
 *     <li>Integration suites cover free versus premium service wiring and {@code ServicesManager}
 *     registration behaviour.</li>
 *     <li>Concurrency regressions ensure sync hand-offs respect the staged lifecycle described in
 *     {@code AGENTS.md}.</li>
 * </ul>
 */
package com.raindropcentral.rdq2.service;
