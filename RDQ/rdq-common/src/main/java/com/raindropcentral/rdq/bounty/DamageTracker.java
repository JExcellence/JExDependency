/*
 * Copyright (c) 2021-2026 Antimatter Zone LLC. All rights reserved.
 *
 * This source code is proprietary and confidential to Antimatter Zone LLC.
 * Unauthorized copying, modification, distribution, display, performance,
 * publication, sublicensing, or creation of derivative works is prohibited
 * without prior written permission from Antimatter Zone LLC, except to the
 * extent permitted by applicable United States law.
 *
 * This notice is intended to preserve all rights and remedies available under
 * the laws of the State of Washington and the United States of America.
 */

package com.raindropcentral.rdq.bounty;

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

    private final Map<UUID, List<DamageEvent>> damageMap = new ConcurrentHashMap<>();
    private final long trackingWindowMs;

    /**
     * Executes DamageTracker.
     */
    public DamageTracker(long trackingWindowMs) {
        if (trackingWindowMs <= 0) {
            throw new IllegalArgumentException("Tracking window must be positive");
        }
        this.trackingWindowMs = trackingWindowMs;
    }

    /**
     * Executes recordDamage.
     */
    public void recordDamage(@NotNull UUID victimUuid, @NotNull UUID attackerUuid, double damage) {
        if (damage <= 0) {
            return; // Ignore non-positive damage
        }

        DamageEvent event = new DamageEvent(attackerUuid, damage, Instant.now());
        damageMap.computeIfAbsent(victimUuid, k -> new ArrayList<>()).add(event);
    }

    /**
     * Gets damageMap.
     */
    public @NotNull Map<UUID, Double> getDamageMap(@NotNull UUID victimUuid) {
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
     * Gets totalDamage.
     */
    public double getTotalDamage(@NotNull UUID victimUuid) {
        return getDamageMap(victimUuid).values().stream()
                .mapToDouble(Double::doubleValue)
                .sum();
    }

    /**
     * Executes clearDamage.
     */
    public void clearDamage(@NotNull UUID victimUuid) {
        damageMap.remove(victimUuid);
    }

    /**
     * Executes cleanupExpiredRecords.
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
     * Gets trackedVictimCount.
     */
    public int getTrackedVictimCount() {
        return damageMap.size();
    }
}
