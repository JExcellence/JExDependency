package de.jexcellence.jexplatform.requirement.validation;

import de.jexcellence.jexplatform.requirement.AbstractRequirement;
import org.jetbrains.annotations.NotNull;

/**
 * Functional interface for validating requirement configurations.
 *
 * @author JExcellence
 * @since 1.0.0
 */
@FunctionalInterface
public interface RequirementValidator {

    /**
     * Validates the given requirement.
     *
     * @param requirement the requirement to validate
     * @return the validation result
     */
    @NotNull ValidationResult validate(@NotNull AbstractRequirement requirement);
}
