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

import com.raindropcentral.rplatform.testutil.ReflectionTestUtils;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests cooldown resolution and validation behavior in {@link PermissionCooldownSection}.
 *
 * @author ItsRainingHP
 * @since 2.0.0
 * @version 1.0.0
 */
class PermissionCooldownSectionTest {

    @Test
    void resolvesDefaultsWhenNoPermissionOverridesExist() {
        final PermissionCooldownSection section = new PermissionCooldownSection(new EvaluationEnvironmentBuilder());

        assertEquals(0L, section.getDefaultCooldownSeconds());
        assertEquals(0L, section.getEffectiveCooldown(Set.of()));
    }

    @Test
    void resolvesBestCooldownFromMatchingPermissions() {
        final PermissionCooldownSection section = new PermissionCooldownSection(new EvaluationEnvironmentBuilder());
        ReflectionTestUtils.setField(section, "defaultCooldownSeconds", 30L);
        ReflectionTestUtils.setField(
            section,
            "permissionCooldowns",
            Map.of(
                "perm.basic", 20L,
                "perm.vip", 10L
            )
        );

        assertEquals(10L, section.getEffectiveCooldown(Set.of("perm.basic", "perm.vip")));
        assertEquals(20L, section.getCooldownForPermission("perm.basic"));
    }

    @Test
    void supportsLegacyDefaultCooldownField() {
        final PermissionCooldownSection section = new PermissionCooldownSection(new EvaluationEnvironmentBuilder());
        ReflectionTestUtils.setField(section, "defaultCooldown", 45L);

        assertEquals(45L, section.getDefaultCooldownSeconds());
    }

    @Test
    void validateRejectsNegativePermissionCooldowns() {
        final PermissionCooldownSection section = new PermissionCooldownSection(new EvaluationEnvironmentBuilder());
        ReflectionTestUtils.setField(section, "permissionCooldowns", Map.of("perm.invalid", -1L));

        assertThrows(IllegalStateException.class, section::validate);
    }
}
