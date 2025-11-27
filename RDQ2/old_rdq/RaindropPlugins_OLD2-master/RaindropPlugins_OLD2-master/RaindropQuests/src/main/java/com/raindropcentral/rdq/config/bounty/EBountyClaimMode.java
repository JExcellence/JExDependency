package com.raindropcentral.rdq.config.bounty;

/**
 * Enumeration of possible modes for awarding a bounty.
 * <p>
 * Determines the criteria used to decide which player receives a bounty reward.
 * </p>
 *
 * @author JExcellence
 * @version 1.0.0
 * @since TBD
 */
public enum EBountyClaimMode {
	/**
	 * The bounty is awarded to the player who dealt the most damage.
	 */
	MOST_DAMAGE,
	
	/**
	 * The bounty is awarded to the player who lands the last hit.
	 */
	LAST_HIT
}