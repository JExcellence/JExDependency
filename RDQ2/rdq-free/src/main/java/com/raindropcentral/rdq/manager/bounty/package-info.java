/**
 * Free edition bounty manager implementation.
 * <p>
 * {@link com.raindropcentral.rdq.manager.bounty.FreeBountyManager} satisfies the shared
 * {@link com.raindropcentral.rdq.manager.bounty.BountyManager} interface with deterministic
 * in-memory behaviour, enforcing the free tier limits reported by
 * {@link com.raindropcentral.rdq.manager.RDQFreeManager}. It also signals unsupported operations so
 * views can surface upgrade prompts while keeping navigation consistent with premium flows.
 * </p>
 */
package com.raindropcentral.rdq.manager.bounty;
