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

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests validator registration and lookup behavior in {@link ValidationRegistry}.
 *
 * @author ItsRainingHP
 * @since 2.0.0
 * @version 1.0.0
 */
class ValidationRegistryTest {

    @Test
    void registerValidateAndUnregisterValidatorByTypeId() {
        final ValidationRegistry registry = ValidationRegistry.getInstance();
        final String typeId = "TEST_" + UUID.randomUUID();

        try {
            registry.registerValidator(
                typeId.toLowerCase(),
                requirement -> ValidationResult.error("invalid")
            );

            assertTrue(registry.hasValidator(typeId));
            assertNotNull(registry.getValidator(typeId.toLowerCase()));

            final ValidationResult result = registry.validate(new DummyRequirement(typeId));
            assertFalse(result.valid());
        } finally {
            registry.unregisterValidator(typeId);
        }

        assertFalse(registry.hasValidator(typeId));
        assertTrue(registry.validate(new DummyRequirement(typeId)).valid());
    }

    private static final class DummyRequirement extends AbstractRequirement {

        private DummyRequirement(final String typeId) {
            super(typeId);
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
            return "requirement.validation.dummy";
        }
    }
}
