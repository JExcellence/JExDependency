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

package com.raindropcentral.rplatform.config.icon;

import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests icon configuration defaults and accessors in {@link IconSection}.
 *
 * @author ItsRainingHP
 * @since 2.0.0
 * @version 1.0.0
 */
class IconSectionTest {

    @Test
    void defaultsAreReturnedWhenValuesAreUnset() {
        final IconSection section = new IconSection(new EvaluationEnvironmentBuilder());

        assertEquals("PAPER", section.getMaterial());
        assertEquals("not_defined", section.getDisplayNameKey());
        assertEquals("not_defined", section.getDescriptionKey());
        assertEquals(1, section.getAmount());
        assertEquals(0, section.getCustomModelData());
        assertFalse(section.getEnchanted());
        assertTrue(section.getHideFlags().isEmpty());
    }

    @Test
    void defaultHideFlagsReturnIndependentLists() {
        final IconSection section = new IconSection(new EvaluationEnvironmentBuilder());

        final List<String> first = section.getHideFlags();
        first.add("HIDE_ATTRIBUTES");

        assertTrue(section.getHideFlags().isEmpty());
    }

    @Test
    void settersExposeConfiguredValues() {
        final IconSection section = new IconSection(new EvaluationEnvironmentBuilder());
        final List<String> flags = new ArrayList<>(List.of("HIDE_ENCHANTS", "HIDE_ATTRIBUTES"));

        section.setMaterial("DIAMOND_BLOCK");
        section.setDisplayNameKey("icon.name");
        section.setDescriptionKey("icon.description");
        section.setAmount(4);
        section.setCustomModelData(12);
        section.setEnchanted(true);
        section.setHideFlags(flags);

        assertEquals("DIAMOND_BLOCK", section.getMaterial());
        assertEquals("icon.name", section.getDisplayNameKey());
        assertEquals("icon.description", section.getDescriptionKey());
        assertEquals(4, section.getAmount());
        assertEquals(12, section.getCustomModelData());
        assertTrue(section.getEnchanted());
        assertEquals(flags, section.getHideFlags());
    }
}
