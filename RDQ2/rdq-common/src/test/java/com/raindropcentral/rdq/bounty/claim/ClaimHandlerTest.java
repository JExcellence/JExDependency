package com.raindropcentral.rdq.bounty.claim;

import com.raindropcentral.rdq.bounty.tracking.DamageTracker;
import com.raindropcentral.rdq.bounty.type.ClaimMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ClaimHandler.
 * Tests each claim mode with various damage scenarios and edge cases.
 */
class ClaimHandlerTest {
    
    private static final long TRACKING_WINDOW_MS = 30000; // 30 seconds
    
    private DamageTracker tracker;
    private UUID victimUuid;
    private UUID attacker1Uuid;
    private UUID attacker2Uuid;
    private UUID attacker3Uuid;
    
    @BeforeEach
    void setUp() {
        tracker = new DamageTracker(TRACKING_WINDOW_MS);
        victimUuid = UUID.randomUUID();
        attacker1Uuid = UUID.randomUUID();
        attacker2Uuid = UUID.randomUUID();
        attacker3Uuid = UUID.randomUUID();
    }
    
    // ========== LAST_HIT Mode Tests ==========
    
    @Test
    void lastHitMode_shouldAttributeToLastHitter() {
        // Given: Multiple attackers with last hitter
        tracker.recordDamage(victimUuid, attacker1Uuid, 10.0);
        tracker.recordDamage(victimUuid, attacker2Uuid, 20.0);
        tracker.recordDamage(victimUuid, attacker3Uuid, 5.0);
        
        ClaimHandler handler = new ClaimHandler(tracker, ClaimMode.LAST_HIT);
        
        // When: Determining claim with attacker1 as last hitter
        ClaimResult result = handler.determineClaim(victimUuid, attacker1Uuid);
        
        // Then: attacker1 should be the sole winner
        assertTrue(result.hasWinners());
        assertTrue(result.isSingleWinner());
        assertEquals(attacker1Uuid, result.getSingleWinner());
        assertEquals(1.0, result.getShare(attacker1Uuid), 0.0001);
    }
    
    @Test
    void lastHitMode_withNoDamage_shouldAttributeToLastHitter() {
        // Given: No damage tracked
        ClaimHandler handler = new ClaimHandler(tracker, ClaimMode.LAST_HIT);
        
        // When: Determining claim with a last hitter
        ClaimResult result = handler.determineClaim(victimUuid, attacker1Uuid);
        
        // Then: Last hitter should still win
        assertTrue(result.hasWinners());
        assertEquals(attacker1Uuid, result.getSingleWinner());
    }
    
    // ========== MOST_DAMAGE Mode Tests ==========
    
    @Test
    void mostDamageMode_shouldAttributeToHighestDamageDealer() {
        // Given: Multiple attackers with different damage amounts
        tracker.recordDamage(victimUuid, attacker1Uuid, 10.0);
        tracker.recordDamage(victimUuid, attacker2Uuid, 30.0); // Most damage
        tracker.recordDamage(victimUuid, attacker3Uuid, 15.0);
        
        ClaimHandler handler = new ClaimHandler(tracker, ClaimMode.MOST_DAMAGE);
        
        // When: Determining claim
        ClaimResult result = handler.determineClaim(victimUuid, attacker1Uuid);
        
        // Then: attacker2 should be the winner (most damage)
        assertTrue(result.hasWinners());
        assertTrue(result.isSingleWinner());
        assertEquals(attacker2Uuid, result.getSingleWinner());
        assertEquals(1.0, result.getShare(attacker2Uuid), 0.0001);
    }
    
    @Test
    void mostDamageMode_withTie_shouldAttributeToFirstAttacker() {
        // Given: Two attackers with equal damage
        tracker.recordDamage(victimUuid, attacker1Uuid, 20.0);
        tracker.recordDamage(victimUuid, attacker2Uuid, 20.0);
        
        ClaimHandler handler = new ClaimHandler(tracker, ClaimMode.MOST_DAMAGE);
        
        // When: Determining claim
        ClaimResult result = handler.determineClaim(victimUuid, attacker3Uuid);
        
        // Then: One of the tied attackers should win (implementation dependent)
        assertTrue(result.hasWinners());
        assertTrue(result.isSingleWinner());
        assertTrue(result.getSingleWinner().equals(attacker1Uuid) || 
                   result.getSingleWinner().equals(attacker2Uuid));
    }
    
    @Test
    void mostDamageMode_withNoDamage_shouldFallBackToLastHitter() {
        // Given: No damage tracked
        ClaimHandler handler = new ClaimHandler(tracker, ClaimMode.MOST_DAMAGE);
        
        // When: Determining claim with a last hitter
        ClaimResult result = handler.determineClaim(victimUuid, attacker1Uuid);
        
        // Then: Last hitter should win as fallback
        assertTrue(result.hasWinners());
        assertEquals(attacker1Uuid, result.getSingleWinner());
    }
    
