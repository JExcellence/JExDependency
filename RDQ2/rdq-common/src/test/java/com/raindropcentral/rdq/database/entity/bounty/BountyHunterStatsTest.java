package com.raindropcentral.rdq.database.entity.bounty;

import com.raindropcentral.rdq.database.entity.player.RDQPlayer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for BountyHunterStats entity.
 * Tests statistics calculations and atomic updates.
 */
@DisplayName("BountyHunterStats Entity Tests")
class BountyHunterStatsTest {

    private RDQPlayer player;
    private BountyHunterStats stats;

    @BeforeEach
    void setUp() {
        player = new RDQPlayer(UUID.randomUUID(), "TestPlayer");
        stats = new BountyHunterStats(player);
    }

    @Test
    @DisplayName("Should create stats with initial values")
    void testStatsCreation() {
        assertNotNull(stats);
        assertEquals(player, stats.getPlayer());
        assertEquals(0, stats.getBountiesClaimed());
        assertEquals(0.0, stats.getTotalRewardValue());
        assertEquals(0.0, stats.getHighestBountyValue());
        assertTrue(stats.getLastClaimTimestamp().isEmpty());
    }

    @Test
    @DisplayName("Should throw exception when creating with null player")
    void testCreateWithNullPlayer() {
        assertThrows(NullPointerException.class, () -> new BountyHunterStats(null));
    }

    @Test
    @DisplayName("Should increment bounties claimed")
    void testIncrementBountiesClaimed() {
        assertEquals(0, stats.getBountiesClaimed());

        stats.incrementBountiesClaimed();
        assertEquals(1, stats.getBountiesClaimed());

        stats.incrementBountiesClaimed();
        assertEquals(2, stats.getBountiesClaimed());
    }

    @Test
    @DisplayName("Should add reward value and update highest")
    void testAddRewardValue() {
        stats.addRewardValue(100.0);
        assertEquals(100.0, stats.getTotalRewardValue());
        assertEquals(100.0, stats.getHighestBountyValue());

        stats.addRewardValue(50.0);
        assertEquals(150.0, stats.getTotalRewardValue());
        assertEquals(100.0, stats.getHighestBountyValue()); // Highest should remain 100

        stats.addRewardValue(200.0);
        assertEquals(350.0, stats.getTotalRewardValue());
        assertEquals(200.0, stats.getHighestBountyValue()); // Highest should update to 200
    }

    @Test
    @DisplayName("Should ignore negative reward values")
    void testAddNegativeRewardValue() {
        stats.addRewardValue(100.0);
        assertEquals(100.0, stats.getTotalRewardValue());

        stats.addRewardValue(-50.0);
        assertEquals(100.0, stats.getTotalRewardValue()); // Should remain unchanged
    }

    @Test
    @DisplayName("Should ignore zero reward values")
    void testAddZeroRewardValue() {
        stats.addRewardValue(100.0);
        assertEquals(100.0, stats.getTotalRewardValue());

        stats.addRewardValue(0.0);
        assertEquals(100.0, stats.getTotalRewardValue()); // Should remain unchanged
    }

    @Test
    @DisplayName("Should update last claim timestamp")
    void testUpdateLastClaimTimestamp() {
        assertTrue(stats.getLastClaimTimestamp().isEmpty());

        long beforeUpdate = System.currentTimeMillis();
        stats.updateLastClaimTimestamp();
        long afterUpdate = System.currentTimeMillis();

        assertTrue(stats.getLastClaimTimestamp().isPresent());
        long timestamp = stats.getLastClaimTimestamp().get();
        assertTrue(timestamp >= beforeUpdate && timestamp <= afterUpdate);
    }

