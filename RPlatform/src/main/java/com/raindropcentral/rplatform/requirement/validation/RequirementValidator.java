package com.raindropcentral.rplatform.requirement.validation;

import com.raindropcentral.rplatform.requirement.AbstractRequirement;
import org.jetbrains.annotations.NotNull;

/**
 * Validator for requirement configurations.
 *
 * <p>Validates requirements before they are used to catch configuration errors early.
 *
 * @param <T> the requirement type to validate
 */
@FunctionalInterface
public interface RequirementValidator<T extends AbstractRequirement> {

    /**
     * Validate a requirement.
     *
     * @param requirement the requirement to validate
     * @return validation result
     */
    @NotNull
    ValidationResult validate(@NotNull T requirement);
}
