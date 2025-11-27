/**
 * Jackson helpers that manage reward polymorphism and Bukkit payloads.
 * <p>
 * {@link com.raindropcentral.rdq.database.json.reward.RewardParser} mirrors the requirement parser but
 * targets {@link com.raindropcentral.rdq.reward.AbstractReward} hierarchies using the
 * {@link com.raindropcentral.rdq.database.json.reward.RewardMixin} type metadata. Converters such as
 * {@link com.raindropcentral.rdq.database.converter.RewardConverter} call into this package so reward
 * definitions embedded in bounty, quest, or rank entities survive the staged enable lifecycle outlined
 * in {@link com.raindropcentral.rdq.RDQ#onEnable()} through repository initialization.
 * </p>
 */
package com.raindropcentral.rdq.database.json.reward;
