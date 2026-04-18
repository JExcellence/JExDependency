package de.jexcellence.jexplatform.reward.exception;

import de.jexcellence.jexplatform.requirement.validation.ValidationResult;
import org.jetbrains.annotations.NotNull;

/**
 * Thrown when reward validation fails.
 *
 * @author JExcellence
 * @since 1.0.0
 */
public class RewardValidationException extends RewardException {

    private final ValidationResult validationResult;

    /** Creates a validation exception. */
    public RewardValidationException(@NotNull ValidationResult validationResult) {
        super("Reward validation failed: " + validationResult.errors());
        this.validationResult = validationResult;
    }

    /** Returns the validation result. */
    public @NotNull ValidationResult getValidationResult() {
        return validationResult;
    }
}
