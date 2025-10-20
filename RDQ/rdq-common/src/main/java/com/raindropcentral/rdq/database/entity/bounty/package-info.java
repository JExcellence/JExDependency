/**
 * Entities that describe bounty contracts and their rewards.
 * <p>
 * {@link com.raindropcentral.rdq.database.entity.bounty.RBounty} binds a
 * {@link com.raindropcentral.rdq.database.entity.player.RDQPlayer} to the commissioner that posted the
 * bounty, the promised {@link com.raindropcentral.rdq.database.entity.reward.RewardItem reward items},
 * and accumulated payout history. Bounty data fuels the GUI flows prepared during the view
 * initialization stage of {@link com.raindropcentral.rdq.RDQ#onEnable()}, so the repository wiring
 * stage loads these records after views are ready to render.</p>
 */
package com.raindropcentral.rdq.database.entity.bounty;
