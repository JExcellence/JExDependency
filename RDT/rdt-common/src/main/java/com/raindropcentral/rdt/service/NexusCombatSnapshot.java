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

package com.raindropcentral.rdt.service;

/**
 * Immutable runtime snapshot of a town Nexus combat state.
 *
 * @param nexusLevel current persisted nexus level
 * @param currentHealth current persisted Nexus health
 * @param maxHealth maximum Nexus health at the current level
 * @param defense flat Nexus defense at the current level
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public record NexusCombatSnapshot(
    int nexusLevel,
    double currentHealth,
    double maxHealth,
    double defense
) {

    /**
     * Creates one immutable Nexus combat snapshot.
     *
     * @param nexusLevel current persisted nexus level
     * @param currentHealth current persisted Nexus health
     * @param maxHealth maximum Nexus health at the current level
     * @param defense flat Nexus defense at the current level
     */
    public NexusCombatSnapshot {
        nexusLevel = Math.max(1, nexusLevel);
        maxHealth = Math.max(1.0D, maxHealth);
        currentHealth = Math.clamp(currentHealth, 0.0D, maxHealth);
        defense = Math.max(0.0D, defense);
    }

    /**
     * Returns the current Nexus health as a boss-bar progress ratio.
     *
     * @return health progress in the inclusive range {@code [0, 1]}
     */
    public float progress() {
        return (float) Math.clamp(this.currentHealth / this.maxHealth, 0.0D, 1.0D);
    }
}
