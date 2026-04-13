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

import java.util.Objects;

/**
 * Immutable stat snapshot tailored for menu rendering and passive lookups.
 *
 * @param statType owning core stat
 * @param allocatedPoints currently spent points in the stat
 * @param passiveValue resolved passive bonus value
 * @param passiveLabel display label for the passive bonus
 * @param passiveUnit unit suffix displayed beside the passive value
 * @param loreDescription short identity summary shown in menus
 */
public record CoreStatSnapshot(
    @NotNull CoreStatType statType,
    int allocatedPoints,
    double passiveValue,
    @NotNull String passiveLabel,
    @NotNull String passiveUnit,
    @NotNull String loreDescription
) {

    /**
     * Creates a stat snapshot.
     */
    public CoreStatSnapshot {
        Objects.requireNonNull(statType, "statType");
        Objects.requireNonNull(passiveLabel, "passiveLabel");
        Objects.requireNonNull(passiveUnit, "passiveUnit");
        Objects.requireNonNull(loreDescription, "loreDescription");
    }
}
