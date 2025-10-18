/**
 * Persistent gameplay aggregates captured as JPA entities.
 * <p>
 * RDQ models players, quests, ranks, perks, rewards, and bounties with dedicated entity classes
 * that encode the relationships between systems. Entities link directly to in-game mechanics:
 * <ul>
 *   <li>{@link com.raindropcentral.rdq.database.entity.player.RDQPlayer} keeps the canonical record
 *   of a player, including owned ranks ({@link com.raindropcentral.rdq.database.entity.rank.RPlayerRank})
 *   and acquired perks ({@link com.raindropcentral.rdq.database.entity.perk.RPlayerPerk}).</li>
 *   <li>{@link com.raindropcentral.rdq.database.entity.bounty.RBounty} stores bounty postings and
 *   reward ledgers connected to a player profile.</li>
 *   <li>Quest, rank, and reward subpackages contain the tables that drive unlock progression and
 *   payouts surfaced via RDQ managers.</li>
 * </ul>
 * Bidirectional associations are configured so repositories can eager- or lazy-load the state needed
 * by the component and view layers initialized earlier in the enable pipeline.
 * </p>
 */
package com.raindropcentral.rdq.database.entity;
