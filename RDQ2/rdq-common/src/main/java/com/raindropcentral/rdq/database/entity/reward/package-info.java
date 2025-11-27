/**
 * Value types that represent persisted reward payloads.
 * <p>
 * {@link com.raindropcentral.rdq.database.entity.reward.RewardItem} captures the serialized details
 * of item-based payouts attached to quests, ranks, or bounty records. The entity is typically stored
 * as part of collections on {@link com.raindropcentral.rdq.database.entity.bounty.RBounty} or quest
 * upgrade records and is reconstructed via the JSON converters when repositories hydrate data after
 * {@link com.raindropcentral.rdq.RDQ#onEnable()} reaches repository wiring.
 * </p>
 */
package com.raindropcentral.rdq.database.entity.reward;
