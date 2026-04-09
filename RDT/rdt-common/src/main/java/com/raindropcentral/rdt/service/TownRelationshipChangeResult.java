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

import com.raindropcentral.rdt.utils.TownRelationshipState;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Immutable result of one attempted town diplomacy action.
 *
 * @param status outcome of the attempted change
 * @param relationship latest relationship snapshot after the attempted change
 * @param requestedState requested target relationship state
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public record TownRelationshipChangeResult(
    @NotNull TownRelationshipChangeStatus status,
    @NotNull TownRelationshipViewEntry relationship,
    @NotNull TownRelationshipState requestedState
) {

    /**
     * Creates an immutable relationship change result.
     */
    public TownRelationshipChangeResult {
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(relationship, "relationship");
        Objects.requireNonNull(requestedState, "requestedState");
    }
}
