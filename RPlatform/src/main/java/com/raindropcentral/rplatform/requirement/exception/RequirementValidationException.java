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

package com.raindropcentral.rplatform.requirement.exception;

import com.raindropcentral.rplatform.requirement.validation.ValidationResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Exception thrown when requirement validation fails.
 */
public final class RequirementValidationException extends RequirementException {

    private final ValidationResult validationResult;

    /**
     * Executes RequirementValidationException.
     */
    public RequirementValidationException(
        @NotNull String message,
        @Nullable String requirementType,
        @NotNull ValidationResult validationResult
    ) {
        super(message, requirementType);
        this.validationResult = validationResult;
    }

    /**
     * Gets validationResult.
     */
    @NotNull
    public ValidationResult getValidationResult() {
        return validationResult;
    }

    /**
     * Gets message.
     */
    @Override
    public String getMessage() {
        return super.getMessage() + " - " + validationResult.getMessage();
    }
}
