package com.raindropcentral.rdq.perk;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PlayerPerkStateTest {

    @Test
    void createInitialState() {
        var playerId = UUID.randomUUID();
        var state = PlayerPerkState.initial(playerId, "speed");

        assertEquals(playerId, state.playerId());
        assertEquals("speed", state.perkId());
        assertFalse(state.unlocked());
        assertFalse(state.active());
        assertNull(state.cooldownExpiry());
        assertFalse(state.isOnCooldown());
    }

    @Test
    void isOnCooldownWhenExpiryInFuture() {
        var playerId = UUID.randomUUID();
        var futureExpiry = Instant.now().plusSeconds(300);
        var state = new PlayerPerkState(playerId, "speed", true, false, futureExpiry);

        assertTrue(state.isOnCooldown());
        assertTrue(state.remainingCooldown().isPresent());
    }

    @Test
    void notOnCooldownWhenExpiryInPast() {
        var playerId = UUID.randomUUID();
        var pastExpiry = Instant.now().minusSeconds(10);
        var state = new PlayerPerkState(playerId, "speed", true, false, pastExpiry);

        assertFalse(state.isOnCooldown());
        assertTrue(state.remainingCooldown().isEmpty());
    }

    @Test
    void notOnCooldownWhenExpiryNull() {
        var playerId = UUID.randomUUID();
        var state = new PlayerPerkState(playerId, "speed", true, false, null);

        assertFalse(state.isOnCooldown());
        assertTrue(state.remainingCooldown().isEmpty());
    }

    @Test
    void withUnlockedCreatesNewState() {
        var playerId = UUID.randomUUID();
        var original = PlayerPerkState.initial(playerId, "speed");
        var unlocked = original.withUnlocked(true);

        assertFalse(original.unlocked());
        assertTrue(unlocked.unlocked());
        assertEquals(original.playerId(), unlocked.playerId());
        assertEquals(original.perkId(), unlocked.perkId());
    }

    @Test
    void withActiveCreatesNewState() {
        var playerId = UUID.randomUUID();
        var original = new PlayerPerkState(playerId, "speed", true, false, null);
        var active = original.withActive(true);

        assertFalse(original.active());
        assertTrue(active.active());
    }

    @Test
    void withCooldownCreatesNewState() {
        var playerId = UUID.randomUUID();
        var original = new PlayerPerkState(playerId, "speed", true, false, null);
        var cooldownExpiry = Instant.now().plusSeconds(300);
        var withCooldown = original.withCooldown(cooldownExpiry);

        assertNull(original.cooldownExpiry());
        assertEquals(cooldownExpiry, withCooldown.cooldownExpiry());
    }

    @Test
    void remainingCooldownCalculation() {
        var playerId = UUID.randomUUID();
        var futureExpiry = Instant.now().plusSeconds(120);
        var state = new PlayerPerkState(playerId, "speed", true, false, futureExpiry);

        var remaining = state.remainingCooldown();
        assertTrue(remaining.isPresent());
        assertTrue(remaining.get().getSeconds() <= 120);
        assertTrue(remaining.get().getSeconds() >= 118);
    }

    @Test
    void rejectNullPlayerId() {
        assertThrows(NullPointerException.class, () ->
            new PlayerPerkState(null, "speed", false, false, null)
        );
    }

    @Test
    void rejectNullPerkId() {
        assertThrows(NullPointerException.class, () ->
            new PlayerPerkState(UUID.randomUUID(), null, false, false, null)
        );
    }
}
