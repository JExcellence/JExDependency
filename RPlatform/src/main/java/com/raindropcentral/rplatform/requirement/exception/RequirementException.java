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
