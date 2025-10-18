/**
 * Free edition manager specializations.
 *
 * <p>Implementations in this package extend the shared
 * {@link com.raindropcentral.rdq.manager.RDQManager} contract defined in {@code rdq-common} and
 * plug into the {@link com.raindropcentral.rdq.RDQ} lifecycle during the component setup phase.
 * They replace persistence-backed services with lightweight, in-memory counterparts so the free
 * jar can demonstrate quest flows without exposing write operations.</p>
 *
 * <p>Feature gating is enforced here by:</p>
 * <ul>
 *     <li>Returning {@code Free*} manager implementations that cap entity counts and disable
 *     mutating APIs.</li>
 *     <li>Declaring {@link com.raindropcentral.rdq.manager.RDQManager#isPremium()} as
 *     {@code false} so the shared infrastructure skips premium-only listeners and repository
 *     wiring.</li>
 *     <li>Emitting lifecycle logs that mirror the premium manager so monitoring remains consistent
 *     across editions.</li>
 * </ul>
 *
 * <p>Whenever {@link com.raindropcentral.rdq.manager.RDQManager} gains new hooks or required
 * collaborators, update these adapters in lockstep to keep the free edition aligned with the
 * staged initialization defined by {@link com.raindropcentral.rdq.RDQ}.</p>
 */
package com.raindropcentral.rdq.manager;
