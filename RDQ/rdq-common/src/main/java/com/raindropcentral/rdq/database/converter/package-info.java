/**
 * Attribute converters that adapt gameplay domain objects for persistence.
 * <p>
 * Each converter couples Jackson-based parsers from {@code com.raindropcentral.rdq.database.json}
 * with Hibernate so complex requirement, reward, and icon models can be written to and restored
 * from text columns. For example, {@link com.raindropcentral.rdq.database.converter.RewardConverter}
 * serializes {@link com.raindropcentral.rdq.reward.AbstractReward} instances using
 * {@link com.raindropcentral.rdq.database.json.reward.RewardParser} and revives them when entities
 * such as {@link com.raindropcentral.rdq.database.entity.bounty.RBounty} are loaded from the
 * database.
 * </p>
 *
 * <p>
 * These converters are registered via {@code @Converter(autoApply = true)} so repository operations
 * transparently exchange JSON payloads during the repository wiring stage described in
 * {@link com.raindropcentral.rdq.RDQ#onEnable()}.
 * </p>
 */
package com.raindropcentral.rdq.database.converter;
