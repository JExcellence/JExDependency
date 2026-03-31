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

package com.raindropcentral.rplatform.requirement.validation;

import com.raindropcentral.rplatform.requirement.AbstractRequirement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Registry for requirement validators.
 *
 * <p>Allows plugins to register custom validators for their requirement types.
 */
public final class ValidationRegistry {

    private static final Logger LOGGER = Logger.getLogger(ValidationRegistry.class.getName());
    private static final ValidationRegistry INSTANCE = new ValidationRegistry();

    private final Map<String, RequirementValidator<?>> validators = new ConcurrentHashMap<>();

    private ValidationRegistry() {}

    /**
     * Gets instance.
     */
    @NotNull
    public static ValidationRegistry getInstance() {
        return INSTANCE;
    }

    /**
     * Register a validator for a requirement type.
     */
    public <T extends AbstractRequirement> void registerValidator(
        @NotNull String typeId,
        @NotNull RequirementValidator<T> validator
    ) {
        validators.put(typeId.toUpperCase(), validator);
    }

    /**
     * Get validator for a requirement type.
     */
    @Nullable
    @SuppressWarnings("unchecked")
    public <T extends AbstractRequirement> RequirementValidator<T> getValidator(@NotNull String typeId) {
        return (RequirementValidator<T>) validators.get(typeId.toUpperCase());
    }

    /**
     * Validate a requirement using its registered validator.
     */
    @NotNull
    public <T extends AbstractRequirement> ValidationResult validate(@NotNull T requirement) {
        RequirementValidator<T> validator = getValidator(requirement.getTypeId());
        if (validator == null) {
            return ValidationResult.success();
        }
        return validator.validate(requirement);
    }

    /**
     * Check if a validator is registered for a type.
     */
    public boolean hasValidator(@NotNull String typeId) {
        return validators.containsKey(typeId.toUpperCase());
    }

    /**
     * Unregister a validator.
     */
    public void unregisterValidator(@NotNull String typeId) {
        validators.remove(typeId.toUpperCase());
    }
}
