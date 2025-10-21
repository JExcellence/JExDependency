package com.raindropcentral.rdq.type;

/**
 * Defines the strategies used by the bounty system to select the reward recipient.
 * <p>
 * Implementations rely on this enumeration when deciding which player should receive a bounty
 * after a target has been eliminated.
 * </p>
 * <p>
 * Available strategies:
 * </p>
 * <ul>
 *     <li>{@link #MOST_DAMAGE} prioritises the participant who contributed the highest cumulative
 *     damage across the encounter.</li>
 *     <li>{@link #LAST_HIT} awards the bounty to the participant responsible for the final blow.</li>
 * </ul>
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
public enum EBountyClaimMode {

    /**
     * Awards the bounty to the participant who contributed the highest total damage to the target.
     */
    MOST_DAMAGE,

    /**
     * Awards the bounty to the participant who defeats the target with the decisive final strike.
     */
    LAST_HIT
}
