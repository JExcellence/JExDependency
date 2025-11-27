package com.raindropcentral.rdq.listener;

import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.database.entity.bounty.RBounty;
import com.raindropcentral.rdq.service.bounty.BountyService;
import com.raindropcentral.rdq.bounty.tracking.DamageTracker;
import com.raindropcentral.rdq.bounty.type.ClaimMode;
import com.raindropcentral.rdq.service.bounty.BountyServiceProvider;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for BountyPlayerDeathListener.
 * Tests death event handling with various scenarios.
 * 
 * Requirements: 21.2
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BountyPlayerDeathListener Tests")
class BountyPlayerDeathListenerTest {
    
    @Mock
    private RDQ rdq;
    
    @Mock
    private JavaPlugin plugin;
    
    @Mock
    private DamageTracker damageTracker;
    
    @Mock
    private BountyService bountyService;
    
    @Mock
    private Player victim;
    
    @Mock
    private Player killer;
    
    @Mock
    private PlayerDeathEvent event;
    
    @Mock
    private Location location;
    
    private BountyPlayerDeathListener listener;
    private UUID victimUuid;
    private UUID killerUuid;
    
    @BeforeEach
    void setUp() {
        victimUuid = UUID.randomUUID();
        killerUuid = UUID.randomUUID();
        
        when(rdq.getPlugin()).thenReturn(plugin);
        when(victim.getUniqueId()).thenReturn(victimUuid);
        when(victim.getName()).thenReturn("VictimPlayer");
        when(victim.getKiller()).thenReturn(killer);
        when(victim.getLocation()).thenReturn(location);
        when(killer.getUniqueId()).thenReturn(killerUuid);
        when(killer.getName()).thenReturn("KillerPlayer");
        when(killer.isOnline()).thenReturn(true);
        when(event.getEntity()).thenReturn(victim);
        
        // Set up bounty service provider
        BountyServiceProvider.setInstance(bountyService);
        
        // Mock BountyConfig
        var bountyConfig = mock(com.raindropcentral.rdq.config.bounty.BountyConfig.class);
        when(bountyConfig.getDamageTrackingWindow()).thenReturn(30);
        when(bountyConfig.getClaimMode()).thenReturn(ClaimMode.LAST_HIT);
        when(rdq.getBountyConfig()).thenReturn(bountyConfig);
        
        listener = new BountyPlayerDeathListener(rdq);
    }
    
    @AfterEach
    void tearDown() {
        BountyServiceProvider.reset();
    }
    
    @Test
    @DisplayName("Should handle death when no bounty exists")
    void testDeathWithNoBounty() {
        // Arrange
        when(bountyService.getBountyByPlayer(victimUuid))
            .thenReturn(CompletableFuture.completedFuture(Optional.empty()));
        
        // Act
        listener.onPlayerDeath(event);
        
        // Assert
        verify(bountyService).getBountyByPlayer(victimUuid);
        verify(damageTracker, timeout(1000)).clearDamage(victimUuid);
        verify(bountyService, never()).updateBounty(any());
    }
    
    @Test
    @DisplayName("Should handle death when bounty is inactive")
    void testDeathWithInactiveBounty() {
        // Arrange
        RBounty inactiveBounty = createBounty(false);
        when(bountyService.getBountyByPlayer(victimUuid))
            .thenReturn(CompletableFuture.completedFuture(Optional.of(inactiveBounty)));
        
        // Act
        listener.onPlayerDeath(event);
        
        // Assert
        verify(bountyService).getBountyByPlayer(victimUuid);
        verify(damageTracker, timeout(1000)).clearDamage(victimUuid);
        verify(bountyService, never()).updateBounty(any());
    }
    
    @Test
    @DisplayName("Should claim bounty when victim has active bounty and is killed")
    void testDeathWithActiveBounty() {
        // Arrange
        RBounty activeBounty = createBounty(true);
        
        when(bountyService.getBountyByPlayer(victimUuid))
            .thenReturn(CompletableFuture.completedFuture(Optional.of(activeBounty)));
        when(bountyService.updateBounty(any()))
            .thenReturn(CompletableFuture.completedFuture(activeBounty));
        when(damageTracker.getDamageMap(victimUuid))
            .thenReturn(Map.of(killerUuid, 20.0));
        
        // Act
        listener.onPlayerDeath(event);
        
        // Assert
        verify(bountyService, timeout(1000)).getBountyByPlayer(victimUuid);
        verify(bountyService, timeout(1000)).updateBounty(any());
        verify(damageTracker, timeout(1000)).clearDamage(victimUuid);
    }
    
    @Test
    @DisplayName("Should handle death with no killer (environmental death)")
    void testDeathWithNoKiller() {
        // Arrange
        RBounty activeBounty = createBounty(true);
        when(victim.getKiller()).thenReturn(null);
        when(bountyService.getBountyByPlayer(victimUuid))
            .thenReturn(CompletableFuture.completedFuture(Optional.of(activeBounty)));
        when(damageTracker.getDamageMap(victimUuid))
            .thenReturn(Collections.emptyMap());
        
        // Act
        listener.onPlayerDeath(event);
        
        // Assert
        verify(bountyService, timeout(1000)).getBountyByPlayer(victimUuid);
        verify(bountyService, never()).updateBounty(any());
        verify(damageTracker, timeout(1000)).clearDamage(victimUuid);
    }
    
    @Test
    @DisplayName("Should use MOST_DAMAGE claim mode correctly")
    void testMostDamageClaimMode() {
        // Arrange
        UUID otherPlayerUuid = UUID.randomUUID();
        RBounty activeBounty = createBounty(true);
        
        // Mock BountyConfig with MOST_DAMAGE mode
        var bountyConfig = mock(com.raindropcentral.rdq.config.bounty.BountyConfig.class);
        when(bountyConfig.getDamageTrackingWindow()).thenReturn(30);
        when(bountyConfig.getClaimMode()).thenReturn(ClaimMode.MOST_DAMAGE);
        when(rdq.getBountyConfig()).thenReturn(bountyConfig);
        
        listener = new BountyPlayerDeathListener(rdq);
        
        when(bountyService.getBountyByPlayer(victimUuid))
            .thenReturn(CompletableFuture.completedFuture(Optional.of(activeBounty)));
        when(bountyService.updateBounty(any()))
            .thenReturn(CompletableFuture.completedFuture(activeBounty));
        when(damageTracker.getDamageMap(victimUuid))
            .thenReturn(Map.of(
                killerUuid, 5.0,
                otherPlayerUuid, 15.0  // Most damage
            ));
        
        // Act
        listener.onPlayerDeath(event);
        
        // Assert
        verify(bountyService, timeout(1000)).getBountyByPlayer(victimUuid);
        verify(bountyService, timeout(1000)).updateBounty(any());
        verify(damageTracker, timeout(1000)).clearDamage(victimUuid);
    }
    
    @Test
    @DisplayName("Should handle bounty service not initialized")
    void testBountyServiceNotInitialized() {
        // Arrange
        BountyServiceProvider.reset();
        
        // Act
        listener.onPlayerDeath(event);
        
        // Assert
        verify(bountyService, never()).getBountyByPlayer(any());
        verify(damageTracker, never()).clearDamage(any());
    }
    
    /**
     * Helper method to create a test bounty.
     */
    private RBounty createBounty(boolean active) {
        RBounty bounty = new RBounty(victimUuid, UUID.randomUUID());
        if (!active) {
            // Claim the bounty to make it inactive
            bounty.claim(UUID.randomUUID());
        }
        return bounty;
    }
}
