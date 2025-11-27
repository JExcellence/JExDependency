package com.raindropcentral.rdq.bounty;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class HunterStatsTest {

    @Test
    void createHunterStats() {
        var playerId = UUID.randomUUID();
        var stats = new HunterStats(
            playerId,
            "TestPlayer",
            5,
            10,
            3,
            BigDecimal.valueOf(5000),
            BigDecimal.valueOf(2000)
        );

        assertEquals(playerId, stats.playerId());
        assertEquals("TestPlayer", stats.playerName());
        assertEquals(5, stats.bountiesPlaced());
        assertEquals(10, stats.bountiesClaimed());
        assertEquals(3, stats.deaths());
        assertEquals(BigDecimal.valueOf(5000), stats.totalEarned());
        assertEquals(BigDecimal.valueOf(2000), stats.totalSpent());
    }

    @Test
    void calculateKDRatioWithDeaths() {
        var stats = new HunterStats(
            UUID.randomUUID(),
            "Hunter",
            0,
            10,
            5,
            BigDecimal.ZERO,
            BigDecimal.ZERO
        );

        assertEquals(2.0, stats.getKDRatio(), 0.001);
    }

    @Test
    void calculateKDRatioWithNoDeaths() {
        var stats = new HunterStats(
            UUID.randomUUID(),
            "Hunter",
            0,
            10,
            0,
            BigDecimal.ZERO,
            BigDecimal.ZERO
        );

        assertEquals(10.0, stats.getKDRatio(), 0.001);
    }

    @Test
    void calculateNetProfit() {
        var stats = new HunterStats(
            UUID.randomUUID(),
            "Hunter",
            5,
            10,
            0,
            BigDecimal.valueOf(5000),
            BigDecimal.valueOf(2000)
        );

        assertEquals(BigDecimal.valueOf(3000), stats.getNetProfit());
    }

    @Test
    void calculateNegativeNetProfit() {
        var stats = new HunterStats(
            UUID.randomUUID(),
            "Hunter",
            10,
            2,
            0,
            BigDecimal.valueOf(1000),
            BigDecimal.valueOf(5000)
        );

        assertEquals(BigDecimal.valueOf(-4000), stats.getNetProfit());
    }

    @Test
    void createEmptyStats() {
        var playerId = UUID.randomUUID();
        var stats = HunterStats.empty(playerId, "NewPlayer");

        assertEquals(playerId, stats.playerId());
        assertEquals("NewPlayer", stats.playerName());
        assertEquals(0, stats.bountiesPlaced());
        assertEquals(0, stats.bountiesClaimed());
        assertEquals(0, stats.deaths());
        assertEquals(BigDecimal.ZERO, stats.totalEarned());
        assertEquals(BigDecimal.ZERO, stats.totalSpent());
    }
}
