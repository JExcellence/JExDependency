/**
 * Bounty-specific view flow built on the unified navigation framework.
 * <p>
 * The views in this package (for example {@link com.raindropcentral.rdq.view.bounty.BountyMainView},
 * {@link com.raindropcentral.rdq.view.bounty.BountyOverviewView}, and
 * {@link com.raindropcentral.rdq.view.bounty.BountyCreationView}) compose multi-step inventory frames
 * that open each other through {@link com.raindropcentral.rplatform.view.BaseView}-provided navigation
 * helpers. They consume the edition-aware {@link com.raindropcentral.rdq.service.bounty.BountyService}
 * facade, ensuring the same menu flow adapts seamlessly whether the active manager uses in-memory or
 * repository-backed storage.
 * </p>
 */
package com.raindropcentral.rdq.view.bounty;
