package com.raindropcentral.rdq.database.entity.perk;

/**
 * Enumeration of perk activation behavior types.
 * <p>
 * Defines how a perk is activated and when its effects are applied.
 * </p>
 *
 * @author JExcellence
 * @version 1.0.0
 */
public enum PerkType {
    /**
     * Passive perks that are always active when enabled.
     * Effects are continuously applied (e.g., potion effects, fly ability).
     */
    PASSIVE,
    
    /**
     * Perks that trigger when specific game events occur.
     * Effects are applied when the configured event happens (e.g., on damage, on kill).
     */
    EVENT_TRIGGERED,
    
    /**
     * Perks with cooldown-based activation.
     * Can be manually activated but have a cooldown period between uses.
     */
    COOLDOWN_BASED,
    
    /**
     * Perks that have a percentage chance to trigger on events.
     * Similar to EVENT_TRIGGERED but with a probability factor.
     */
    PERCENTAGE_BASED
}
