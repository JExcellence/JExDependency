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

package com.raindropcentral.rplatform.reward.exception;

import com.raindropcentral.rplatform.requirement.validation.ValidationResult;
import org.jetbrains.annotations.NotNull;

/**
 * Represents the RewardValidationException API type.
 */
public final class RewardValidationException extends RewardException {

    private final ValidationResult validationResult;

    /**
     * Executes RewardValidationException.
     */
    public RewardValidationException(@NotNull ValidationResult validationResult) {
        super("Reward validation failed: " + validationResult.getMessage());
        this.validationResult = validationResult;
    }

    /**
     * Executes RewardValidationException.
     */
    public RewardValidationException(@NotNull String message, @NotNull ValidationResult validationResult) {
        super(message);
        this.validationResult = validationResult;
    }

    /**
     * Gets validationResult.
     */
    public @NotNull ValidationResult getValidationResult() {
        return validationResult;
    }

    /**
     * Gets message.
     */
    @Override
    public String getMessage() {
        return super.getMessage() + "\nErrors: " + validationResult.errors() +
               "\nWarnings: " + validationResult.warnings();
    }
}
