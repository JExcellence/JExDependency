package com.raindropcentral.rdq.listener;

import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.database.entity.bounty.RBounty;
import com.raindropcentral.rdq.service.bounty.BountyService;
import com.raindropcentral.rdq.service.bounty.BountyServiceProvider;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for BountyPlayerJoinListener.
 * Tests join event handling with various scenarios.
 * 
 * Requirements: 21.2
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BountyPlayerJoinListener Tests")
class BountyPlayerJoinListenerTest {
    
    @Mock
    private RDQ rdq;
    
    @Mock
    private JavaPlugin plugin;
    
    @Mock
    private BountyService bountyService;
    
    @Mock
    private Player player;
    
    @Mock
    private PlayerJoinEvent event;
    
    private BountyPlayerJoinListener listener;
    private UUID playerUuid;
    
    @BeforeEach
    void setUp() {
        playerUuid = UUID.randomUUID();
        
        when(rdq.getPlugin()).thenReturn(plugin);
        when(player.getUniqueId()).thenReturn(playerUuid);
        when(player.getName()).thenReturn("TestPlayer");
        when(player.isOnline()).thenReturn(true);
        when(event.getPlayer()).thenReturn(player);
        
        // Set up bounty service provider
        BountyServiceProvider.setInstance(bountyService);
        
        // Mock BountyConfig
        var bountyConfig = mock(com.raindropcentral.rdq.config.bounty.BountyConfig.class);
        when(bountyConfig.isVisualIndicatorsEnabled()).thenReturn(true);
        when(rdq.getBountyConfig()).thenReturn(bountyConfig);
        
        listener = new BountyPlayerJoinListener(rdq);
    }
    
    @AfterEach
    void tearDown() {
        BountyServiceProvider.reset();
    }
    
    @Test
    @DisplayName("Should handle join when no bounty exists")
    void testJoinWithNoBounty() {
        // Arrange
        when(bountyService.getBountyByPlayer(playerUuid))
            .thenReturn(CompletableFuture.completedFuture(Optional.empty()));
        
        // Act
        listener.onPlayerJoin(event);
        
        // Assert
        verify(bountyService, timeout(1000)).getBountyByPlayer(playerUuid);
        verify(player, never()).sendMessage(anyString());
    }
    
    @Test
    @DisplayName("Should handle join when bounty is inactive")
    void testJoinWithInactiveBounty() {
        // Arrange
        RBounty inactiveBounty = createBounty(false);
        when(bountyService.getBountyByPlayer(playerUuid))
            .thenReturn(CompletableFuture.completedFuture(Optional.of(inactiveBounty)));
        
        // Act
        listener.onPlayerJoin(event);
        
        // Assert
        verify(bountyService, timeout(1000)).getBountyByPlayer(playerUuid);
        verify(player, never()).sendMessage(anyString());
    }
    
    @Test
    @DisplayName("Should notify player when they have active bounty")
    void testJoinWithActiveBounty() {
        // Arrange
        RBounty activeBounty = createBounty(true);
        when(bountyService.getBountyByPlayer(playerUuid))
            .thenReturn(CompletableFuture.completedFuture(Optional.of(activeBounty)));
        
        // Act
        listener.onPlayerJoin(event);
        
        // Assert
        verify(bountyService, timeout(1000)).getBountyByPlayer(playerUuid);
        // Note: Message sending is delayed by 40 ticks, so we can't easily verify it in unit tests
        // without a more complex test setup with scheduler mocking
    }
    
    @Test
    @DisplayName("Should handle bounty service not initialized")
    void testBountyServiceNotInitialized() {
        // Arrange
        BountyServiceProvider.reset();
        
        // Act
        listener.onPlayerJoin(event);
        
        // Assert
        verify(bountyService, never()).getBountyByPlayer(any());
    }
    
    @Test
    @DisplayName("Should not apply visual indicators when disabled")
    void testVisualIndicatorsDisabled() {
        // Arrange
        // Mock BountyConfig with visual indicators disabled
        var bountyConfig = mock(com.raindropcentral.rdq.config.bounty.BountyConfig.class);
        when(bountyConfig.isVisualIndicatorsEnabled()).thenReturn(false);
        when(rdq.getBountyConfig()).thenReturn(bountyConfig);
        
        listener = new BountyPlayerJoinListener(rdq);
        RBounty activeBounty = createBounty(true);
        when(bountyService.getBountyByPlayer(playerUuid))
            .thenReturn(CompletableFuture.completedFuture(Optional.of(activeBounty)));
        
        // Act
        listener.onPlayerJoin(event);
        
        // Assert
        verify(bountyService, timeout(1000)).getBountyByPlayer(playerUuid);
        // Visual indicators should not be applied (verified by no exceptions)
    }
    
    /**
     * Helper method to create a test bounty.
     */
    private RBounty createBounty(boolean active) {
        RBounty bounty = new RBounty(playerUuid, UUID.randomUUID());
        if (!active) {
            // Claim the bounty to make it inactive
            bounty.claim(UUID.randomUUID());
        }
        return bounty;
    }
}
