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

package com.raindropcentral.rda;

import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Objects;

/**
 * Immutable player-build snapshot used by menus and runtime HUD updates.
 *
 * @param earnedPoints total currently entitled build points
 * @param spentPoints total currently spent build points
 * @param unspentPoints currently available build points
 * @param currentMana current mana
 * @param maxMana maximum mana
 * @param manaRegenPerSecond mana regenerated per second
 * @param manaDisplayMode selected player HUD mode
 * @param statSnapshots stat snapshots keyed by core stat
 */
public record PlayerBuildSnapshot(
    int earnedPoints,
    int spentPoints,
    int unspentPoints,
    double currentMana,
    double maxMana,
    double manaRegenPerSecond,
    @NotNull ManaDisplayMode manaDisplayMode,
    @NotNull Map<CoreStatType, CoreStatSnapshot> statSnapshots
) {

    /**
     * Creates a player-build snapshot.
     */
    public PlayerBuildSnapshot {
        Objects.requireNonNull(manaDisplayMode, "manaDisplayMode");
        Objects.requireNonNull(statSnapshots, "statSnapshots");
    }
}
