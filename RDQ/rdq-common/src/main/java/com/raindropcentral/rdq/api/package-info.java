/**
 * Public RaindropQuests (RDQ) API exposed to external plugins.
 *
 * <p>Edition modules register their services after the staged enable pipeline completes:
 * <ol>
 *     <li><strong>Platform bootstrap</strong> – RDQ resolves its {@code RPlatform} facade and
 *     spins up executors on background threads.</li>
 *     <li><strong>Component &amp; view wiring</strong> – GUI frames, commands, and listeners are
 *     prepared on the Paper main thread.</li>
 *     <li><strong>Repository wiring</strong> – database-backed repositories and service singletons
 *     are bound and, finally, {@link com.raindropcentral.rdq.service.BountyService}
 *     implementations are registered with Bukkit's {@code ServicesManager}.</li>
 * </ol>
 * </p>
 *
 * <h2>Consuming the API</h2>
 * <ul>
 *     <li>Wait until RDQ finishes enabling. Either declare a dependency in {@code plugin.yml},
 *     listen for {@code ServiceRegisterEvent} targeting {@code BountyService.class}, or poll
 *     {@link com.raindropcentral.rdq.service.BountyServiceProvider#isInitialized()} from a
 *     synchronous task scheduled <em>after</em> RDQ is enabled.</li>
 *     <li>Retrieve the service via Bukkit:
 *     <pre>{@code
 * BountyService service = Bukkit.getServicesManager()
 *         .load(BountyService.class);
 * // or when RDQ is your hard dependency
 * BountyService service = BountyServiceProvider.getInstance();
 *     }</pre>
 *     RDQ publishes the concrete implementation per edition, so consumers always interact with the
 *     most feature-complete version that is available.</li>
 *     <li>All service methods return {@link java.util.concurrent.CompletableFuture}s. RDQ resolves
 *     them on its virtual-thread executor (falling back to a fixed pool when necessary). Avoid
 *     blocking the Paper main thread; instead, compose futures and re-enter the server thread with
 *     {@link org.bukkit.scheduler.BukkitScheduler#runTask(org.bukkit.plugin.Plugin, Runnable)} when
 *     world state must be mutated.</li>
 *     <li>Use {@link com.raindropcentral.rdq.service.RCoreBridge} if you need to mirror the
 *     cross-plugin lookups performed by RDQ itself when reconciling rewards with the {@code RCore}
 *     plugin.</li>
 * </ul>
 *
 * <p>RDQ keeps API contracts binary compatible across the {@code 2.x} line. New optional methods
 * may appear as {@code default} implementations. Breaking changes are reserved for major releases
 * and will be announced in advance.</p>
 */
package com.raindropcentral.rdq.api;
