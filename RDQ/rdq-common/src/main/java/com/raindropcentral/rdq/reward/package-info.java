/**
 * Runtime reward implementations.
 * <p>
 * Concrete {@link com.raindropcentral.rdq2.reward.AbstractReward} types are instantiated from
 * {@link com.raindropcentral.rdq2.config.reward.RewardSection} definitions stored in YAML resources.
 * {@link com.raindropcentral.rdq2.database.json.reward.RewardParser} and
 * {@link com.raindropcentral.rdq2.database.converter.RewardConverter} coordinate serialization so the same model powers
 * persistence, configuration, and in-game execution.
 * </p>
 * <p>
 * Keep the enum identifiers in {@link com.raindropcentral.rdq2.reward.AbstractReward.Type} synchronized with the
 * {@code type} fields in {@code rank/paths/*.yml} and {@code perks/*.yml}. After adding a new reward type, update the
 * relevant config sections and run {@code ./gradlew :RDQ:rdq-common:check} to exercise the serialization pipeline.
 * </p>
 */
package com.raindropcentral.rdq.reward;
