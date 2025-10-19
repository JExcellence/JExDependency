package com.raindropcentral.rdq.type;

/**
 * Enumerates the supported perk behavior categories and their activation semantics.
 * <p>
 * Each constant represents a lifecycle profile that determines whether a perk can be
 * toggled, reacts to events, or applies cooldown management for balancing purposes.
 * <ul>
 *     <li>{@link #TOGGLEABLE_PASSIVE} &mdash; Passive perks with manual toggling and no cooldown.</li>
 *     <li>{@link #EVENT_TRIGGERED} &mdash; Event-driven perks that fire automatically with cooldown handling.</li>
 *     <li>{@link #INSTANT_USE} &mdash; Immediate-use perks that consume cooldown on activation.</li>
 *     <li>{@link #DURATION_BASED} &mdash; Time-bound perks that expire after a duration while respecting cooldown.</li>
 * </ul>
 * </p>
 *
 * @author JExcellence
 * @version 1.0.1
 * @since 1.0.0
 */
public enum EPerkType {

    /**
     * Toggleable perks that can be enabled or disabled without incurring cooldown.
     * These perks typically provide passive effects while active.
     * Examples include night vision toggles, speed boost toggles, or flight modes.
     */
    TOGGLEABLE_PASSIVE(false, true, false),

    /**
     * Event-triggered perks that activate automatically when qualifying gameplay events occur.
     * These perks enforce cooldown tracking and respond directly to event data.
     * Examples include granting strength on potion consumption or damage reduction on taking hits.
     */
    EVENT_TRIGGERED(true, false, true),

    /**
     * Instant-use perks that provide one-off effects at the moment of activation.
     * These perks enforce cooldown and deliver immediate utility.
     * Examples include instant healing, teleporting to spawn, or clearing inventory.
     */
    INSTANT_USE(true, false, false),

    /**
     * Duration-based perks that remain active for a configured length of time.
     * These perks enforce cooldown and automatically expire once the duration lapses.
     * Examples include temporary invincibility, time-limited flight, or short-term strength boosts.
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
     * Indicates whether this perk profile requires cooldown management between activations.
     *
     * @return {@code true} if perks of this type track cooldown, {@code false} otherwise
     */
    public boolean hasCooldown() {
        return this.hasCooldown;
    }

    /**
     * Indicates whether perks following this profile support manual toggling by the player.
     *
     * @return {@code true} if the perk type is toggleable, {@code false} otherwise
     */
    public boolean isToggleable() {
        return this.isToggleable;
    }

    /**
     * Indicates whether perks adhering to this profile are activated by external events.
     *
     * @return {@code true} if the perk type is event-based, {@code false} otherwise
     */
    public boolean isEventBased() {
        return this.isEventBased;
    }

    /**
     * Provides a short human-readable description summarizing the perk category.
     *
     * @return descriptive text outlining the activation behavior for this perk type
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