/**
 * Bounty domain managers shared across RDQ editions.
 * <p>
 * The {@link com.raindropcentral.rdq.manager.bounty.BountyManager} contract abstracts bounty access
 * so edition-specific implementations can provide different storage models without impacting callers.
 * Free builds expose {@code FreeBountyManager} with in-memory limits, whereas premium builds wire
 * {@code PremiumBountyManager} against asynchronous repositories such as
 * {@link com.raindropcentral.rdq.database.repository.RBountyRepository}. The contract includes
 * pagination, creation, mutation, and entitlement checks, allowing the coordinating
 * {@link com.raindropcentral.rdq.manager.RDQManager} to defer edition-specific policy to the
 * implementation while keeping UI flows and services edition-agnostic.
 * </p>
 */
package com.raindropcentral.rdq.manager.bounty;