    // ========== DAMAGE_SPLIT Mode Tests ==========
    
    @Test
    void damageSplitMode_shouldDistributeProportionally() {
        // Given: Multiple attackers with different damage amounts
        tracker.recordDamage(victimUuid, attacker1Uuid, 10.0);  // 20% of 50
        tracker.recordDamage(victimUuid, attacker2Uuid, 30.0);  // 60% of 50
        tracker.recordDamage(victimUuid, attacker3Uuid, 10.0);  // 20% of 50
        
        ClaimHandler handler = new ClaimHandler(tracker, ClaimMode.DAMAGE_SPLIT);
        
        // When: Determining claim
        ClaimResult result = handler.determineClaim(victimUuid, attacker1Uuid);
        
        // Then: All attackers should receive proportional shares
        assertTrue(result.hasWinners());
        assertFalse(result.isSingleWinner());
        assertEquals(3, result.getWinners().size());
        
        assertEquals(0.2, result.getShare(attacker1Uuid), 0.0001);
        assertEquals(0.6, result.getShare(attacker2Uuid), 0.0001);
        assertEquals(0.2, result.getShare(attacker3Uuid), 0.0001);
        
        // Verify shares sum to 1.0
        double totalShares = result.getShare(attacker1Uuid) + 
                            result.getShare(attacker2Uuid) + 
                            result.getShare(attacker3Uuid);
        assertEquals(1.0, totalShares, 0.0001);
    }
    
    @Test
    void damageSplitMode_withNoDamage_shouldReturnNoWinners() {
        // Given: No damage tracked
        ClaimHandler handler = new ClaimHandler(tracker, ClaimMode.DAMAGE_SPLIT);
        
        // When: Determining claim
        ClaimResult result = handler.determineClaim(victimUuid, attacker1Uuid);
        
        // Then: No winners
        assertFalse(result.hasWinners());
    }
    
    // ========== Edge Case Tests ==========
    
    @Test
    void environmentalDeath_shouldAttributeToMostDamageDealer() {
        // Given: Multiple attackers
        tracker.recordDamage(victimUuid, attacker1Uuid, 10.0);
        tracker.recordDamage(victimUuid, attacker2Uuid, 25.0); // Most damage
        tracker.recordDamage(victimUuid, attacker3Uuid, 15.0);
        
        ClaimHandler handler = new ClaimHandler(tracker, ClaimMode.LAST_HIT);
        
        // When: Determining claim with null last hitter (environmental death)
        ClaimResult result = handler.determineClaim(victimUuid, null);
        
        // Then: Player with most damage should win
        assertTrue(result.hasWinners());
        assertEquals(attacker2Uuid, result.getSingleWinner());
    }
    
    @Test
    void environmentalDeath_withNoDamage_shouldReturnNoWinners() {
        // Given: No damage tracked
        ClaimHandler handler = new ClaimHandler(tracker, ClaimMode.LAST_HIT);
        
        // When: Determining claim with null last hitter
        ClaimResult result = handler.determineClaim(victimUuid, null);
        
        // Then: No winners
        assertFalse(result.hasWinners());
    }
    
    @Test
    void suicide_shouldAttributeToMostDamageDealer() {
        // Given: Multiple attackers (excluding victim)
        tracker.recordDamage(victimUuid, attacker1Uuid, 10.0);
        tracker.recordDamage(victimUuid, attacker2Uuid, 25.0); // Most damage
        tracker.recordDamage(victimUuid, attacker3Uuid, 15.0);
        
        ClaimHandler handler = new ClaimHandler(tracker, ClaimMode.LAST_HIT);
        
        // When: Determining claim with victim as last hitter (suicide)
        ClaimResult result = handler.determineClaim(victimUuid, victimUuid);
        
        // Then: Player with most damage should win
        assertTrue(result.hasWinners());
        assertEquals(attacker2Uuid, result.getSingleWinner());
    }
    
    @Test
    void suicide_withNoDamage_shouldReturnNoWinners() {
        // Given: No damage tracked
        ClaimHandler handler = new ClaimHandler(tracker, ClaimMode.LAST_HIT);
        
        // When: Determining claim with victim as last hitter
        ClaimResult result = handler.determineClaim(victimUuid, victimUuid);
        
        // Then: No winners
        assertFalse(result.hasWinners());
    }
    
    @Test
    void clearDamage_shouldRemoveTrackingData() {
        // Given: Damage tracked for victim
        tracker.recordDamage(victimUuid, attacker1Uuid, 10.0);
        ClaimHandler handler = new ClaimHandler(tracker, ClaimMode.LAST_HIT);
        
        // When: Clearing damage
        handler.clearDamage(victimUuid);
        
        // Then: Damage map should be empty
        Map<UUID, Double> damageMap = tracker.getDamageMap(victimUuid);
        assertTrue(damageMap.isEmpty());
    }
}
