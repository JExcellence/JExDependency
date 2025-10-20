/**
 * Asynchronous player lifecycle listeners for the free-edition runtime.
 *
 * <p>{@link com.raindropcentral.core.listener.PlayerPreLogin} drives the variant-specific pre-login flow:</p>
 * <ul>
 *     <li>Delegates lookups and persistence to the shared
 *     {@link com.raindropcentral.core.database.repository.RPlayerRepository} exposed by
 *     {@link com.raindropcentral.core.RCoreFreeImpl}.</li>
 *     <li>Creates baseline statistics through the common
 *     {@link com.raindropcentral.core.service.RPlayerStatisticService} helpers so the free edition inherits the
 *     same aggregate defaults as other distributions.</li>
 *     <li>Overrides edition behaviour by stamping the free plugin namespace, updating login counters, and preventing
 *     logins when asynchronous processing fails.</li>
 * </ul>
 *
 * <p>The listener executes on {@link java.util.concurrent.ExecutorService} supplied by
 * {@link com.raindropcentral.core.api.RCoreBackend}, ensuring Bukkit's async pre-login contract is honoured while the
 * shared {@link com.raindropcentral.core.service.RCoreService} surface remains unchanged for downstream consumers.</p>
 */
package com.raindropcentral.core.listener;
