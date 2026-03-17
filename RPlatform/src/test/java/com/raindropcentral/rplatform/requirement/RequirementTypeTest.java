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

package com.raindropcentral.rplatform.requirement;

import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests factory helpers and null-guard behavior in {@link RequirementType}.
 *
 * @author ItsRainingHP
 * @since 2.0.0
 * @version 1.0.0
 */
class RequirementTypeTest {

    @Test
    void coreFactoryUsesRPlatformPluginId() {
        final RequirementType type = RequirementType.core("TEST", DummyRequirement.class);

        assertEquals("TEST", type.id());
        assertEquals("rplatform", type.pluginId());
        assertEquals("rplatform:TEST", type.getQualifiedName());
    }

    @Test
    void pluginFactoryUsesProvidedPluginId() {
        final RequirementType type = RequirementType.plugin("CUSTOM", "rdq", DummyRequirement.class);

        assertEquals("CUSTOM", type.id());
        assertEquals("rdq", type.pluginId());
        assertEquals("rdq:CUSTOM", type.getQualifiedName());
    }

    @Test
    void constructorRejectsNullValues() {
        assertThrows(
            NullPointerException.class,
            () -> new RequirementType(null, "rdq", DummyRequirement.class)
        );
        assertThrows(
            NullPointerException.class,
            () -> new RequirementType("TEST", null, DummyRequirement.class)
        );
        assertThrows(
            NullPointerException.class,
            () -> new RequirementType("TEST", "rdq", null)
        );
    }

    private static final class DummyRequirement extends AbstractRequirement {

        private DummyRequirement() {
            super("DUMMY");
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
            return "requirement.dummy";
        }
    }
}
