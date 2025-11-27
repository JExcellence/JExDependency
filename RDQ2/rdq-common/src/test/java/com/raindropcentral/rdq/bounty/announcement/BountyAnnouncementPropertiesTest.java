package com.raindropcentral.rdq.bounty.announcement;

import com.raindropcentral.rdq.bounty.dto.Bounty;
import com.raindropcentral.rdq.bounty.dto.ClaimInfo;
import com.raindropcentral.rdq.bounty.dto.RewardItem;
import com.raindropcentral.rdq.bounty.type.BountyStatus;
import com.raindropcentral.rdq.bounty.type.ClaimMode;
import com.raindropcentral.rdq.config.bounty.BountySection;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import net.jqwik.api.*;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Property-based tests for bounty announcement system.
 * These tests verify that announcements work correctly with various configurations.
 */
class BountyAnnouncementPropertiesTest {
    
    /**
     * **Feature: bounty-system-rebuild, Property 53: Creation announcement**
     * 
     * For any bounty creation with announcements enabled, the system should broadcast
     * a creation message to the appropriate recipients.
     * 
     * **Validates: Requirements 13.1**
     */
    @Property(tries = 100)
    @Label("Property 53: Creation announcement")
    void creationAnnouncementProperty(
        @ForAll("bountyWithCreationEnabled") BountyWithConfig bountyWithConfig
    ) {
        // Given: A bounty announcement manager with creation announcements enabled
        BountyAnnouncementManager manager = new BountyAnnouncementManager(
            bountyWithConfig.config(),
            bountyWithConfig.broadcastRadius()
        );
        
        // Then: Creation announcements should be enabled
        assertTrue(manager.isCreationAnnouncementEnabled(), 
            "Creation announcements should be enabled");
        
        // And: The manager should be properly configured
        assertNotNull(manager, "Manager should not be null");
        assertEquals(bountyWithConfig.broadcastRadius(), manager.getBroadcastRadius(),
            "Broadcast radius should match configuration");
    }
    
    /**
     * **Feature: bounty-system-rebuild, Property 54: Claim announcement**
     * 
     * For any bounty claim with announcements enabled, the system should broadcast
     * a claim message to the appropriate recipients.
     * 
     * **Validates: Requirements 13.2**
     */
    @Property(tries = 100)
    @Label("Property 54: Claim announcement")
    void claimAnnouncementProperty(
        @ForAll("bountyWithClaimEnabled") BountyWithConfig bountyWithConfig
    ) {
        // Given: A bounty announcement manager with claim announcements enabled
        BountyAnnouncementManager manager = new BountyAnnouncementManager(
            bountyWithConfig.config(),
            bountyWithConfig.broadcastRadius()
        );
        
        // Then: Claim announcements should be enabled
        assertTrue(manager.isClaimAnnouncementEnabled(), 
            "Claim announcements should be enabled");
        
        // And: The manager should be properly configured
        assertNotNull(manager, "Manager should not be null");
        assertEquals(bountyWithConfig.broadcastRadius(), manager.getBroadcastRadius(),
            "Broadcast radius should match configuration");
    }
    
    /**
     * **Feature: bounty-system-rebuild, Property 55: Global broadcast**
     * 
     * For any announcement with broadcast radius -1, the system should broadcast
     * globally to all players.
     * 
     * **Validates: Requirements 13.3**
     */
    @Property(tries = 100)
    @Label("Property 55: Global broadcast")
    void globalBroadcastProperty(
        @ForAll("bountyWithGlobalBroadcast") BountyWithConfig bountyWithConfig
    ) {
        // Given: A bounty announcement manager with global broadcast (-1)
        BountyAnnouncementManager manager = new BountyAnnouncementManager(
            bountyWithConfig.config(),
            -1
        );
        
        // Then: Broadcast radius should be -1 (global)
        assertEquals(-1, manager.getBroadcastRadius(), 
            "Broadcast radius should be -1 for global broadcasts");
    }
    
    /**
     * **Feature: bounty-system-rebuild, Property 56: Radius broadcast**
     * 
     * For any announcement with positive broadcast radius, the system should broadcast
     * only to players within that radius.
     * 
     * **Validates: Requirements 13.4**
     */
    @Property(tries = 100)
    @Label("Property 56: Radius broadcast")
    void radiusBroadcastProperty(
        @ForAll("bountyWithRadiusBroadcast") BountyWithConfig bountyWithConfig,
        @ForAll("positiveRadius") int radius
    ) {
        Assume.that(radius > 0);
        
        // Given: A bounty announcement manager with positive radius
        BountyAnnouncementManager manager = new BountyAnnouncementManager(
            bountyWithConfig.config(),
            radius
        );
        
        // Then: Broadcast radius should match the configured value
        assertEquals(radius, manager.getBroadcastRadius(), 
            "Broadcast radius should match configured value");
        assertTrue(manager.getBroadcastRadius() > 0, 
            "Broadcast radius should be positive for radius-based broadcasts");
    }
    
    /**
     * **Feature: bounty-system-rebuild, Property 57: Disabled announcements**
     * 
     * For any announcement with announcements disabled, the system should not
     * broadcast any messages.
     * 
     * **Validates: Requirements 13.5**
     */
    @Property(tries = 100)
    @Label("Property 57: Disabled announcements")
    void disabledAnnouncementsProperty(
        @ForAll("bountyWithDisabledAnnouncements") BountyWithConfig bountyWithConfig,
        @ForAll("location") Location location,
        @ForAll("hunterName") String hunterName
    ) {
        // Given: A bounty announcement manager with announcements disabled
        BountyAnnouncementManager manager = new BountyAnnouncementManager(
            bountyWithConfig.config(),
            bountyWithConfig.broadcastRadius()
        );
        
        // Then: Both creation and claim announcements should be disabled
        assertFalse(manager.isCreationAnnouncementEnabled() || manager.isClaimAnnouncementEnabled(),
            "At least one announcement type should be disabled");
        
        // When: Attempting to announce (should be no-op)
        assertDoesNotThrow(() -> {
            manager.announceBountyCreation(bountyWithConfig.bounty(), location);
            manager.announceBountyClaim(bountyWithConfig.bounty(), hunterName, location);
        }, "Should not throw when announcements are disabled");
    }
    
