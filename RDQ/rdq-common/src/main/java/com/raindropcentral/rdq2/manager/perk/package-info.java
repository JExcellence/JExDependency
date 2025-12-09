/**
 * Contracts for perk management across RDQ editions.
 * <p>
 * {@link com.raindropcentral.rdq2.manager.perk.PerkManager} defines the extension point that each
 * edition-specific {@link com.raindropcentral.rdq2.manager.RDQManager} supplies. Implementations may
 * expose different data sources or capability sets, but the shared contract keeps services and views
 * synchronized with the staged lifecycle enforced by the parent manager.
 * </p>
 */
package com.raindropcentral.rdq2.manager.perk;
