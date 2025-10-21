/**
 * Service Provider Interfaces (SPI) that allow editions and external modules to swap RDQ
 * persistence strategies.
 *
 * <p>The bootstrap sequence supplies a {@link com.raindropcentral.rdq.api.spi.PersistenceRegistry}
 * when instantiating the {@code RDQ} runtime. Implementers can populate the registry with
 * asynchronous adapters:</p>
 * <pre>{@code
 * PersistenceRegistry registry = PersistenceRegistry.builder()
 *         .bountyPersistence(new CustomBountyPersistence(remoteClient))
 *         .playerPersistence(new CustomPlayerPersistence(remoteClient))
 *         .build();
 * RDQ rdq = new RDQ(plugin, "Enterprise", registry) { ... };
 * }</pre>
 *
 * <h2>Extension guidance</h2>
 * <ul>
 *     <li>{@link com.raindropcentral.rdq.api.spi.BountyPersistence} is responsible for creating and
 *     deleting {@code RBounty} entities. It is invoked from RDQ's virtual-thread executor; maintain
 *     the asynchronous contract by returning already-running
 *     {@link java.util.concurrent.CompletableFuture} instances.</li>
 *     <li>{@link com.raindropcentral.rdq.api.spi.PlayerPersistence} handles updates to
 *     {@code RDQPlayer} records. It is typically used after bounty settlements when RDQ needs to
 *     persist statistics.</li>
 *     <li>Adapters should be idempotent and resilient to retries. RDQ will treat failures as
 *     recoverable and may reschedule operations during the repository wiring stage.</li>
 * </ul>
 *
 * <h2>Stability promises</h2>
 * <ul>
 *     <li>Method signatures remain stable across RDQ {@code 2.x}. Additional hooks, if required,
 *     will be introduced as {@code default} methods.</li>
 *     <li>Implementations are loaded reflectively; type names and package locations constitute the
 *     compatibility surface. Renames will be accompanied by deprecation windows of at least one
 *     minor release.</li>
 *     <li>Lifecycle expectations mirror the API layer: SPI providers may assume executors are
 *     initialized before RDQ invokes any persistence method, and shutdown signals are delivered from
 *     {@code RDQ#onDisable()}.</li>
 * </ul>
 */
package com.raindropcentral.rdq.api.spi;
