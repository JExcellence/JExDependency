package com.raindropcentral.rplatform.requirement.exception;

import org.jetbrains.annotations.NotNull;

/**
 * Exception thrown when a requirement type is not found in the registry.
 */
public final class RequirementNotFoundException extends RequirementException {

    public RequirementNotFoundException(@NotNull String requirementType) {
        super("Requirement type not found: " + requirementType, requirementType);
    }

    public RequirementNotFoundException(@NotNull String requirementType, @NotNull Throwable cause) {
        super("Requirement type not found: " + requirementType, requirementType, cause);
    }
}
