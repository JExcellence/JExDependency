/**
 * Requirement parsing and persistence helpers.
 * <p>
 * {@link com.raindropcentral.rdq2.utility.requirement.RequirementFactory} converts configuration
 * sections into typed {@link com.raindropcentral.rdq2.database.entity.rank.RRequirement} models,
 * persists them asynchronously, and exposes defensive validation that managers can reuse when
 * preparing rank upgrade flows. This keeps requirement presentation in the UI aligned with repository
 * state while supporting asynchronous execution across editions.
 * </p>
 */
package com.raindropcentral.rdq2.utility.requirement;
