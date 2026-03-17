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
