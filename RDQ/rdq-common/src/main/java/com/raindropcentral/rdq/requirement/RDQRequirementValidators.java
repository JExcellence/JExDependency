/*
 * Copyright (c) 2021-2026 Antimatter Zone LLC. All rights reserved.
 *
 * This source code is proprietary and confidential to Antimatter Zone LLC.
 * Unauthorized copying, modification, distribution, display, performance,
 * publication, sublicensing, or creation of derivative works is prohibited
 * without prior written permission from Antimatter Zone LLC, except to the
 * extent permitted by applicable United States law.
 *
 * This notice is intended to preserve all rights and remedies available under
 * the laws of the State of Washington and the United States of America.
 */

package com.raindropcentral.rdq.requirement;

import com.raindropcentral.rplatform.requirement.impl.ChoiceRequirement;
import com.raindropcentral.rplatform.requirement.impl.CompositeRequirement;
import com.raindropcentral.rplatform.requirement.impl.ExperienceLevelRequirement;
import com.raindropcentral.rplatform.requirement.impl.ItemRequirement;
import com.raindropcentral.rplatform.requirement.impl.PermissionRequirement;
import com.raindropcentral.rplatform.requirement.validation.RequirementValidator;
import com.raindropcentral.rplatform.requirement.validation.ValidationRegistry;
import com.raindropcentral.rplatform.requirement.validation.ValidationResult;
import org.jetbrains.annotations.NotNull;

/**
 * Validators for RDQ-specific requirement validation.
 *
 * <p>Provides comprehensive validation for all requirement types used in RDQ.
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
        
        if (requirement instanceof com.raindropcentral.rplatform.requirement.AbstractRequirement abstractReq) {
            ValidationResult result = registry.validate(abstractReq);
            if (!result.valid()) {
                throw new IllegalArgumentException("Invalid " + typeId + " requirement: " + result.getMessage());
            }
        }
    }
}
