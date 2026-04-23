package de.jexcellence.quests.database.entity;

/**
 * How a perk is consumed by the runtime.
 *
 * <ul>
 *   <li>{@code PASSIVE} — effect always on when owned, no toggle</li>
 *   <li>{@code TOGGLE} — player flips it on/off, state persists</li>
 *   <li>{@code ACTIVE} — one-shot trigger with cooldown</li>
 * </ul>
 */
public enum PerkKind {
    PASSIVE,
    TOGGLE,
    ACTIVE
}
