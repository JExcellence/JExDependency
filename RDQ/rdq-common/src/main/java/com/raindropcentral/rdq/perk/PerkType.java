package com.raindropcentral.rdq.perk;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Sealed interface representing perk activation types.
 *
 * <ul>
 *   <li>{@link Toggleable} - Player manually activates/deactivates</li>
 *   <li>{@link EventBased} - Triggers automatically on specific events</li>
 *   <li>{@link Passive} - Always active once unlocked</li>
 * </ul>
 *
 * @see Perk
 */
public sealed interface PerkType {

    record Toggleable() implements PerkType {
    }

    record EventBased(@NotNull String eventType) implements PerkType {
        public EventBased {
            Objects.requireNonNull(eventType, "eventType");
        }
    }

    record Passive() implements PerkType {
    }
}
