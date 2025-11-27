package com.raindropcentral.rdq.bounty.tracking;

import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Tracks damage dealt to players within a configured time window.
 * This component is used to determine bounty attribution based on damage dealt.
 * Thread-safe implementation using concurrent data structures.
 */
public class DamageTracker {
    
    /**
     * Represents a single damage event.
     */
    private record DamageEvent(
        @NotNull UUID attackerUuid,
        double damage,
        @NotNull Instant timestamp
    ) {}
    
    /**
     * Map of victim UUID to list of damage events.
     */
    private final Map<UUID, List<DamageEvent>> damageMap = new ConcurrentHashMap<>();
    
    /**
     * Time window in milliseconds for tracking damage.
     */
    private final long trackingWindowMs;
    
    /**
     * Creates a new DamageTracker with the specified tracking window.
     *
     * @param trackingWindowMs the time window in milliseconds
     */
    public DamageTracker(long trackingWindowMs) {
        if (trackingWindowMs <= 0) {
            throw new IllegalArgumentException("Tracking window must be positive");
        }
        this.trackingWindowMs = trackingWindowMs;
    }
    
    /**
     * Records damage dealt to a victim.
     *
     * @param victimUuid the UUID of the victim
     * @param attackerUuid the UUID of the attacker
     * @param damage the amount of damage dealt
     */
    public void recordDamage(@NotNull UUID victimUuid, @NotNull UUID attackerUuid, double damage) {
        Objects.requireNonNull(victimUuid, "victimUuid cannot be null");
        Objects.requireNonNull(attackerUuid, "attackerUuid cannot be null");
        
        if (damage <= 0) {
            return; // Ignore non-positive damage
        }
        
        DamageEvent event = new DamageEvent(attackerUuid, damage, Instant.now());
        damageMap.computeIfAbsent(victimUuid, k -> new ArrayList<>()).add(event);
    }
    
    /**
     * Gets the total damage dealt by each attacker to a victim within the tracking window.
     *
     * @param victimUuid the UUID of the victim
     * @return a map of attacker UUID to total damage dealt
     */
    @NotNull
    public Map<UUID, Double> getDamageMap(@NotNull UUID victimUuid) {
        Objects.requireNonNull(victimUuid, "victimUuid cannot be null");
        
        List<DamageEvent> events = damageMap.get(victimUuid);
        if (events == null || events.isEmpty()) {
            return Collections.emptyMap();
        }
        
        Instant cutoff = Instant.now().minusMillis(trackingWindowMs);
        
        return events.stream()
            .filter(event -> event.timestamp().isAfter(cutoff))
            .collect(Collectors.groupingBy(
                DamageEvent::attackerUuid,
                Collectors.summingDouble(DamageEvent::damage)
            ));
    }
    
    /**
     * Gets the total damage dealt to a victim within the tracking window.
     *
     * @param victimUuid the UUID of the victim
     * @return the total damage dealt
     */
    public double getTotalDamage(@NotNull UUID victimUuid) {
        return getDamageMap(victimUuid).values().stream()
            .mapToDouble(Double::doubleValue)
            .sum();
    }
    
    /**
     * Clears all damage records for a victim.
     * This should be called when a bounty is claimed or when the victim respawns.
     *
     * @param victimUuid the UUID of the victim
     */
    public void clearDamage(@NotNull UUID victimUuid) {
        Objects.requireNonNull(victimUuid, "victimUuid cannot be null");
        damageMap.remove(victimUuid);
    }
    
    /**
     * Cleans up expired damage records for all victims.
     * This should be called periodically to prevent memory leaks.
     */
    public void cleanupExpiredRecords() {
        Instant cutoff = Instant.now().minusMillis(trackingWindowMs);
        
        damageMap.entrySet().removeIf(entry -> {
            List<DamageEvent> events = entry.getValue();
            events.removeIf(event -> event.timestamp().isBefore(cutoff));
            return events.isEmpty();
        });
    }
    
    /**
     * Gets the number of victims currently being tracked.
     *
     * @return the number of tracked victims
     */
    public int getTrackedVictimCount() {
        return damageMap.size();
    }
}
