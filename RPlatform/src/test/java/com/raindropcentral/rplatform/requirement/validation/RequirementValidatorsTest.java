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
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests registration and fallback validation behavior in {@link RequirementValidators}.
 *
 * @author ItsRainingHP
 * @since 2.0.0
 * @version 1.0.0
 */
class RequirementValidatorsTest {

    @Test
    void fallbackValidationFlagsBlankIdentifiers() {
        final ValidationResult result = RequirementValidators.validate(new InvalidRequirement());

        assertFalse(result.valid());
        assertTrue(result.getMessage().contains("Description key is null or empty"));
        assertTrue(result.getMessage().contains("Requirement type ID is null or empty"));
    }

    @Test
    void registeredValidatorOverridesFallbackValidation() {
        RequirementValidators.registerValidator(
            RegisteredRequirement.class,
            requirement -> ValidationResult.warning("custom warning")
        );

        final ValidationResult result = RequirementValidators.validate(new RegisteredRequirement());
        assertTrue(result.valid());
        assertTrue(result.warnings().contains("custom warning"));
    }

    @Test
    void validateOrThrowRaisesWhenRequirementIsInvalid() {
        assertThrows(
            IllegalStateException.class,
            () -> RequirementValidators.validateOrThrow(new InvalidRequirement())
        );
    }

    private static final class InvalidRequirement extends AbstractRequirement {

        private InvalidRequirement() {
            super("");
        }

        @Override
        public boolean isMet(final Player player) {
            return false;
        }

        @Override
        public double calculateProgress(final Player player) {
            return 0.0D;
        }

        @Override
        public void consume(final Player player) {
        }

        @Override
        public String getDescriptionKey() {
            return "";
        }
    }

    private static final class RegisteredRequirement extends AbstractRequirement {

        private RegisteredRequirement() {
            super("REGISTERED");
        }

        @Override
        public boolean isMet(final Player player) {
            return true;
        }

        @Override
        public double calculateProgress(final Player player) {
            return 1.0D;
        }

        @Override
        public void consume(final Player player) {
        }

        @Override
        public String getDescriptionKey() {
            return "requirement.registered";
        }
    }
}
