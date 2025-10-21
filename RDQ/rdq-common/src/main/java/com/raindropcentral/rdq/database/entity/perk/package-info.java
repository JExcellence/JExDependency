/**
 * Entities that capture perk catalog entries and a player's perk lifecycle.
 * <p>
 * {@link com.raindropcentral.rdq.database.entity.perk.RPerk} defines the static metadata for each
 * perk, while {@link com.raindropcentral.rdq.database.entity.perk.RPlayerPerk} records when a
 * {@link com.raindropcentral.rdq.database.entity.player.RDQPlayer} acquired, activated, or exhausted a
 * perk, including cooldowns and expiration timestamps. This state drives activation limits checked by
 * managers once repositories become available after the enable lifecycle described in
 * {@link com.raindropcentral.rdq.RDQ#onEnable()}.
 * </p>
 */
package com.raindropcentral.rdq.database.entity.perk;
