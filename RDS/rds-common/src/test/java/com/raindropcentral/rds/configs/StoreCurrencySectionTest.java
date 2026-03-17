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

package com.raindropcentral.rds.configs;

import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests {@link StoreCurrencySection} default and normalization behavior.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
class StoreCurrencySectionTest {

    @Test
    void returnsExpectedDefaults() {
        final StoreCurrencySection section = new StoreCurrencySection(new EvaluationEnvironmentBuilder());

        assertEquals("vault", section.getType());
        assertEquals(1000.0D, section.getInitialCost());
        assertEquals(1.125D, section.getGrowthRate());
    }

    @Test
    void normalizesConfiguredValues() {
        final StoreCurrencySection section = new StoreCurrencySection(new EvaluationEnvironmentBuilder());
        section.setContext("  Coins  ", 250.5D, 1.33D);

        assertEquals("coins", section.getType());
        assertEquals(250.5D, section.getInitialCost());
        assertEquals(1.33D, section.getGrowthRate());
    }

    @Test
    void fallsBackToVaultWhenConfiguredTypeIsBlank() {
        final StoreCurrencySection section = new StoreCurrencySection(new EvaluationEnvironmentBuilder());
        section.setContext("   ", 100.0D, 1.2D);

        assertEquals("vault", section.getType());
    }
}
