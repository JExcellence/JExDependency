package com.raindropcentral.rplatform.reward.exception;

import com.raindropcentral.rplatform.requirement.validation.ValidationResult;
import org.jetbrains.annotations.NotNull;

public final class RewardValidationException extends RewardException {

    private final ValidationResult validationResult;

    public RewardValidationException(@NotNull ValidationResult validationResult) {
        super("Reward validation failed: " + validationResult.getMessage());
        this.validationResult = validationResult;
    }

    public RewardValidationException(@NotNull String message, @NotNull ValidationResult validationResult) {
        super(message);
        this.validationResult = validationResult;
    }

    public @NotNull ValidationResult getValidationResult() {
        return validationResult;
    }

    @Override
    public String getMessage() {
        return super.getMessage() + "\nErrors: " + validationResult.errors() +
               "\nWarnings: " + validationResult.warnings();
    }
}
