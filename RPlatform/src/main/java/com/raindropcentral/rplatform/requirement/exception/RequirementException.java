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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Base exception for requirement-related errors.
 */
public class RequirementException extends RuntimeException {

    private final String requirementType;

    /**
     * Executes RequirementException.
     */
    public RequirementException(@NotNull String message) {
        super(message);
        this.requirementType = null;
    }

    /**
     * Executes RequirementException.
     */
    public RequirementException(@NotNull String message, @Nullable String requirementType) {
        super(message);
        this.requirementType = requirementType;
    }

    /**
     * Executes RequirementException.
     */
    public RequirementException(@NotNull String message, @NotNull Throwable cause) {
        super(message, cause);
        this.requirementType = null;
    }

    /**
     * Executes RequirementException.
     */
    public RequirementException(@NotNull String message, @Nullable String requirementType, @NotNull Throwable cause) {
        super(message, cause);
        this.requirementType = requirementType;
    }

    /**
     * Gets requirementType.
     */
    @Nullable
    public String getRequirementType() {
        return requirementType;
    }
}
