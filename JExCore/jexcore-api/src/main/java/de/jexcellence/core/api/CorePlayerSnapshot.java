package de.jexcellence.core.api;

import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Immutable projection of a {@code CorePlayer} row exposed to third-party
 * consumers. Decouples API callers from the persistence entity.
 *
 * @param uniqueId player UUID
 * @param playerName latest cached player name
 * @param firstSeen first-seen timestamp
 * @param lastSeen most recent last-seen timestamp
 */
public record CorePlayerSnapshot(
        @NotNull UUID uniqueId,
        @NotNull String playerName,
        @NotNull LocalDateTime firstSeen,
        @NotNull LocalDateTime lastSeen
) {
}
