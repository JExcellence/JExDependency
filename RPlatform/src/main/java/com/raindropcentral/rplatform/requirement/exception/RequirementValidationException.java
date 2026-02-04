package com.raindropcentral.rplatform.requirement.exception;

import com.raindropcentral.rplatform.requirement.validation.ValidationResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Exception thrown when requirement validation fails.
 */
public final class RequirementValidationException extends RequirementException {

    private final ValidationResult validationResult;

    public RequirementValidationException(
        @NotNull String message,
        @Nullable String requirementType,
        @NotNull ValidationResult validationResult
    ) {
        super(message, requirementType);
        this.validationResult = validationResult;
    }

    @NotNull
    public ValidationResult getValidationResult() {
        return validationResult;
    }

    @Override
    public String getMessage() {
        return super.getMessage() + " - " + validationResult.getMessage();
    }
}
