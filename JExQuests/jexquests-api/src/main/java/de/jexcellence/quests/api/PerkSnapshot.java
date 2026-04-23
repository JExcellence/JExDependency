package de.jexcellence.quests.api;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Immutable snapshot of one player's ownership state for a perk.
 *
 * @param playerUuid owning player
 * @param perkIdentifier perk identifier
 * @param kind {@code "PASSIVE"} / {@code "TOGGLE"} / {@code "ACTIVE"}
 * @param enabled per-player toggle state
 * @param cooldownRemainingSeconds seconds left until the perk may be
 *        activated again ({@code 0} when off-cooldown or non-ACTIVE)
 * @param activationCount lifetime activations for this player
 */
public record PerkSnapshot(
        @NotNull UUID playerUuid,
        @NotNull String perkIdentifier,
        @NotNull String kind,
        boolean enabled,
        long cooldownRemainingSeconds,
        long activationCount
) {
}
