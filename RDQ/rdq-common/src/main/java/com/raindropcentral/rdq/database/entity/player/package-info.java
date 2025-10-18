/**
 * Core player profile entity definitions.
 * <p>
 * {@link com.raindropcentral.rdq.database.entity.player.RDQPlayer} anchors nearly every gameplay
 * relationship—linking to the active bounty ({@link com.raindropcentral.rdq.database.entity.bounty.RBounty}),
 * owned ranks ({@link com.raindropcentral.rdq.database.entity.rank.RPlayerRank}), rank paths,
 * and {@link com.raindropcentral.rdq.database.entity.perk.RPlayerPerk} records. The entity mirrors the
 * data surfaced by {@code BountyManager}, {@code RankSystemFactory}, and other services created before
 * repository wiring, allowing those systems to hydrate player state once the
 * {@link com.raindropcentral.rdq.RDQ#onEnable()} lifecycle reaches repository wiring.
 * </p>
 */
package com.raindropcentral.rdq.database.entity.player;
