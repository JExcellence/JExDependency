/**
 * Premium edition manager implementations.
 *
 * <p>These classes extend the shared {@link com.raindropcentral.rdq.manager.RDQManager} from
 * {@code rdq-common} and are wired during the component setup phase of
 * {@link com.raindropcentral.rdq.RDQ}. They provide persistence-backed managers that surface the
 * full premium feature set while still honoring the shared staged lifecycle.</p>
 *
 * <p>Key responsibilities:</p>
 * <ul>
 *     <li>Provide repository-backed manager instances so premium builds can execute full CRUD
 *     workflows while reusing the command/view orchestration from the common module.</li>
 *     <li>Report {@link com.raindropcentral.rdq.manager.RDQManager#isPremium()} as {@code true} so
 *     the common lifecycle enables premium-only listeners, repositories, and background tasks.</li>
 *     <li>Emit lifecycle logging aligned with the free manager to keep operational dashboards in
 *     sync across editions.</li>
 * </ul>
 *
 * <p>Whenever the shared manager contract or the {@link com.raindropcentral.rdq.RDQ}
 * initialization order changes, update these implementations to maintain compatibility with the
 * shared infrastructure.</p>
 */
package com.raindropcentral.rdq.manager;
