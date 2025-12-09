/**
 * Reward configuration mapping.
 * <p>
 * {@link com.raindropcentral.rdq2.config.reward.RewardSection} mirrors YAML reward definitions so they can be transformed into
 * {@link com.raindropcentral.rdq2.reward.AbstractReward} instances. Parsing flows through
 * {@link com.raindropcentral.rdq2.database.json.reward.RewardParser} and persistence adapters such as
 * {@link com.raindropcentral.rdq2.database.converter.RewardConverter}.
 * </p>
 * <p>
 * Reward blocks appear in rank definitions ({@code rank/paths/*.yml}) and perk files ({@code perks/*.yml}). Keep those documents
 * synchronized with the section fields and run {@code ./gradlew :RDQ:rdq-common:check} after updates to confirm serialization logic.
 * </p>
 */
package com.raindropcentral.rdq2.config.reward;
