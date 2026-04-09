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

package com.raindropcentral.rdt.configs;

/**
 * Immutable combat-stat snapshot for one Nexus level.
 *
 * @param level nexus level these stats belong to
 * @param maxHealth maximum health available at this level
 * @param defense flat defense value available at this level
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public record NexusCombatStats(
    int level,
    double maxHealth,
    double defense
) {

    private static final double DEFAULT_BASE_HEALTH = 1_000.0D;
    private static final double DEFAULT_HEALTH_PER_LEVEL = 250.0D;
    private static final double DEFAULT_BASE_DEFENSE = 0.0D;
    private static final double DEFAULT_DEFENSE_PER_LEVEL = 2.0D;

    /**
     * Creates one immutable combat-stat snapshot.
     *
     * @param level nexus level these stats belong to
     * @param maxHealth maximum health available at this level
     * @param defense flat defense value available at this level
     */
    public NexusCombatStats {
        level = Math.max(1, level);
        maxHealth = Math.max(1.0D, maxHealth);
        defense = Math.max(0.0D, defense);
    }

    /**
     * Returns the default combat stats for one Nexus level.
     *
     * @param level nexus level to resolve
     * @return default combat stats for the supplied level
     */
    public static NexusCombatStats createDefault(final int level) {
        final int normalizedLevel = Math.max(1, level);
        return new NexusCombatStats(
            normalizedLevel,
            DEFAULT_BASE_HEALTH + (normalizedLevel - 1) * DEFAULT_HEALTH_PER_LEVEL,
            DEFAULT_BASE_DEFENSE + (normalizedLevel - 1) * DEFAULT_DEFENSE_PER_LEVEL
        );
    }
}
