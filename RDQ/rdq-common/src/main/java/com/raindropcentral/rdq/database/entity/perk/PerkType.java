/*
 * Copyright (c) 2021-2026 Antimatter Zone LLC. All rights reserved.
 *
 * This source code is proprietary and confidential to Antimatter Zone LLC.
 * Unauthorized copying, modification, distribution, display, performance,
 * publication, sublicensing, or creation of derivative works is prohibited
 * without prior written permission from Antimatter Zone LLC, except to the
 * extent permitted by applicable United States law.
 *
 * This notice is intended to preserve all rights and remedies available under
 * the laws of the State of Washington and the United States of America.
 */

package com.raindropcentral.rdq.database.entity.perk;

/**
 * Enumeration of perk activation behavior types.
 *
 * <p>Defines how a perk is activated and when its effects are applied.
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
