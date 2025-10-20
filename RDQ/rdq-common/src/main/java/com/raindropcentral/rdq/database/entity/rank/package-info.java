/**
 * Rank hierarchies and player advancement entities.
 * <p>
 * {@link com.raindropcentral.rdq.database.entity.rank.RRankTree} and
 * {@link com.raindropcentral.rdq.database.entity.rank.RRank} define rank ladders exposed by
 * {@link com.raindropcentral.rdq.utility.rank.RankSystemFactory}, while
 * {@link com.raindropcentral.rdq.database.entity.rank.RPlayerRank} and
 * {@link com.raindropcentral.rdq.database.entity.rank.RPlayerRankPath} capture where each
 * {@link com.raindropcentral.rdq.database.entity.player.RDQPlayer} currently sits within those ladders.
 * Upgrade checkpoints and pending requirements are tracked by
 * {@link com.raindropcentral.rdq.database.entity.rank.RPlayerRankUpgradeProgress} and related
 * requirement entities, relying on the JSON converters to persist complex requirement payloads until
 * repositories are wired during the {@link com.raindropcentral.rdq.RDQ#onEnable()} lifecycle.
 * </p>
 */
package com.raindropcentral.rdq.database.entity.rank;
