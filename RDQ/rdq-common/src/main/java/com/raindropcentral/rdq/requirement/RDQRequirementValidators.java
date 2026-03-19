package com.raindropcentral.rdq.requirement;

import com.raindropcentral.rplatform.requirement.AbstractRequirement;
import com.raindropcentral.rplatform.requirement.impl.*;
import com.raindropcentral.rplatform.requirement.validation.RequirementValidator;
import com.raindropcentral.rplatform.requirement.validation.ValidationRegistry;
import com.raindropcentral.rplatform.requirement.validation.ValidationResult;
import org.jetbrains.annotations.NotNull;

/**
 * Validators for RDQ-specific requirement validation.
 * <p>
 * Provides comprehensive validation for all requirement types used in RDQ.
 * </p>
 */
public final class RDQRequirementValidators {

    private RDQRequirementValidators() {}

    /**
     * Registers all RDQ requirement validators.
     */
    public static void registerAll() {
        var registry = ValidationRegistry.getInstance();

        // Item requirement validator
        registry.registerValidator("ITEM", (RequirementValidator<ItemRequirement>) req ->
            ValidationResult.builder()
                .addErrorIf(req.getRequiredItems().isEmpty(), "Item requirement must have at least one item")
                .addWarningIf(req.getRequiredItems().size() > 10, "Large number of required items may impact performance")
                .build()
        );

        // Experience level requirement validator
        registry.registerValidator("EXPERIENCE_LEVEL", (RequirementValidator<ExperienceLevelRequirement>) req ->
            ValidationResult.builder()
                .addErrorIf(req.getRequiredLevel() < 0, "Experience level cannot be negative")
                .addWarningIf(req.getRequiredLevel() > 100, "Very high experience levels may be difficult to achieve")
                .addWarningIf(req.getRequiredLevel() == 0, "Experience level 0 is always met")
                .build()
        );

        // Permission requirement validator
        registry.registerValidator("PERMISSION", (RequirementValidator<PermissionRequirement>) req ->
            ValidationResult.builder()
                .addErrorIf(req.getRequiredPermissions().isEmpty(), "Permission requirement must specify at least one permission")
                .addErrorIf(req.getRequiredPermissions().stream().anyMatch(perm -> perm == null || perm.isBlank()), 
                           "Permission names cannot be null or empty")
                .addWarningIf(req.getRequiredPermissions().size() > 5, "Many permissions may make requirements complex")
                .build()
        );

        // Composite requirement validator
        registry.registerValidator("COMPOSITE", (RequirementValidator<CompositeRequirement>) req ->
            ValidationResult.builder()
                .addErrorIf(req.getRequirements().isEmpty(), "Composite requirement must contain at least one sub-requirement")
                .addWarningIf(req.getRequirements().size() > 10, "Many sub-requirements may impact performance")
                .build()
        );

        // Choice requirement validator
        registry.registerValidator("CHOICE", (RequirementValidator<ChoiceRequirement>) req ->
            ValidationResult.builder()
                .addErrorIf(req.getChoices().isEmpty(), "Choice requirement must have at least one choice")
                .addWarningIf(req.getChoices().size() == 1, "Single choice makes this equivalent to a regular requirement")
                .build()
        );
    }

    /**
     * Validates a requirement and throws if invalid.
     */
    public static void validateOrThrow(@NotNull String typeId, @NotNull Object requirement) {
        var registry = ValidationRegistry.getInstance();
        
        if (requirement instanceof AbstractRequirement abstractReq) {
            ValidationResult result = registry.validate(abstractReq);
            if (!result.valid()) {
                throw new IllegalArgumentException("Invalid " + typeId + " requirement: " + result.getMessage());
            }
        }
    }
}
