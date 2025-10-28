package com.raindropcentral.rdq.service;

import com.raindropcentral.rdq.perk.runtime.CooldownService;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CooldownServiceTest {

    @Mock
    private Player player;

    private CooldownService cooldownService;
    private UUID playerId;

    @BeforeEach
    void setUp() {
        cooldownService = new CooldownService();
        playerId = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(playerId);
    }

    @Test
    void testStartCooldown() {
        cooldownService.startCooldown(player, "speed", 10);

        assertTrue(cooldownService.isOnCooldown(player, "speed"));
    }

    @Test
    void testIsNotOnCooldownInitially() {
        assertFalse(cooldownService.isOnCooldown(player, "speed"));
    }

    @Test
    void testGetRemainingCooldown() {
        cooldownService.startCooldown(player, "speed", 10);

        long remaining = cooldownService.getRemainingCooldown(player, "speed");

        assertTrue(remaining > 0);
        assertTrue(remaining <= 10);
    }

    @Test
    void testGetRemainingCooldownWhenNotOnCooldown() {
        long remaining = cooldownService.getRemainingCooldown(player, "speed");

        assertEquals(0, remaining);
    }

    @Test
    void testEndCooldown() {
        cooldownService.startCooldown(player, "speed", 10);
        assertTrue(cooldownService.isOnCooldown(player, "speed"));

        cooldownService.endCooldown(player, "speed");

        assertFalse(cooldownService.isOnCooldown(player, "speed"));
    }

    @Test
    void testClearAllCooldowns() {
        cooldownService.startCooldown(player, "speed", 10);
        cooldownService.startCooldown(player, "jump", 10);

        assertTrue(cooldownService.isOnCooldown(player, "speed"));
        assertTrue(cooldownService.isOnCooldown(player, "jump"));

        cooldownService.clearAllCooldowns(player);

        assertFalse(cooldownService.isOnCooldown(player, "speed"));
        assertFalse(cooldownService.isOnCooldown(player, "jump"));
    }

    @Test
    void testMultiplePerkCooldowns() {
        cooldownService.startCooldown(player, "speed", 10);
        cooldownService.startCooldown(player, "jump", 20);

        assertTrue(cooldownService.isOnCooldown(player, "speed"));
        assertTrue(cooldownService.isOnCooldown(player, "jump"));

        long speedRemaining = cooldownService.getRemainingCooldown(player, "speed");
        long jumpRemaining = cooldownService.getRemainingCooldown(player, "jump");

        assertTrue(speedRemaining <= 10);
        assertTrue(jumpRemaining <= 20);
    }

    @Test
    void testCooldownExpiration() throws InterruptedException {
        cooldownService.startCooldown(player, "speed", 1);

        assertTrue(cooldownService.isOnCooldown(player, "speed"));

        Thread.sleep(1100);

        assertFalse(cooldownService.isOnCooldown(player, "speed"));
    }

    @Test
    void testMultiplePlayersIndependent() {
        Player player2 = org.mockito.Mockito.mock(Player.class);
        UUID playerId2 = UUID.randomUUID();
        org.mockito.Mockito.when(player2.getUniqueId()).thenReturn(playerId2);

        cooldownService.startCooldown(player, "speed", 10);
        cooldownService.startCooldown(player2, "speed", 20);

        assertTrue(cooldownService.isOnCooldown(player, "speed"));
        assertTrue(cooldownService.isOnCooldown(player2, "speed"));

        long player1Remaining = cooldownService.getRemainingCooldown(player, "speed");
        long player2Remaining = cooldownService.getRemainingCooldown(player2, "speed");

        assertTrue(player1Remaining <= 10);
        assertTrue(player2Remaining <= 20);
    }

    @Test
    void testStartCooldownWithZeroDuration() {
        cooldownService.startCooldown(player, "speed", 0);

        assertFalse(cooldownService.isOnCooldown(player, "speed"));
    }

    @Test
    void testStartCooldownWithNegativeDuration() {
        cooldownService.startCooldown(player, "speed", -5);

        assertFalse(cooldownService.isOnCooldown(player, "speed"));
    }
}
