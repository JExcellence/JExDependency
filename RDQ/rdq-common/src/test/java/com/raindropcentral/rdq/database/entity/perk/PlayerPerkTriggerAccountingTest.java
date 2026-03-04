/*
 * PlayerPerkTriggerAccountingTest.java
 *
 * @author RaindropCentral
 * @version 6.0.0
 */

package com.raindropcentral.rdq.database.entity.perk;

import com.raindropcentral.rdq.database.entity.player.RDQPlayer;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * Verifies that player perk trigger counts are tracked independently from enabled session state.
 *
 * @author RaindropCentral
 * @since 6.0.0
 * @version 6.0.0
 */
class PlayerPerkTriggerAccountingTest {

    @Test
    void markActivatedDoesNotIncrementTriggerCount() {
        final PlayerPerk playerPerk = new PlayerPerk(
            new RDQPlayer(UUID.randomUUID(), "TestPlayer"),
            mock(Perk.class)
        );

        playerPerk.markActivated();

        assertTrue(playerPerk.isActive());
        assertEquals(0, playerPerk.getActivationCount());
    }

    @Test
    void recordTriggerIncrementsTriggerCountWithoutChangingActiveState() {
        final PlayerPerk playerPerk = new PlayerPerk(
            new RDQPlayer(UUID.randomUUID(), "TestPlayer"),
            mock(Perk.class)
        );

        playerPerk.recordTrigger();
        playerPerk.recordTrigger();

        assertFalse(playerPerk.isActive());
        assertEquals(2, playerPerk.getActivationCount());
    }

    @Test
    void markDeactivatedKeepsRecordedTriggerCount() {
        final PlayerPerk playerPerk = new PlayerPerk(
            new RDQPlayer(UUID.randomUUID(), "TestPlayer"),
            mock(Perk.class)
        );

        playerPerk.markActivated();
        playerPerk.recordTrigger();
        playerPerk.markDeactivated();

        assertFalse(playerPerk.isActive());
        assertEquals(1, playerPerk.getActivationCount());
    }
}
