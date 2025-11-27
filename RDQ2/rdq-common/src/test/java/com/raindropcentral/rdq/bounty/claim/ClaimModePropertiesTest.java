package com.raindropcentral.rdq.bounty.claim;

import com.raindropcentral.rdq.bounty.tracking.DamageTracker;
import com.raindropcentral.rdq.bounty.type.ClaimMode;
import net.jqwik.api.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for claim mode logic.
 * These tests verify that bounty attribution works correctly across all claim modes.
 */
class ClaimModePropertiesTest {
    
    private static final long TRACKING_WINDOW_MS = 30000; // 30 seconds
    
    /**
     * **Feature: bounty-system-rebuild, Property 40: Last hit attribution**
     * 
     * For any kill with LAST_HIT mode, the bounty should be attributed to the player 
     * who dealt the final blow.
     * 
     * **Validates: Requirements 10.2**
     */
    @Property(tries = 100)
    @Label("Property 40: Last hit attribution")
    void lastHitAttributionProperty(
        @ForAll("victimUuid") UUID victimUuid,
        @ForAll("lastHitterUuid") UUID lastHitterUuid,
        @ForAll("damageEvents") List<DamageEvent> damageEvents
    ) {
        // Given: A damage tracker with LAST_HIT claim mode
        DamageTracker tracker = new DamageTracker(TRACKING_WINDOW_MS);
        ClaimHandler handler = new ClaimHandler(tracker, ClaimMode.LAST_HIT);
        
        // Record all damage events
        for (DamageEvent event : damageEvents) {
            tracker.recordDamage(victimUuid, event.attackerUuid(), event.damage());
        }
        
        // When: Determining the claim with a last hitter
        ClaimResult result = handler.determineClaim(victimUuid, lastHitterUuid);
        
        // Then: The last hitter should be the sole winner with 100% share
        assertTrue(result.hasWinners(), "Result should have winners");
        assertTrue(result.isSingleWinner(), "Result should have a single winner");
        assertEquals(lastHitterUuid, result.getSingleWinner(), "Last hitter should be the winner");
        assertEquals(1.0, result.getShare(lastHitterUuid), 0.0001, "Winner should have 100% share");
    }
    
    /**
     * **Feature: bounty-system-rebuild, Property 41: Most damage attribution**
     * 
     * For any kill with MOST_DAMAGE mode, the bounty should be attributed to the player 
     * who dealt the most damage within the tracking window.
     * 
     * **Validates: Requirements 10.3**
     */
    @Property(tries = 100)
    @Label("Property 41: Most damage attribution")
    void mostDamageAttributionProperty(
        @ForAll("victimUuid") UUID victimUuid,
        @ForAll("lastHitterUuid") UUID lastHitterUuid,
        @ForAll("damageEventsWithClearWinner") DamageEventsWithWinner eventsWithWinner
    ) {
        // Given: A damage tracker with MOST_DAMAGE claim mode
        DamageTracker tracker = new DamageTracker(TRACKING_WINDOW_MS);
        ClaimHandler handler = new ClaimHandler(tracker, ClaimMode.MOST_DAMAGE);
        
        // Record all damage events
        for (DamageEvent event : eventsWithWinner.events()) {
            tracker.recordDamage(victimUuid, event.attackerUuid(), event.damage());
        }
        
        // When: Determining the claim
        ClaimResult result = handler.determineClaim(victimUuid, lastHitterUuid);
        
        // Then: The player with the most damage should be the sole winner
        assertTrue(result.hasWinners(), "Result should have winners");
        assertTrue(result.isSingleWinner(), "Result should have a single winner");
        assertEquals(eventsWithWinner.expectedWinner(), result.getSingleWinner(), 
            "Player with most damage should be the winner");
        assertEquals(1.0, result.getShare(eventsWithWinner.expectedWinner()), 0.0001, 
            "Winner should have 100% share");
    }
    
    /**
     * **Feature: bounty-system-rebuild, Property 42: Damage split distribution**
     * 
     * For any kill with DAMAGE_SPLIT mode, rewards should be distributed proportionally 
     * among all damage dealers.
     * 
     * **Validates: Requirements 10.4**
     */
    @Property(tries = 100)
    @Label("Property 42: Damage split distribution")
    void damageSplitDistributionProperty(
        @ForAll("victimUuid") UUID victimUuid,
        @ForAll("lastHitterUuid") UUID lastHitterUuid,
        @ForAll("damageEvents") List<DamageEvent> damageEvents
    ) {
        Assume.that(!damageEvents.isEmpty());
        
        // Given: A damage tracker with DAMAGE_SPLIT claim mode
        DamageTracker tracker = new DamageTracker(TRACKING_WINDOW_MS);
        ClaimHandler handler = new ClaimHandler(tracker, ClaimMode.DAMAGE_SPLIT);
        
        // Record all damage events and calculate expected shares
        Map<UUID, Double> totalDamageByPlayer = new HashMap<>();
        for (DamageEvent event : damageEvents) {
            tracker.recordDamage(victimUuid, event.attackerUuid(), event.damage());
            totalDamageByPlayer.merge(event.attackerUuid(), event.damage(), Double::sum);
        }
        
        double totalDamage = totalDamageByPlayer.values().stream()
            .mapToDouble(Double::doubleValue)
            .sum();
        
        // When: Determining the claim
        ClaimResult result = handler.determineClaim(victimUuid, lastHitterUuid);
        
        // Then: All damage dealers should receive proportional shares
        assertTrue(result.hasWinners(), "Result should have winners");
        assertEquals(totalDamageByPlayer.keySet(), result.getWinners(), 
            "All damage dealers should be winners");
        
        // Verify each player's share is proportional to their damage
        for (Map.Entry<UUID, Double> entry : totalDamageByPlayer.entrySet()) {
            UUID player = entry.getKey();
            double expectedShare = entry.getValue() / totalDamage;
            double actualShare = result.getShare(player);
            
            // Allow small floating point error
            assertEquals(expectedShare, actualShare, 0.0001, 
                "Player's share should be proportional to damage dealt");
        }
        
        // Verify all shares sum to 1.0
        double totalShares = result.shares().values().stream()
            .mapToDouble(Double::doubleValue)
            .sum();
        assertEquals(1.0, totalShares, 0.0001, "All shares should sum to 1.0");
    }
    
