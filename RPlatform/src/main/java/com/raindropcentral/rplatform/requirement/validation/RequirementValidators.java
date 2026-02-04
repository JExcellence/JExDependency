package com.raindropcentral.rplatform.requirement.validation;

import com.raindropcentral.rplatform.requirement.AbstractRequirement;
import com.raindropcentral.rplatform.requirement.async.AsyncRequirement;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Utility class for validating requirements.
 */
public final class RequirementValidators {
    
    private static final Map<Class<? extends AbstractRequirement>, RequirementValidator<?>> validators = new ConcurrentHashMap<>();
    
    private RequirementValidators() {}
    
    /**
     * Registers a validator for a requirement type.
     */
    public static <T extends AbstractRequirement> void registerValidator(
        @NotNull Class<T> requirementClass,
        @NotNull RequirementValidator<T> validator
    ) {
        validators.put(requirementClass, validator);
    }
    
    /**
     * Validates a requirement using its registered validator.
     */
    @NotNull
    public static <T extends AbstractRequirement> ValidationResult validate(@NotNull T requirement) {
        @SuppressWarnings("unchecked")
        RequirementValidator<T> validator = (RequirementValidator<T>) validators.get(requirement.getClass());
        
        if (validator != null) {
            return validator.validate(requirement);
        }
        
        // Default validation
        return validateDefault(requirement);
    }
    
    /**
     * Default validation for all requirements.
     */
    @NotNull
    private static ValidationResult validateDefault(@NotNull AbstractRequirement requirement) {
        var result = ValidationResult.success();
        
        // Check description key
        String descKey = requirement.getDescriptionKey();
        if (descKey.isBlank()) {
            result = result.withError("Description key is null or empty");
        }
        
        // Check type ID
        String typeId = requirement.getTypeId();
        if (typeId.isBlank()) {
            result = result.withError("Requirement type ID is null or empty");
        }
        
        // Async requirement specific checks
        if (requirement instanceof AsyncRequirement) {
            // Could add async-specific validation here
        }
        
        return result;
    }
    
    /**
     * Validates and throws if invalid.
     */
    public static <T extends AbstractRequirement> void validateOrThrow(@NotNull T requirement) {
        ValidationResult result = validate(requirement);
        if (!result.valid()) {
            throw new IllegalStateException("Invalid requirement: " + result.getMessage());
        }
    }
}
