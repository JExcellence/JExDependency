package com.raindropcentral.rdq.integration;

import com.raindropcentral.rdq.bounty.Bounty;
import com.raindropcentral.rdq.bounty.BountyRequest;
import com.raindropcentral.rdq.bounty.BountyStatus;
import com.raindropcentral.rdq.bounty.HunterStats;
import com.raindropcentral.rdq.fixtures.TestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Bounty Flow Integration Tests")
class BountyFlowIntegrationTest {

    @Test
    @DisplayName("Create bounty request with valid data")
    void createBountyRequestWithValidData() {
        var placerId = UUID.randomUUID();
        var targetId = UUID.randomUUID();
        var amount = BigDecimal.valueOf(1000);

        var request = new BountyRequest(placerId, targetId, amount, "coins");

        assertEquals(placerId, request.placerId());
        assertEquals(targetId, request.targetId());
        assertEquals(amount, request.amount());
        assertEquals("coins", request.currency());
    }

    @Test
    @DisplayName("Create bounty request fails for self-targeting")
    void createBountyRequestFailsForSelfTargeting() {
        var playerId = UUID.randomUUID();
        var amount = BigDecimal.valueOf(1000);

        assertThrows(IllegalArgumentException.class, () ->
            new BountyRequest(playerId, playerId, amount, "coins")
        );
    }

    @Test
    @DisplayName("Create bounty request fails for zero amount")
    void createBountyRequestFailsForZeroAmount() {
        var placerId = UUID.randomUUID();
        var targetId = UUID.randomUUID();

        assertThrows(IllegalArgumentException.class, () ->
            new BountyRequest(placerId, targetId, BigDecimal.ZERO, "coins")
        );
    }

    @Test
    @DisplayName("Create bounty request fails for negative amount")
    void createBountyRequestFailsForNegativeAmount() {
        var placerId = UUID.randomUUID();
        var targetId = UUID.randomUUID();

        assertThrows(IllegalArgumentException.class, () ->
            new BountyRequest(placerId, targetId, BigDecimal.valueOf(-100), "coins")
        );
    }

    @Test
    @DisplayName("Bounty creation and status tracking")
    void bountyCreationAndStatusTracking() {
        var placerId = UUID.randomUUID();
        var targetId = UUID.randomUUID();

        var bounty = TestData.activeBounty(placerId, targetId, 1000);

        assertEquals(BountyStatus.ACTIVE, bounty.status());
        assertTrue(bounty.isActive());
        assertFalse(bounty.isExpired());
        assertEquals(placerId, bounty.placerId());
        assertEquals(targetId, bounty.targetId());
    }

    @Test
    @DisplayName("Bounty claim updates status correctly")
    void bountyClaimUpdatesStatusCorrectly() {
        var placerId = UUID.randomUUID();
        var targetId = UUID.randomUUID();
        var hunterId = UUID.randomUUID();

        var bounty = TestData.claimedBounty(placerId, targetId, hunterId, 1000);

        assertEquals(BountyStatus.CLAIMED, bounty.status());
        assertFalse(bounty.isActive());
        assertEquals(hunterId, bounty.claimedBy());
        assertNotNull(bounty.claimedAt());
    }

    @Test
    @DisplayName("Hunter stats calculation")
    void hunterStatsCalculation() {
        var playerId = UUID.randomUUID();
        var stats = TestData.hunterStats(playerId, "TestHunter", 10, 5);

        assertEquals(10, stats.bountiesClaimed());
        assertEquals(5, stats.bountiesPlaced());
        assertEquals(BigDecimal.valueOf(1000), stats.totalEarned());
        assertEquals(BigDecimal.valueOf(500), stats.totalSpent());
    }

    @Test
    @DisplayName("Hunter stats KD ratio calculation")
    void hunterStatsKdRatioCalculation() {
        var playerId = UUID.randomUUID();
        var stats = new HunterStats(
            playerId,
            "TestHunter",
            5,
            10,
            2,
            BigDecimal.valueOf(1000),
            BigDecimal.valueOf(500)
        );

        assertEquals(5.0, stats.getKDRatio());
    }

    @Test
    @DisplayName("Hunter stats KD ratio with zero deaths")
    void hunterStatsKdRatioWithZeroDeaths() {
        var playerId = UUID.randomUUID();
        var stats = new HunterStats(
            playerId,
            "TestHunter",
            5,
            10,
            0,
            BigDecimal.valueOf(1000),
            BigDecimal.valueOf(500)
        );

        assertEquals(10.0, stats.getKDRatio());
    }

    @Test
    @DisplayName("Bounty expiration detection")
    void bountyExpirationDetection() {
        var placerId = UUID.randomUUID();
        var targetId = UUID.randomUUID();

        var expiredBounty = Bounty.create(
            placerId,
            targetId,
            BigDecimal.valueOf(1000),
            "coins",
            Instant.now().minus(Duration.ofHours(1))
        );

        assertTrue(expiredBounty.isExpired());
        assertFalse(expiredBounty.isActive());
    }

    @Test
    @DisplayName("Active bounty is not expired")
    void activeBountyIsNotExpired() {
        var placerId = UUID.randomUUID();
        var targetId = UUID.randomUUID();

        var activeBounty = Bounty.create(
            placerId,
            targetId,
            BigDecimal.valueOf(1000),
            "coins",
            Instant.now().plus(Duration.ofDays(7))
        );

        assertFalse(activeBounty.isExpired());
        assertTrue(activeBounty.isActive());
    }
}
