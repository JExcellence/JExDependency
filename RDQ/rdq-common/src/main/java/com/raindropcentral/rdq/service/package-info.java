/**
 * RDQ service layer responsible for integrating gameplay rewards with RCore and economy engines.
 *
 * <p>Calculation flow:</p>
 * <ol>
 *     <li>Resolve the upstream {@code RCoreService} via {@link com.raindropcentral.rdq.service.RCoreBridge}.</li>
 *     <li>Translate gameplay events into balance/statistic mutations and forward
 *     them through the asynchronous RCore APIs.</li>
 *     <li>Schedule any Bukkit-main-thread mutations by re-entering the sync
 *     executor supplied by the platform bootstrap.</li>
 * </ol>
 *
 * <p>Reconciliation:</p>
 * <ul>
 *     <li>Every deposit or withdrawal request expects a reconciliation token
 *     from RCore; RDQ caches the token until the engine confirms persistence.</li>
 *     <li>Timeouts surface as optional failures that callers must handle by
 *     presenting retry affordances to players.</li>
 * </ul>
 *
 * <p>Concurrency &amp; persistence guarantees:</p>
 * <ul>
 *     <li>Services expose only non-blocking {@link java.util.concurrent.CompletableFuture}
 *     contracts; call sites must never block the Paper main thread.</li>
 *     <li>Callbacks run on RDQ's virtual-thread executor by default, with fall
 *     back to the fixed executor when virtual threads are unavailable.</li>
 *     <li>Persistence acknowledgements originate from RCore; RDQ does not commit
 *     any state locally until those futures complete successfully.</li>
 * </ul>
 *
 * <p>Testing expectations:</p>
 * <ul>
 *     <li>MockBukkit-based service tests cover reward issuance, cancellation,
 *     and timeout propagation.</li>
 *     <li>Integration tests validate the bridge wiring across free/premium
 *     editions.</li>
 *     <li>Concurrency regression tests assert that sync hand-offs respect the
 *     staged lifecycle documented in {@code AGENTS.md}.</li>
 * </ul>
 */
package com.raindropcentral.rdq.service;
