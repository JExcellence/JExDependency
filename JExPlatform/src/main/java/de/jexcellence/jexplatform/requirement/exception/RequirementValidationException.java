package de.jexcellence.jexplatform.requirement.exception;

import de.jexcellence.jexplatform.requirement.validation.ValidationResult;
import org.jetbrains.annotations.NotNull;

/**
 * Thrown when requirement validation fails.
 *
 * @author JExcellence
 * @since 1.0.0
 */
public class RequirementValidationException extends RequirementException {

    private final ValidationResult validationResult;

    /**
     * Creates a validation exception with the failing result.
     *
     * @param validationResult the result containing errors
     */
    public RequirementValidationException(@NotNull ValidationResult validationResult) {
        super("Requirement validation failed: " + validationResult.errors());
        this.validationResult = validationResult;
    }

    /**
     * Returns the validation result that caused this exception.
     *
     * @return the validation result
     */
    public @NotNull ValidationResult getValidationResult() {
        return validationResult;
    }
}
