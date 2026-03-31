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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests immutable and builder APIs on {@link ValidationResult}.
 *
 * @author ItsRainingHP
 * @since 2.0.0
 * @version 1.0.0
 */
class ValidationResultTest {

    @Test
    void successAndFactoryMethodsProduceExpectedFlags() {
        assertTrue(ValidationResult.success().valid());
        assertFalse(ValidationResult.error("failure").valid());
        assertTrue(ValidationResult.warning("warn").valid());
    }

    @Test
    void withErrorAndWithWarningAccumulateMessages() {
        final ValidationResult result = ValidationResult.success()
            .withWarning("first warning")
            .withError("first error");

        assertFalse(result.valid());
        assertEquals(1, result.errors().size());
        assertEquals(1, result.warnings().size());
        assertEquals("Errors: first error; Warnings: first warning", result.getMessage());
    }

    @Test
    void combineMergesStateErrorsAndWarnings() {
        final ValidationResult left = ValidationResult.error("left error").withWarning("left warning");
        final ValidationResult right = ValidationResult.warning("right warning");

        final ValidationResult combined = left.combine(right);

        assertFalse(combined.valid());
        assertEquals(1, combined.errors().size());
        assertEquals(2, combined.warnings().size());
    }

    @Test
    void builderSupportsConditionalEntryMethods() {
        final ValidationResult result = ValidationResult.builder()
            .addErrorIf(true, "missing field")
            .addErrorIf(false, "should not exist")
            .addWarningIf(true, "deprecated option")
            .build();

        assertFalse(result.valid());
        assertEquals(1, result.errors().size());
        assertEquals(1, result.warnings().size());
    }
}