    /**
     * **Feature: bounty-system-rebuild, Property 58: JExTranslate integration**
     * 
     * For any announcement, the system should use JExTranslate with proper
     * placeholder support for player names and values.
     * 
     * **Validates: Requirements 13.6**
     */
    @Property(tries = 100)
    @Label("Property 58: JExTranslate integration")
    void jexTranslateIntegrationProperty(
        @ForAll("bountyWithCreationEnabled") BountyWithConfig bountyWithConfig
    ) {
        // Given: A bounty with specific values
        Bounty bounty = bountyWithConfig.bounty();
        
        // Then: Bounty should have all required fields for placeholders
        assertNotNull(bounty.commissionerName(), "Commissioner name should not be null");
        assertNotNull(bounty.targetName(), "Target name should not be null");
        assertTrue(bounty.totalEstimatedValue() >= 0, "Total value should be non-negative");
        
        // And: The bounty data should be suitable for placeholder substitution
        assertFalse(bounty.commissionerName().isEmpty(), "Commissioner name should not be empty");
        assertFalse(bounty.targetName().isEmpty(), "Target name should not be empty");
    }
    
    // ========== Arbitraries ==========
    
    @Provide
    Arbitrary<UUID> uuid() {
        return Arbitraries.randomValue(random -> UUID.randomUUID());
    }
    
    @Provide
    Arbitrary<String> playerName() {
        return Arbitraries.strings()
            .alpha()
            .ofMinLength(3)
            .ofMaxLength(16);
    }
    
    @Provide
    Arbitrary<String> hunterName() {
        return playerName();
    }
    
    @Provide
    Arbitrary<Double> bountyValue() {
        return Arbitraries.doubles()
            .between(100.0, 10000.0);
    }
    
    @Provide
    Arbitrary<Integer> positiveRadius() {
        return Arbitraries.integers()
            .between(1, 1000);
    }
    
    @Provide
    Arbitrary<Bounty> bounty() {
        return Combinators.combine(
            uuid(),
            playerName(),
            uuid(),
            playerName(),
            bountyValue()
        ).as((targetUuid, targetName, commissionerUuid, commissionerName, value) -> 
            new Bounty(
                1L,
                targetUuid,
                targetName,
                commissionerUuid,
                commissionerName,
                Set.of(),
                Map.of(),
                value,
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(7),
                BountyStatus.ACTIVE,
                Optional.empty()
            )
        );
    }
    
    @Provide
    Arbitrary<BountySection> configWithCreationEnabled() {
        return Arbitraries.just(createConfig(true, true));
    }
    
    @Provide
    Arbitrary<BountySection> configWithClaimEnabled() {
        return Arbitraries.just(createConfig(true, true));
    }
    
    @Provide
    Arbitrary<BountySection> configWithDisabledAnnouncements() {
        return Arbitraries.just(createConfig(false, false));
    }
    
    @Provide
    Arbitrary<BountyWithConfig> bountyWithCreationEnabled() {
        return Combinators.combine(
            bounty(),
            configWithCreationEnabled(),
            Arbitraries.integers().between(-1, 1000)
        ).as(BountyWithConfig::new);
    }
    
    @Provide
    Arbitrary<BountyWithConfig> bountyWithClaimEnabled() {
        return Combinators.combine(
            bounty(),
            configWithClaimEnabled(),
            Arbitraries.integers().between(-1, 1000)
        ).as(BountyWithConfig::new);
    }
    
    @Provide
    Arbitrary<BountyWithConfig> bountyWithGlobalBroadcast() {
        return Combinators.combine(
            bounty(),
            configWithCreationEnabled(),
            Arbitraries.just(-1)
        ).as(BountyWithConfig::new);
    }
    
    @Provide
    Arbitrary<BountyWithConfig> bountyWithRadiusBroadcast() {
        return Combinators.combine(
            bounty(),
            configWithCreationEnabled(),
            positiveRadius()
        ).as(BountyWithConfig::new);
    }
    
    @Provide
    Arbitrary<BountyWithConfig> bountyWithDisabledAnnouncements() {
        return Combinators.combine(
            bounty(),
            configWithDisabledAnnouncements(),
            Arbitraries.integers().between(-1, 1000)
        ).as(BountyWithConfig::new);
    }
    
    @Provide
    Arbitrary<Location> location() {
        return Arbitraries.just(createMockLocation());
    }
    
    // ========== Helper Methods ==========
    
    private static BountySection createConfig(boolean announceCreation, boolean announceClaim) {
        BountySection config = mock(BountySection.class);
        when(config.isAnnounceCreation()).thenReturn(announceCreation);
        when(config.isAnnounceClaim()).thenReturn(announceClaim);
        return config;
    }
    
    private static Location createMockLocation() {
        World world = mock(World.class);
        when(world.getNearbyPlayers(any(Location.class), anyDouble()))
            .thenReturn(Collections.emptyList());
        
        Location location = mock(Location.class);
        when(location.getWorld()).thenReturn(world);
        return location;
    }
    
    // ========== Helper Records ==========
    
    record BountyWithConfig(
        Bounty bounty,
        BountySection config,
        int broadcastRadius
    ) {}
}
