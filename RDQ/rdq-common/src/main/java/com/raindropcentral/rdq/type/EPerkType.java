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

package com.raindropcentral.rdq.type;

/**
 * Enumeration defining different types of perks and their activation behaviors.
 *
 * <p>This enum categorizes perks based on how they function:
 * <ul>
 *     <li>{@link #TOGGLEABLE_PASSIVE} - Perks that can be toggled on/off without cooldown</li>
 *     <li>{@link #EVENT_TRIGGERED} - Perks that activate on specific events and have cooldown</li>
 *     <li>{@link #INSTANT_USE} - Perks that provide immediate effects and have cooldown</li>
 *     <li>{@link #DURATION_BASED} - Perks that last for a specific duration with cooldown</li>
 * </ul>
 *
 * @author JExcellence
 * @version 1.0.0
 * @since TBD
 */
public enum EPerkType {
    
    /**
     * Toggleable perks that can be enabled/disabled without cooldown.
     * These perks typically provide passive effects while active.
     * Examples: Night vision toggle, speed boost toggle, flight mode
     */
    TOGGLEABLE_PASSIVE(false, true, false),
    
    /**
     * Event-triggered perks that activate automatically on specific events.
     * These perks have cooldown and trigger based on game events.
     * Examples: Strength boost on potion drink, damage reduction on taking damage
     */
    EVENT_TRIGGERED(true, false, true),
    
    /**
     * Instant-use perks that provide immediate effects when activated.
     * These perks have cooldown and provide one-time effects.
     * Examples: Instant heal, teleport to spawn, clear inventory
     */
    INSTANT_USE(true, false, false),
    
    /**
     * Duration-based perks that last for a specific time period.
     * These perks have cooldown and automatically deactivate after duration.
     * Examples: Temporary invincibility, time-limited flight, temporary strength
     */
    DURATION_BASED(true, false, true);
    
    private final boolean hasCooldown;
    private final boolean isToggleable;
    private final boolean isEventBased;
    
    /**
     * Constructs a perk type with specified characteristics.
     *
     * @param hasCooldown   whether this perk type uses cooldown
     * @param isToggleable  whether this perk type can be toggled on/off
     * @param isEventBased  whether this perk type is triggered by events
     */
    EPerkType(
        final boolean hasCooldown,
        final boolean isToggleable,
        final boolean isEventBased
    ) {
        this.hasCooldown = hasCooldown;
        this.isToggleable = isToggleable;
        this.isEventBased = isEventBased;
    }
    
    /**
     * Checks if this perk type uses cooldown.
     *
     * @return {@code true} if this perk type has cooldown, {@code false} otherwise
     */
    public boolean hasCooldown() {
        return this.hasCooldown;
    }
    
    /**
     * Checks if this perk type can be toggled on/off.
     *
     * @return {@code true} if this perk type is toggleable, {@code false} otherwise
     */
    public boolean isToggleable() {
        return this.isToggleable;
    }
    
    /**
     * Checks if this perk type is triggered by events.
     *
     * @return {@code true} if this perk type is event-based, {@code false} otherwise
     */
    public boolean isEventBased() {
        return this.isEventBased;
    }
    
    /**
     * Gets a description of this perk type's behavior.
     *
     * @return a descriptive string explaining the perk type
     */
    public String getDescription() {
        return switch (this) {
            case TOGGLEABLE_PASSIVE -> "Toggleable passive effect without cooldown";
            case EVENT_TRIGGERED -> "Automatically triggered by events with cooldown";
            case INSTANT_USE -> "Immediate effect with cooldown";
            case DURATION_BASED -> "Temporary effect with duration and cooldown";
        };
    }
}