    /**
     * Property: Environmental death handling
     * 
     * For any environmental death (null last hitter), the bounty should be awarded 
     * to the player who dealt the most damage, if any.
     */
    @Property(tries = 100)
    @Label("Environmental death awards to most damage dealer")
    void environmentalDeathProperty(
        @ForAll("victimUuid") UUID victimUuid,
        @ForAll("damageEvents") List<DamageEvent> damageEvents
    ) {
        Assume.that(!damageEvents.isEmpty());
        
        // Given: A damage tracker with any claim mode
        DamageTracker tracker = new DamageTracker(TRACKING_WINDOW_MS);
        ClaimHandler handler = new ClaimHandler(tracker, ClaimMode.LAST_HIT);
        
        // Record all damage events
        Map<UUID, Double> totalDamageByPlayer = new HashMap<>();
        for (DamageEvent event : damageEvents) {
            tracker.recordDamage(victimUuid, event.attackerUuid(), event.damage());
            totalDamageByPlayer.merge(event.attackerUuid(), event.damage(), Double::sum);
        }
        
        UUID expectedWinner = totalDamageByPlayer.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(null);
        
        // When: Determining the claim with null last hitter (environmental death)
        ClaimResult result = handler.determineClaim(victimUuid, null);
        
        // Then: The player with most damage should win
        assertTrue(result.hasWinners(), "Result should have winners");
        assertTrue(result.isSingleWinner(), "Result should have a single winner");
        assertEquals(expectedWinner, result.getSingleWinner(), 
            "Player with most damage should win on environmental death");
    }
    
    /**
     * Property: Suicide handling
     * 
     * For any suicide (victim is last hitter), the bounty should be awarded 
     * to the player who dealt the most damage, if any.
     */
    @Property(tries = 100)
    @Label("Suicide awards to most damage dealer")
    void suicideProperty(
        @ForAll("victimUuid") UUID victimUuid,
        @ForAll("damageEvents") List<DamageEvent> damageEvents
    ) {
        Assume.that(!damageEvents.isEmpty());
        
        // Given: A damage tracker with any claim mode
        DamageTracker tracker = new DamageTracker(TRACKING_WINDOW_MS);
        ClaimHandler handler = new ClaimHandler(tracker, ClaimMode.LAST_HIT);
        
        // Record all damage events (excluding victim as attacker)
        Map<UUID, Double> totalDamageByPlayer = new HashMap<>();
        for (DamageEvent event : damageEvents) {
            if (!event.attackerUuid().equals(victimUuid)) {
                tracker.recordDamage(victimUuid, event.attackerUuid(), event.damage());
                totalDamageByPlayer.merge(event.attackerUuid(), event.damage(), Double::sum);
            }
        }
        
        Assume.that(!totalDamageByPlayer.isEmpty());
        
        UUID expectedWinner = totalDamageByPlayer.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(null);
        
        // When: Determining the claim with victim as last hitter (suicide)
        ClaimResult result = handler.determineClaim(victimUuid, victimUuid);
        
        // Then: The player with most damage should win
        assertTrue(result.hasWinners(), "Result should have winners");
        assertTrue(result.isSingleWinner(), "Result should have a single winner");
        assertEquals(expectedWinner, result.getSingleWinner(), 
            "Player with most damage should win on suicide");
    }
    
    // ========== Arbitraries ==========
    
    @Provide
    Arbitrary<UUID> victimUuid() {
        return Arbitraries.randomValue(random -> UUID.randomUUID());
    }
    
    @Provide
    Arbitrary<UUID> lastHitterUuid() {
        return Arbitraries.randomValue(random -> UUID.randomUUID());
    }
    
    @Provide
    Arbitrary<UUID> attackerUuid() {
        return Arbitraries.randomValue(random -> UUID.randomUUID());
    }
    
    @Provide
    Arbitrary<DamageEvent> damageEvent() {
        return Combinators.combine(
            attackerUuid(),
            Arbitraries.doubles().between(0.1, 20.0)
        ).as(DamageEvent::new);
    }
    
    @Provide
    Arbitrary<List<DamageEvent>> damageEvents() {
        return damageEvent().list().ofMinSize(1).ofMaxSize(10);
    }
    
    @Provide
    Arbitrary<DamageEventsWithWinner> damageEventsWithClearWinner() {
        return Combinators.combine(
            attackerUuid().list().ofMinSize(2).ofMaxSize(5).uniqueElements(),
            Arbitraries.integers().between(0, 4)
        ).as((attackers, winnerIndex) -> {
            List<DamageEvent> events = new ArrayList<>();
            UUID winner = attackers.get(winnerIndex);
            
            // Winner deals significantly more damage
            events.add(new DamageEvent(winner, 100.0));
            
            // Others deal less damage
            for (int i = 0; i < attackers.size(); i++) {
                if (i != winnerIndex) {
                    events.add(new DamageEvent(attackers.get(i), 10.0));
                }
            }
            
            return new DamageEventsWithWinner(events, winner);
        });
    }
    
    // ========== Helper Records ==========
    
    record DamageEvent(UUID attackerUuid, double damage) {}
    
    record DamageEventsWithWinner(List<DamageEvent> events, UUID expectedWinner) {}
}
