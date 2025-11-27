package com.raindropcentral.rdq.bounty.announcement;

import com.raindropcentral.rdq.bounty.dto.Bounty;
import com.raindropcentral.rdq.bounty.type.BountyStatus;
import com.raindropcentral.rdq.config.bounty.BountySection;
import org.bukkit.Location;
import org.bukkit.World;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

/**
 * Unit tests for BountyAnnouncementManager.
 * Tests message broadcasting with various configurations and JExTranslate integration.
 */
@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class BountyAnnouncementManagerTest {
    
    @Mock
    private BountySection config;
    
    @Mock
    private World world;
    
    @Mock
    private Location location;
    
    private Bounty testBounty;
    
    @BeforeEach
    void setUp() {
        // Set up test bounty
        testBounty = new Bounty(
            1L,
            UUID.randomUUID(),
            "TestTarget",
            UUID.randomUUID(),
            "TestCommissioner",
            Set.of(),
            Map.of(),
            1000.0,
            LocalDateTime.now(),
            LocalDateTime.now().plusDays(7),
            BountyStatus.ACTIVE,
            Optional.empty()
        );
        
        // Set up location mock with lenient stubbing
        lenient().when(location.getWorld()).thenReturn(world);
        lenient().when(world.getNearbyPlayers(any(Location.class), anyDouble()))
            .thenReturn(Collections.emptyList());
    }
    
    @Test
    void testCreationAnnouncementEnabled() {
        // Given: Config with creation announcements enabled
        when(config.isAnnounceCreation()).thenReturn(true);
        BountyAnnouncementManager manager = new BountyAnnouncementManager(config, -1);
        
        // Then: Creation announcements should be enabled
        assertTrue(manager.isCreationAnnouncementEnabled());
    }
    
    @Test
    void testCreationAnnouncementDisabled() {
        // Given: Config with creation announcements disabled
        when(config.isAnnounceCreation()).thenReturn(false);
        BountyAnnouncementManager manager = new BountyAnnouncementManager(config, -1);
        
        // Then: Creation announcements should be disabled
        assertFalse(manager.isCreationAnnouncementEnabled());
    }
    
    @Test
    void testClaimAnnouncementEnabled() {
        // Given: Config with claim announcements enabled
        when(config.isAnnounceClaim()).thenReturn(true);
        BountyAnnouncementManager manager = new BountyAnnouncementManager(config, -1);
        
        // Then: Claim announcements should be enabled
        assertTrue(manager.isClaimAnnouncementEnabled());
    }
    
    @Test
    void testClaimAnnouncementDisabled() {
        // Given: Config with claim announcements disabled
        when(config.isAnnounceClaim()).thenReturn(false);
        BountyAnnouncementManager manager = new BountyAnnouncementManager(config, -1);
        
        // Then: Claim announcements should be disabled
        assertFalse(manager.isClaimAnnouncementEnabled());
    }
    
    @Test
    void testGlobalBroadcastRadius() {
        // Given: Config with global broadcast (-1)
        when(config.isAnnounceCreation()).thenReturn(true);
        BountyAnnouncementManager manager = new BountyAnnouncementManager(config, -1);
        
        // Then: Broadcast radius should be -1
        assertEquals(-1, manager.getBroadcastRadius());
    }
    
    @Test
    void testRadiusBroadcast() {
        // Given: Config with radius broadcast (100)
        when(config.isAnnounceCreation()).thenReturn(true);
        BountyAnnouncementManager manager = new BountyAnnouncementManager(config, 100);
        
        // Then: Broadcast radius should be 100
        assertEquals(100, manager.getBroadcastRadius());
    }
    
    @Test
    void testAnnounceBountyCreationWhenDisabled() {
        // Given: Config with creation announcements disabled
        when(config.isAnnounceCreation()).thenReturn(false);
        BountyAnnouncementManager manager = new BountyAnnouncementManager(config, -1);
        
        // When/Then: Should not throw when announcing (no-op)
        assertDoesNotThrow(() -> 
            manager.announceBountyCreation(testBounty, location)
        );
    }
    
    @Test
    void testAnnounceBountyClaimWhenDisabled() {
        // Given: Config with claim announcements disabled
        when(config.isAnnounceClaim()).thenReturn(false);
        BountyAnnouncementManager manager = new BountyAnnouncementManager(config, -1);
        
        // When/Then: Should not throw when announcing (no-op)
        assertDoesNotThrow(() -> 
            manager.announceBountyClaim(testBounty, "TestHunter", location)
        );
    }
    
    @Test
    void testConstructorWithNullConfigThrows() {
        // When/Then: Constructor should throw when config is null
        assertThrows(NullPointerException.class, () -> 
            new BountyAnnouncementManager(null, -1)
        );
    }
    
    @Test
    void testAnnounceBountyCreationWithNullBountyThrows() {
        // Given: A valid manager
        when(config.isAnnounceCreation()).thenReturn(true);
        BountyAnnouncementManager manager = new BountyAnnouncementManager(config, -1);
        
        // When/Then: Should throw when bounty is null
        assertThrows(NullPointerException.class, () -> 
            manager.announceBountyCreation(null, location)
        );
    }
    
    @Test
    void testAnnounceBountyCreationWithNullLocationThrows() {
        // Given: A valid manager
        when(config.isAnnounceCreation()).thenReturn(true);
        BountyAnnouncementManager manager = new BountyAnnouncementManager(config, -1);
        
        // When/Then: Should throw when location is null
        assertThrows(NullPointerException.class, () -> 
            manager.announceBountyCreation(testBounty, null)
        );
    }
    
    @Test
    void testAnnounceBountyClaimWithNullBountyThrows() {
        // Given: A valid manager
        when(config.isAnnounceClaim()).thenReturn(true);
        BountyAnnouncementManager manager = new BountyAnnouncementManager(config, -1);
        
        // When/Then: Should throw when bounty is null
        assertThrows(NullPointerException.class, () -> 
            manager.announceBountyClaim(null, "TestHunter", location)
        );
    }
    
    @Test
    void testAnnounceBountyClaimWithNullHunterNameThrows() {
        // Given: A valid manager
        when(config.isAnnounceClaim()).thenReturn(true);
        BountyAnnouncementManager manager = new BountyAnnouncementManager(config, -1);
        
        // When/Then: Should throw when hunter name is null
        assertThrows(NullPointerException.class, () -> 
            manager.announceBountyClaim(testBounty, null, location)
        );
    }
    
    @Test
    void testAnnounceBountyClaimWithNullLocationThrows() {
        // Given: A valid manager
        when(config.isAnnounceClaim()).thenReturn(true);
        BountyAnnouncementManager manager = new BountyAnnouncementManager(config, -1);
        
        // When/Then: Should throw when location is null
        assertThrows(NullPointerException.class, () -> 
            manager.announceBountyClaim(testBounty, "TestHunter", null)
        );
    }
    
    @Test
    void testBountyDataForPlaceholders() {
        // Given: A bounty with specific values
        Bounty bounty = new Bounty(
            1L,
            UUID.randomUUID(),
            "TargetPlayer",
            UUID.randomUUID(),
            "CommissionerPlayer",
            Set.of(),
            Map.of(),
            5000.0,
            LocalDateTime.now(),
            LocalDateTime.now().plusDays(7),
            BountyStatus.ACTIVE,
            Optional.empty()
        );
        
        // Then: Bounty should have all required fields for placeholders
        assertNotNull(bounty.commissionerName());
        assertNotNull(bounty.targetName());
        assertEquals("CommissionerPlayer", bounty.commissionerName());
        assertEquals("TargetPlayer", bounty.targetName());
        assertEquals(5000.0, bounty.totalEstimatedValue());
    }
}
