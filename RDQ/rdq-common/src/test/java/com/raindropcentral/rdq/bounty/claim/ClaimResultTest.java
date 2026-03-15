package com.raindropcentral.rdq.bounty.claim;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests winner mapping and helper constructors on {@link ClaimResult}.
 *
 * @author ItsRainingHP
 * @since 6.0.0
 * @version 6.0.0
 */
class ClaimResultTest {

    @Test
    void emptyResultContainsNoWinners() {
        final ClaimResult result = ClaimResult.empty();

        assertFalse(result.hasWinners());
        assertEquals(0, result.getWinnerCount());
        assertTrue(result.winners().isEmpty());
    }

    @Test
    void singleWinnerIncludesFullRewardProportion() {
        final UUID winnerUniqueId = UUID.randomUUID();

        final ClaimResult result = ClaimResult.singleWinner(winnerUniqueId);

        assertTrue(result.hasWinners());
        assertEquals(1, result.getWinnerCount());
        assertEquals(1.0D, result.winners().get(winnerUniqueId), 0.000_1D);
    }

    @Test
    void winnersMapIsExposedAsUnmodifiableView() {
        final UUID winnerUniqueId = UUID.randomUUID();
        final Map<UUID, Double> winners = new HashMap<>();
        winners.put(winnerUniqueId, 1.0D);

        final ClaimResult result = new ClaimResult(winners);

        assertThrows(
            UnsupportedOperationException.class,
            () -> result.winners().put(UUID.randomUUID(), 0.5D)
        );
    }
}
