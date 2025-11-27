package com.raindropcentral.rdq.listener;

import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.bounty.tracking.DamageTracker;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerQuitEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.Mockito.*;

/**
 * Unit tests for BountyPlayerQuitListener.
 * Tests quit event handling with various scenarios.
 * 
 * Requirements: 21.2
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BountyPlayerQuitListener Tests")
class BountyPlayerQuitListenerTest {
    
    @Mock
    private RDQ rdq;
    
    @Mock
    private DamageTracker damageTracker;
    
    @Mock
    private Player player;
    
    @Mock
    private PlayerQuitEvent event;
    
    private BountyPlayerQuitListener listener;
    private UUID playerUuid;
    
    @BeforeEach
    void setUp() {
        playerUuid = UUID.randomUUID();
        
        when(player.getUniqueId()).thenReturn(playerUuid);
        when(player.getName()).thenReturn("TestPlayer");
        when(event.getPlayer()).thenReturn(player);
        
        // Mock BountyConfig
        var bountyConfig = mock(com.raindropcentral.rdq.config.bounty.BountyConfig.class);
        when(bountyConfig.getDamageTrackingWindow()).thenReturn(30);
        when(rdq.getBountyConfig()).thenReturn(bountyConfig);
        
        listener = new BountyPlayerQuitListener(rdq);
    }
    
    @Test
    @DisplayName("Should clear damage tracking on player quit")
    void testQuitClearsDamageTracking() {
        // Act
        listener.onPlayerQuit(event);
        
        // Assert
        verify(damageTracker).clearDamage(playerUuid);
    }
    
    @Test
    @DisplayName("Should handle multiple quit events")
    void testMultipleQuitEvents() {
        // Act
        listener.onPlayerQuit(event);
        listener.onPlayerQuit(event);
        listener.onPlayerQuit(event);
        
        // Assert
        verify(damageTracker, times(3)).clearDamage(playerUuid);
    }
    
    @Test
    @DisplayName("Should handle quit for different players")
    void testQuitForDifferentPlayers() {
        // Arrange
        UUID player2Uuid = UUID.randomUUID();
        Player player2 = mock(Player.class);
        PlayerQuitEvent event2 = mock(PlayerQuitEvent.class);
        
        when(player2.getUniqueId()).thenReturn(player2Uuid);
        when(player2.getName()).thenReturn("Player2");
        when(event2.getPlayer()).thenReturn(player2);
        
        // Act
        listener.onPlayerQuit(event);
        listener.onPlayerQuit(event2);
        
        // Assert
        verify(damageTracker).clearDamage(playerUuid);
        verify(damageTracker).clearDamage(player2Uuid);
    }
}
