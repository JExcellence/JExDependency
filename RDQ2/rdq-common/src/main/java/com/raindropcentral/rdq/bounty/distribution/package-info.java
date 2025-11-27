/**
 * Reward distribution implementations for the bounty system.
 * <p>
 * This package contains different strategies for distributing bounty rewards to hunters:
 * </p>
 * <ul>
 *   <li>{@link com.raindropcentral.rdq.bounty.distribution.InstantRewardDistributor} - 
 *       Adds items directly to hunter's inventory</li>
 *   <li>{@link com.raindropcentral.rdq.bounty.distribution.VirtualRewardDistributor} - 
 *       Credits rewards to virtual storage</li>
 *   <li>{@link com.raindropcentral.rdq.bounty.distribution.DropRewardDistributor} - 
 *       Drops items at death location</li>
 *   <li>{@link com.raindropcentral.rdq.bounty.distribution.ChestRewardDistributor} - 
 *       Places rewards in a chest at death location</li>
 * </ul>
 *
 * @author JExcellence
 * @version 1.0.0
 * @since 2.0.0
 */
package com.raindropcentral.rdq.bounty.distribution;
