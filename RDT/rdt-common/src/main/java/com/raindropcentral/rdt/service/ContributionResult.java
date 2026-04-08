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
 * Immutable result of one partial contribution action.
 *
 * @param status contribution status
 * @param contributedAmount amount successfully contributed
 * @param requirementCompleted whether the acted-on requirement is now complete
 * @param levelReady whether the target level is now ready to finalize
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public record ContributionResult(
    @NotNull ContributionStatus status,
    double contributedAmount,
    boolean requirementCompleted,
    boolean levelReady
) {

    /**
     * Creates an immutable contribution result.
     *
     * @param status contribution status
     * @param contributedAmount amount successfully contributed
     * @param requirementCompleted whether the acted-on requirement is now complete
     * @param levelReady whether the target level is now ready to finalize
     */
    public ContributionResult {
        status = Objects.requireNonNull(status, "status");
        contributedAmount = Math.max(0.0D, contributedAmount);
    }
}
