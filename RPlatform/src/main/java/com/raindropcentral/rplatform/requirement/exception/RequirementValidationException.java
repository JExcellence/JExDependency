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
