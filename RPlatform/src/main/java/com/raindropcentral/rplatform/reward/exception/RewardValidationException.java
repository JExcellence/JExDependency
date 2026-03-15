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
