/**
 * Premium bounty service implementations.
 *
 * <p>These classes provide the Bukkit-facing premium bounty APIs. They are built on top of the
 * repositories provisioned by {@code rdq-common} and are registered during the repository wiring
 * phase of {@link com.raindropcentral.rdq.RDQ}'s lifecycle.</p>
 *
 * <p>Feature gating and responsibilities:</p>
 * <ul>
 *     <li>Expose unlimited CRUD access while respecting the asynchronous
 *     {@link java.util.concurrent.CompletableFuture} contracts expected by the shared infrastructure.</li>
 *     <li>Return edition-aware limits so user interfaces from the common module can present premium
 *     affordances (for example, unlimited counts indicated by {@code -1}).</li>
 *     <li>Delegate all persistence to the repositories supplied through the premium manager to keep
 *     data access aligned with the free edition's mock implementations.</li>
 * </ul>
 *
 * <p>Keep these services synchronized with changes in the shared lifecycle or bounty schemas to
 * avoid regressions when editions are built together.</p>
 */
package com.raindropcentral.rdq.service.bounty;
