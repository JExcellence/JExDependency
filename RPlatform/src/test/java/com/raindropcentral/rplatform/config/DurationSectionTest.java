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

package com.raindropcentral.rplatform.config;

import com.raindropcentral.rplatform.testutil.ReflectionTestUtils;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests duration parsing and validation behavior in {@link DurationSection}.
 *
 * @author ItsRainingHP
 * @since 2.0.0
 * @version 1.0.0
 */
class DurationSectionTest {

    @Test
    void defaultsToZeroDurationWhenUnset() {
        final DurationSection section = new DurationSection(new EvaluationEnvironmentBuilder());

        assertEquals(0L, section.getSeconds());
        assertEquals(0L, section.getMilliseconds());
        assertFalse(section.hasDuration());
        assertEquals("0 seconds", section.getFormattedDuration());
    }

    @Test
    void parsesCompoundDurationStringIntoSeconds() {
        final DurationSection section = new DurationSection(new EvaluationEnvironmentBuilder());
        ReflectionTestUtils.setField(section, "duration", "1d 2h 3m 4s");

        assertEquals(93_784L, section.getSeconds());
        assertEquals(93_784_000L, section.getMilliseconds());
        assertEquals("1 day 2 hours 3 minutes 4 seconds", section.getFormattedDuration());
        assertTrue(section.hasDuration());
    }

    @Test
    void parsesSimpleUnitNotationAndNumericSeconds() {
        final DurationSection simple = new DurationSection(new EvaluationEnvironmentBuilder());
        ReflectionTestUtils.setField(simple, "time", "90m");
        assertEquals(5_400L, simple.getSeconds());

        final DurationSection numeric = new DurationSection(new EvaluationEnvironmentBuilder());
        ReflectionTestUtils.setField(numeric, "duration", "120");
        assertEquals(120L, numeric.getSeconds());
    }

    @Test
    void fallsBackToStructuredValuesWhenRawDurationIsInvalid() {
        final DurationSection section = new DurationSection(new EvaluationEnvironmentBuilder());
        ReflectionTestUtils.setField(section, "duration", "invalid format");
        ReflectionTestUtils.setField(section, "minutes", 2L);

        assertEquals(120L, section.getSeconds());
        assertThrows(IllegalStateException.class, section::validate);
    }

    @Test
    void validatesNegativeStructuredDurationsAsInvalid() {
        final DurationSection section = new DurationSection(new EvaluationEnvironmentBuilder());
        ReflectionTestUtils.setField(section, "seconds", -1L);

        assertThrows(IllegalStateException.class, section::validate);
    }
}
