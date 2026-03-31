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

package com.raindropcentral.rplatform.config.permission;

import com.raindropcentral.rplatform.config.DurationSection;
import com.raindropcentral.rplatform.testutil.ReflectionTestUtils;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests duration override and bounds behavior in {@link PermissionDurationSection}.
 *
 * @author ItsRainingHP
 * @since 2.0.0
 * @version 1.0.0
 */
class PermissionDurationSectionTest {

    @Test
    void resolvesLongestPermissionDurationThenAppliesBounds() {
        final PermissionDurationSection section = new PermissionDurationSection(new EvaluationEnvironmentBuilder());
        final DurationSection defaultDuration = duration("1h");
        final DurationSection basic = duration("30m");
        final DurationSection vip = duration("2h");
        final DurationSection maxDuration = duration("90m");

        ReflectionTestUtils.setField(section, "defaultDuration", defaultDuration);
        ReflectionTestUtils.setField(
            section,
            "permissionDurations",
            Map.of(
                "perm.basic", basic,
                "perm.vip", vip
            )
        );
        ReflectionTestUtils.setField(section, "maxDuration", maxDuration);

        assertEquals(3_600L, section.getDefaultDurationSeconds());
        assertEquals(5_400L, section.getEffectiveDuration(Set.of("perm.basic", "perm.vip")));
        assertEquals("2 hours", section.getFormattedEffectiveDuration(Set.of("perm.vip")));
    }

    @Test
    void validateRejectsInvertedDurationBounds() {
        final PermissionDurationSection section = new PermissionDurationSection(new EvaluationEnvironmentBuilder());
        ReflectionTestUtils.setField(section, "minDuration", duration("2h"));
        ReflectionTestUtils.setField(section, "maxDuration", duration("1h"));

        assertThrows(IllegalStateException.class, section::validate);
    }

    private static DurationSection duration(final String rawDuration) {
        final DurationSection durationSection = new DurationSection(new EvaluationEnvironmentBuilder());
        ReflectionTestUtils.setField(durationSection, "duration", rawDuration);
        return durationSection;
    }
}
