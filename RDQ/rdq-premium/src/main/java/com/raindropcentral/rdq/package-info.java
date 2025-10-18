/**
 * Premium edition bindings for the RaindropQuests runtime.
 *
 * <p>The entry points in this package ({@link com.raindropcentral.rdq.RDQPremium} and
 * {@link com.raindropcentral.rdq.RDQPremiumImpl}) wrap the shared
 * {@link com.raindropcentral.rdq.RDQ lifecycle} provided by {@code rdq-common}. They participate
 * in the same staged startup—platform bootstrap, component/view setup, and repository wiring—so
 * that premium-specific services stay synchronized with cross-edition infrastructure.</p>
 *
 * <p>Premium responsibilities include:</p>
 * <ul>
 *     <li>Registering persistence providers and Bukkit services that expose premium-only APIs.</li>
 *     <li>Surfacing unlimited gameplay capacities and CRUD operations back through the common RDQ
 *     base so commands, views, and metrics reflect the paid feature set.</li>
 *     <li>Failing fast when shared lifecycle steps throw to avoid diverging from the baseline
 *     behavior exercised by {@link com.raindropcentral.rdq.RDQ}.</li>
 * </ul>
 *
 * <p>Any lifecycle change in {@code rdq-common} must be mirrored here to keep premium deployments
 * compatible with the shared platform and to preserve parity with the free edition.</p>
 */
package com.raindropcentral.rdq;
