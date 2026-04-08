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

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Immutable result of a final level-up action.
 *
 * @param status level-up status
 * @param previousLevel persisted level before the attempt
 * @param newLevel persisted level after the attempt
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public record LevelUpResult(
    @NotNull LevelUpStatus status,
    int previousLevel,
    int newLevel
) {

    /**
     * Creates an immutable level-up result.
     *
     * @param status level-up status
     * @param previousLevel persisted level before the attempt
     * @param newLevel persisted level after the attempt
     */
    public LevelUpResult {
        status = Objects.requireNonNull(status, "status");
        previousLevel = Math.max(1, previousLevel);
        newLevel = Math.max(1, newLevel);
    }
}