    @Test
    @DisplayName("Should record claim atomically")
    void testRecordClaim() {
        assertEquals(0, stats.getBountiesClaimed());
        assertEquals(0.0, stats.getTotalRewardValue());
        assertTrue(stats.getLastClaimTimestamp().isEmpty());

        long beforeClaim = System.currentTimeMillis();
        stats.recordClaim(150.0);
        long afterClaim = System.currentTimeMillis();

        assertEquals(1, stats.getBountiesClaimed());
        assertEquals(150.0, stats.getTotalRewardValue());
        assertEquals(150.0, stats.getHighestBountyValue());
        assertTrue(stats.getLastClaimTimestamp().isPresent());
        
        long timestamp = stats.getLastClaimTimestamp().get();
        assertTrue(timestamp >= beforeClaim && timestamp <= afterClaim);
    }

    @Test
    @DisplayName("Should record multiple claims correctly")
    void testRecordMultipleClaims() {
        stats.recordClaim(100.0);
        assertEquals(1, stats.getBountiesClaimed());
        assertEquals(100.0, stats.getTotalRewardValue());
        assertEquals(100.0, stats.getHighestBountyValue());

        stats.recordClaim(75.0);
        assertEquals(2, stats.getBountiesClaimed());
        assertEquals(175.0, stats.getTotalRewardValue());
        assertEquals(100.0, stats.getHighestBountyValue());

        stats.recordClaim(200.0);
        assertEquals(3, stats.getBountiesClaimed());
        assertEquals(375.0, stats.getTotalRewardValue());
        assertEquals(200.0, stats.getHighestBountyValue());
    }

    @Test
    @DisplayName("Should validate bounties claimed is non-negative")
    void testSetBountiesClaimedValidation() {
        stats.setBountiesClaimed(-5);
        assertEquals(0, stats.getBountiesClaimed());

        stats.setBountiesClaimed(10);
        assertEquals(10, stats.getBountiesClaimed());
    }

    @Test
    @DisplayName("Should validate total reward value is non-negative")
    void testSetTotalRewardValueValidation() {
        stats.setTotalRewardValue(-100.0);
        assertEquals(0.0, stats.getTotalRewardValue());

        stats.setTotalRewardValue(500.0);
        assertEquals(500.0, stats.getTotalRewardValue());
    }

    @Test
    @DisplayName("Should validate highest bounty value is non-negative")
    void testSetHighestBountyValueValidation() {
        stats.setHighestBountyValue(-50.0);
        assertEquals(0.0, stats.getHighestBountyValue());

        stats.setHighestBountyValue(250.0);
        assertEquals(250.0, stats.getHighestBountyValue());
    }

    @Test
    @DisplayName("Should provide player UUID convenience method")
    void testGetPlayerUniqueId() {
        assertEquals(player.getUniqueId(), stats.getPlayerUniqueId());
    }

    @Test
    @DisplayName("Should provide player name convenience method")
    void testGetPlayerName() {
        assertEquals(player.getPlayerName(), stats.getPlayerName());
    }

    @Test
    @DisplayName("Should use Optional for nullable timestamp")
    void testOptionalTimestamp() {
        assertTrue(stats.getLastClaimTimestamp().isEmpty());

        stats.setLastClaimTimestamp(123456789L);
        assertTrue(stats.getLastClaimTimestamp().isPresent());
        assertEquals(123456789L, stats.getLastClaimTimestamp().get());

        stats.setLastClaimTimestamp(null);
        assertTrue(stats.getLastClaimTimestamp().isEmpty());
    }

    @Test
    @DisplayName("Should implement equals based on player")
    void testEquals() {
        BountyHunterStats stats1 = new BountyHunterStats(player);
        BountyHunterStats stats2 = new BountyHunterStats(player);
        
        assertEquals(stats1, stats2);
        
        RDQPlayer otherPlayer = new RDQPlayer(UUID.randomUUID(), "OtherPlayer");
        BountyHunterStats stats3 = new BountyHunterStats(otherPlayer);
        
        assertNotEquals(stats1, stats3);
    }

    @Test
    @DisplayName("Should implement hashCode based on player")
    void testHashCode() {
        BountyHunterStats stats1 = new BountyHunterStats(player);
        BountyHunterStats stats2 = new BountyHunterStats(player);
        
        assertEquals(stats1.hashCode(), stats2.hashCode());
    }
}
