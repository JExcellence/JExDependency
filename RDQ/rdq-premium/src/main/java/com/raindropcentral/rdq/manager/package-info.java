/**
 * Premium edition manager implementations.
 * <p>
 * {@link com.raindropcentral.rdq.manager.RDQPremiumManager} wires repository-backed managers that
 * respect the shared lifecycle contract defined by {@code rdq-common}. It receives repository
 * factories during construction, initializes them during the lifecycle {@code initialize()} stage,
 * and exposes full CRUD services to the UI while preserving the same navigation surface offered by
 * the free tier.
 * </p>
 */
package com.raindropcentral.rdq.manager;
