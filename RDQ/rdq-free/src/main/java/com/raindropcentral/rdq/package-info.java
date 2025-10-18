/**
 * Free edition wiring for the RaindropQuests runtime.
 *
 * <p>The classes in this package host the Bukkit delegate ({@link com.raindropcentral.rdq.RDQFree})
 * and its implementation ({@link com.raindropcentral.rdq.RDQFreeImpl}) that bootstrap the
 * {@link com.raindropcentral.rdq.RDQ common lifecycle}. The delegates defer to the shared
 * three-stage startup sequence—platform initialization, component/view setup, and repository
 * wiring—documented in {@code AGENTS.md}. Staying aligned with that lifecycle keeps the free
 * module synchronized with the infrastructure provided by {@code rdq-common}.</p>
 *
 * <p>Edition responsibilities and feature gating:</p>
 * <ul>
 *     <li>Expose the free edition identity and metrics identifiers to the shared RDQ base.</li>
 *     <li>Install view-only managers and in-memory data sources so that gameplay features remain
 *     demonstrative without persisting data.</li>
 *     <li>Guard premium-only hooks by returning no-op views/services during the shared lifecycle
 *     callbacks. The common module treats the responses as authoritative when enabling UI frames
 *     and registering commands.</li>
 * </ul>
 *
 * <p>Whenever the shared lifecycle evolves (for example new initialization phases or additional
 * callbacks on {@link com.raindropcentral.rdq.RDQ}), the free edition must forward those changes
 * to maintain parity with premium and common services.</p>
 */
package com.raindropcentral.rdq;
