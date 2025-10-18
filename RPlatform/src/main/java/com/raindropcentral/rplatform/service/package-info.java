/**
 * Service discovery and registration utilities backed by {@link com.raindropcentral.rplatform.service.ServiceRegistry}.
 *
 * <h2>Asynchronous loading</h2>
 * <p>{@link com.raindropcentral.rplatform.service.ServiceRegistry.ServiceRegistrationBuilder#load() load()} polls Bukkit's
 * {@link org.bukkit.plugin.ServicesManager} asynchronously until a provider becomes available or the configured retry budget is
 * exhausted. Default behaviour retries ten times at 500&nbsp;ms intervals. Use {@link com.raindropcentral.rplatform.service.ServiceRegistry.ServiceRegistrationBuilder#maxAttempts(int)}
 * and {@link com.raindropcentral.rplatform.service.ServiceRegistry.ServiceRegistrationBuilder#retryDelay(long)} to adjust these
 * thresholds for slower dependencies.</p>
 *
 * <h2>Post-initialization registration</h2>
 * <p>Call {@link com.raindropcentral.rplatform.RPlatform#initialize()} before scheduling registrations so the translation manager,
 * command updater, and database connections are ready for service callbacks. Chain registrations off the returned
 * {@link java.util.concurrent.CompletableFuture} or the scheduler supplied by {@link com.raindropcentral.rplatform.api.PlatformAPI}
 * to avoid racing plugins that publish their providers during startup.</p>
 *
 * <h2>Error handling</h2>
 * <p>Mark critical services with {@link com.raindropcentral.rplatform.service.ServiceRegistry.ServiceRegistrationBuilder#required()}
 * so the registry emits warnings when providers never materialize. Optional services should supply failure callbacks to record
 * telemetry or disable feature flags gracefully.</p>
 *
 * <h2>Caching & logging</h2>
 * <p>{@link com.raindropcentral.rplatform.service.ServiceRegistry} maintains a thread-safe cache backed by
 * {@link java.util.concurrent.ConcurrentHashMap}, ensuring lookups remain safe when futures complete on worker threads.
 * Successful registrations log at {@code INFO} level while exhausted required registrations escalate to {@code WARNING} so
 * operators can differentiate expected optional misses from critical outages.</p>
 */
package com.raindropcentral.rplatform.service;
