package com.raindropcentral.rdq.api;

/**
 * Free edition perk service with limited features.
 *
 * <p>Provides perk functionality with a single active perk at a time.
 * Premium perk types are not available in the free edition.
 *
 * @see PerkService
 * @see PremiumPerkService
 */
public non-sealed interface FreePerkService extends PerkService {
}
